package com.krscripts.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krscripts.core.R
import com.krscripts.core.ui.dialog.DialogHelper

object PermissionUtil {

    const val REQUEST_CODE_FILE_ACCESS = 0x11

    fun requestAccessFilesDialog(
        context: Activity,
        onSkip: () -> Unit = { }
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle("权限缺失")
            .setMessage("请授予文件读写与应用列表等权限")
            .setPositiveButton("授予") { _, _ ->
                val requested = mutableListOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                )
                // API 33+ 读取已安装应用列表需声明该权限，API 34+ 起为运行时权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requested += "android.permission.GET_INSTALLED_APPS"
                }
                ActivityCompat.requestPermissions(
                    context,
                    requested.toTypedArray(),
                    REQUEST_CODE_FILE_ACCESS
                )
            }
            .setNegativeButton(R.string.btn_exit) { _, _ ->
                context.finishAffinity()
            }
            .setNeutralButton(R.string.btn_skip) { _, _ ->
                onSkip()
            }
            .setCancelable(false)
        DialogHelper.animDialog(context, builder)
    }

    fun checkAccessFiles(context: Context): Boolean {
        return checkPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) &&
                checkPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun checkPermission(context: Context, permission: String): Boolean {
        return PermissionChecker.checkSelfPermission(
            context,
            permission
        ) == PermissionChecker.PERMISSION_GRANTED
    }
}