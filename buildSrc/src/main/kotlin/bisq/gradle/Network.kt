package bisq.gradle

import java.io.IOException
import java.net.ServerSocket

object Network {
    fun isPortFree(port: Int): Boolean =
        try {
            val server = ServerSocket(port)
            server.close()
            true
        } catch (e: IOException) {
            false
        }
}