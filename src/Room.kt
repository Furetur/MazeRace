package com.example

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlin.IllegalStateException

typealias RoomId = Long

typealias PlayerId = Long

@Serializable
data class PlayerInLobbyState(val id: PlayerId, val name: String, val isReady: Boolean)

@Serializable
data class LobbyState(val roomId: RoomId, val players: List<PlayerInLobbyState>)

@Serializable
data class PlayerInGameState(val id: PlayerId, val position: Position)

class Room(val id: RoomId) {
    private val mutex = Mutex()

    private val players = mutableListOf<Player>()
    private lateinit var maze: Maze

    suspend fun isEmpty() = mutex.withLock { players.isEmpty() }

    private val lobbyState = object : RoomState() {
        override suspend fun handleAction(action: Action, from: Player) {
            if (action !is LobbyAction) throw IllegalActionException(action)
            val event = from.doAction(action)
            notifyAll(event)
            if (action is ReadyUpdate && players.size == PLAYERS_NUMBER && players.all { it.isReady }) {
                startGame()
            }
        }

        private suspend fun startGame() {
            val mazeConfig = generateMaze()
            maze = Maze(mazeConfig)
            state = inGameState
            for (player in players) {
                player.handleGameStart(maze.spawnCharacter())
            }
            notifyAll(StartingGame(mazeConfig))
        }
    }

    private val inGameState = object : RoomState() {
        override suspend fun handleAction(action: Action, from: Player) {
            if (action !is GameAction) throw IllegalActionException(action)
            if (action is PlayerInput) {
                val event = from.doAction(action)
                notifyAll(event)
                checkWinner()
            }
        }

        private suspend fun checkWinner() {
            val winner = players.find { it.isOnFinishingPosition } ?: return
            endGame(winner)
        }

        private suspend fun endGame(winner: Player) {
            state = endedState
            notifyAll(PlayerWon(winner.id))
            for (player in players) {
                player.disconnectOnGameEnd()
            }
        }
    }

    private val endedState = object : RoomState() {
        override suspend fun handleAction(action: Action, from: Player) {
            throw IllegalActionException(action)
        }
    }

    private var state: RoomState = lobbyState

    suspend fun joinPlayer(connection: Connection): PlayerSink = mutex.withLock {
        if (players.size >= PLAYERS_NUMBER) throw FullLobbyException()
        val player = Player(connection).also { players.add(it) }
        notifyAll(PlayerJoinLobby(player.id, player.name))
        return PlayerSink(player.id)
    }

    private fun getJoinedPlayer(playerId: PlayerId) =
        players.find { it.id == playerId } ?: throw PlayerNotInRoomException()

    private suspend fun handlePlayerDisconnect(playerSink: PlayerSink) {
        val player = players.find { it.id == playerSink.id } ?: return
        players.remove(player)
        state = endedState
        notifyAll(PlayerDisconnected(player.id))
    }

    private suspend fun notifyAll(event: Event) {
        for (player in players) {
            player.notify(event)
        }
    }

    suspend fun getInLobbyState(): LobbyState = mutex.withLock { LobbyState(id, players.map { it.getInLobbyState() }) }

    private fun getInGameState(): GameStateUpdate {
        return GameStateUpdate(players.map { it.inGameState })
    }

    override fun toString(): String = "Room(id=$id)"

    private abstract inner class RoomState {
        abstract suspend fun handleAction(action: Action, from: Player)
    }

    inner class PlayerSink(val id: PlayerId) {
        val room = this@Room

        private fun getPlayer() = getJoinedPlayer(id)

        suspend fun doAction(action: Action) = mutex.withLock {
            state.handleAction(action, getPlayer())
        }

        suspend fun handlePlayerDisconnect() = mutex.withLock {
            handlePlayerDisconnect(this)
        }

        override fun toString(): String = "PlayerSink(id=$id)"
    }

    private inner class Player(val connection: Connection) {
        val id = connection.id
        val name = connection.name

        var isReady = false

        lateinit var character: Maze.Character

        val position; get() = character.position
        val inGameState; get() = PlayerInGameState(id, position)
        val isOnFinishingPosition; get() = character.isOnFinishingPosition

        fun getInLobbyState(): PlayerInLobbyState = PlayerInLobbyState(id, name, isReady)

        fun doAction(action: LobbyAction): Event {
            return if (action is ReadyUpdate) {
                isReady = action.isReady
                PlayerReadyEvent(id, isReady)
            } else {
                throw IllegalActionException(action)
            }
        }

        fun doAction(action: GameAction): Event {
            return if (action is PlayerInput) {
                character.move(action.direction)
                getInGameState()
            } else {
                throw IllegalActionException(action)
            }
        }

        fun handleGameStart(character: Maze.Character) {
            this.character = character
        }

        suspend fun notify(event: Event) {
            connection.sendMessage(event)
        }

        suspend fun disconnectOnGameEnd() = connection.disconnectOnGameEnd()

        override fun toString(): String = "Player(id=$id, name=$name)"
    }
}

class FullLobbyException : IllegalStateException("The lobby is full")

class IllegalActionException(action: Action) : IllegalStateException("Action $action is illegal at the current state")

class PlayerNotInRoomException : IllegalStateException("Player not in room")
