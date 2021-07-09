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

}

fun List<Point>.toArea(): Area {
    val poly = Polygon()

    for (p in this) {
        poly.addPoint(p.x * 100, p.y * 100)
    }

    return Area(poly)
}
