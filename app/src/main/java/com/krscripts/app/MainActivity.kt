package com.krscripts.app

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krscripts.app.databinding.ActivityMainBinding
import com.krscripts.app.util.chooseFilePath
import com.krscripts.core.config.PageConfigReader
import com.krscripts.core.config.PageConfigSh
import com.krscripts.core.model.ClickableNode
import com.krscripts.core.model.ConfigNode
import com.krscripts.core.model.KrScriptActionHandler
import com.krscripts.core.model.NavNode
import com.krscripts.core.model.PageNode
import com.krscripts.core.model.RunnableNode
import com.krscripts.core.ui.ActionListFragment
import com.krscripts.core.ui.DialogHelper
import com.krscripts.core.ui.ParamsFileChooserRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : KrActivity() {

    private var krScriptConfig = KrScriptConfig()
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setSupportActionBar(binding.toolbar)

        lifecycleScope.launch {
            progressBarDialog.showDialog(getString(R.string.please_wait))

            krScriptConfig = KrScriptConfig()
            val pageConfigs = krScriptConfig.pageListConfig
            buildNavgationMenu(pageConfigs)

            progressBarDialog.hideDialog()
        }
    }

    private suspend fun buildNavgationMenu(pageConfigs: List<PageNode>) = withContext(Dispatchers.Main) {

        binding.viewPager.apply {
            adapter = PageFragmentAdapter(this@MainActivity, pageConfigs)
            offscreenPageLimit = 1
            isUserInputEnabled = false
        }

        val menu = binding.bottomNavView.menu
        menu.clear()

        pageConfigs.forEachIndexed { index, page ->

            getConfig(page)?.let { config ->

                config.pageHandlerSh.takeIf { it.isNotEmpty() }?.let {
                    menuHandler = it
                }

                config.pageMenuOptions.let {
                    menuOptions.addAll(it)
                    invalidateOptionsMenu()
                }

                val menuName =
                    config.content.lastOrNull()?.title?.takeIf { it.isNotEmpty() && config.content.last() is NavNode }
                        ?: page.pageConfigPath.substringAfterLast('/')

                menu.add(menuName).apply {
                    icon = ContextCompat.getDrawable(
                        this@MainActivity,
                        R.drawable.baseline_bookmark_24
                    )!!
                    setOnMenuItemClickListener {
                        binding.viewPager.setCurrentItem(index, false)
                        false
                    }
                }
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNavView.menu[position].isChecked = true
            }
        })
    }

    private fun getConfig(pageNode: PageNode): ConfigNode? {
        var config: ConfigNode? = null

        if (pageNode.pageConfigSh.isNotEmpty()) {
            config = PageConfigSh(this, pageNode.pageConfigSh, null).execute()
        }
        if (config == null && pageNode.pageConfigPath.isNotEmpty()) {
            config = PageConfigReader(this.applicationContext, pageNode.pageConfigPath, null).readConfigXml()
        }

        return config
    }

    private fun reloadTab(pageNode: PageNode, index: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val items = getConfig(pageNode)
            withContext(Dispatchers.Main) {
                items?.let { newItems ->
                    val itemId = (binding.viewPager.adapter as? PageFragmentAdapter)?.getItemId(index) ?: return@withContext
                    val tag = "f$itemId"
                    val fragment = supportFragmentManager.findFragmentByTag(tag) as? ActionListFragment
                    fragment?.update(newItems.content, getKrScriptActionHandler(pageNode, index))
                }
            }
        }
    }

    private fun getKrScriptActionHandler(pageNode: PageNode, index: Int): KrScriptActionHandler {
        return object : KrScriptActionHandler {
            override fun onActionCompleted(runnableNode: RunnableNode) {
                if (runnableNode.autoFinish ) {
                    finishAndRemoveTask()
                } else if (runnableNode.reloadPage) {
                    reloadTab(pageNode, index)
                }
            }

            override fun createShortcut(clickableNode: ClickableNode, createShortcutHandler: KrScriptActionHandler.CreateShortcutHandler) {
                val page = clickableNode as? PageNode
                    ?: if (clickableNode is RunnableNode) {
                        pageNode
                    } else {
                        return
                    }

                val intent = Intent()

                intent.component = ComponentName(this@MainActivity.applicationContext, ActionPage::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

                if (clickableNode is RunnableNode) {
                    intent.putExtra("autoRunItemId", clickableNode.key)
                }
                intent.putExtra("page", page)

                createShortcutHandler.onCreateShortcut(clickableNode, intent)
            }

            override fun onSubPageClick(pageNode: PageNode) {
                openPage(pageNode)
            }

            override fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean {
                fileSelectorInterface = fileSelectedInterface
                return chooseFilePath(fileSelectedInterface)
            }
        }
    }

    fun openPage(pageNode: PageNode) {
        OpenPageHelper(this).openPage(pageNode)
    }

    private fun getThemeColor(attrRes: Int): Int {
        val typedValue = TypedValue()
        this.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menu.clear()
        menuInflater.inflate(R.menu.main, menu)

        for (i in menuOptions.indices) {
            val menuOption = menuOptions[i]
            if (menuOption.isFab) {
                addFab(menuOption, binding.fab)
            } else {
                menu.add(-1, i, i, menuOption.title)
            }
        }

        menu.findItem(R.id.option_menu_info)?.icon?.setTint(
            getThemeColor(com.google.android.material.R.attr.colorOnSurface)
        )

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.option_menu_info -> {
                val layoutInflater = LayoutInflater.from(this)
                val layout = layoutInflater.inflate(R.layout.dialog_about, null)

                val appVersion = try {
                    packageManager.getPackageInfo(packageName, 0).versionName
                } catch (_: Exception) {
                    ".null"
                }

                val tvAppVersion = layout.findViewById<TextView>(R.id.tv_app_version)
                tvAppVersion.text = getString(R.string.app_version, appVersion)

                val frameworkVersion = BuildConfig.FRAMEWORK_VERSION
                val tvFrameworkInfo = layout.findViewById<TextView>(R.id.tv_framework_info)
                tvFrameworkInfo.text = getString(R.string.framework_info, frameworkVersion)

                DialogHelper.animDialog(this, MaterialAlertDialogBuilder(this).setView(layout).setTitle(getString(R.string.title_about)))
            }
            else -> {
                onMenuItemClick(menuOptions[item.itemId])
            }
        }
        return true
    }

    inner class PageFragmentAdapter(
        activity: FragmentActivity,
        private val pages: List<PageNode>
    ) : FragmentStateAdapter(activity) {

        private val sessionId: Long = System.nanoTime()

        override fun getItemCount() = pages.size

        override fun getItemId(position: Int): Long {
            return sessionId + position
        }

        override fun containsItem(itemId: Long): Boolean {
            return itemId in sessionId until (sessionId + pages.size)
        }

        override fun createFragment(position: Int): Fragment {
            val page = pages[position]
            val items = getConfig(page) ?: ConfigNode()
            return ActionListFragment.create(items.content, getKrScriptActionHandler(page, position), null, false)
        }
    }
}
