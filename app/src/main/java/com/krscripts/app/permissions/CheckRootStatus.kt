package com.krscripts.app.permissions

import android.Manifest
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.PermissionChecker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krscripts.common.shell.KeepShellPublic
import com.krscripts.common.ui.DialogHelper
import com.krscripts.app.R
import kotlin.system.exitProcess

/**
 * 检查获取root权限
 * Created by helloklf on 2017/6/3.
 * Edited by buylan on 2026/07/15
 */

class CheckRootStatus(var context: Context, private var next: Runnable? = null) {
    var myHandler: Handler = Handler(Looper.getMainLooper())

    var thread: Thread? = null
    fun forceGetRoot() {
        if (lastCheckResult) {
            next?.let { myHandler.post(it) }
        } else {
            var completed = false
            thread = Thread {
                rootStatus = KeepShellPublic.checkRoot()
                if (completed) {
                    return@Thread
                }

                completed = true

                if (lastCheckResult) {
                    next?.let { myHandler.post(it) }
                } else {
                    myHandler.post {
                        KeepShellPublic.tryExit()
                        val builder = MaterialAlertDialogBuilder(context)
                            .setTitle(context.getString(R.string.error_root_title))
                                .setMessage(R.string.error_root_message)
                                .setPositiveButton(R.string.btn_retry) { _, _ ->
                                    KeepShellPublic.tryExit()
                                    if (thread != null && thread!!.isAlive && !thread!!.isInterrupted) {
                                        thread!!.interrupt()
                                        thread = null
                                    }
                                    forceGetRoot()
                                }
                                .setNegativeButton(R.string.btn_exit) { _, _ ->
                                    exitProcess(0)
                                    //android.os.Process.killProcess(android.os.Process.myPid())
                                }
                        if (!context.resources.getBoolean(R.bool.force_root)) {
                            builder.setNeutralButton(com.krscripts.core.R.string.btn_skip) { _, _ ->
                                next?.let { myHandler.post(it) }
                            }
                        }
                        DialogHelper.animDialog(context, builder).setCancelable(false)
                    }
                }
            }
            thread!!.start()
        }
    }

    companion object {
        private var rootStatus = false
        private fun checkPermission(context: Context, permission: String): Boolean = PermissionChecker.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
        fun grantPermission(context: Context) {
            val cmds = StringBuilder()
            /*
            // 必需的权限
            val requiredPermission = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            requiredPermission.forEach {
                if (!checkPermission(context, it)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val option = it.substring("android.permission.".length)
                        cmds.append("appops set ${context.packageName} ${option} allow\n")
                    }
                    cmds.append("pm grant ${context.packageName} $it\n")
                }
            }
            */

            if (!checkPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) {
                cmds.append("dumpsys deviceidle whitelist +${context.packageName};\n")
            }
            KeepShellPublic.doCmdSync(cmds.toString())
        }

        val lastCheckResult: Boolean get() = rootStatus
    }
}
