package com.krscripts.core.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.krscripts.core.R
import com.krscripts.core.config.IconPathAnalysis
import com.krscripts.core.executor.ScriptEnvironment
import com.krscripts.core.model.SwitchNode
import java.util.Locale.getDefault

class ListItemSwitch(
    private val context: Context,
    private val config: SwitchNode
): ListItemView(context, R.layout.kr_switch_list_item, config) {

    private var switchView: MaterialSwitch? = layout.findViewById(R.id.kr_switch)
    private var onCheckedChangeListener: OnCheckedChangeListener? = null
    private var iconView: ImageView? = layout.findViewById(R.id.kr_icon)
    private var isAdjusting: Boolean = false

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
        isAdjusting = true
        switchView?.isChecked = config.checked
        isAdjusting = false
    }

    init {
        title = config.title
        desc = config.desc
        summary = config.summary

        switchView?.setOnCheckedChangeListener { _, isChecked ->
            if (!isAdjusting) {
                onCheckedChangeListener?.onCheckedChanged(this, isChecked)
            }
        }

        if (iconView != null) {
            iconView?.visibility = View.GONE
            if (config.iconPath.isNotEmpty()) {
                IconPathAnalysis().loadIcon(context, config)?.run {
                    iconView?.setImageDrawable(this)
                    iconView?.visibility = View.VISIBLE
                }
            }
        }
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(item: ListItemSwitch, isChecked: Boolean)
    }
}