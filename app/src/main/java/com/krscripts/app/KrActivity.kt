package com.krscripts.app

import android.content.Intent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.krscripts.app.util.chooseFilePath
import com.krscripts.app.util.handleFileSelectorResult
import com.krscripts.common.ui.DialogHelper
import com.krscripts.common.ui.ProgressBarDialog
import com.krscripts.core.HiddenTaskThread
import com.krscripts.core.R
import com.krscripts.core.config.IconPathAnalysis
import com.krscripts.core.model.PageMenuOption
import com.krscripts.core.model.RunnableNode
import com.krscripts.core.ui.DialogLogFragment
import com.krscripts.core.ui.ParamsFileChooserRender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class KrActivity: AppCompatActivity() {

    protected val progressBarDialog = ProgressBarDialog(this)
    protected var menuOptions = ArrayList<PageMenuOption>()
    protected var menuHandler: String? = null
    protected var fileSelectorInterface: ParamsFileChooserRender.FileSelectedInterface? = null

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onMenuItemClick(menuOptions[item.itemId])
        return true
    }

    protected fun onMenuItemClick(
        menuOption: PageMenuOption
    ) {
        when(menuOption.type) {
            "refresh", "reload" -> {
                onReload()
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

    protected open fun onReload() = recreate()

    protected open fun menuItemExecute(menuOption: PageMenuOption, params: HashMap<String, String>) {
        val onDismiss = Runnable {
            if (menuOption.autoFinish) {
                finish()
            } else if (menuOption.reloadPage) {
                onReload()
            } else if (menuOption.updateBlocks != null) {
                // TODO rootGroup.triggerUpdateByKey(item.updateBlocks!!)
            }
        }

        val scripts = menuHandler ?: "echo Handler not found"

        fun runScripts() {
            if (menuOption.shell == RunnableNode.shellModeHidden) {
                HiddenTaskThread.startTask(this, scripts, params, menuOption, { }, onDismiss)
            } else {
                val dialog = DialogLogFragment.create(
                    menuOption,
                    { },
                    onDismiss,
                    scripts,
                    params
                )
                dialog.show(supportFragmentManager, "")
                dialog.isCancelable = false
            }
        }

        if (menuOption.confirm) {
            DialogHelper.openConfirmAlert(this, menuOption.title, menuOption.desc.ifEmpty { "真的要这么做么" }) {
                runScripts()
            }
        } else {
            runScripts()
        }
    }

    protected fun menuItemChooseFile(menuOption: PageMenuOption) {
        fileSelectorInterface = object: ParamsFileChooserRender.FileSelectedInterface{
            override fun onFileSelected(path: String?) {
                if (path != null) {
                    lifecycleScope.launch(Dispatchers.Main) {
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

    protected fun addFab(
        menuOption: PageMenuOption,
        fab: FloatingActionButton
    ) {
        fab.run {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleFileSelectorResult(this, resultCode, requestCode, data, fileSelectorInterface)
        fileSelectorInterface = null
        super.onActivityResult(requestCode, resultCode, data)
    }
}