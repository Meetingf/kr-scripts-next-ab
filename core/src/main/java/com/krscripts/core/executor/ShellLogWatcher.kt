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
                while (true) {
                    val ch = reader.read()
                    if (ch == -1) break
                    when (val c = ch.toChar()) {
                        '\n' -> {
                            if (buf.isNotEmpty()) {
                                onLine(shellTranslation.resolveRow(buf.toString()))
                                buf.setLength(0)
                            }
                        }
                        '\r' -> {
                            // \r\n 是换行；单独的 \r 是回车刷新（需保留到文本中）
                            reader.mark(1)
                            val next = reader.read()
                            if (next == -1) {
                                // 行尾为 \r，直接结束
                            } else if (next.toChar() == '\n') {
                                if (buf.isNotEmpty()) {
                                    onLine(shellTranslation.resolveRow(buf.toString()))
                                    buf.setLength(0)
                                }
                            } else {
                                reader.reset()
                                buf.append('\r')
                            }
                        }
                        else -> buf.append(c)
                    }
                }
                if (buf.isNotEmpty()) {
                    onLine(shellTranslation.resolveRow(buf.toString()))
                }
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