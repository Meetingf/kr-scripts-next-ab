package com.krscripts.core.ui

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.krscripts.core.R
import com.krscripts.core.model.ActionParamInfo
import com.krscripts.core.model.SelectItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParamsAppChooserRender(
    private var actionParamInfo: ActionParamInfo,
    private var context: FragmentActivity
) : DialogAppChooser.Callback {
    private lateinit var editText: TextInputEditText
    private lateinit var inputLayout: TextInputLayout
    private lateinit var packages: ArrayList<AdapterAppChooser.AppInfo>
    private val packagesReady = CompletableDeferred<Unit>()
    private val progressDialog = ProgressBarDialog(context)

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_edit_text, null)
        editText = layout.findViewById(R.id.kr_param_text)
        inputLayout = layout.findViewById(R.id.textInputLayout)
        editText.apply {
            hint = context.getString(R.string.kr_please_choose_app)
            setCursorVisible(false)
            setFocusable(false)
            setFocusableInTouchMode(false)
        }

        initView()

        inputLayout.apply {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            endIconDrawable = AppCompatResources.getDrawable(context, R.drawable.baseline_android_24)
            setEndIconOnClickListener { openAppChooser() }
        }

        editText.setOnClickListener { openAppChooser() }

        editText.tag = actionParamInfo.name

        return layout
    }

    private fun openAppChooser() {
        context.lifecycleScope.launch {
            if (!::packages.isInitialized) {
                progressDialog.showDialog()
                packagesReady.await()
                progressDialog.hideDialog()
            }
            setSelectStatus()
            DialogAppChooser(
                packages,
                actionParamInfo.multiple,
                this@ParamsAppChooserRender
            ).show(context.supportFragmentManager, "app-chooser")
        }
    }

    private suspend fun loadPackages(includeMissing: Boolean = false): List<AdapterAppChooser.AppInfo> {
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val filter = actionParamInfo.optionsFromShell?.map {
                it.value
            }

            val packages = pm.getInstalledPackages(0).filter {
                filter == null || filter.contains(it.packageName)
            }

            val options = ArrayList(packages.map {
                AdapterAppChooser.AppInfo().apply {
                    appName = "" + it.applicationInfo!!.loadLabel(pm)
                    packageName = it.packageName
                }
            })

            // 是否包含丢失的应用程序
            if (includeMissing && actionParamInfo.optionsFromShell != null) {
                for (item in actionParamInfo.optionsFromShell!!) {
                    if (options.none { it.packageName == item.value }) {
                        options.add(AdapterAppChooser.AppInfo().apply {
                            appName = "" + item.title
                            packageName = "" + item.value
                        })
                    }
                }
            }

            options
        }
    }

    private fun setSelectStatus() {
        packages.forEach {
            it.selected = false
        }
        val currentValue = editText.text
        if (actionParamInfo.multiple) {
            currentValue?.split(actionParamInfo.separator)?.forEach { value ->
                val app = packages.find { it.packageName == value }
                if (app != null) {
                    app.selected = true
                }
            }
        } else {
            val current = packages.find { it.packageName == currentValue?.toString() }
            val currentIndex = if (current != null) packages.indexOf(current) else -1
            if (currentIndex > -1) {
                packages[currentIndex].selected = true
            }
        }
    }

    // 设置界面显示和元素赋值
    private fun initView() {
        val lifecycleScope = CoroutineScope(SupervisorJob())
        lifecycleScope.launch {
            packages = ArrayList(loadPackages(actionParamInfo.type == "packages"))

            packages.run {
                val values = map { it.packageName }.toTypedArray()
                if (actionParamInfo.multiple) {
                    ActionParamsLayoutRender.getParamValues(actionParamInfo)?.forEach { value ->
                        val app = packages.find { it.packageName == value }
                        if (app != null) {
                            app.selected = true
                        }
                    }

                    withContext(Dispatchers.Main) {
                        onConfirm((packages.filter { it.selected }))
                    }
                } else {
                    // TODO: 这里有过多的数据包装盒解包，需要进行优化
                    val validOptions = ArrayList(packages.map {
                        SelectItem().apply {
                            title = it.appName
                            value = it.packageName
                        }
                    }.toList())

                    val currentIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(
                        actionParamInfo,
                        validOptions
                    )

                    withContext(Dispatchers.Main) {
                        if (currentIndex > -1) {
                            editText.setText(values[currentIndex])
                        } else {
                            editText.setText(null)
                        }
                    }
                }
            }

            if (!packagesReady.isCompleted) {
                packagesReady.complete(Unit)
            }
        }
    }

    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
        if (actionParamInfo.multiple) {
            val values = apps.joinToString(actionParamInfo.separator) { it.packageName }
            editText.setText(values)
        } else {
            val item = apps.firstOrNull()
            if (item == null) {
                editText.setText(null)
            } else {
                editText.setText(item.packageName)
            }
        }
    }
}
