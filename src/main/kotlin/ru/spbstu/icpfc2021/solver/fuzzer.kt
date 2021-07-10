package ru.spbstu.icpfc2021.solver

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import ru.spbstu.icpfc2021.model.*
import ru.spbstu.wheels.MapToSet
import java.math.BigInteger
import javax.xml.crypto.Data
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

fun <T> List<Set<T>>.cartesian(): Set<PersistentList<T>> =
    fold(setOf(persistentListOf())) { acc, set ->
        acc.flatMapTo(mutableSetOf()) { list: PersistentList<T> ->
            set.map { element -> list.add(element) }
        }
    }

class fuzzer(
    val problem: Problem
) {

    val verifier = Verifier(problem)
    val holePoints = verifier.getHolePoints()

    var currentFigure: Figure = problem.figure

    val nodesToEdges: MapToSet<Int, DataEdge> = MapToSet()
    init {
        for (edge in problem.figure.edges) {
            nodesToEdges[edge.startIndex] += edge
            nodesToEdges[edge.endIndex] += edge
        }
    }
    val edgeSet: Set<DataEdge> = problem.figure.edges.toSet()

    val abstractSquares: Map<BigInteger, MutableSet<Point>>
    init {
        abstractSquares = problem.figure.calculatedEdges.mapTo(mutableSetOf()) { it.squaredLength.big }
            .associateWith { realDistance ->
                val result = mutableSetOf<Point>()
                val delta = problem.epsilon.big * realDistance
                val distance = realDistance.millions

                val range = (distance - delta)..(distance + delta)

                val outerRadius = ceil(sqrt(((delta + distance) / million).toDouble())).toInt() + 10
                val innerRadius = floor(sqrt((((distance - delta).toDouble() / sqrt(2.0)) / million.toDouble()))).toInt() - 10

                for (x in innerRadius..outerRadius) {
                    for (y in -outerRadius..outerRadius) {
                        val edgeLen = Edge(Point(0, 0), Point(x, y)).squaredLength.big.millions
                        if (edgeLen !in range) continue

                        result += Point(x, y)
                        result += Point(-x, y)
                    }
                }

                for (x in -outerRadius..outerRadius) {
                    for (y in innerRadius..outerRadius) {
                        val edgeLen = Edge(Point(0, 0), Point(x, y)).squaredLength.big.millions
                        if (edgeLen !in range) continue

                        result += Point(x, y)
                        result += Point(x, -y)
                    }
                }

                result
            }
    }

    fun DataEdge.calculateOriginal() = Edge(problem.figure.vertices[startIndex], problem.figure.vertices[endIndex])
    fun DataEdge.calculateCurrent() = Edge(currentFigure.vertices[startIndex], currentFigure.vertices[endIndex])

    fun onePointCandidates(pi: Int): Set<Point> {
        val dataEdges = nodesToEdges[pi]

        val currentPos = currentFigure.vertices[pi]
        return dataEdges.mapNotNull {
            val el = it.calculateOriginal().squaredLength.big
            val neighbor = currentFigure.vertices[it.oppositeVertex(pi)]
            val candidates = abstractSquares[el]!!.mapTo(mutableSetOf()) { neighbor + it }
            candidates.retainAll { it in holePoints }
            candidates.retainAll { !verifier.check(Edge(it, neighbor)) }
            candidates.removeAll { it == currentPos }
            candidates.takeIf { it.isNotEmpty() }
        }.reduce { a: Set<Point>, b: Set<Point> -> a intersect b }
    }

    fun multipointCandidates(pis: List<Int>): Set<List<Point>> {
        val pisToSet = pis.toSet()
        val res = mutableSetOf<List<Point>>()
        val personalSets: List<Set<Point>> = pis.map { pi ->
            val dataEdges = nodesToEdges[pi]

            val currentPos = currentFigure.vertices[pi]
            dataEdges.mapNotNull {
                val opposite = it.oppositeVertex(pi)
                if (opposite in pisToSet) return@mapNotNull null

                val el = it.calculateOriginal().squaredLength.big
                val neighbor = currentFigure.vertices[it.oppositeVertex(pi)]
                val candidates = abstractSquares[el]!!.mapTo(mutableSetOf()) { neighbor + it }
                candidates.retainAll { it in holePoints }
                candidates.retainAll { !verifier.check(Edge(it, neighbor)) }
                candidates.removeAll { it == currentPos }
                candidates.takeIf { it.isNotEmpty() }
            }.reduce { a: Set<Point>, b: Set<Point> -> a intersect b }
        }
        if (personalSets.any { it.isEmpty() }) return emptySet()

        return emptySet()
    }

}