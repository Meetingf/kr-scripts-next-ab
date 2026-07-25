package com.krscripts.core.ui

import android.content.Context
import com.google.android.material.materialswitch.MaterialSwitch
import com.krscripts.core.R
import com.krscripts.core.executor.ScriptEnvironment
import com.krscripts.core.model.SwitchNode
import java.util.Locale.getDefault

class ListItemSwitch(
    private val context: Context,
    private val config: SwitchNode
): ListItemView(context, R.layout.kr_switch_list_item, config) {

    var switchView: MaterialSwitch? = layout.findViewById(R.id.kr_switch)
    private var onCheckedChangeListener: OnCheckedChangeListener? = null

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener): ListItemSwitch {
        this.onCheckedChangeListener = listener
        return this
    }

    override fun updateViewByShell() {
        super.updateViewByShell()

        if (config.getState.isNotEmpty()) {
            val shellResult = ScriptEnvironment.executeResultRoot(context, config.getState, config)
            config.checked = shellResult == "1" || shellResult.lowercase(getDefault()) == "true"
        }
    }

    init {
        title = config.title
        desc = config.desc
        summary = config.summary

        switchView?.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChangeListener?.onCheckedChanged(this, isChecked)
        }
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(item: ListItemSwitch, isChecked: Boolean)
    }
}