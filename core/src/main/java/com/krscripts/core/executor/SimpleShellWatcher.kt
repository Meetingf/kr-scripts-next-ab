package com.krscripts.core.executor;

import android.content.Context
import com.krscripts.common.shell.ShellTranslation
import com.krscripts.core.model.ShellHandlerBase
import java.io.InputStream

class SimpleShellWatcher {
    /**
     * 设置日志处理Handler
     *
     * @param process          Runtime进程
     * @param shellHandlerBase ShellHandlerBase
     */
    fun setHandler(
        context: Context,
        process: Process,
        shellHandlerBase: ShellHandlerBase,
        onExit: Runnable?
    ) {
        val shellTranslation = ShellTranslation(context)

        fun readStream(stream: InputStream, eventType: Int): Thread = Thread {
            stream.bufferedReader().use { reader ->
                var line: String?
                try {
                    while ((reader.readLine().also { line = it }) != null) {
                        shellHandlerBase.sendMessage(
                            shellHandlerBase.obtainMessage(
                                eventType,
                                shellTranslation.resolveRow(line!!) + "\n"
                            )
                        )
                    }
                } catch (_: Exception) {

                }
            }
        }

        val reader = readStream(process.inputStream, ShellHandlerBase.EVENT_REDE)
        val readerError = readStream(process.errorStream, ShellHandlerBase.EVENT_READ_ERROR)

        val waitExit = Thread {
            var status = -1
            try {
                status = process.waitFor()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } finally {
                shellHandlerBase.sendMessage(
                    shellHandlerBase.obtainMessage(
                        ShellHandlerBase.EVENT_EXIT,
                        status
                    )
                )
                if (reader.isAlive) {
                    reader.interrupt()
                }
                if (readerError.isAlive) {
                    readerError.interrupt()
                }
                onExit?.run()
            }
        }

        reader.start()
        readerError.start()
        waitExit.start()
    }
}