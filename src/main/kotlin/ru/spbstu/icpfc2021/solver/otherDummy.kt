package ru.spbstu.icpfc2021.solver

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import ru.spbstu.icpfc2021.model.*
import ru.spbstu.ktuples.Tuple3
import ru.spbstu.wheels.MapToSet
import java.math.BigInteger

private val million = BigInteger.valueOf(1_000_000)
val Long.big get() = BigInteger.valueOf(this)
val Int.big get() = BigInteger.valueOf(this.toLong())
val BigInteger.millions get() = times(million)

class OtherDummySolver(
    val allHolePoints: Set<Point>,
    val problem: Problem
) {
    val verifier = Verifier(problem)
    val allPointsToEdges: MapToSet<Point, Edge> = MapToSet()
    val distancesToEdges = MapToSet<BigInteger, Edge>()
    val verticesToEdges = MapToSet<Int, DataEdge>()
    fun solve(): Figure {
        val ranking: MapToSet<Point, Edge> = MapToSet()
        for (edge in problem.figure.calculatedEdges) {
            ranking[edge.start] += edge
            ranking[edge.end] += edge
        }
        for (edge in problem.figure.edges) {
            verticesToEdges[edge.startIndex] += edge
            verticesToEdges[edge.endIndex] += edge
        }

        for (edge in problem.figure.calculatedEdges) {
            distancesToEdges[edge.squaredLength.big.millions] = mutableSetOf()
        }

        for (a in allHolePoints) {
            for (b in allHolePoints) if (a !== b) {
                val edge = Edge(a, b)
                if (verifier.check(edge)) {
                    continue
                }
                allPointsToEdges[edge.start] += edge
                allPointsToEdges[edge.end] += edge

                val delta = problem.epsilon.big * edge.squaredLength.big
                val distance = million * edge.squaredLength.big

                for (realDistance in distancesToEdges.inner.keys) {
                    if (realDistance in (distance - delta)..(distance + delta)) {
                        distancesToEdges[realDistance] += edge
                    }
                }
            }
        }

        println("Start search: ${distancesToEdges.inner.values.sumOf { it.size }}")

        val initialContext = Context(
            problem.figure.edges.toPersistentSet(),
            MutableList(problem.figure.vertices.size) { null }.toPersistentList()
        )
        val initialEdge = problem.figure.edges.maxByOrNull { it.calculate().squaredLength }!!
        val result = propagateEdge(initialEdge, initialContext.withEdge(initialEdge))
        if (result == null) error("PEZDA")
        return problem.figure.copy(vertices = result.assigment.toList() as List<Point>)
    }

    private fun DataEdge.calculate() =
        let { Edge(problem.figure.vertices[it.startIndex], problem.figure.vertices[it.endIndex]) }


    val cache: MutableSet<Tuple3<DataEdge, DataEdge, Point?>> = mutableSetOf()

    private fun propagateEdge(edge: DataEdge, ctx: Context): Context? {
//        println("edge = ${edge}")
//        println("point = ${point}")

        val assigmentIsComplete = ctx.assigment.all { it != null }
        if (assigmentIsComplete) {
            val fig = problem.figure.copy(vertices = ctx.assigment.toList() as List<Point>)
            if (!checkCorrect(problem.figure, fig, problem.epsilon)) {
//        println("Found incorrect assigment")
                return null
            }
            return ctx
        }

        var edgeCandidates = distancesToEdges[edge.calculate().squaredLength.big.millions]
        edgeCandidates =
            ctx.assigment[edge.startIndex]?.let { point -> edgeCandidates.filterTo(mutableSetOf()) { it.start == point || it.end == point } }
                ?: edgeCandidates
        edgeCandidates =
            ctx.assigment[edge.endIndex]?.let { point -> edgeCandidates.filterTo(mutableSetOf()) { it.start == point || it.end == point } }
                ?: edgeCandidates

        for (candidate in edgeCandidates) {
            var newCtx = checkOrCreateAssigment(edge.startIndex, candidate.start, ctx) ?: continue
            newCtx = checkOrCreateAssigment(edge.endIndex, candidate.end, newCtx) ?: continue
            val candidateEdges = listOf(edge.startIndex, edge.endIndex)
                .flatMap { p -> verticesToEdges[p] }
                .filter { it in ctx.edges }
            val assigmentIsIncomplete = newCtx.assigment.any { it == null }
            if (candidateEdges.isEmpty() && assigmentIsIncomplete) {
                println("Empty candidates")
                val nextEdge = newCtx.edges.firstOrNull()
                if (nextEdge != null) {
                    return propagateEdge(nextEdge, newCtx.withEdge(nextEdge))
                }
            }
            if (candidateEdges.isEmpty()) {
                if (assigmentIsIncomplete) error("Not connected vertices")
                val fig = problem.figure.copy(vertices = newCtx.assigment.toList() as List<Point>)
                if (!checkCorrect(problem.figure, fig, problem.epsilon)) {
                    println("Found incorrect assigment")
                    return null
                }
                return newCtx
            }
            val allEdgesPossible = candidateEdges.all { e ->
                distancesToEdges[e.calculate().squaredLength.big.millions].any {
                    it.start == candidate.start || it.end == candidate.start || it.start == candidate.end || it.end == candidate.end
                }
            }
            if (!allEdgesPossible) continue
            //val xcandidate = candidateEdges.maxByOrNull { it.first.calculate().squaredLength }!!
            val xcandidate =
                candidateEdges.minByOrNull { distancesToEdges[it.calculate().squaredLength.big.millions].size }!!
//            if (Tuple(xcandidate.first, edge, xcandidate.second) in cache) {
//                //println("ALREADY BEEN HERE!!!!")
//                continue
//            }
//            cache.add(Tuple(xcandidate.first, edge, xcandidate.second))
            val res = propagateEdge(xcandidate, newCtx.withEdge(xcandidate))
            if (res != null) return res
//            for(xcandidate in candidateEdges.sortedByDescending { it.first.calculate().squaredLength }){
//                val res = propagateEdge(xcandidate.first, newCtx.withEdge(xcandidate.first), xcandidate.second)
//                if (res != null) return res
//
//            }
        }
//        println("OBOSRATUSHKI")
        return null
    }

    private fun checkOrCreateAssigment(vertex: Int, point: Point, ctx: Context) =
        when (ctx.assigment[vertex]) {
            null -> ctx.assignVertex(vertex, point)
            point -> ctx
            else -> null
        }

    data class Context(val edges: PersistentSet<DataEdge>, val assigment: PersistentList<Point?>) {
        fun assignVertex(vertex: Int, point: Point) = Context(edges, assigment.set(vertex, point))
        fun withEdge(edge: DataEdge) = Context(edges.remove(edge), assigment)
    }

}