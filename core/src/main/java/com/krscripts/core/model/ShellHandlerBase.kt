package com.krscripts.core.model

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan

/**
 * Created by Hello on 2018/04/01.
 * Refactored by buylan on 2026/07/26
 */

abstract class ShellHandlerBase : Handler(Looper.getMainLooper()) {
    protected abstract fun onProgress(current: Int, total: Int)

    protected abstract fun onStart(msg: Any?)

    abstract fun onStart(forceStop: Runnable?)

    protected abstract fun onExit(msg: Any?)

    /**
     * 输出格式化内容
     *
     * @param msg
     */
    protected abstract fun updateLog(msg: SpannableString?)

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            EVENT_EXIT -> onExit(msg.obj)
            EVENT_START -> onStart(msg.obj)
            EVENT_READ -> onReaderMsg(msg.obj)
            EVENT_READ_ERROR -> onError(msg.obj)
            EVENT_WRITE -> onWrite(msg.obj)
        }
    }

    protected fun onReaderMsg(msg: Any?) {
        if (msg != null) {
            val log = msg.toString().trim()
            val match = PROGRESS_PATTERN.matchEntire(log)
            if (match != null) {
                val current = match.groupValues[1].toInt()
                val total = match.groupValues[2].toInt()
                onProgress(current, total)
            } else {
                onReader(msg)
            }
        }
    }

    protected open fun onReader(msg: Any?) {
        updateLog(msg, "#00cc55")
    }

    protected open fun onWrite(msg: Any?) {
        updateLog(msg, "#808080")
    }

    protected open fun onError(msg: Any?) {
        updateLog(msg, "#ff0000")
    }

    /**
     * 输出指定颜色的内容
     *
     * @param msg
     * @param color
     */
    protected fun updateLog(msg: Any?, color: String?) {
        if (msg != null) {
            updateLog(msg, Color.parseColor(color))
        }
    }

    protected fun updateLog(msg: Any?, color: Int) {
        if (msg != null) {
            val msgStr = msg.toString()
            val spannableString = SpannableString(msgStr)
            spannableString.setSpan(
                ForegroundColorSpan(color),
                0,
                msgStr.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            updateLog(spannableString)
        }
    }

    companion object {
        const val EVENT_START: Int = 0
        const val EVENT_READ: Int = 2
        const val EVENT_READ_ERROR: Int = 4
        const val EVENT_WRITE: Int = 6
        const val EVENT_EXIT: Int = -2
        private val PROGRESS_PATTERN = """^progress:\[(-?\d+)/(-?\d+)]$""".toRegex()
    }
}
