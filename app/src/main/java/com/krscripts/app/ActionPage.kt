package com.krscripts.app

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.krscripts.app.databinding.ActivityActionPageBinding
import com.krscripts.app.util.chooseFilePath
import com.krscripts.app.util.handleFileSelectorResult
import com.krscripts.common.ui.ProgressBarDialog
import com.krscripts.core.R
import com.krscripts.core.TryOpenActivity
import com.krscripts.core.config.IconPathAnalysis
import com.krscripts.core.config.PageConfigReader
import com.krscripts.core.config.PageConfigSh
import com.krscripts.core.executor.ScriptEnvironment
import com.krscripts.core.model.AutoRunTask
import com.krscripts.core.model.ClickableNode
import com.krscripts.core.model.ConfigNode
import com.krscripts.core.model.KrScriptActionHandler
import com.krscripts.core.model.PageMenuOption
import com.krscripts.core.model.PageNode
import com.krscripts.core.model.RunnableNode
import com.krscripts.core.shortcut.ActionShortcutManager
import com.krscripts.core.ui.ActionListFragment
import com.krscripts.core.ui.DialogLogFragment
import com.krscripts.core.ui.PageMenuLoader
import com.krscripts.core.ui.ParamsFileChooserRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


open class ActionPage : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)
    private var actionsLoaded = false
    private lateinit var pageConfigCompat: PageNode
    private var autoRunItemId: String? = null
    private var fileSelectorInterface: ParamsFileChooserRender.FileSelectedInterface? = null
    private var menuOptions = ArrayList<PageMenuOption>()
    private var config: ConfigNode? = null
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
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
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
                extras.containsKey("page") -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) extras.getSerializable(
                    "page",
                    PageNode::class.java
                ) else @Suppress("DEPRECATION") extras.getSerializable("page") as PageNode

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

                if (page.link.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, page.link.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    this.startActivity(intent)
                }

                if (page.title.isNotEmpty()) {
                    title = page.title
                }
                pageConfigCompat = page
            } ?: {
                Toast.makeText(this, "页面信息无效", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        if (pageConfigCompat.pageConfigPath.isEmpty() && pageConfigCompat.pageConfigSh.isEmpty()) {
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

        override fun createShortcut(clickableNode: ClickableNode, createShortcutHandler: KrScriptActionHandler.CreateShortcutHandler) {
            val page = clickableNode as? PageNode
                ?: if (clickableNode is RunnableNode) {
                    pageConfigCompat
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

            createShortcutHandler.onCreateShortcut(clickableNode, intent)
        }

        override fun onSubPageClick(pageNode: PageNode) {
            OpenPageHelper(this@ActionPage).openPage(pageNode)
        }

        override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
            fileSelectorInterface = fileSelectedInterface
            return chooseFilePath(fileSelectedInterface)
        }
    }

    // 右上角菜单的创建
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()

        PageMenuLoader(applicationContext, pageConfigCompat).load()?.let {
            menuOptions.addAll(it)
        }

        if (menu != null) {
            for (i in menuOptions.indices) {
                val menuOption = menuOptions[i]
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

        onMenuItemClick(menuOptions[item.itemId])

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
            pageConfigCompat.pageHandlerSh,
            params
        )
        dialog.show(supportFragmentManager, "")
        dialog.isCancelable = false
    }

    private fun menuItemChooseFile(menuOption: PageMenuOption) {
        fileSelectorInterface = object: ParamsFileChooserRender.FileSelectedInterface{
            override fun onFileSelected(path: String?) {
                if (path != null) {
                    lifecycleScope.launch {
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
        }
        fileSelectorInterface?.let { chooseFilePath(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleFileSelectorResult(this, resultCode, requestCode, data, fileSelectorInterface)
        fileSelectorInterface = null
        super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun showDialog(msg: String) = withContext(Dispatchers.Main) {
        progressBarDialog.showDialog(msg)
    }

    private suspend fun hideDialog() = withContext(Dispatchers.Main) {
        progressBarDialog.hideDialog()
    }

    override fun onResume() {
        super.onResume()

        if (!actionsLoaded) {
            loadPageConfig()
        }
    }

    private fun loadPageConfig() {
        val activity = this

        lifecycleScope.launch(Dispatchers.IO) {
            pageConfigCompat.run {
                if (beforeRead.isNotEmpty()) {
                    showDialog(getString(R.string.kr_page_before_load))
                    ScriptEnvironment.executeResultRoot(activity, beforeRead, this)
                }

                showDialog(getString(R.string.kr_page_loading))

                if (pageConfigSh.isNotEmpty()) {
                    config = PageConfigSh(this@ActionPage, pageConfigSh, this).execute()
                }

                if (config == null && pageConfigPath.isNotEmpty()) {
                    config = PageConfigReader(
                        applicationContext,
                        pageConfigPath,
                        pageConfigDir
                    ).readConfigXml()
                }

                if (afterRead.isNotEmpty()) {
                    showDialog(getString(R.string.kr_page_after_load))
                    ScriptEnvironment.executeResultRoot(activity, afterRead, this)
                }

                config?.let { config ->
                    if (loadSuccess.isNotEmpty()) {
                        showDialog(getString(R.string.kr_page_load_success))
                        ScriptEnvironment.executeResultRoot(activity, loadSuccess, this)
                    }

                    withContext(Dispatchers.Main) {
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

                        config.pageMenuOptions.let {
                            menuOptions.clear()
                            menuOptions.addAll(it)
                            invalidateOptionsMenu()
                        }

                        val fragment = ActionListFragment.create(
                            config.content,
                            actionShortClickHandler,
                            autoRunTask
                        )
                        supportFragmentManager.beginTransaction()
                            .replace(com.krscripts.app.R.id.main_list, fragment)
                            .commitAllowingStateLoss()
                        hideDialog()
                        actionsLoaded = true
                    }
                } ?: if (loadFail.isNotEmpty()) {
                        showDialog(getString(R.string.kr_page_load_fail))
                        ScriptEnvironment.executeResultRoot(activity, loadFail, this)
                        hideDialog()
                    } else {
                        withContext(Dispatchers.Main) {
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
        }
    }
}
