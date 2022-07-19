package com.github.redbadger

import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.SocketChannel

private const val NUMBER_OF_ATTEMPTS = 10

/**
 * Run the NATS messaging system server
 *
 * Useful for running unit or integration tests on the localhost
 *
 * The server must be in your path under the name `nats-server` or you must set the `NATS_SERVER_PATH` environment variable to point to it
 * ```kotlin
 * NatsServerRunner(1234).use{
 *   println("Server running on port: " + it.uri)
 *   val c = Nats.connect(it.uri);
 *   ...
 * }
 * ```
 *
 * Inspired by [java-nats-server-runner](https://github.com/nats-io/java-nats-server-runner), but corrected and simplified
 */
class NatsServerRunner(private val port: Int) : AutoCloseable {
    private val process: Process
    val uri get() = "nats://localhost:$port"

    init {
        val serverPath = System.getenv("NATS_SERVER_PATH") ?: "nats-server"
        this.process = ProcessBuilder(serverPath, "-p", port.toString())
            .redirectErrorStream(true)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        waitForSocketToBeAvailable(InetSocketAddress("localhost", port))
    }

    private fun waitForSocketToBeAvailable(address: SocketAddress) {
        for (i in 1..NUMBER_OF_ATTEMPTS) {
            try {
                SocketChannel.open(address).close()
                return
            } catch (_: Exception) {
            }
            Thread.sleep(100)
        }
        throw Exception("Could not connect to $address after after $NUMBER_OF_ATTEMPTS attempts")
    }

    override fun close() {
        process.destroy()
    }
}
