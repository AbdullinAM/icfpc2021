package ru.spbstu.icpfc2021.model

import java.awt.Polygon
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D

val GOLDEN_RATIO = 0.25

val midPointCoeffs = listOf(GOLDEN_RATIO, 0.5, 1 - GOLDEN_RATIO)

class Verifier(val problem: Problem) {

    enum class Status {
        OVERLAP, EDGE_VIOLATION, OK
    }

    val awtHole by lazy {
        problem.hole.toPoly()
    }

    val holeVertexes by lazy {
        problem.hole.toSet()
    }

    val holeSides by lazy {
        awtHole.sides().toList()
    }



    fun check(edge: Edge): Status {
        val awtLine = Line2D.Double(edge.start, edge.end)

        if (hasIntersections(holeSides, awtLine))
            return Status.EDGE_VIOLATION

        for (c in midPointCoeffs) {
            val midPoint = Point2D.Double(
                awtLine.x1 + c * (awtLine.x2 - awtLine.x1),
                awtLine.y1 + c * (awtLine.y2 - awtLine.y1)
            )

            if (isOutOfBounds(midPoint))
                return Status.EDGE_VIOLATION
        }

        return Status.OK
    }

    fun countInvalidEdges(figure: Figure): Int =
        problem.figure.calculatedEdges.zip(figure.calculatedEdges).count { (from, to) ->
            check(to) != Status.OK || !checkCorrect(from, to, problem.epsilon)
        }

    fun getInvalidEdges(figure: Figure): List<DataEdge> =
        problem.figure.edges.zip(figure.calculatedEdges).filter { (data, to) ->
            val from = Edge(problem.figure.vertices[data.startIndex], problem.figure.vertices[data.endIndex])
            check(to) != Status.OK || !checkCorrect(from, to, problem.epsilon)
        }.map { it.first }

    fun check(figure: Figure): Status {
        for (edge in figure.calculatedEdges) {
            val awtLine = Line2D.Double(edge.start, edge.end)

            if (hasIntersections(holeSides, awtLine))
                return Status.OVERLAP

            for (c in midPointCoeffs) {
                val midPoint = Point2D.Double(
                    awtLine.x1 + c * (awtLine.x2 - awtLine.x1),
                    awtLine.y1 + c * (awtLine.y2 - awtLine.y1)
                )

                if (isOutOfBounds(midPoint))
                    return Status.OVERLAP
            }
        }

        return when {
            !checkCorrect(problem.figure, figure, problem.epsilon) -> Status.EDGE_VIOLATION
            else -> Status.OK
        }
    }

    fun isOutOfBounds(p2d: Point2D): Boolean {
        val p = Point(p2d.x.toInt(), p2d.y.toInt())

        if (p in holeVertexes) return false

        for (side in holeSides) {
            val rCCW = Line2D.relativeCCW(side.x1, side.y1, side.x2, side.y2, p2d.x, p2d.y)

            if (rCCW == 0) return false
        }

        return !awtHole.contains(p2d.x, p2d.y)
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

fun List<Point>.toArea(): Area = Area(this.toPoly())

fun List<Point>.toPoly(): Polygon {
    val poly = Polygon()

    for (p in this) {
        poly.addPoint(p.x, p.y)
    }

    return poly
}

fun Polygon.sides(): Sequence<Line2D.Double> = sequence {
    val polyIt = this@sides.getPathIterator(null) //Getting an iterator along the polygon path

    val coords = DoubleArray(6) //Double array with length 6 needed by iterator
    val firstCoords = DoubleArray(2) //First point (needed for closing polygon path)
    val lastCoords = DoubleArray(2) //Previously visited point

    polyIt.currentSegment(firstCoords) //Getting the first coordinate pair
    lastCoords[0] = firstCoords[0] //Priming the previous coordinate pair
    lastCoords[1] = firstCoords[1]
    polyIt.next()

    while (!polyIt.isDone) {
        val type = polyIt.currentSegment(coords)

        val currentLine: Line2D.Double

        when (type) {
            PathIterator.SEG_LINETO -> {
                currentLine = Line2D.Double(lastCoords[0], lastCoords[1], coords[0], coords[1])

                lastCoords[0] = coords[0]
                lastCoords[1] = coords[1]
            }
            PathIterator.SEG_CLOSE -> {
                currentLine = Line2D.Double(coords[0], coords[1], firstCoords[0], firstCoords[1])
            }
            else -> {
                throw Exception("Unsupported PathIterator segment type")
            }
        }

        yield(currentLine)

        polyIt.next()
    }
}

fun Polygon.hasIntersections(line: Line2D.Double): Boolean = hasIntersections(this.sides().toList(), line)

fun hasIntersections(sides: List<Line2D.Double>, line: Line2D.Double): Boolean {
    for (currentLine in sides) {
        val rCCW01 = Line2D.relativeCCW(
            currentLine.x1, currentLine.y1,
            currentLine.x2, currentLine.y2,
            line.x1, line.y1
        )

        val rCCW02 = Line2D.relativeCCW(
            currentLine.x1, currentLine.y1,
            currentLine.x2, currentLine.y2,
            line.x2, line.y2
        )

        val rCCW03 = Line2D.relativeCCW(
            line.x1, line.y1,
            line.x2, line.y2,
            currentLine.x1, currentLine.y1
        )

        val rCCW04 = Line2D.relativeCCW(
            line.x1, line.y1,
            line.x2, line.y2,
            currentLine.x2, currentLine.y2
        )

        if (currentLine.p1 == line.p1 ||
            currentLine.p1 == line.p2 ||
            currentLine.p2 == line.p1 ||
            currentLine.p2 == line.p2 ||
            rCCW01 * rCCW02 * rCCW03 * rCCW04 == 0
        ) {
            // skip
        } else if (currentLine.intersectsLine(line)) {
            return true
        }
    }
    return false
}
