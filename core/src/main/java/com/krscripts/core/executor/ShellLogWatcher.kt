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
                val buf = StringBuilder()
                // flush：把当前累积的行发给上层。普通行以换行(\n)提交；\r 刷新行不带换行，供覆盖当前行。
                fun flush(withNewline: Boolean) {
                    if (buf.isEmpty() && !withNewline) return
                    val line = buf.toString()
                    buf.setLength(0)
                    onLine(shellTranslation.resolveRow(if (withNewline) line + "\n" else line))
                }
                while (true) {
                    val ch = reader.read()
                    if (ch == -1) break
                    when (val c = ch.toChar()) {
                        '\n' -> flush(true)
                        // \r\n 是换行；单独的 \r 是回车刷新——遇到即把当前行发出去覆盖，
                        // 从而让进度百分比逐段刷新（1%、2%、…、100%）
                        '\r' -> {
                            reader.mark(1)
                            val next = reader.read()
                            if (next == -1) {
                                flush(false)
                            } else if (next.toChar() == '\n') {
                                flush(true)
                            } else {
                                reader.reset()
                                flush(false)
                            }
                        }
                        else -> buf.append(c)
                    }
                }
                flush(false)
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