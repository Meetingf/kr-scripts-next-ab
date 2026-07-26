package com.krscripts.app

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.krscripts.app.databinding.ActivityActionPageBinding
import com.krscripts.app.util.chooseFilePath
import com.krscripts.app.util.handleFileSelectorResult
import com.krscripts.common.shared.FilePathResolver
import com.krscripts.common.ui.ProgressBarDialog
import com.krscripts.core.R
import com.krscripts.core.TryOpenActivity
import com.krscripts.core.config.IconPathAnalysis
import com.krscripts.core.config.PageConfigReader
import com.krscripts.core.config.PageConfigSh
import com.krscripts.core.executor.ScriptEnvironment
import com.krscripts.core.model.AutoRunTask
import com.krscripts.core.model.ClickableNode
import com.krscripts.core.model.KrScriptActionHandler
import com.krscripts.core.model.NodeInfoBase
import com.krscripts.core.model.PageMenuOption
import com.krscripts.core.model.PageNode
import com.krscripts.core.model.RunnableNode
import com.krscripts.core.shortcut.ActionShortcutManager
import com.krscripts.core.ui.ActionListFragment
import com.krscripts.core.ui.DialogLogFragment
import com.krscripts.core.ui.PageMenuLoader
import com.krscripts.core.ui.ParamsFileChooserRender


