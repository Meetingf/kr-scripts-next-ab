package com.krscripts.core

import android.content.Context
import android.text.SpannableString
import android.widget.Toast
import com.krscripts.core.executor.ShellExecutor
import com.krscripts.core.model.RunnableNode
import com.krscripts.core.model.ShellHandlerBase


object HiddenTaskThread {

    class ServiceShellHandler(private val context: Context) : ShellHandlerBase() {
        private var errorRows = ArrayList<String>()
        private var isFinished = false

        override fun onError(msg: Any?) {
            synchronized(errorRows) {
                errorRows.add(msg?.toString().toString())
            }
        }

        override fun onExit(msg: Any?) {
            isFinished = true
            if (errorRows.isNotEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.kr_script_task_has_error) + ": " + errorRows.joinToString(", ").trim(),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        override fun updateLog(msg: SpannableString?) {}
        override fun onReader(msg: Any?) {}
        override fun onWrite(msg: Any?) {}
        override fun onProgress(current: Int, total: Int) {}
        override fun onStart(msg: Any?) {}
        override fun onStart(forceStop: Runnable?) {}
    }

    fun startTask(
        context: Context,
        script: String,
        params: HashMap<String, String>?,
        nodeInfo: RunnableNode,
        onExit: Runnable,
        onDismiss: Runnable
    ) {
        val applicationContext = context.applicationContext

        val handler = ServiceShellHandler(applicationContext)
        ShellExecutor().execute(
            context,
            nodeInfo,
            script,
            {
                try {
                    onExit.run()
                    onDismiss.run()
                } catch (_: Exception) {
                }
            },
            params,
            handler
        )
    }
}
