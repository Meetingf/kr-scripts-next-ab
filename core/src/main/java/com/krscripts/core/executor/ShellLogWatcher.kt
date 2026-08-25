package com.krscripts.core.executor;

import android.content.Context
import com.krscripts.core.shell.ShellEventSource
import com.krscripts.core.shell.ShellTranslation
import java.io.InputStream

object ShellLogWatcher {
    fun setWatcher(
        context: Context,
        process: Process,
        shellEventSource: ShellEventSource,
        onExit: Runnable?
    ) {
        val shellTranslation = ShellTranslation(context)

        fun readStream(
            stream: InputStream,
            onLine: (String) -> Unit
        ): Thread = Thread {
            // 不能用 readLine()：它会把 \r 当作行分隔符，导致脚本用 \r 刷新的进度百分比
            // 被拆成多个独立事件而无法原地刷新。这里逐字符读取，只以 \n 切行并保留 \r。
            val reader = stream.bufferedReader()
            try {
                val line = StringBuilder()
                var overwrite = false
                // emit：把当前累积的行发给上层。\r 前缀表示"覆盖上一行"（进度刷新段），
                // \n 后缀表示该行在此结束。这样每一段进度（1%…100%）都会覆盖当前行逐步更新。
                fun emit(withNewline: Boolean) {
                    if (line.isEmpty() && !withNewline) return
                    val body = line.toString()
                    line.setLength(0)
                    val content = (if (overwrite) "\r" else "") + body + (if (withNewline) "\n" else "")
                    overwrite = false
                    onLine(shellTranslation.resolveRow(content))
                }
                while (true) {
                    val ch = reader.read()
                    if (ch == -1) break
                    when (val c = ch.toChar()) {
                        '\n' -> emit(true)
                        // 单独的 \r 表示回车刷新：先发当前行，并标记后续内容为覆盖段。
                        // \r\n 则只是 Windows 换行。
                        '\r' -> {
                            reader.mark(1)
                            val next = reader.read()
                            if (next == -1) {
                                emit(false)
                            } else if (next.toChar() == '\n') {
                                emit(true)
                            } else {
                                reader.reset()
                                emit(false)
                                overwrite = true
                            }
                        }
                        else -> line.append(c)
                    }
                }
                emit(false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val reader = readStream(process.inputStream, shellEventSource::postRead)
        val readerError = readStream(process.errorStream, shellEventSource::postReadError)

        val waitExit = Thread {
            var status = -1
            try {
                status = process.waitFor()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {

                try {
                    reader.join()
                    readerError.join()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }

                shellEventSource.postExit(status)
                onExit?.run()
            }
        }

        reader.start()
        readerError.start()
        waitExit.start()
    }
}