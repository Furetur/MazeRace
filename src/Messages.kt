package com.example

import kotlinx.serialization.Serializable

@Serializable
sealed class Action
@Serializable
sealed class LobbyAction : Action()
@Serializable
data class ReadyUpdate(val isReady: Boolean) : LobbyAction()
@Serializable
sealed class GameAction : Action()
@Serializable
data class PlayerInput(val direction: Direction) : GameAction()
@Serializable
sealed class Event
@Serializable
sealed class LobbyEvent : Event()
@Serializable
data class JoinedLobby(val myId: PlayerId, val lobby: LobbyState) : LobbyEvent()
@Serializable
data class PlayerJoinLobby(val playerId: PlayerId, val playerName: String) : LobbyEvent()
@Serializable
data class PlayerReadyEvent(val playerId: PlayerId, val isReady: Boolean) : LobbyEvent()
@Serializable
data class StartingGame(val mazeConfig: MazeConfig) : LobbyEvent()
@Serializable
sealed class GameEvent : Event()
@Serializable
data class GameStateUpdate(val playerStates: List<PlayerInGameState>) : GameEvent()
@Serializable
data class PlayerWon(val winner: PlayerId) : GameEvent()
@Serializable
data class PlayerDisconnected(val disconnectedPlayerId: PlayerId) : GameEvent()