package ru.spbstu.icpfc2021.solver

import ru.spbstu.icpfc2021.model.Figure
import ru.spbstu.icpfc2021.model.Point
import ru.spbstu.icpfc2021.model.Problem
import ru.spbstu.icpfc2021.model.checkCorrect
import ru.spbstu.icpfc2021.result.saveResult


fun dummySolution(problem: Problem) {
    if (problem.hole.size != problem.figure.vertices.size) return
    val xxx = search(emptyList(), problem.hole.toSet(), problem)
    println(problem.number)
    for (fig in xxx) {
        saveResult(problem, fig)
    }
}

fun search(points: List<Point>, unassigned: Set<Point>, problem: Problem): List<Figure> {
    if (unassigned.isEmpty()) {
        val newFigure = problem.figure.copy(vertices = points)
        if (checkCorrect(problem.figure, newFigure, problem.epsilon)) {
            return listOf(newFigure)
        } else {
            return emptyList()
        }
    }
    return unassigned.flatMap { p -> search(points + p, unassigned - p, problem) }
}