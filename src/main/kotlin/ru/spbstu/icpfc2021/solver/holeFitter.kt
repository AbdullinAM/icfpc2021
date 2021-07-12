package ru.spbstu.icpfc2021.solver

import ru.spbstu.icpfc2021.model.*
import ru.spbstu.wheels.defaultCopy
import ru.spbstu.wheels.toMutableMap
import java.io.File

fun main(args: Array<String>) {
    if (args.isNotEmpty()) {
        val index = args[0].toInt()
        val json = File("problems/$index.problem").readText()
        println("$index.problem")
        val problem = readProblem(index, json)
        println(problem)

        val hf = HoleFitter(problem)

        val res = hf.fit().toList()

        if (res.isNotEmpty()) println("Problem ${problem.number}: ${res.size}")

        return
    }

    for (i in 1..132) {
        val json = File("problems/$i.problem").readText()
        val problem = readProblem(i, json)

        val hf = HoleFitter(problem)

        val res = hf.fit().toList()

        if (res.isNotEmpty()) println("Problem ${problem.number}: ${res.size}")
    }
}

class HoleFitter(val problem: Problem) {

    val verifier = Verifier(problem)

    fun fit(): Sequence<List<Pair<DataEdge, Edge>>> = sequence {
        if (problem.number == 70) return@sequence

        val holeEdges = (problem.hole + problem.hole.first()).zipWithNext { a, b ->
            Edge(a, b)
        }

        val startingMatchingEdges = mutableMapOf<Edge, MutableSet<DataEdge>>()

        for (he in holeEdges) {
            val hl = he.squaredLength

            for (fe in problem.figure.edges) {
                val fee = fe.asEdge()
                val fl = fee.squaredLength

                if (areWithinEpsilon(fl, hl, problem.epsilon)) {
                    startingMatchingEdges.getOrPut(he) { mutableSetOf() }.add(fe)
                }
            }
        }

        for (start in holeEdges.indices) {

            var step = holeEdges.size
            val limit = 1

            while (step >= limit) {
                var curr = start
                var next = curr + step

                val paths = mutableSetOf<List<DataEdge>>()

                val matchingEdges = startingMatchingEdges.map { it.key to it.value.toMutableSet() }.toMutableMap()

                val currPiece = (holeEdges + holeEdges).subList(curr, next)
                findPath(currPiece, matchingEdges, mutableSetOf(), linkedSetOf(), paths)

                if (paths.isNotEmpty()) {
                    val res = mutableListOf<Pair<DataEdge, Edge>>()
                    val fixedVertexes = mutableSetOf<Int>()

                    val goodPath = paths.maxByOrNull { it.size }!!
                    matchingEdges.values.forEach { it.removeAll(goodPath) }
                    paths.clear()

                    res += goodPath.zip(currPiece)
                    fixedVertexes.addAll(goodPath.flatMap { listOf(it.startIndex, it.endIndex) })
                    while (curr < holeEdges.size) {
                        var inner_step = step
                        while (inner_step > limit) {
                            val currPiece = (holeEdges + holeEdges).subList(curr, curr+inner_step)
                            findPath(currPiece, matchingEdges, fixedVertexes, linkedSetOf(), paths)
                            if(paths.isNotEmpty()){
                                break
                            }
                            inner_step -= 1
                        }
                        if (paths.isNotEmpty()) {
                            val goodPath = paths.maxByOrNull { it.size }!!
                            matchingEdges.values.forEach { it.removeAll(goodPath) }
                            paths.clear()

                            res += goodPath.zip(currPiece)
                            fixedVertexes.addAll(goodPath.flatMap { listOf(it.startIndex, it.endIndex) })
                            curr += inner_step
                            next = curr + step
                            continue
                        }
                        curr = next
                        next = curr + step
                    }

                    yield(res)
                }

                step -= 1
            }
        }

        return@sequence
    }

    fun findPath(
        holeEdges: List<Edge>,
        matchingEdges: Map<Edge, Set<DataEdge>>,
        fixedVertexes: MutableSet<Int>,
        path: LinkedHashSet<DataEdge>,
        paths: MutableSet<List<DataEdge>>
    ) {
        if (holeEdges.isEmpty()) {
            paths.add(path.toList())
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
                when {
                    isOkFrom && from in fixedVertexes -> continue
                    isOkTo && to in fixedVertexes -> continue
                }

                val e = if (isOkFrom) matchingEdge else reverseMatchingEdge
                val v = if (isOkFrom) from else to

                path.add(e)
                fixedVertexes.add(v)

                findPath(newHoleEdges, matchingEdges, fixedVertexes, path, paths)

                path.remove(e)
                fixedVertexes.remove(v)
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
