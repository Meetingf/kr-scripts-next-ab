package com.krscripts.core.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import com.krscripts.core.R
import com.krscripts.core.executor.ScriptEnvironment
import com.krscripts.core.model.SwitchNode
import java.util.Locale.getDefault

class ListItemSwitch(
    private val context: Context,
    private val config: SwitchNode
): ListItemView(context, R.layout.kr_action_list_item, config) {

    private var switchView: MaterialSwitch? = layout.findViewById(R.id.kr_switch)
    private var onCheckedChangeListener: OnCheckedChangeListener? = null
    private var iconView: ShapeableImageView? = layout.findViewById(R.id.kr_icon)
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

        switchView?.isChecked = config.checked

        switchView?.setOnCheckedChangeListener { _, isChecked ->
            if (!isAdjusting) {
                onCheckedChangeListener?.onCheckedChanged(this, isChecked)
            }
        }

        switchView?.visibility = View.VISIBLE
        layout.findViewById<ImageView>(R.id.kr_widget).visibility = View.GONE

        iconView?.apply {
            IconHelper.applyIcon(
                context = context,
                view = this,
                iconPath = config.iconPath,
                configPath = config.pageConfigDir,
                clip = config.iconClip,
            )
        }
    }

    interface OnCheckedChangeListener {
        fun onCheckedChanged(item: ListItemSwitch, isChecked: Boolean)
    }
}