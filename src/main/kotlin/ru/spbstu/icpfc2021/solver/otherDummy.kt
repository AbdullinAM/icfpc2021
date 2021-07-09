package ru.spbstu.icpfc2021.solver

import ru.spbstu.icpfc2021.model.*
import ru.spbstu.wheels.MDMap
import ru.spbstu.wheels.MapToSet
import java.math.BigInteger

private val million = BigInteger.valueOf(1_000_000)
val Long.big get() = BigInteger.valueOf(this)
val Int.big get() = BigInteger.valueOf(this.toLong())

class OtherDummySolver(val allHolePoints: Set<Point>,
                       val problem: Problem) {
    val verifier = Verifier(problem)
    fun solve() {
        val ranking: MapToSet<Point, Edge> = MapToSet()
        for (edge in problem.figure.calculatedEdges) {
            ranking[edge.start] += edge
            ranking[edge.end] += edge
        }

        val allPointsToEdges: MapToSet<Point, Edge> = MapToSet()
        for (a in allHolePoints) {
            for (b in allHolePoints) if (a !== b) {
                val edge = Edge(a, b)
                if (!verifier.check(edge)) continue
                allPointsToEdges[edge.start] += edge
                allPointsToEdges[edge.end] += edge

                val max = million * edge.squaredLength.big + problem.epsilon.big * edge.squaredLength.big
                val min = problem.epsilon.big * edge.squaredLength.big - million * edge.squaredLength.big


            }
        }
    }
}