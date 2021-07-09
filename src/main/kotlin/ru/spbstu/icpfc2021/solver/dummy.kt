package ru.spbstu.icpfc2021.solver

import ru.spbstu.icpfc2021.model.Figure
import ru.spbstu.icpfc2021.model.Point
import ru.spbstu.icpfc2021.model.Problem
import ru.spbstu.icpfc2021.model.checkCorrect
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.ktuples.zip
import kotlin.math.absoluteValue


fun dummySolution(problem: Problem) {
    if ((problem.hole.size - problem.figure.vertices.size).absoluteValue > 7) return
    val figures = search(emptyList(), problem.hole.toSet(), problem, problem.figure.vertices.size)
    println(problem.number)
    for (fig in figures) {
        saveResult(problem, fig)
    }
}

fun search(points: List<Point>, unassigned: Set<Point>, problem: Problem, size: Int): Sequence<Figure> = sequence {
    if (points.size == size) {
        val newFigure = problem.figure.copy(vertices = points)
        if (checkCorrect(problem.figure, newFigure, problem.epsilon)) {
            yield(newFigure)
        }
        return@sequence
    }
    yieldAll(unassigned.flatMap { p -> search(points + p, unassigned - p, problem, size) })
    yieldAll(searchWithFreeVars(points, problem, size))
}


fun searchWithFreeVars(
    points: List<Point>,
    problem: Problem,
    size: Int
): Sequence<Figure> {
    val numberOfFreePoints = size - points.size
    if (numberOfFreePoints == 0 || numberOfFreePoints > 3) return emptySequence()
    val figuresWithFreePoints = findPossibleFreePoints(points, problem)
    if (figuresWithFreePoints.isEmpty()) return emptySequence()
    val bboxXl = problem.hole.minOf { it.x }
    val bboxYl = problem.hole.minOf { it.y }
    val bboxXr = problem.hole.maxOf { it.x }
    val bboxYr = problem.hole.maxOf { it.y }
    println("Start free search: problem ${problem.number} | ${figuresWithFreePoints.size}")
    return sequence {
        figuresWithFreePoints.flatMap { fig ->
            searchInBbox(
                fig.vertices,
                problem,
                size,
                bboxXl,
                bboxXr,
                bboxYl,
                bboxYr
            )
        }
    }
}

private val freePoint = Point(-100, -100)
fun findPossibleFreePoints(points: List<Point>, problem: Problem): List<Figure> {
    val originEdges = problem.figure.calculatedEdges
    val freePointsNumber = problem.figure.vertices.size - points.size
    val possiblePointIndices = pointIndexVariants(freePointsNumber, problem.figure.vertices.size - 1)
        .filter { it.size == freePointsNumber }
    val withFreePoints = possiblePointIndices.map { variant ->
        val pointsIt = points.iterator()
        val result = MutableList(problem.figure.vertices.size) { i ->
            if (i in variant) freePoint
            else pointsIt.next()
        }
        check(result.size == problem.figure.vertices.size)
        result
    }
    val result = mutableListOf<Figure>()
    val possibleFigures = withFreePoints.map { problem.figure.copy(vertices = it) }
    for (fig in possibleFigures) {
        val edges = fig.calculatedEdges
        val isCorrect = zip(originEdges, edges).all { (a, b) ->
            if (b.start == freePoint || b.end == freePoint) return@all true
            checkCorrect(a, b, problem.epsilon)
        }
        if (isCorrect) {
            result.add(fig)
        }
    }
    return result
}

fun pointIndexVariants(depth: Int, size: Int): List<Set<Int>> {
    if (depth == 0) return (0..size).map { setOf(it) }
    val nested = pointIndexVariants(depth - 1, size)
    return (0..size).flatMap { i -> nested.map { it + i } }
}

fun searchInBbox(
    points: List<Point>,
    problem: Problem,
    size: Int,
    bboxXl: Int,
    bboxXr: Int,
    bboxYl: Int,
    bboxYr: Int
): Sequence<Figure> = sequence {
    val indexOfFreePoint = points.indexOfFirst { it == freePoint }
    if (indexOfFreePoint == -1) {
        val figure = problem.figure.copy(vertices = points)
        if (checkCorrect(problem.figure, figure, problem.epsilon)) {
            yield(figure)
        }
        return@sequence
    }
    val newPoints = points.toMutableList()
    for (x in bboxXl..bboxXr) {
        for (y in bboxYl..bboxYr) {
            val point = Point(x, y)
            newPoints[indexOfFreePoint] = point
            yieldAll(searchInBbox(newPoints, problem, size, bboxXl, bboxXr, bboxYl, bboxYr))
        }
    }
}