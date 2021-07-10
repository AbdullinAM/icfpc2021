package ru.spbstu.icpfc2021.solver

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import ru.spbstu.icpfc2021.gui.*
import ru.spbstu.icpfc2021.model.*
import ru.spbstu.icpfc2021.result.loadSolution
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.ktuples.Tuple
import ru.spbstu.ktuples.Tuple3
import ru.spbstu.ktuples.Tuple4
import ru.spbstu.wheels.MDMap
import ru.spbstu.wheels.MapToSet
import ru.spbstu.wheels.out
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.geom.GeneralPath
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import javax.swing.JFrame
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

private val million = BigInteger.valueOf(1_000_000)
val Long.big get() = BigInteger.valueOf(this)
val Int.big get() = BigInteger.valueOf(this.toLong())
val BigInteger.millions get() = times(million)

class OtherDummySolver(
    val allHolePoints: Set<Point>,
    val problem: Problem,
    val findAllSolutions: Boolean = false
) {
    val verifier = Verifier(problem)
//    val canvas: TransformablePanel
    val overlays = mutableListOf<Pair<Drawable, Color>>()
    val allPointsToEdges: MapToSet<Point, Edge> = MapToSet()
//    val distancesToEdges: MDMap<BigInteger, MapToSet<Point, Edge>> = MDMap.withDefault { MapToSet() }
    lateinit var abstractSquares: Map<BigInteger, Set<Point>>

    fun Set<Int>.best(): Int? = maxByOrNull {
            v -> verticesToEdges[v].sumOf { it.calculate().squaredLength }
    }

    init {

//        val figure = problem.figure
//        val hole2DVertices = problem.hole.map { it.to2D() }
//        canvas = dumbCanvas(1000, 1000) {
//            withPaint(Color.GRAY.brighter().brighter()) {
//                val hole2D = GeneralPath()
//                hole2D.moveTo(hole2DVertices.first())
//                for (point in hole2DVertices.drop(1)) hole2D.lineTo(point)
//                hole2D.closePath()
//                fill(hole2D)
//            }
//
//            for (edge in figure.calculatedEdges) {
//                withPaint(Color.BLUE) {
//                    val line = java.awt.geom.Line2D.Double(edge.start, edge.end)
//                    draw(Drawable.Shape(BasicStroke(0.2f).createStrokedShape(line)))
//                }
//            }
//
//            for ((index, point) in figure.vertices.withIndex()) {
//                val color = if (verifier.isOutOfBounds(point)) Color.RED else Color.BLUE
//                withPaint(color) {
//                    fill(Ellipse2D(point, 2.0))
//                    drawString("$index", point.x, point.y)
//                }
//            }
//
//            for (b in problem.bonuses.orEmpty()) {
//                val color = when(b.bonus) {
//                    BonusType.GLOBALIST -> Color.YELLOW
//                    BonusType.BREAK_A_LEG -> Color.MAGENTA
//                }
//                withPaint(color) {
//                    fill(Ellipse2D(b.position, 2.0))
//                }
//                withPaint(Color.BLACK) {
//                    draw(Ellipse2D(b.position, 2.0))
//                }
//            }
//
//
//            for ((overlay, color) in overlays) {
//                withPaint(color) {
//                    draw(overlay)
//                }
//            }
//
//        }
//        canvas.scale(10.0)
//        canvas.translate(20.0, 20.0)
//        canvas.onKey("DOWN") {
//            val ty = 20.0 / canvas.transform.scaleY
//            canvas.translate(0.0, ty)
//        }
//        canvas.onKey("UP") {
//            val ty = -20.0 / canvas.transform.scaleY
//            canvas.translate(0.0, ty)
//        }
//        canvas.onKey("LEFT") {
//            val tx = -20.0 / canvas.transform.scaleX
//            canvas.translate(tx, 0.0)
//        }
//        canvas.onKey("RIGHT") {
//            val tx = 20.0 / canvas.transform.scaleX
//            canvas.translate(tx, 0.0)
//        }
//        canvas.onKey(KeyStroke.getKeyStroke('+', 0)) {
//            canvas.scale(1.1)
//        }
//        canvas.onKey(KeyStroke.getKeyStroke('-', 0)) {
//            canvas.scale(0.9)
//        }
//        canvas.onMouseWheel { e ->
//            val rot = e.preciseWheelRotation
//            val point = e.canvasPoint
//            val scale = 1.0 - rot * 0.1
//            canvas.zoomTo(point, scale)
//        }
//        canvas.onMousePan(filter = { SwingUtilities.isRightMouseButton(it) }) { _, prev, e ->
//            val st = prev.canvasPoint
//            val end = e.canvasPoint
//            canvas.translate(end.x - st.x, end.y - st.y)
//        }
//        dumbFrame(canvas).defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    }

    operator fun MapToSet<Point, Edge>.plusAssign(edge: Edge) {
        this[edge.start].add(edge)
        this[edge.end].add(edge)
    }
    operator fun MapToSet<Point, Edge>.contains(edge: Edge) =
        this[edge.start].contains(edge) || this[edge.end].contains(edge)

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

//        for (edge in problem.figure.calculatedEdges) {
//            distancesToEdges[edge.squaredLength.big.millions] = MapToSet()
//        }

        abstractSquares = problem.figure.calculatedEdges.mapTo(mutableSetOf()) { it.squaredLength.big }
            .associateWith { realDistance ->
            val result = mutableSetOf<Point>()
            val delta = problem.epsilon.big * realDistance
            val distance = realDistance.millions

            val range = (distance - delta)..(distance + delta)

            val outerRadius = ceil(sqrt(((delta + distance) / million).toDouble())).toInt() + 10
            val innerRadius = floor(sqrt((((distance - delta).toDouble() / sqrt(2.0)) / million.toDouble()))).toInt() - 10

            println("outerRadius = ${outerRadius}")
            println("innerRadius = ${innerRadius}")


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
//        dumbFrame(canvas).defaultCloseOperation = JFrame.EXIT_ON_CLOSE

//        println("NAHUYACHENO")
//
//        dumbCanvas {
//            withPaint(Color.MAGENTA) {
//                for (p in abstractSquares[abstractSquares.keys.random()]!!) {
//                    fill(Rectangle2D(p, 2.0))
//                }
//            }
//        }.apply {
//            translate(300.0, 300.0)
//            scale(10.0)
//
//            dumbFrame(this).defaultCloseOperation = JFrame.EXIT_ON_CLOSE
//            invokeRepaint()
//        }
//
//        println("NARISOVATO")


//        for ((i, a) in allHolePoints.withIndex()) {
//            for ((j, b) in allHolePoints.withIndex()) if (i < j) {
//                val edge = Edge(a, b)
//                val otherEdge = Edge(b, a)
//                if (verifier.check(edge) && verifier.check(otherEdge)) {
//                    continue
//                }
//                allPointsToEdges[edge.start] += edge
//                allPointsToEdges[edge.start] += otherEdge
//                allPointsToEdges[edge.end] += edge
//                allPointsToEdges[edge.end] += otherEdge
//
//                val delta = problem.epsilon.big * edge.squaredLength.big
//                val distance = million * edge.squaredLength.big
//
//                for (realDistance in distancesToEdges.inner.keys) {
//                    if (realDistance in (distance - delta)..(distance + delta)) {
//                        distancesToEdges[realDistance][edge.start] += edge
//                        distancesToEdges[realDistance][edge.end] += edge
//                        distancesToEdges[realDistance][otherEdge.start] += otherEdge
//                        distancesToEdges[realDistance][otherEdge.end] += otherEdge
//                    }
//                }
//            }
//        }
//
//        for (p in allHolePoints) {
//            for (x in problem.figure.vertices.indices) {
//                val isValid = verticesToEdges[x].all { e ->
//                    val emil = e.calculate().squaredLength.big.millions
//                    allPointsToEdges[p].any { distancesToEdges[emil].contains(it) }
//                }
//                if (isValid) validIndices[p] += x
//            }
//        }
//
//        repeat(3) {
//            val entriesCopy = validIndices.inner.entries.toSet()
//            entriesCopy.forEach { (p, ixs) ->
//                val toRemove = ixs.retainAll { ix ->
//                    verticesToEdges[ix].all { e ->
//                        val emil = e.calculate().squaredLength.big.millions
//                        allPointsToEdges[p].any {
//                            distancesToEdges[emil].contains(it)
//                                    && (e.endIndex in validIndices[it.start] && e.startIndex in validIndices[it.end]
//                                    || e.startIndex in validIndices[it.end] && e.startIndex in validIndices[it.start])
//                        }
//                    }
//                }
//            }
//        }

//        println("Start search: ${distancesToEdges.inner.values.sumOf { it.inner.values.sumOf { it.size } }}")
        println("Start search")

        val vctx = VertexCtx(
            MutableList(problem.figure.vertices.size) { allHolePoints }.toPersistentList(),
            MutableList(problem.figure.vertices.size) { null }.toPersistentList(),
            (0 until problem.figure.vertices.size).toPersistentSet()
        )

        val startingIdx = vctx.vertices.best() ?: return problem.figure

        val result = searchVertex(startingIdx, vctx.withVertex(startingIdx))
        if (result == null) error("No solution found")
        return problem.figure.copy(vertices = result.assigment.toList() as List<Point>)

//        val initialContext = Context(
//            problem.figure.edges.toPersistentSet(),
//            MutableList(problem.figure.vertices.size) { null }.toPersistentList()
//        )
//        val initialEdge = problem.figure.edges.maxByOrNull { it.calculate().squaredLength }!!
//        val result = propagateEdge(initialEdge, initialContext.withEdge(initialEdge))
//        if (result == null) error("PEZDA")
//        return problem.figure.copy(vertices = result.assigment.toList() as List<Point>)
    }

    private fun DataEdge.calculate() =
        let { Edge(problem.figure.vertices[it.startIndex], problem.figure.vertices[it.endIndex]) }


    val cache: MutableSet<Tuple4<DataEdge, DataEdge, Point?, Point?>> = mutableSetOf()

    val validIndices = MapToSet<Point, Int>()

//    private fun propagateEdge(edge: DataEdge, ctx: Context): Context? {
//        //println("edge = ${edge}")
//
//        val assigmentIsComplete = ctx.assigment.all { it != null }
//        if (assigmentIsComplete) {
//            val fig = problem.figure.copy(vertices = ctx.assigment.toList() as List<Point>)
//            if (!checkCorrect(problem.figure, fig, problem.epsilon)) {
//                println("Found incorrect assigment")
//                return null
//            }
//            return ctx
//        }
//
//        val edgeCandidates = distancesToEdges[edge.calculate().squaredLength.big.millions].toMutableSet()
//        ctx.assigment[edge.startIndex]?.let { point -> edgeCandidates.retainAll { it.start == point || it.end == point } }
//        ctx.assigment[edge.endIndex]?.let { point -> edgeCandidates.retainAll { it.start == point || it.end == point } }
//        edgeCandidates.retainAll {
//            edge.startIndex in validIndices[it.start] && edge.endIndex in validIndices[it.end]
//                    || edge.endIndex in validIndices[it.start] && edge.startIndex in validIndices[it.end]
//        }
//
//        for (candidate in edgeCandidates) {
//            var newCtx = checkOrCreateAssigment(edge.startIndex, candidate.start, ctx) ?: continue
//            newCtx = checkOrCreateAssigment(edge.endIndex, candidate.end, newCtx) ?: continue
//            val candidateEdges = listOf(edge.startIndex, edge.endIndex)
//                .flatMap { p -> verticesToEdges[p] }
//                .filter { it in ctx.edges }
//            val assigmentIsIncomplete = newCtx.assigment.any { it == null }
//            if (candidateEdges.isEmpty() && assigmentIsIncomplete) {
//                println("Empty candidates")
//                val nextEdge = newCtx.edges.firstOrNull()
//                if (nextEdge != null) {
//                    return propagateEdge(nextEdge, newCtx.withEdge(nextEdge))
//                }
//            }
//            if (candidateEdges.isEmpty()) {
//                if (assigmentIsIncomplete) error("Not connected vertices")
//                val fig = problem.figure.copy(vertices = newCtx.assigment.toList() as List<Point>)
//                if (!checkCorrect(problem.figure, fig, problem.epsilon)) {
//                    println("Found incorrect assigment")
//                    return null
//                }
//                return newCtx
//            }
//            val allEdgesPossible = candidateEdges.all { e ->
//                distancesToEdges[e.calculate().squaredLength.big.millions].any {
//                    it.start == candidate.start || it.end == candidate.start || it.start == candidate.end || it.end == candidate.end
//                }
//            }
//            if (!allEdgesPossible) continue
//            //val xcandidate = candidateEdges.maxByOrNull { it.first.calculate().squaredLength }!!
//            val xcandidate =
//                candidateEdges.minByOrNull { distancesToEdges[it.calculate().squaredLength.big.millions].size }!!
////            if (Tuple(xcandidate.first, edge, xcandidate.second) in cache) {
////                //println("ALREADY BEEN HERE!!!!")
////                continue
////            }
////            cache.add(Tuple(xcandidate.first, edge, xcandidate.second))
//            val res = propagateEdge(xcandidate, newCtx.withEdge(xcandidate))
//            if (res != null) return res
////            for(xcandidate in candidateEdges.sortedByDescending { it.first.calculate().squaredLength }){
////                val res = propagateEdge(xcandidate.first, newCtx.withEdge(xcandidate.first), xcandidate.second)
////                if (res != null) return res
////
////            }
//        }
////        println("OBOSRATUSHKI")
//        return null
//    }

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


    data class VertexCtx(
        val possiblePoints: PersistentList<Set<Point>>,
        val assigment: PersistentList<Point?>,
        val vertices: PersistentSet<Int>
    ) {
        fun vertexPoints(vertex: Int, points: Set<Point>) =
            VertexCtx(possiblePoints.set(vertex, points), assigment, vertices)

        fun assignVertex(vertex: Int, point: Point) = VertexCtx(possiblePoints, assigment.set(vertex, point), vertices)
        fun withVertex(vertex: Int) = VertexCtx(possiblePoints, assigment, vertices.remove(vertex))
    }

    private fun DataEdge.oppositeVertex(vertex: Int) = if (startIndex == vertex) endIndex else startIndex
    private fun DataEdge.isReversed(vertex: Int) = startIndex != vertex
    private fun Edge.vertexPoint(vertex: Int, abstractEdge: DataEdge) =
        if (abstractEdge.startIndex == vertex) start else end

    fun searchVertex(vid: Int, ctx: VertexCtx): VertexCtx? {
        val allAbstractEdges = verticesToEdges[vid]
        val allConcreteGroups = mutableMapOf<Point, MutableMap<DataEdge, Set<Edge>>>()
        val currentVertexPossiblePoints = ctx.possiblePoints[vid]
        // THIS IS FOR NEW YEAR!!!!!!!!!
//        println("Assignments: ${ctx.assigment}")
//        println("Current $vid")
//        overlays.clear()
//        for (possiblePoint in currentVertexPossiblePoints) {
//            overlays += Drawable.Shape(Ellipse2D(possiblePoint, 0.5)) to Color.PINK.darker().darker()
//        }
//        for (assignment in ctx.assigment) {
//            if (assignment != null) {
//                overlays += Drawable.Shape(Ellipse2D(assignment, 2.0)) to Color.CYAN
//            }
//        }
//        canvas.invokeRepaint()
//        System.`in`.bufferedReader().readLine()
        for (abstractEdge in allAbstractEdges) {
            val otherVertexPossiblePoints = ctx.possiblePoints[abstractEdge.oppositeVertex(vid)]
            val possibleDistances = (abstractSquares[abstractEdge.calculate().squaredLength.big] ?: emptySet())
            val groupedConcreteEdges = currentVertexPossiblePoints.associateWith { startPoint ->
                possibleDistances.mapNotNull { distance ->
                    val endPoint = startPoint + distance
                    if(endPoint !in otherVertexPossiblePoints) return@mapNotNull null
                    val edge =if(abstractEdge.isReversed(vid)) Edge(endPoint, startPoint) else Edge(startPoint, endPoint)
                    if(!verifier.check(edge)) edge else null
                }
            }.filterValues { it.isNotEmpty() }
            for ((keyPoint, edges) in groupedConcreteEdges) {
                val vertexEdges = allConcreteGroups.getOrPut(keyPoint) { mutableMapOf() }
                vertexEdges[abstractEdge] = edges.toSet()
            }
        }
        val possibleConcreteGroups = allConcreteGroups.filterValues { edges -> edges.keys == allAbstractEdges }
        val holeVertices = problem.hole.toSet()
        val possibleConcreteGroupsWithHolePriority =  possibleConcreteGroups.entries.sortedByDescending {
            it.key in holeVertices
        }
        for ((vertexPoint, edges) in possibleConcreteGroupsWithHolePriority) {
            var newCtx = ctx
            for ((abstractEdge, concreteEdges) in edges) {
                val vertex = abstractEdge.oppositeVertex(vid)
                val points = concreteEdges.map { it.vertexPoint(vertex, abstractEdge) }.toSet()
                val newPoints = points.intersect(newCtx.possiblePoints[vertex])
                newCtx = newCtx.vertexPoints(vertex, newPoints)
            }
            newCtx = newCtx.vertexPoints(vid, setOf(vertexPoint))
            newCtx = newCtx.assignVertex(vid, vertexPoint)
            val nextVertex = newCtx.vertices.best()
            if (nextVertex == null) {
                val assigmentIsComplete = newCtx.assigment.all { it != null }
                if (!assigmentIsComplete)
                    error("Not connected vertices")
                val fig = problem.figure.copy(vertices = newCtx.assigment.toList() as List<Point>)
                if (!checkCorrect(problem.figure, fig, problem.epsilon)) {
                    println("Found incorrect assigment")
                    return null
                }
                if (findAllSolutions) {
                    saveResult(problem, fig)
                    return null
                }
                return newCtx
            }
            val res = searchVertex(nextVertex, newCtx.withVertex(nextVertex))
            if (res != null) return res
        }
        return null
    }

}