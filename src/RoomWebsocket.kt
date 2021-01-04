package com.example

import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

val logger = LoggerFactory.getLogger("RoomWebsocket")

val lastConnectionId = AtomicLong(0)

fun Routing.roomWebsocket() {
    webSocket(LOBBY_WS_ENDPOINT) {
        val connection = Connection(lastConnectionId.getAndIncrement(), this)
        logger.info("New connection $connection")
        val player = try {
            RoomService.joinRoom(connection)
        } catch (e: FullLobbyException) {
            logger.info("Player could not join the lobby because it was full")
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Full lobby"))
            return@webSocket
        } catch (e: RoomNotFoundException) {
            logger.info("Player could not join the lobby because it was not found")
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Lobby Not Found"))
            return@webSocket
        }
        logger.info("Player $player joined room ${player.room}")
        connection.sendMessage(JoinedLobby(connection.id, player.room.getInLobbyState()))
        try {
            for (frame in incoming) {
                player.handleFrame(frame)
            }
        } catch (e: SerializationException) {
            logger.info("Could not deserialize action $e")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid message format"))
        } catch (e: IllegalArgumentException) {
            logger.info("Could not deserialize action $e")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid message format"))
        } catch (e: IllegalActionException) {
            logger.info("Action sent by $player was illegal: $e")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, e.message ?: ""))
        } finally {
            logger.info("Player $player disconnected")
            player.handlePlayerDisconnect()
            RoomService.checkIfRoomShouldBeCleared(player.room)
        }
    }
}

private suspend fun Room.PlayerSink.handleFrame(frame: Frame) {
    if (frame is Frame.Text) {
        val action = Json.decodeFromString<Action>(frame.readText())
        try {
            logger.info("Player $this sent an action $action")
            doAction(action)
        } catch (e: InvalidMoveDirectionException) {
            logger.info("Player tried to move in invalid direction $e")
        }
    }
}

class Connection(val id: Long, private val session: DefaultWebSocketServerSession) {
    val name = "player$id"
    val roomId = session.call.request.queryParameters[ROOM_ID_QUERY_KEY]?.toLongOrNull()

    suspend fun sendMessage(event: Event) {
        logger.info("Sending $event to $this")
        session.send(Json.encodeToString(event))
    }

    suspend fun disconnectOnGameEnd() {
        logger.info("Disconnecting $this because of game end")
        session.close(CloseReason(CloseReason.Codes.NORMAL, "Game Ended"))
    }

    override fun toString(): String = "Connection(id=$id, name=$name)"
}
