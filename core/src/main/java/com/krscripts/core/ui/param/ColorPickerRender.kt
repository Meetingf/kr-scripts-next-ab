package com.krscripts.core.ui.param

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.krscripts.core.R
import com.krscripts.core.model.ActionParamInfo
import com.krscripts.core.ui.dialog.DialogHelper

class ColorPickerRender(
    override var actionParamInfo: ActionParamInfo,
    private val context: Context
): ParamRenderer {

    private var editText: EditText? = null

    override fun getValue(): String? {
        try {
            return editText?.text?.toString()?.toColorInt()?.toString()
        } catch (_: Exception) {
            throw Exception(context.getString(R.string.kr_invalid_color))
        }
    }

    override fun render(): View {
        val layout = LayoutInflater.from(context).inflate(R.layout.kr_param_color, null)
        editText = layout.findViewById(R.id.kr_param_color_text)
        val invalidView = layout.findViewById<ImageView>(R.id.kr_param_color_invalid)
        val preview = layout.findViewById<View>(R.id.kr_param_color_preview)

        editText?.apply {
            tag = actionParamInfo.name
            addTextChangedListener(
                object : TextWatcher {
                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {}

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {}

                    override fun afterTextChanged(s: Editable?) {
                        updateColorPreview(editText!!, invalidView, preview, s!!.toString())
                    }
                }
            )
            if (actionParamInfo.valueFromShell != null) {
                setText(actionParamInfo.valueFromShell!!)
            } else if (actionParamInfo.value != null) {
                setText(actionParamInfo.value!!)
            }

            updateColorPreview(this, invalidView, preview, this.text.toString())
            layout.findViewById<View>(R.id.kr_param_color_picker).setOnClickListener {
                openColorPicker(this, invalidView, preview)
            }
        }

        return layout
    }

    private fun updateColorPreview(textView: TextView, invalidView: ImageView, preview: View, colorStr: String): Boolean {
        try {
            val color = colorStr.toColorInt()
            invalidView.visibility = View.GONE
            preview.visibility = View.VISIBLE
            preview.background = color.toDrawable()
            return true
        } catch (_: Exception) {
            invalidView.visibility = View.VISIBLE
            preview.visibility = View.GONE
            return false
        }
    }

    private fun currentColor(colorStr: CharSequence?): Int {
        if (!colorStr.isNullOrEmpty()) {
            try {
                return colorStr.toString().toColorInt()
            } catch (_: Exception) {
            }
        }
        return (0xff000000).toInt()
    }

    private fun openColorPicker(textView: TextView, invalidView: ImageView, preview: View) {
        val view = LayoutInflater.from(context).inflate(R.layout.kr_color_picker, null)
        val defValue = currentColor(textView.text)

        val alphaBar = view.findViewById<SeekBar>(R.id.color_alpha)
        val redBar = view.findViewById<SeekBar>(R.id.color_red)
        val greenBar = view.findViewById<SeekBar>(R.id.color_green)
        val blueBar = view.findViewById<SeekBar>(R.id.color_blue)
        val colorPreview = view.findViewById<Button>(R.id.color_preview)
        val colorPreviewText = view.findViewById<TextView>(R.id.color_preview_text)

        alphaBar.progress = Color.alpha(defValue)
        redBar.progress = Color.red(defValue)
        greenBar.progress = Color.green(defValue)
        blueBar.progress = Color.blue(defValue)
        colorPreview.setBackgroundColor(defValue)
        colorPreviewText.text = parseHexStr(alphaBar.progress, redBar.progress, greenBar.progress, blueBar.progress)

        val listener = object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val color = Color.argb(alphaBar.progress, redBar.progress, greenBar.progress, blueBar.progress)
                colorPreview.setBackgroundColor(color)
                colorPreviewText.text = "#" + color.toHexString().uppercase()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
        alphaBar.setOnSeekBarChangeListener(listener)
        redBar.setOnSeekBarChangeListener(listener)
        greenBar.setOnSeekBarChangeListener(listener)
        blueBar.setOnSeekBarChangeListener(listener)

        DialogHelper.animDialog(
            context, MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.kr_color_picker))
            .setView(view)
            .setPositiveButton(context.getString(R.string.btn_confirm)) { _, _ ->
                val color = Color.argb(
                    alphaBar.progress,
                    redBar.progress,
                    greenBar.progress,
                    blueBar.progress
                )
                colorPreview.setBackgroundColor(color)
                try {
                    textView.text = parseHexStr(
                        alphaBar.progress,
                        redBar.progress,
                        greenBar.progress,
                        blueBar.progress
                    )
                    invalidView.visibility = View.GONE
                    preview.background = color.toDrawable()
                } catch (ex: Exception) {
                }
                // Integer.toHexString(color) // "argb(${alphaBar.progress}, ${redBar.progress}, ${greenBar.progress}, ${blueBar.progress}, )"
            }
            .setNegativeButton(context.getString(R.string.btn_cancel)) { _, _ -> })
    }

    private fun parseHexStr(a: Int, r: Int, g: Int, b: Int): String {
        return String.format("#%02x%02x%02x%02x", a, r, g, b)
    }
}
