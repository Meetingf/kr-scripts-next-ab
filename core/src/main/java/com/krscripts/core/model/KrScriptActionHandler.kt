package com.krscripts.core.model

import android.content.Intent
import android.view.View
import com.krscripts.core.ui.ParamsFileChooserRender

interface KrScriptActionHandler {
    fun openFileChooser(fileSelectedInterface: ParamsFileChooserRender.FileSelectedInterface): Boolean
    fun onSubPageClick(pageNode: PageNode)
    fun onActionCompleted(runnableNode: RunnableNode)
    fun createShortcut(clickableNode: ClickableNode, createShortcutHandler: CreateShortcutHandler)
    fun openParamsPage(actionNode: ActionNode, view: View, onCompleted: Runnable): Boolean {
        return false
    }

    interface CreateShortcutHandler {
        fun onCreateShortcut(clickableNode: ClickableNode, intent: Intent?)
    }
}
