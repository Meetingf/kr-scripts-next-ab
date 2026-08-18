package com.krscripts.core.ui.param

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import com.krscripts.core.R
import com.krscripts.core.databinding.KrParamRowBinding
import com.krscripts.core.model.ActionParamInfo
import com.krscripts.core.model.SelectItem

class ParamLayoutRender(
    private var linearLayout: LinearLayout,
    private val context: FragmentActivity
) {
    private val renderers = mutableListOf<ParamRenderer>()

    companion object {

        // Label is hidden in these params
        private val HIDE_LABEL_TYPES = setOf("bool", "checkbox", "switch")

        /**
         * 获取当前选中项索引（单选）
         * @param actionParamInfo 参数信息
         * @param options 使用getParamOptions获得的数据（不为空时）
         */
        fun getParamOptionsCurrentIndex(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): Int {
            var selectedIndex = -1

            val valList = ArrayList<String>()
            if (actionParamInfo.valueFromShell != null)
                valList.add(actionParamInfo.valueFromShell!!)
            // TODO:这里可能有点争议
            if (actionParamInfo.value != null) {
                valList.add(actionParamInfo.value!!)
            }
            if (valList.isNotEmpty()) {
                for (j in valList.indices) {
                    for ((index, option) in options.withIndex()) {
                        if (option.value == valList[j]) {
                            selectedIndex = index
                            break
                        }
                    }
                    if (selectedIndex > -1)
                        break
                }
            }
            return selectedIndex
        }

        /**
         * 获取当前选中项索引（多选）
         * @param actionParamInfo 参数信息
         * @param options 使用getParamOptions获得的数据（不为空时）
         */
        fun getParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): BooleanArray {
            val status = BooleanArray(options.size)
            val values = getParamValues(actionParamInfo)

            options.forEachIndexed { index, item ->
                status[index] = (values != null && values.contains(item.value))
            }
            return status
        }

        /**
         * 设置列表的选中状态
         * @param actionParamInfo 参数信息
         * @param options 使用getParamOptions获得的数据（不为空时）
         */
        fun setParamOptionsSelectedStatus(actionParamInfo: ActionParamInfo, options: ArrayList<SelectItem>): ArrayList<SelectItem> {
            val values = getParamValues(actionParamInfo)

            for (element in options) {
                element.selected = (values != null && values.contains(element.value))
            }
            return options
        }

        // 获取多选下拉的选中值列表
        fun getParamValues(actionParamInfo: ActionParamInfo): List<String>? {
            val value = actionParamInfo.valueFromShell ?: actionParamInfo.value
            val values = value?.split(actionParamInfo.separator)
            return values
        }
    }

    fun renderList(actionParamInfos: ArrayList<ActionParamInfo>, fileChooser: FileChooserRender.FileChooserInterface?) {
        for (actionParamInfo in actionParamInfos) {
            val options = actionParamInfo.optionsFromShell
            val render: ParamRenderer = when(actionParamInfo.type) {
                // CheckBox
                "bool", "checkbox" -> CheckboxRender(actionParamInfo, context)
                // Switch
                "switch" -> SwitchRender(actionParamInfo, context)
                // SeekBar
                "seekbar" -> SliderRender(actionParamInfo, context)
                // FileSelector
                "file", "folder" -> FileChooserRender(actionParamInfo, context, fileChooser)
                // AppsSelector
                "app", "packages" -> AppChooserRender(actionParamInfo, context)
                // ColorPicker
                "color" -> ColorPickerRender(actionParamInfo, context)

                else -> {
                    if (options != null) {
                        // Selector
                        if (actionParamInfo.multiple) {
                            MultipleSelectRender(actionParamInfo, context)
                        } else {
                            SingleSelectRender(actionParamInfo, context)
                        }
                    } else {
                        // EditText
                        EditTextRender(actionParamInfo, context)
                    }
                }
            }

            addRender(actionParamInfo, render)
        }
    }

    private fun addRender(
        actionParamInfo: ActionParamInfo,
        render: ParamRenderer
    ) {
        val view = render.render()
        addToLayout(view, actionParamInfo)
        renderers.add(render)
    }

    private fun addToLayout(inputView: View, actionParamInfo: ActionParamInfo) {
        val binding = KrParamRowBinding.inflate(LayoutInflater.from(context))
        with(binding) {
            // title
            val title = actionParamInfo.title
            krParamTitle.isVisible = !title.isNullOrEmpty()
            krParamTitle.text = title.orEmpty()

            // label
            val label = actionParamInfo.label
            val showLabel = !label.isNullOrEmpty() && !HIDE_LABEL_TYPES.contains(actionParamInfo.type)
            krParamLabel.isVisible = showLabel
            krParamLabel.text = label.orEmpty()
            krParamLabelDivier.isVisible = showLabel

            // desc
            val desc = actionParamInfo.desc
            krParamDesc.isVisible = !desc.isNullOrEmpty()
            krParamDesc.text = desc.orEmpty()

            krParamInput.addView(inputView)
            linearLayout.addView(root)

            (inputView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun getFieldTips(actionParamInfo: ActionParamInfo): String {
        return buildString {
            if (!actionParamInfo.title.isNullOrEmpty()) {
                append(actionParamInfo.title)
                append(" ")
            }
            if (!actionParamInfo.label.isNullOrEmpty()) {
                append(actionParamInfo.label)
                append(" ")
            }
            append("(")
            append(actionParamInfo.name)
            append(") ")
        }
    }

    fun readParamsValue(): HashMap<String, String> {
        val params = HashMap<String, String>()
        for (renderer in renderers) {

            var value: String? = null
            try {
                value = renderer.getValue()
            } catch (e: Exception) {
                throw Exception(getFieldTips(renderer.actionParamInfo) + ": " + e.message)
            }

            val paramName = renderer.paramName

            if (value == null) continue

            if (value.isEmpty() && renderer.actionParamInfo.required) {
                throw Exception(getFieldTips(renderer.actionParamInfo) + context.getString(R.string.do_not_empty))
            } else {
                paramName?.let { name ->
                    params[name] = value
                    // It's out of responsibility of the function,
                    // but reserve that match the orign behavior
                    renderer.actionParamInfo.value = value
                }
            }
        }
        return params
    }
}