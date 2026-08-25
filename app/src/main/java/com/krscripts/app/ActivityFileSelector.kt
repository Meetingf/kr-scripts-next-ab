package com.krscripts.app

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.krscripts.app.databinding.ActivityFileSelectorBinding
import com.krscripts.app.ui.AdapterFileSelector
import com.krscripts.core.ui.dialog.ProgressBarDialog
import com.krscripts.core.util.PermissionUtil
import java.io.File

class ActivityFileSelector : AppCompatActivity() {
    companion object {
        const val MODE_FILE = 0
        const val MODE_FOLDER = 1
        const val ACTION_FILE_PATH_CHOOSER = 65400
        const val ACTION_FILE_PATH_CHOOSER_INNER = 65300
    }

    private var adapterFileSelector: AdapterFileSelector? = null
    var extension = ""
    var mode = MODE_FILE

    private lateinit var binding: ActivityFileSelectorBinding

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtil.REQUEST_CODE_FILE_ACCESS) {
            loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityFileSelectorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.fileSelector) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { _ ->
            finish()
        }

        intent.extras?.run {
            if (containsKey("extension")) {
                extension = intent.extras!!.getString("extension").toString()
                if (!extension.startsWith(".")) {
                    extension = ".$extension"
                }
                if (extension.isNotEmpty()) {
                    title = "$title($extension)"
                }
            }
            if (containsKey("mode")) {
                mode = getInt("mode")
                if (mode == MODE_FOLDER) {
                    title = getString(R.string.title_activity_folder_selector)
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && adapterFileSelector != null && adapterFileSelector!!.goParent()) {
            return true
        } else {
            setResult(RESULT_CANCELED, Intent())
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        if (PermissionUtil.checkAccessFiles(this)) {
            val sdcard = File(Environment.getExternalStorageDirectory().absolutePath)
            if (sdcard.exists() && sdcard.isDirectory) {
                val list = sdcard.listFiles()
                if (list == null) {
                    Toast.makeText(applicationContext, "获取文件列表失败！", Toast.LENGTH_LONG).show()
                    return
                }
                val onSelected =  Runnable {
                    val file: File? = adapterFileSelector!!.selectedFile
                    if (file != null) {
                        this.setResult(RESULT_OK, Intent().putExtra("file", file.absolutePath))
                        this.finish()
                    }
                }
                adapterFileSelector = if (mode == MODE_FOLDER) {
                    AdapterFileSelector.folderChooser(sdcard, onSelected, ProgressBarDialog(this))
                } else {
                    AdapterFileSelector.fileChooser(sdcard, onSelected, ProgressBarDialog(this), extension)
                }

                binding.fileSelectorList.adapter = adapterFileSelector
            }
        } else {
            PermissionUtil.requestAccessFilesDialog(this) {
                this.finish()
            }
        }
    }
}
