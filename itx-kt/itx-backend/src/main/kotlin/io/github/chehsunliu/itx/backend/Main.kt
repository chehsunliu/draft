package io.github.chehsunliu.itx.backend

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import sun.misc.Signal

private data class Args(
    val host: String,
    val port: Int,
) {
    companion object {
        fun parse(argv: Array<String>): Args {
            var host = "127.0.0.1"
            var port = 8080
            var i = 0
            while (i < argv.size) {
                when (argv[i]) {
                    "--host" -> {
                        host = argv[++i]
                    }
                    "--port" -> {
                        port = argv[++i].toInt()
                    }
                    "--log-level" -> {
                        System.setProperty("itx.log.level", argv[++i])
                    }
                    else -> {}
                }
                i++
            }
            return Args(host, port)
        }
    }
}

fun main(argv: Array<String>) {
    val args = Args.parse(argv)
    val state = AppState.fromEnv()
    val engine = embeddedServer(Netty, host = args.host, port = args.port) { itxModule(state) }

    // The integration test driver asserts proc.wait() == 0 on SIGINT. Default JVM behavior
    // exits 130 on signal, so trap SIGINT/SIGTERM, drain the engine, and exit cleanly.
    val shutdown = {
        runCatching { engine.stop(gracePeriodMillis = 1_000, timeoutMillis = 5_000) }
        runCatching { state.close() }
        Runtime.getRuntime().halt(0)
    }
    Signal.handle(Signal("INT")) { shutdown() }
    Signal.handle(Signal("TERM")) { shutdown() }

    // wait = true blocks the main thread until the engine stops, which keeps the JVM alive even
    // when Netty's worker threads are daemons.
    engine.start(wait = true)
}
