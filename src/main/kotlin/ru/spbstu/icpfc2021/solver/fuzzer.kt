package ru.spbstu.icpfc2021.solver

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import ru.spbstu.icpfc2021.gui.drawFigure
import ru.spbstu.icpfc2021.model.*
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.wheels.MapToSet
import ru.spbstu.wheels.ints
import java.io.File
import java.math.BigInteger
import javax.xml.crypto.Data
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

fun <T> List<Set<T>>.cartesian(): MutableList<PersistentList<T>> =
    fold(mutableListOf(persistentListOf())) { acc, set ->
        acc.flatMapTo(mutableListOf()) { list: PersistentList<T> ->
            set.map { element -> list.add(element) }
        }
    }

fun <T> Collection<MutableSet<T>>.intersectAll(): MutableSet<T> {
    if (isEmpty()) return mutableSetOf()
    val it = iterator()
    var current = it.next()
    for (set in it) {
        var other = set
        if (other.size > current.size) other = current.also { current = set }
        current.retainAll(other)
    }
    return current
}

class fuzzer(
    val problem: Problem,
    var currentFigure: Figure = problem.figure
) {

    val verifier = Verifier(problem)
    val holePoints = verifier.getHolePoints()

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
        return dataEdges.map {
            val el = it.calculateOriginal().squaredLength.big
            val neighbor = currentFigure.vertices[it.oppositeVertex(pi)]
            val candidates = abstractSquares[el]!!.mapTo(mutableSetOf()) { neighbor + it }
            candidates.retainAll { it in holePoints }
            candidates.retainAll { !verifier.check(Edge(it, neighbor)) }
            candidates.removeAll { it == currentPos }
            candidates
        }.intersectAll()
    }

    fun pickNeighbours(no: Int, seed: Int, res: MutableSet<Int> = mutableSetOf()): Set<Int> {
        val edges = nodesToEdges[seed]
        res += edges.shuffled().map { it.oppositeVertex(seed) }.take(no - res.size)
        if (res.size < no) pickNeighbours(no, res.random(), res)
        //while (res.size < no) {
            //
        //}
        return res
    }

    fun randomPoints(no: Int, seed: Int): List<Int> {
        return listOf(seed) + pickNeighbours(no - 1, seed)
    }

    fun multipointCandidates(pis: List<Int>): List<List<Point>> {
        val pisToSet = pis.toSet()

        val innerEdges = pis.flatMapTo(mutableSetOf()) { nodesToEdges[it] }
        innerEdges.retainAll {
            it.startIndex in pisToSet && it.endIndex in pisToSet
        }

        val personalSets: List<Set<Point>> = pis.map { pi ->
            val dataEdges = nodesToEdges[pi]

            val currentPos = currentFigure.vertices[pi]
            dataEdges.mapNotNull f@{
                val opposite = it.oppositeVertex(pi)
                if (opposite in pisToSet) return@f null

                val el = it.calculateOriginal().squaredLength.big
                val neighbor = currentFigure.vertices[it.oppositeVertex(pi)]
                val candidates = abstractSquares[el]!!.mapTo(mutableSetOf()) { neighbor + it }
                candidates.retainAll { it in holePoints }
                candidates.retainAll { !verifier.check(Edge(it, neighbor)) }
                candidates.removeAll { it == currentPos }
                candidates
            }.intersectAll()
        }
        if (personalSets.any { it.isEmpty() }) return emptyList()

        val cart = personalSets.cartesian()
        cart.retainAll { solution ->
            innerEdges.all { edge ->
                val ourStartIndex = pis.indexOf(edge.startIndex)
                val ourEndIndex = pis.indexOf(edge.endIndex)
                val calculated = Edge(solution[ourStartIndex], solution[ourEndIndex])
                !verifier.check(calculated)
                        && calculated.squaredLength.big.millions in problem.distanceToMillionsRange(edge.calculateOriginal().squaredLength.big)
            }
        }

        return cart
    }

    fun fuzz() {
        val numPoints = Random.nextInt(5) + 1
        val randomPoints = randomPoints(numPoints, currentFigure.vertices.indices.random())
        println("Fuzzer: picked points $randomPoints")
        val candidates = multipointCandidates(randomPoints).map { newPoint ->
            currentFigure.run {
                val acc = vertices.toMutableList()
                for ((ix, i) in randomPoints.withIndex()) {
                    acc[i] = newPoint[ix]
                }
                copy(vertices = acc)
            }
        }
        val baseline = dislikes(problem.hole, currentFigure.currentPose)
        val bestSol = candidates.map { it to dislikes(problem.hole, it.currentPose) }.minByOrNull { it.second }
        when {
            bestSol == null -> println("No solutions found =(")
            bestSol.second > baseline -> println("Cannot improve current solution")
            else -> currentFigure = bestSol.first
        }
    }

}

fun main(args: Array<String>) {
    val index = args[0].toInt()
    val problemJson = File("problems/$index.problem").readText()
    println("$index.problem")
    val problem = readProblem(index, problemJson)
    println(problem)
    val solutionJson = File("solutions/$index.sol").readText()
    val solution = readValue<Pose>(solutionJson)
    println("$index.sol")

    val startFigure = problem.figure.copy(vertices = solution.vertices)
    val fuzzer = fuzzer(problem, startFigure)
    val gui = drawFigure(problem, startFigure)
    while(true) {
        //System.`in`.bufferedReader().readLine()
        fuzzer.fuzz()
        gui.setFigure(fuzzer.currentFigure)
        gui.invokeRepaint()

        saveResult(problem, fuzzer.currentFigure)
    }
}