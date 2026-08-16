package com.krscripts.core.ui

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.krscripts.core.R
import com.krscripts.core.model.ActionParamInfo
import com.krscripts.core.model.ParamInfoFilter

class ParamsEditText(private var actionParamInfo: ActionParamInfo, private var context: Context) {
    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_edit_text, null)
        val inputLayout = layout.findViewById<TextInputLayout>(R.id.textInputLayout)
        val editText = layout.findViewById<TextInputEditText>(R.id.kr_param_text)

        editText.run {
            tag = actionParamInfo.name
            if (actionParamInfo.valueFromShell != null)
                setText(actionParamInfo.valueFromShell)
            else if (actionParamInfo.value != null) {
                setText(actionParamInfo.value)
            }
            filters = arrayOf(ParamInfoFilter(actionParamInfo))
            isEnabled = !actionParamInfo.readonly
            if (actionParamInfo.placeholder.isNotEmpty()) {
                hint = actionParamInfo.placeholder
            } else if (
                    (actionParamInfo.type == "int" || actionParamInfo.type == "number")
                    &&
                    (actionParamInfo.min != Int.MIN_VALUE || actionParamInfo.max != Int.MAX_VALUE)
            ) {
                hint = "${actionParamInfo.min} ~ ${actionParamInfo.max}"
            }
        }

        if (actionParamInfo.required) {
            fun validateRequired(text: CharSequence?) {
                val trimmed = text?.toString()
                val isError = trimmed.isNullOrEmpty()
                inputLayout.isErrorEnabled = isError
                inputLayout.error = if (isError) "此项不能为空" else null
            }

            validateRequired(editText.text)

            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    validateRequired(s)
                }
            })
        }

        return layout
    }
}
