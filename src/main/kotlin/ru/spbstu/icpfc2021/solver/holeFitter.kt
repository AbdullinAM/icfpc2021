package ru.spbstu.icpfc2021.solver

import ru.spbstu.icpfc2021.model.*
import java.io.File

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val index = args[0].toInt()
        val json = File("problems/$index.problem").readText()
        println("$index.problem")
        val problem = readProblem(index, json)
        println(problem)

        val hf = HoleFitter(problem)

        hf.fit()

        return
    }

    for (i in 1..132) {
        val json = File("problems/$i.problem").readText()
        val problem = readProblem(i, json)

        val hf = HoleFitter(problem)

        hf.fit()
    }
}

class HoleFitter(val problem: Problem) {

    val verifier = Verifier(problem)

    fun fit(): List<List<Pair<DataEdge, Edge>>> {
        val holeEdges = (problem.hole + problem.hole.first()).zipWithNext { a, b ->
            Edge(a, b)
        }

        val matchingEdges = mutableMapOf<Edge, MutableSet<DataEdge>>()

        for (he in holeEdges) {
            val hl = he.squaredLength

            for (fe in problem.figure.edges) {
                val fee = fe.asEdge()
                val fl = fee.squaredLength

                if (areWithinEpsilon(fl, hl, problem.epsilon)) {
                    matchingEdges.getOrPut(he) { mutableSetOf() }.add(fe)
                }
            }
        }

        val res = mutableListOf<List<Pair<DataEdge, Edge>>>()

        val paths = mutableSetOf<LinkedHashSet<DataEdge>>()

        var step = holeEdges.size
        val limit = 3

        while (step > limit) {
            val currPiece = holeEdges.subList(0, step)
            findPath(currPiece, matchingEdges, linkedSetOf(), paths)

            if (paths.isNotEmpty()) {
                val goodPath = paths.random()
                matchingEdges.values.forEach { it.removeAll(goodPath) }
                paths.clear()

                res += goodPath.zip(currPiece)

                var curr = step
                var next = curr + curr

                while (next <= holeEdges.size) {
                    val currPiece = holeEdges.subList(0, step)
                    findPath(holeEdges.subList(curr, next), matchingEdges, linkedSetOf(), paths)

                    if (paths.isNotEmpty()) {
                        val goodPath = paths.random()
                        matchingEdges.values.forEach { it.removeAll(goodPath) }
                        paths.clear()

                        res += goodPath.zip(currPiece)
                    }

                    curr = next
                    next = curr + step
                }

                break
            }

            step /= 2
        }

        res.removeIf { it.isEmpty() }

        if (res.isNotEmpty()) println("Problem ${problem.number}: $res")

        return res
    }

    fun findPath(
        holeEdges: List<Edge>,
        matchingEdges: Map<Edge, Set<DataEdge>>,
        path: LinkedHashSet<DataEdge>,
        paths: MutableSet<LinkedHashSet<DataEdge>>
    ) {
        if (holeEdges.isEmpty()) {
            paths.add(path)
            return
        }

        val nextEdge = holeEdges.first()
        val newHoleEdges = holeEdges.drop(1)

        for (matchingEdge in matchingEdges[nextEdge] ?: emptySet()) {
            val (from, to) = matchingEdge
            val reverseMatchingEdge = DataEdge(to, from)

            if (matchingEdge in path || reverseMatchingEdge in path) continue

            val isOkFrom = (path.lastOrNull()?.endIndex ?: from) == from
            val isOkTo = (path.lastOrNull()?.endIndex ?: to) == to

            if (isOkFrom || isOkTo) {
                path.add(if (isOkFrom) matchingEdge else reverseMatchingEdge)

                findPath(newHoleEdges, matchingEdges, path, paths)

                path.remove(matchingEdge)
            }
        }

        return
    }

    fun mkPair(a: Int, b: Int): Pair<Int, Int> = if (a < b) a to b else b to a

    fun DataEdge.mkPair(): Pair<Int, Int> = mkPair(startIndex, endIndex)

    fun DataEdge.asEdge(): Edge {
        return with(problem.figure) {
            Edge(vertices[startIndex], vertices[endIndex])
        }
    }

    val DataEdge.squaredLength: Long
        get() {
            return asEdge().squaredLength
        }

    fun <A, B> Pair<A, B>.reversed() = second to first

}
