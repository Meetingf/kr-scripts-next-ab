package com.krscripts.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krscripts.core.R
import com.krscripts.core.ui.dialog.DialogHelper

object PermissionUtil {

    private val storagePermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    fun requestAccessFilesDialog(
        context: Activity,
        permissionRequester: ActivityResultLauncher<Array<String>>? = null,
        onSkip: () -> Unit = { }
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle("权限缺失")
            .setMessage("请授予文件读写权限")
            .setPositiveButton("授予") { _, _ ->
                if (permissionRequester != null) {
                    permissionRequester.launch(storagePermissions)
                } else {
                    ActivityCompat.requestPermissions(context, storagePermissions, 0x11)
                }
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