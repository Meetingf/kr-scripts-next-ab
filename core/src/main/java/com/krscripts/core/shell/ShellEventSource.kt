package com.krscripts.core.shell

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

enum class ShellLogType { OUTPUT, OUTPUT_ERROR, INPUT }

sealed interface ShellEvent {
    data class Started(val forceStop: Runnable?) : ShellEvent
    data class Exited(val payload: Any?) : ShellEvent
    data class Log(val type: ShellLogType, val text: String) : ShellEvent
}

class ShellEventSource {

    private val _events = Channel<ShellEvent>(capacity = Channel.UNLIMITED)
    val events: Flow<ShellEvent> = _events.receiveAsFlow()

    val progress = MutableStateFlow<Pair<Int, Int>?>(null)

    fun postStart(forceStop: Runnable?) {
        _events.trySend(ShellEvent.Started(forceStop))
    }

    fun postExit(payload: Any?) {
        _events.trySend(ShellEvent.Exited(payload))
    }

    fun postWrite(line: String) {
        _events.trySend(ShellEvent.Log(ShellLogType.INPUT, line))
    }

    fun postReadError(line: String) {
        _events.trySend(ShellEvent.Log(ShellLogType.OUTPUT_ERROR, line))
    }

    fun postRead(rawLine: String) {
        // 保留语义标记：\r 前缀=覆盖上一行(进度刷新段)，\n 后缀=该行结束
        val overwrite = rawLine.startsWith('\r')
        val newline = rawLine.endsWith('\n')
        val log = rawLine.trim()
        val match = PROGRESS_PATTERN.matchEntire(log)
        if (match != null) {
            val current = match.groupValues[1].toIntOrNull() ?: return
            val total = match.groupValues[2].toIntOrNull() ?: return
            progress.value = current to total
        } else {
            val sb = StringBuilder()
            if (overwrite) sb.append('\r')
            sb.append(log)
            if (newline) sb.append('\n')
            _events.trySend(ShellEvent.Log(ShellLogType.OUTPUT, sb.toString()))
        }
    }

    fun destroy() {
        _events.close()
    }

    companion object {
        private val PROGRESS_PATTERN = """^progress:\[(-?\d+)/(-?\d+)]$""".toRegex()
    }
}
