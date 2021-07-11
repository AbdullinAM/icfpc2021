package ru.spbstu.icpfc2021.solver

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import ru.spbstu.icpfc2021.gui.drawFigure
import ru.spbstu.icpfc2021.model.*
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.wheels.MapToSet
import java.io.File
import java.lang.Math.pow
import java.math.BigInteger
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
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
    var currentFigure: Figure = problem.figure,
    val constrainSearchSpace: Boolean = true,
    val strictlyLowerDislikes: Boolean = false,
    val maxVariantsPerSet: Int = 1000000,
    val invalidityMode: Boolean = false
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
            candidates.retainAll { verifier.check(Edge(it, neighbor)) == Verifier.Status.OK }
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

        val personalSets: MutableList<Set<Point>> = pis.mapTo(mutableListOf()) { pi ->
            val dataEdges = nodesToEdges[pi]

            val currentPos = currentFigure.vertices[pi]
            dataEdges.mapNotNull f@{
                val opposite = it.oppositeVertex(pi)
                if (opposite in pisToSet) return@f null

                val el = it.calculateOriginal().squaredLength.big
                val neighbor = currentFigure.vertices[it.oppositeVertex(pi)]
                val candidates = abstractSquares[el]!!.mapTo(mutableSetOf()) { neighbor + it }
                candidates.retainAll { it in holePoints }
                if (!invalidityMode)
                    candidates.retainAll { verifier.check(Edge(it, neighbor)) == Verifier.Status.OK }
                candidates.removeAll { it == currentPos }
                candidates
            }.intersectAll()
        }

        println("personalSets size = ${personalSets.map { it.size }}")
        if (personalSets.any { it.isEmpty() }) return emptyList()
        if (constrainSearchSpace) {
            for (i in personalSets.indices) {
                personalSets[i] = personalSets[i].shuffled().take(
                    pow(maxVariantsPerSet.toDouble(), 1.0 / pis.size).roundToInt()
                ).toSet()
            }
        }
        val cart = personalSets.cartesian()
        println("cart size = ${cart.sumOf { it.size }}")
        return cart.filter { solution ->
            innerEdges.all { edge ->
                val ourStartIndex = pis.indexOf(edge.startIndex)
                val ourEndIndex = pis.indexOf(edge.endIndex)
                val calculated = Edge(solution[ourStartIndex], solution[ourEndIndex])
                verifier.check(calculated) == Verifier.Status.OK
                        && calculated.squaredLength.big.millions in problem.distanceToMillionsRange(edge.calculateOriginal().squaredLength.big)
            }
        }
    }

    fun fuzz() {
        val numPoints = (Random.nextInt(minOf(8, currentFigure.vertices.size)) + 1)
        val seed: Int
        if (invalidityMode) {
            val invalidPoints = verifier.getInvalidEdges(currentFigure).flatMapTo(mutableSetOf()) { listOf(it.startIndex, it.endIndex) }
            seed = invalidPoints.random()
        } else seed = currentFigure.vertices.indices.random()
        val randomPoints = randomPoints(numPoints, seed).shuffled()
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
        val baseline = dislikes(problem.hole, currentFigure.currentPose) + verifier.countInvalidEdges(currentFigure)
        var bestSol = candidates.map {
            it to (dislikes(problem.hole, it.currentPose) + verifier.countInvalidEdges(it))
        }.minByOrNull { it.second }
        bestSol = when {
            invalidityMode -> bestSol
            bestSol != null && verifier.check(bestSol.first) == Verifier.Status.OK -> bestSol
            else -> null
        }
        when {
            bestSol == null -> println("No solutions found =(")
            bestSol.second > baseline -> {
                println("Cannot improve current solution")
                if(!strictlyLowerDislikes && bestSol.second.toDouble() - baseline < baseline/20.0) {
                    currentFigure = bestSol.first
                    println(currentFigure.currentPose.toJsonString())
                }
            }
            else -> {
                currentFigure = bestSol.first
                println(currentFigure.currentPose.toJsonString())
            }
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

    val fuzzer = fuzzer(problem, startFigure,
        strictlyLowerDislikes = args.contains("--strict"),
        invalidityMode = args.contains("--invalid"))
    if (args.contains("--no-gui")) {
        while(true) {
            fuzzer.fuzz()
            saveResult(problem, fuzzer.currentFigure)
        }
        return
    }
    val gui = drawFigure(problem, startFigure)
    while(true) {
        //System.`in`.bufferedReader().readLine()
        fuzzer.fuzz()
        gui.setFigure(fuzzer.currentFigure)
        gui.invokeRepaint()

        saveResult(problem, fuzzer.currentFigure)
    }
}