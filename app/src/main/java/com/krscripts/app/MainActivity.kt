package com.krscripts.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krscripts.app.databinding.ActivityMainBinding
import com.krscripts.app.util.chooseFilePath
import com.krscripts.app.util.handleFileSelectorResult
import com.krscripts.common.ui.DialogHelper
import com.krscripts.common.ui.ProgressBarDialog
import com.krscripts.core.config.PageConfigReader
import com.krscripts.core.config.PageConfigSh
import com.krscripts.core.model.ClickableNode
import com.krscripts.core.model.KrScriptActionHandler
import com.krscripts.core.model.NavNode
import com.krscripts.core.model.NodeInfoBase
import com.krscripts.core.model.PageNode
import com.krscripts.core.model.RunnableNode
import com.krscripts.core.ui.ActionListFragment
import com.krscripts.core.ui.ParamsFileChooserRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val progressBarDialog = ProgressBarDialog(this)
    private var krScriptConfig = KrScriptConfig()
    lateinit var binding: ActivityMainBinding
    private var currentPageNode: PageNode? = null
    private var fileSelectorInterface: ParamsFileChooserRender.FileSelectedInterface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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

    private suspend fun buildNavgationMenu(pageConfigs: MutableList<PageNode?>) = withContext(Dispatchers.Main) {
        val menu = binding.bottomNavView.menu
        menu.clear()

        var firstTabFragment: Fragment? = null

        pageConfigs.forEachIndexed { index, page ->

            page ?: return@forEachIndexed

            getItems(page)?.let { pageItems ->

                val tabFragment = createTab(pageItems, page)

                if (index == 0) {
                    firstTabFragment = tabFragment
                }

                val menuName =
                    pageItems.lastOrNull()?.title?.takeIf { it.isNotEmpty() && pageItems.last() is NavNode }
                        ?: page.pageConfigPath.substringAfterLast('/')

                menu.add(menuName).apply {
                    icon = ContextCompat.getDrawable(
                        this@MainActivity,
                        R.drawable.baseline_bookmark_24
                    )!!
                    setOnMenuItemClickListener {
                        currentPageNode = page
                        updateTab(tabFragment)
                        false
                    }
                }
            }
        }

        firstTabFragment?.let {
            currentPageNode = pageConfigs[0]
            updateTab(it)
        }
    }

    private fun getItems(pageNode: PageNode): ArrayList<NodeInfoBase>? {
        var items: ArrayList<NodeInfoBase>? = null

        if (pageNode.pageConfigSh.isNotEmpty()) {
            items = PageConfigSh(this, pageNode.pageConfigSh, null).execute()
        }
        if (items == null && pageNode.pageConfigPath.isNotEmpty()) {
            items = PageConfigReader(this.applicationContext, pageNode.pageConfigPath, null).readConfigXml()
        }

        return items
    }

    private fun updateTab(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, fragment).commitAllowingStateLoss()
    }

    private fun createTab(items: ArrayList<NodeInfoBase>, pageNode: PageNode): Fragment {
        return ActionListFragment.create(items, getKrScriptActionHandler(pageNode), null, false)
    }

    private fun reloadTab() {
        val pageNode = currentPageNode ?: return
        Thread {
            val pageItems = getItems(pageNode)
            pageItems?.let { items ->
                lifecycleScope.launch {
                    val newTab = createTab(items, pageNode)
                    updateTab(newTab)
                }
            }
        }.start()
    }

    private fun getKrScriptActionHandler(pageNode: PageNode): KrScriptActionHandler {
        return object : KrScriptActionHandler {
            override fun onActionCompleted(runnableNode: RunnableNode) {
                if (runnableNode.autoFinish ) {
                    finishAndRemoveTask()
                } else if (runnableNode.reloadPage) {
                    reloadTab()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleFileSelectorResult(this, resultCode, requestCode, data, fileSelectorInterface)
        fileSelectorInterface = null
        super.onActivityResult(requestCode, resultCode, data)
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
        menuInflater.inflate(R.menu.main, menu)
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
                DialogHelper.animDialog(this, MaterialAlertDialogBuilder(this).setView(layout))
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