class ActionPage : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)
    private var actionsLoaded = false
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var currentPageConfig: PageNode
    private var autoRunItemId: String? = null

    private lateinit var binding: ActivityActionPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Jump to splash when is no initialed
        if (!ScriptEnvironment.isInitialed) {
            val initIntent = Intent(this.applicationContext, SplashActivity::class.java)
            initIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            initIntent.putExtras(this.intent)
            initIntent.putExtra("JumpActionPage", true)
            startActivity(initIntent)
            finish()
            return
        }

        enableEdgeToEdge()

        binding = ActivityActionPageBinding.inflate(layoutInflater)
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

        intent?.extras?.let { extras ->

            val page = when {
                extras.containsKey("page") -> extras.getSerializable("page") as PageNode?
                extras.containsKey("shortcutId") -> ActionShortcutManager(this).getShortcutTarget(
                    extras.getString("shortcutId")
                )
                else -> null
            }

            page?.let { page ->
                autoRunItemId =
                    if (extras.containsKey("autoRunItemId")) extras.getString("autoRunItemId") else null

                if (page.activity.isNotEmpty()) {
                    if (TryOpenActivity(this, page.activity).tryOpen()) {
                        finish()
                        return
                    }
                }

                if (page.onlineHtmlPage.isNotEmpty()) {
                    try {
                        startActivity(Intent(this, ActionPageOnline::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("config", page.onlineHtmlPage)
                        })
                    } catch (_: Exception) {
                    }
                }

                if (page.title.isNotEmpty()) {
                    title = page.title
                }
                currentPageConfig = page
            } ?: {
                Toast.makeText(this, "页面信息无效", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        if (currentPageConfig.pageConfigPath.isEmpty() && currentPageConfig.pageConfigSh.isEmpty()) {
            setResult(2)
            finish()
        }
    }

    private var actionShortClickHandler = object : KrScriptActionHandler {
        override fun onActionCompleted(runnableNode: RunnableNode) {
            if (runnableNode.autoFinish) {
                finishAndRemoveTask()
            } else if (runnableNode.reloadPage) {
                loadPageConfig()
            }
        }

        override fun addToFavorites(clickableNode: ClickableNode, addToFavoritesHandler: KrScriptActionHandler.AddToFavoritesHandler) {
            val page = clickableNode as? PageNode
                ?: if (clickableNode is RunnableNode) {
                    currentPageConfig
                } else {
                    return
                }

            val intent = Intent()

            intent.component = ComponentName(this@ActionPage.applicationContext, ActionPage::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            if (clickableNode is RunnableNode) {
                intent.putExtra("autoRunItemId", clickableNode.key)
            }

            intent.putExtra("page", page)

            addToFavoritesHandler.onAddToFavorites(clickableNode, intent)
        }

        override fun onSubPageClick(pageNode: PageNode) {
            openPage(pageNode)
        }

        override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
            fileSelectorInterface = fileSelectedInterface
            return chooseFilePath(fileSelectedInterface)
        }
    }

    private var fileSelectorInterface: ParamsFileChooserRender.FileSelectedInterface? = null

    private var menuOptions:ArrayList<PageMenuOption>? = null

    // 右上角菜单的创建
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menuOptions == null) {
            menuOptions = PageMenuLoader(applicationContext, currentPageConfig).load()
        }

        if (menuOptions != null && menu != null) {
            for (i in menuOptions!!.indices) {
                val menuOption = menuOptions!![i]
                if (menuOption.isFab) {
                    addFab(menuOption)
                } else {
                    menu.add(-1, i, i, menuOption.title)
                }
            }
        }

        return true // super.onCreateOptionsMenu(menu)
    }

    private fun addFab(menuOption: PageMenuOption) {
        binding.actionPageFab.run {
            visibility = View.VISIBLE
            setOnClickListener {
                onMenuItemClick(menuOption)
            }

            if (menuOption.type == "file" && menuOption.iconPath.isEmpty()) {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_folder_24))
            } else if (menuOption.iconPath.isNotEmpty()) {
                val icon = IconPathAnalysis().loadLogo(context, menuOption, false)
                if (icon != null) {
                    setImageDrawable(icon)
                } else {
                    setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_menu_24))
                }
            } else {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.baseline_menu_24))
            }
        }
    }

    // 右上角菜单的点击操作
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (menuOptions == null) {
            return false
        }

        onMenuItemClick(menuOptions!![item.itemId])

        return true
    }

    private fun onMenuItemClick(menuOption: PageMenuOption) {
        when(menuOption.type) {
            "refresh", "reload" -> {
                recreate()
            }
            "exit", "finish", "close" -> {
                finish()
            }
            "file" -> {
                menuItemChooseFile(menuOption)
            }
            else -> {
                menuItemExecute(menuOption, HashMap<String, String>().apply{
                    put("state", menuOption.key)
                    put("menu_id", menuOption.key)
                })
            }
        }
    }

    private fun menuItemExecute(menuOption: PageMenuOption, params: HashMap<String, String>) {
        val onDismiss = Runnable {
            if (menuOption.autoFinish) {
                finish()
            } else if (menuOption.reloadPage) {
                recreate()
            } else if (menuOption.updateBlocks != null) {
                // TODO rootGroup.triggerUpdateByKey(item.updateBlocks!!)
            }
        }

        val dialog = DialogLogFragment.create(
            menuOption,
            { },
            onDismiss,
            currentPageConfig.pageHandlerSh,
            params
        )
        dialog.show(supportFragmentManager, "")
        dialog.isCancelable = false
    }

    private fun menuItemChooseFile(menuOption: PageMenuOption) {
        chooseFilePath(object: ParamsFileChooserRender.FileSelectedInterface{
            override fun onFileSelected(path: String?) {
                if (path != null) {
                    handler.post {
                        menuItemExecute(menuOption, HashMap<String, String>().apply{
                            put("state", menuOption.key)
                            put("menu_id", menuOption.key)
                            put("file", path)
                            put("folder", path)
                        })
                    }
                }
            }

            override fun mimeType(): String? {
                return menuOption.mime.ifEmpty { null }
            }

            override fun suffix(): String? {
                return menuOption.suffix.ifEmpty { null }
            }

            override fun type(): Int {
                return when(menuOption.type) {
                    "folder" -> ParamsFileChooserRender.FileSelectedInterface.TYPE_FOLDER
                    "file" -> ParamsFileChooserRender.FileSelectedInterface.TYPE_FILE
                    else -> ParamsFileChooserRender.FileSelectedInterface.TYPE_FILE
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleFileSelectorResult(this, resultCode, requestCode, data, fileSelectorInterface)
        fileSelectorInterface = null
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun showDialog(msg: String) {
        handler.post {
            progressBarDialog.showDialog(msg)
        }
    }

    private fun hideDialog() {
        handler.post {
            progressBarDialog.hideDialog()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!actionsLoaded) {
            loadPageConfig()
        }
    }

    private fun loadPageConfig() {
        val activity = this

        Thread {
            currentPageConfig.run {
                if (beforeRead.isNotEmpty()) {
                    showDialog(getString(R.string.kr_page_before_load))
                    ScriptEnvironment.executeResultRoot(activity, beforeRead, this)
                }

                showDialog(getString(R.string.kr_page_loading))
                var items: ArrayList<NodeInfoBase>? = null

                if (pageConfigSh.isNotEmpty()) {
                    items = PageConfigSh(this@ActionPage, pageConfigSh, this).execute()
                }

                if (items == null && pageConfigPath.isNotEmpty()) {
                    items = PageConfigReader(
                        applicationContext,
                        pageConfigPath,
                        pageConfigDir
                    ).readConfigXml()
                }

                if (afterRead.isNotEmpty()) {
                    showDialog(getString(R.string.kr_page_after_load))
                    ScriptEnvironment.executeResultRoot(activity, afterRead, this)
                }

                if (!items.isNullOrEmpty()) {
                    if (loadSuccess.isNotEmpty()) {
                        showDialog(getString(R.string.kr_page_load_success))
                        ScriptEnvironment.executeResultRoot(activity, loadSuccess, this)
                    }

                    handler.post {
                        val autoRunTask = if (actionsLoaded) null else object : AutoRunTask {
                            override val key = autoRunItemId
                            override fun onCompleted(result: Boolean?) {
                                if (result != true) {
                                    Toast.makeText(
                                        this@ActionPage,
                                        getString(R.string.kr_auto_run_item_losted),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }

                        val fragment = ActionListFragment.create(
                            items,
                            actionShortClickHandler,
                            autoRunTask
                        )
                        supportFragmentManager.beginTransaction()
                            .replace(com.krscripts.app.R.id.main_list, fragment)
                            .commitAllowingStateLoss()
                        hideDialog()
                        actionsLoaded = true
                    }
                } else {
                    if (loadFail.isNotEmpty()) {
                        showDialog(getString(R.string.kr_page_load_fail))
                        ScriptEnvironment.executeResultRoot(activity, loadFail, this)
                        hideDialog()
                    }

                    handler.post {
                        Toast.makeText(
                            this@ActionPage,
                            getString(R.string.kr_page_load_fail),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    hideDialog()
                    finish()
                }
            }
        }.start()
    }

    fun openPage(pageNode: PageNode) {
        OpenPageHelper(this).openPage(pageNode)
    }

    override fun onDestroy() {
        this.setExcludeFromRecents()
        super.onDestroy()
    }

    private fun setExcludeFromRecents() {
        if (isTaskRoot) {
            try {
                val service = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                for (task in service.appTasks) {
                    if (task.taskInfo!!.id == this.taskId) {
                        task.setExcludeFromRecents(true)
                    }
                }
            } catch (_: Exception) {
            }
        }
    }


}
