package com.krscripts.core.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.krscripts.core.R
import com.krscripts.core.model.ActionParamInfo
import com.krscripts.core.model.SelectItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParamsAppChooserRender(
    private var actionParamInfo: ActionParamInfo,
    private var context: FragmentActivity
) : DialogAppChooser.Callback {
    private lateinit var valueView: TextView
    private lateinit var nameView: TextView
    private lateinit var packages: ArrayList<AdapterAppChooser.AppInfo>

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_app, null)
        valueView = layout.findViewById(R.id.kr_param_app_package)
        nameView = layout.findViewById(R.id.kr_param_app_name)

        setTextView()

        layout.findViewById<View>(R.id.kr_param_app_btn).setOnClickListener {
            openAppChooser()
        }
        nameView.setOnClickListener {
            openAppChooser()
        }

        valueView.tag = actionParamInfo.name

        return layout
    }

    private fun openAppChooser() {
        if (!::packages.isInitialized) {
            Toast.makeText(context, "应用列表加载中", Toast.LENGTH_SHORT).show()
            return
        }
        setSelectStatus()
        DialogAppChooser(packages, actionParamInfo.multiple, this).show(context.supportFragmentManager, "app-chooser")
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
        val currentValue = valueView.text
        if (actionParamInfo.multiple) {
            currentValue.split(actionParamInfo.separator).run {
                this.forEach {
                    val value = it
                    val app = packages.find { it.packageName == value }
                    if (app != null) {
                        app.selected = true
                    }
                }
            }
        } else {
            val current = packages.find { it.packageName == currentValue }
            val currentIndex = if (current != null) packages.indexOf(current) else -1
            if (currentIndex > -1) {
                packages.get(currentIndex).selected = true
            }
        }
    }

    // 设置界面显示和元素赋值
    private fun setTextView() {
        val lifecycleScope = CoroutineScope(SupervisorJob())
        lifecycleScope.launch {
            packages = ArrayList(loadPackages(actionParamInfo.type == "packages"))

            packages.run {
                val labels = map { it.appName }.toTypedArray()
                val values = map { it.packageName }.toTypedArray()
                if (actionParamInfo.multiple) {
                    ActionParamsLayoutRender.getParamValues(actionParamInfo)?.run {
                        this.forEach {
                            val value = it
                            val app = packages.find { it.packageName == value }
                            if (app != null) {
                                app.selected = true
                            }
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
                            valueView.text = values[currentIndex]
                            nameView.text = labels[currentIndex]
                        } else {
                            valueView.text = ""
                            nameView.text = ""
                        }
                    }
                }
            }
        }
    }

    override fun onConfirm(apps: List<AdapterAppChooser.AppInfo>) {
        if (actionParamInfo.multiple) {
            val values = apps.map { it.packageName }.joinToString(actionParamInfo.separator)
            val labels = apps.map { it.appName }.joinToString("，")
            valueView.text = values
            nameView.text = labels
        } else {
            val item = apps.firstOrNull()
            if (item == null) {
                valueView.text = ""
                nameView.text = ""
            } else {
                valueView.text = item.packageName
                nameView.text = item.appName
            }
        }
    }
}
