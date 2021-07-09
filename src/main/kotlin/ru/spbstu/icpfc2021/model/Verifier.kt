package ru.spbstu.icpfc2021.model

import java.awt.Polygon
import java.awt.geom.Area

class Verifier(val problem: Problem) {

    enum class Status {
        OVERLAP, EDGE_VIOLATION, OK
    }

    val awtHole by lazy {
        problem.hole.toArea()
    }

    fun check(figure: Figure): Status {
        val awtFigure = figure.vertices.toArea()

        awtFigure.subtract(awtHole)

        return when {
            !awtFigure.isEmpty -> Status.OVERLAP
            !checkCorrect(problem.figure, figure, problem.epsilon) -> Status.EDGE_VIOLATION
            else -> Status.OK
        }
    }

    fun isOutOfBounds(p: Point): Boolean {
        return !awtHole.contains(p.x * 100.0, p.y * 100.0)
    }

    fun getHolePoints(): List<Point> {
        val xRange = (problem.hole.minByOrNull { it.x }?.x ?: 0)..(problem.hole.maxByOrNull { it.x }?.x ?: 0)
        val yRange = (problem.hole.minByOrNull { it.y }?.y ?: 0)..(problem.hole.maxByOrNull { it.y }?.y ?: 0)
        val result = mutableListOf<Point>()
        for (i in xRange) {
            for (j in yRange) {
                val pt = Point(i, j)
                if (!isOutOfBounds(pt)) {
                    result += pt
                }
            }
        }
        return result
    }

}

fun List<Point>.toArea(): Area {
    val poly = Polygon()

    for (p in this) {
        poly.addPoint(p.x * 100, p.y * 100)
    }

    return Area(poly)
}
