package com.krscripts.app

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.View
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krscripts.app.databinding.ActivityActionPageOnlineBinding
import com.krscripts.app.util.chooseFilePath
import com.krscripts.app.util.handleFileSelectorResult
import com.krscripts.common.shared.FilePathResolver
import com.krscripts.common.ui.DialogHelper
import com.krscripts.common.ui.ProgressBarDialog
import com.krscripts.core.R
import com.krscripts.core.WebViewInjector
import com.krscripts.core.downloader.Downloader
import com.krscripts.core.ui.ParamsFileChooserRender
import com.krscripts.core.util.PermissionUtil.checkManageFile
import com.krscripts.core.util.PermissionUtil.showManageFileDialog
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

class ActionPageOnline : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)

    private lateinit var binding: ActivityActionPageOnlineBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityActionPageOnlineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        setTitle(com.krscripts.app.R.string.app_name)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        loadIntentData()
    }

    private fun loadIntentData() {
        intent.extras?.let { extras ->
            if (extras.containsKey("title")) {
                title = extras.getString("title")!!
            }

            when {
                extras.containsKey("config") -> initWebview(extras.getString("config"))
                extras.containsKey("url") -> initWebview(extras.getString("url"))
                extras.containsKey("downloadUrl") -> initDownload(
                    extras.getString("downloadUrl")!!,
                    extras.getString("taskId"),
                    extras.getBoolean("autoClose")
                )
            }
        }
    }

    private fun initDownload(url: String, taskId: String?, autoClose: Boolean) {
        val downloader = Downloader(this)

        val taskAliasId = taskId ?: UUID.randomUUID().toString()

        if (!checkManageFile(this)) {
            downloader.saveTaskStatus(taskAliasId, 0)
            showManageFileDialog(this)
        } else {
            val downloadId = downloader.download(url, null, null, taskAliasId)
            if (downloadId != null) {
                binding.krDownloadUrl.text = url
                downloader.saveTaskStatus(taskAliasId, 0)
                watchDownloadProgress(downloadId, autoClose, taskAliasId)
            } else {
                downloader.saveTaskStatus(taskAliasId, -1)
            }
        }
    }

    private fun initWebview(url: String?) {
        binding.krOnlineWebview.visibility = View.VISIBLE
        binding.krOnlineWebview.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                DialogHelper.animDialog(
                    this@ActionPageOnline,
                    MaterialAlertDialogBuilder(this@ActionPageOnline)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.btn_confirm) { _, _ -> }
                        .setOnDismissListener {
                            result?.confirm()
                        }
                )
                return true // super.onJsAlert(view, url, message, result)
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                DialogHelper.animDialog(
                    this@ActionPageOnline,
                    MaterialAlertDialogBuilder(this@ActionPageOnline)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.btn_confirm) { _, _ ->
                            result?.confirm()
                        }
                        .setNeutralButton(R.string.btn_cancel) { _, _ ->
                            result?.cancel()
                        }
                )
                return true // super.onJsConfirm(view, url, message, result)
            }
        }

        binding.krOnlineWebview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBarDialog.hideDialog()
                view?.run {
                    setTitle(this.title)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBarDialog.showDialog(getString(com.krscripts.app.R.string.please_wait))
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                try {
                    val requestUrl = request?.url
                    if (requestUrl != null && requestUrl.scheme?.startsWith("http") != true) {
                        val intent = Intent(Intent.ACTION_VIEW, requestUrl.toString().toUri())
                        startActivity(intent)
                        return true
                    } else {
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                } catch (_: Exception) {
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
        }

        binding.krOnlineWebview.loadUrl(url!!)

        WebViewInjector(binding.krOnlineWebview,
                object : ParamsFileChooserRender.FileChooserInterface {
                    override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                        fileSelectorInterface = fileSelectedInterface
                        return chooseFilePath(fileSelectedInterface)
                    }
                }).inject(this, url.startsWith("file:///android_asset"))
    }

    private var fileSelectorInterface: ParamsFileChooserRender.FileSelectedInterface? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleFileSelectorResult(this, resultCode, requestCode, data, fileSelectorInterface)
        fileSelectorInterface = null
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.krOnlineWebview.canGoBack()) {
            binding.krOnlineWebview.goBack()
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        stopWatchDownloadProgress()
        super.onDestroy()
    }

    private fun stopWatchDownloadProgress() {
        if (progressPolling != null) {
            progressPolling?.cancel()
            progressPolling = null
        }
    }

    var progressPolling: Timer? = null
    /**
     * 监视下载进度
     */
    private fun watchDownloadProgress(downloadId: Long, autoClose: Boolean, taskAliasId: String) {
        binding.krDownloadState.visibility = View.VISIBLE

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)

        binding.krDownloadNameCopy.setOnClickListener {
            val myClipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val myClip = ClipData.newPlainText("text", binding.krDownloadName.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(this@ActionPageOnline, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }
        binding.krDownloadUrlCopy.setOnClickListener {
            val myClipboard: ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val myClip = ClipData.newPlainText("text", binding.krDownloadUrl.text.toString())
            myClipboard.setPrimaryClip(myClip)
            Toast.makeText(this@ActionPageOnline, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
        }

        val handler = Handler()
        val downloader = Downloader(this)
        progressPolling = Timer()
        progressPolling?.schedule(object : TimerTask() {
            override fun run() {
                val cursor = downloadManager.query(query)
                var fileName = ""
                var absPath: String? = null
                if (cursor.moveToFirst()) {
                    val downloadBytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesIdx = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val totalBytes = cursor.getLong(totalBytesIdx)
                    val downloadBytes = cursor.getLong(downloadBytesIdx)
                    val ratio = (downloadBytes * 100 / totalBytes).toInt()
                    if (fileName.isEmpty()) {
                        try {
                            val nameColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                            fileName = cursor.getString(nameColumn)
                            absPath = FilePathResolver().getPath(this@ActionPageOnline,
                                fileName.toUri())
                            if (!absPath.isNullOrEmpty()) {
                                fileName = absPath
                            }
                        } catch (_: java.lang.Exception) {
                        }
                    }

                    handler.post {
                        binding.krDownloadName.text = fileName
                        binding.krDownloadProgress.progress = ratio
                        binding.krDownloadProgress.isIndeterminate = false
                        setTitle(R.string.kr_download_downloading)
                        downloader.saveTaskStatus(taskAliasId, ratio)
                    }

                    absPath?.let { path ->
                        if (ratio >= 100) {
                            // 保存下载成功后的路径
                            downloader.saveTaskCompleted(downloadId, path)

                            handler.post {
                                setTitle(R.string.kr_download_completed)
                                binding.krDownloadProgress.visibility = View.GONE
                                stopWatchDownloadProgress()

                                val result = Intent()
                                result.putExtra("absPath", path)
                                setResult(0, result)

                                if (autoClose) {
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }, 200, 500)
    }
}
