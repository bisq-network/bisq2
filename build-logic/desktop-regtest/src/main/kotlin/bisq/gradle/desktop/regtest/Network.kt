package bisq.gradle.desktop.regtest

import java.io.IOException
import java.net.ServerSocket
import java.util.*

object Network {
    fun isPortFree(port: Int): Boolean =
        try {
            val server = ServerSocket(port)
            server.close()
            true
        } catch (e: IOException) {
            false
        }

    fun findFreeSystemPort(): Int {
        return try {
            val server = ServerSocket(0)
            val port = server.localPort
            server.close()
            port
        } catch (ignored: IOException) {
            Random().nextInt(10000) + 50000
        }
    }
}