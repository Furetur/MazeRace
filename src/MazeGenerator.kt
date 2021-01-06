package com.example

import java.util.Stack
import kotlin.random.Random

class MazeGenerator(private val dimension: Int) {
    private val stack = Stack<Position>()
    private val maze: MutableList<MutableList<MazeCell>> =
        MutableList(dimension) { MutableList(dimension) { MazeCell.WALL } }

    private val coordinateRange = 0 until dimension

    // 1 path, 0 wall

    fun generateMaze(): MazeConfig {
        val start = Position(0, 0)
        stack.push(start)
        while (!stack.empty()) {
            val cur = stack.pop()
            if (cur.valid()) {
                maze[cur.y][cur.x] = MazeCell.EMPTY
                randomlyAddCellsToStack(cur.neighbours())
            }
        }
        val finish = coordinateRange.map { Position(it, coordinateRange.last) }.filter { it.isEmpty() }.random()
        return MazeConfig(maze, start, finish)
    }

    /**
     * Returns all cells in the maze that are located on a square 'ring' around the given cell
     * x x x
     * x 0 x
     * x x x
     */
    private fun Position.ringAround(): List<Position> {
        val circle = mutableListOf<Position>()
        val xNeighborhoodRange = x - 1..x + 1
        val yNeighborhoodRange = y - 1..y + 1

        for (curY in yNeighborhoodRange) {
            for (curX in xNeighborhoodRange) {
                val currentPosition = Position(curX, curY)

                if (currentPosition.isOnGrid() && currentPosition != this) {
                    circle.add(currentPosition)
                }
            }
        }
        return circle
    }

    private fun Position.valid(): Boolean {
        if (isEmpty()) {
            return false
        }
        val neighbouringEmptyCells = ringAround().filter { it.isEmpty() }.count()
        return neighbouringEmptyCells < 3
    }

    private fun randomlyAddCellsToStack(cells: List<Position>) {
        val mutableCells = cells.toMutableList()
        while (mutableCells.isNotEmpty()) {
            val randomIndex = Random.Default.nextInt(mutableCells.size)
            val cell = mutableCells.removeAt(randomIndex)
            stack.push(cell)
        }
    }

    /**
     * Returns all cells in the maze that share a side with the given cell
     * . x .
     * x 0 x
     * . x .
     */
    private fun Position.neighbours(): List<Position> = ringAround().filter { it.x == x || it.y == y }

    private fun Position.isOnGrid(): Boolean = x in coordinateRange && y in coordinateRange

    private fun Position.isEmpty() = maze[y][x] == MazeCell.EMPTY
}
