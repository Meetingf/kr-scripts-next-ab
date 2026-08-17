package com.krscripts.core.ui.param

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.slider.RangeSlider
import com.krscripts.core.R
import com.krscripts.core.model.ActionParamInfo

class SeekBarRender(
    override var actionParamInfo: ActionParamInfo,
    private var context: Context
): ParamRenderer {
    private var rangeSlider: RangeSlider? = null

    override fun getValue(): String? {
        return rangeSlider?.values?.firstOrNull()?.toInt()?.toString()
    }

    override fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_seekbar, null)
        rangeSlider = layout.findViewById(R.id.kr_param_seekbar)

        val minValue = actionParamInfo.min.toFloat()
        val maxValue = actionParamInfo.max.toFloat()

        rangeSlider?.run {
            valueFrom = minValue
            valueTo = maxValue
            stepSize = 1.0f

            val initialValue = getInitialValue(minValue, maxValue)
            values = listOf(initialValue)
            tag = actionParamInfo.name

            val minusBtn = layout.findViewById<ImageButton>(R.id.kr_param_seekbar_minus)
            val plusBtn = layout.findViewById<ImageButton>(R.id.kr_param_seekbar_plus)
            val textView = layout.findViewById<TextView>(R.id.kr_param_seekbar_value)
            textView.text = formatValue(initialValue)

            addOnChangeListener { _, value, _ ->
                textView.text = formatValue(value)
            }

            minusBtn.setOnClickListener {
                val current = values[0]
                if (current > minValue) {
                    values = listOf(current - 1)
                }
            }
            plusBtn.setOnClickListener {
                val current = values[0]
                if (current < maxValue) {
                    values = listOf(current + 1)
                }
            }
        }

        return layout
    }

    private fun getInitialValue(min: Float, max: Float): Float {
        val raw = actionParamInfo.valueFromShell ?: actionParamInfo.value
        val intValue = raw?.toIntOrNull() ?: return min
        return intValue.toFloat().coerceIn(min, max)
    }

    private fun formatValue(value: Float): String {
        return value.toInt().toString()
    }
}
