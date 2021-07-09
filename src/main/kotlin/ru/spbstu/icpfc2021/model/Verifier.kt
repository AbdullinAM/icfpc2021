package ru.spbstu.icpfc2021.model

import java.awt.Polygon
import java.awt.geom.Area

class Verifier(val hole: Hole) {

    val awtHole by lazy {
        hole.toArea()
    }

    fun check(figure: Figure): Boolean {
        val awtFigure = figure.vertices.toArea()

        awtFigure.subtract(awtHole)

        return awtFigure.isEmpty
    }

}

fun Polygon.contains(p: Point): Boolean = contains(p.x, p.y)

fun List<Point>.toArea(): Area {
    val poly = Polygon()

    for (p in this) {
        poly.addPoint(p.x, p.y)
    }

    return Area(poly)
}
