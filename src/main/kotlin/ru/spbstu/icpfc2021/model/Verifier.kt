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

}

fun List<Point>.toArea(): Area {
    val poly = Polygon()

    for (p in this) {
        poly.addPoint(p.x, p.y)
    }

    return Area(poly)
}
