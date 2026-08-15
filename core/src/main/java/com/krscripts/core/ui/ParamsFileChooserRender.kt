package com.krscripts.core.ui

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.krscripts.core.R
import com.krscripts.core.model.ActionParamInfo

class ParamsFileChooserRender(private var actionParamInfo: ActionParamInfo, private var context: Context, private var fileChooser: FileChooserInterface?) {
    interface FileChooserInterface {
        fun openFileChooser(fileSelectedInterface: FileSelectedInterface): Boolean
    }

    interface FileSelectedInterface {
        companion object {
            val TYPE_FILE: Int
                get() = 0
            val TYPE_FOLDER: Int
                get() = 1
        }

        fun onFileSelected(path: String?) { }
        fun onFileSelected(path: Uri?) { }
        fun mimeType():String?
        fun suffix():String?
        fun type(): Int
    }


    fun setEditTextReadOnly(view: TextInputEditText) {
        view.setCursorVisible(false)
        view.setFocusable(false)
        view.setFocusableInTouchMode(false)
    }

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_edit_text, null)
        val inputLayout = layout.findViewById<TextInputLayout>(R.id.textInputLayout)
        val editText = layout.findViewById<TextInputEditText>(R.id.kr_param_text)

        if (!actionParamInfo.editable) {
            setEditTextReadOnly(editText)
        }

        editText.hint = if (actionParamInfo.type == "folder") {
            context.getString(R.string.kr_please_choose_folder)
        } else {
            context.getString(R.string.kr_please_choose_file)
        }

        inputLayout.apply {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            endIconDrawable = AppCompatResources.getDrawable(context, R.drawable.baseline_folder_24)
            setEndIconOnClickListener {
                fileChooser?.openFileChooser(object : FileSelectedInterface {
                    override fun onFileSelected(path: String?) {
                        if (path.isNullOrEmpty()) {
                            if (type() == FileSelectedInterface.TYPE_FOLDER) {
                                inputLayout.hint =
                                    context.getString(R.string.kr_please_choose_folder)
                            } else {
                                inputLayout.hint = context.getString(R.string.kr_please_choose_file)
                            }
                            editText.setText("")
                        } else {
                            editText.setText(path)
                        }
                    }

                    override fun mimeType(): String? {
                        if (actionParamInfo.mime.isNotEmpty()) {
                            return actionParamInfo.mime
                        }
                        return null
                    }

                    override fun suffix(): String? {
                        if (actionParamInfo.suffix.isNotEmpty()) {
                            return actionParamInfo.suffix
                        }
                        return null
                    }

                    override fun type(): Int {
                        return when (actionParamInfo.type) {
                            "folder" -> FileSelectedInterface.TYPE_FOLDER
                            else -> FileSelectedInterface.TYPE_FILE
                        }
                    }
                })
            }
        }

        if (actionParamInfo.valueFromShell != null) {
            editText.setText(actionParamInfo.valueFromShell)
        } else if (!actionParamInfo.value.isNullOrEmpty()) {
            editText.setText(actionParamInfo.value)
        }

        editText.tag = actionParamInfo.name

        return layout
    }
}
