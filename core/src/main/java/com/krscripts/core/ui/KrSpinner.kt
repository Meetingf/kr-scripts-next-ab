package com.krscripts.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSpinner

class KrSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatSpinner(context, attrs) {

    var onDialogOpen: (() -> Unit)? = null
    var showMenuAsDialog: Boolean = false

    override fun performClick(): Boolean {
        if (showMenuAsDialog) {
            onDialogOpen?.invoke()
            return true
        } else {
            return super.performClick()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (showMenuAsDialog) {
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    val inBounds = event.x >= 0f &&
                            event.x <= width &&
                            event.y >= 0f &&
                            event.y <= height
                    isPressed = inBounds
                    true
                }

                else -> super.onTouchEvent(event)
            }
        } else {
            super.onTouchEvent(event)
        }
    }
}