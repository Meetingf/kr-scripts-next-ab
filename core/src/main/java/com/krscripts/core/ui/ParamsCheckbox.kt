package com.krscripts.core.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import com.krscripts.core.R
import com.krscripts.core.model.ActionParamInfo

class ParamsCheckbox(
    override var actionParamInfo: ActionParamInfo,
    private var context: Context
): ParamRenderer {
    private var checkbox: CheckBox? = null

    override fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_checkbox, null)
        checkbox = layout.findViewById(R.id.kr_param_checkbox)

        checkbox?.run {
            tag = actionParamInfo.name
            isChecked = getCheckState(actionParamInfo)
            if (!actionParamInfo.label.isNullOrEmpty()) {
                text = actionParamInfo.label
            }
        }

        return layout
    }

    private fun getCheckState(
        actionParamInfo: ActionParamInfo,
        defaultValue: Boolean = false
    ): Boolean {
        val value = actionParamInfo.valueFromShell ?: actionParamInfo.value ?: return defaultValue
        return value == "1" || value.lowercase() == "true"
    }

    override fun getValue(): String? {
        return if (checkbox?.isChecked == true) "1" else "0"
    }
}
