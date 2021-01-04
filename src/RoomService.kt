package com.example

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

object RoomService {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val mutex = Mutex()

    private val lastRoomId = AtomicLong(0)
    private val rooms = mutableMapOf<RoomId, Room>()

    suspend fun joinRoom(connection: Connection): Room.PlayerSink = mutex.withLock {
        val roomId = connection.roomId
        return if (roomId == null) {
            // new room
            registerRoom().joinPlayer(connection)
        } else {
            // join existing room
            val room = rooms[connection.roomId] ?: throw RoomNotFoundException(connection.roomId)
            room.joinPlayer(connection)
        }
    }

    suspend fun checkIfRoomShouldBeCleared(room: Room) = mutex.withLock {
        if (!room.isEmpty()) {
            logger.info("Room $room should not be cleared because it is not empty")
            return
        }
        val removedRoom = rooms.remove(room.id)
        if (removedRoom == null) {
            logger.info("Tried to clear room $room but it was not in the Map")
        } else {
            logger.info("Room $room cleared")
        }
    }

    private fun registerRoom(): Room {
        return Room(lastRoomId.getAndIncrement()).also { rooms[it.id] = it }
    }
}

class RoomNotFoundException(id: RoomId) : IllegalArgumentException("Room with id=$id not found")
