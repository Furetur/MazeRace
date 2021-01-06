package com.example

import kotlinx.serialization.Serializable
@Serializable
data class Position(val x: Int, val y: Int) {
    fun move(direction: Direction): Position {
        return when (direction) {
            Direction.UP -> Position(x, y - 1)
            Direction.RIGHT -> Position(x + 1, y)
            Direction.DOWN -> Position(x, y + 1)
            Direction.LEFT -> Position(x - 1, y)
        }
    }
}
@Serializable
enum class Direction {
    UP,
    RIGHT,
    DOWN,
    LEFT
}
@Serializable
enum class MazeCell {
    EMPTY,
    WALL
}
@Serializable
data class MazeConfig(
    val map: List<List<MazeCell>>,
    val start: Position,
    val finish: Position
)

class Maze(private val config: MazeConfig) {
    private val map = config.map

    init {
        require(map.isNotEmpty()) { "Data should be not empty" }
        val firstRowSize = map.first().size
        require(map.all { it.size == firstRowSize }) { "Data should be a rectangular matrix" }
    }

    private val yRange = map.indices

    private val xRange = map[0].indices

    fun canMoveHere(position: Position): Boolean =
        position.y in yRange && position.x in xRange && map[position.y][position.x] == MazeCell.EMPTY

    fun spawnCharacter() = Character()

    inner class Character {
        var position: Position = config.start; private set

        val isOnFinishingPosition; get() = position == config.finish
        fun move(direction: Direction) {
            val newPosition = position.move(direction)
            if (canMoveHere(newPosition)) {
                position = newPosition
            } else {
                throw InvalidMoveDirectionException(position)
            }
        }
    }
}

class InvalidMoveDirectionException(position: Position) : IllegalStateException("Cannot move to $position")

fun generateMaze(): MazeConfig {
    val mazeGenerator = MazeGenerator(MAZE_SIZE)
    return mazeGenerator.generateMaze()
}