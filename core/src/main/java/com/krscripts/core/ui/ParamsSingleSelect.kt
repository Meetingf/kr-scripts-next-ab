package com.krscripts.core.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.FragmentActivity
import com.krscripts.core.R
import com.krscripts.core.model.ActionParamInfo
import com.krscripts.core.model.SelectItem

class ParamsSingleSelect(
        private var actionParamInfo: ActionParamInfo,
        private var context: FragmentActivity
) {
    val options = actionParamInfo.optionsFromShell!!
    var selectedIndex = ActionParamsLayoutRender.getParamOptionsCurrentIndex(actionParamInfo, options) // 获取当前选中项索引

    fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_spinner, null)
        val spinner = layout.findViewById<KrSpinner>(R.id.kr_param_spinner)

        spinner.run {
            onDialogOpen = {
                openSingleSelectDialog { setSelection(it) }
            }
            showMenuAsDialog = options.size >= 5
            tag = actionParamInfo.name

            if (!actionParamInfo.value.isNullOrEmpty() && selectedIndex > -1 && selectedIndex < options.size) {
                setSelection(selectedIndex)
            } else {
                println("ree")
            }

            adapter = ArrayAdapter(context, R.layout.kr_spinner_default, R.id.text, options).apply {
                setDropDownViewResource(R.layout.kr_spinner_dropdown)
            }
            isEnabled = !actionParamInfo.readonly
        }

        return layout
    }

    private fun openSingleSelectDialog(
        onConfirm: (Int) -> Unit
    ) {
        DialogItemChooser(ArrayList(options.mapIndexed{ index, item->
            SelectItem().apply {
                title = item.title
                selected = index == selectedIndex
            }
        }), false, object : DialogItemChooser.Callback {
            override fun onConfirm(selected: List<SelectItem>, status: BooleanArray) {
                selectedIndex = status.indexOf(true)
                onConfirm(selectedIndex)
            }
        }).show(context.supportFragmentManager, "params-single-select")
    }
}
