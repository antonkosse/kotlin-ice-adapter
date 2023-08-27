package com.faforever.ice.game

import com.faforever.ice.IceAdapterDiedException
import com.faforever.ice.IceOptions
import com.faforever.ice.util.ReusableComponent
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.Closeable
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

private val logger = KotlinLogging.logger {}

class LobbyConnectionProxy(
    iceOptions: IceOptions
) : ReusableComponent, Closeable {
    private val lobbyPort = iceOptions.lobbyPort

    private val inQueue: BlockingQueue<ByteArray> = ArrayBlockingQueue(32, true)

    private val objectLock = Object()
    private var closing: Boolean = false
    private var socket: DatagramSocket? = null
    private var gameWriterThread: Thread? = null

    fun sendData(data: ByteArray) = inQueue.add(data)

    override fun start() {
        synchronized(objectLock) {
            closing = false
            inQueue.clear()

            socket = try {
                DatagramSocket(lobbyPort)
            } catch (e: IOException) {
                logger.error(e) { "Couldn't start LobbyConnectionProxy on port $lobbyPort" }
                throw IceAdapterDiedException("Couldn't start LobbyConnectionProxy on port $lobbyPort", e)
            }
        }

        setupSocketToGameInstance()

        logger.info { "LobbyConnectionProxy started" }
    }

    private fun setupSocketToGameInstance() {
        // A while loop seems unnecessary overhead, we do not accept multiple connections from different games,
        // nor do we expect multiple connection attempts from the same game
        try {
            val socket = requireNotNull(socket)

            gameWriterThread = Thread(
                { writeMessagesFromQueue(socket) }, "writeLobbyDataToGame"
            ).apply { start() }

            logger.info { "Connection to game instance established" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to establish connection to game instance" }
            close()
            throw IceAdapterDiedException("Failed to establish connection to game instance", e)
        }
    }

    private fun writeMessagesFromQueue(socket: DatagramSocket) {
        logger.debug { "Ready to write messages" }

        while (!closing) {
            val data = inQueue.take()
            val packet =
                DatagramPacket(data, 0, data.size - 1, InetAddress.getLoopbackAddress(), lobbyPort)
                socket.send(packet)
        }
    }

    override fun stop() {
        close()
    }

    override fun close() {
        logger.debug { "LobbyConnectionProxy closing" }

        synchronized(objectLock) {
            closing = true
            gameWriterThread?.apply {
                interrupt()
                logger.debug { "writeLobbyDataToGame interrupted" }
            } ?: run {
                logger.warn { "No writeLobbyDataToGame available for closing" }
            }

            socket?.close()
        }

        logger.info { "LobbyConnectionProxy closed" }
    }
}