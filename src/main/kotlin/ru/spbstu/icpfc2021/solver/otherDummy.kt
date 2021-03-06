package ru.spbstu.icpfc2021.solver

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import ru.spbstu.icpfc2021.gui.*
import ru.spbstu.icpfc2021.model.*
import ru.spbstu.icpfc2021.result.saveInvalidResult
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.wheels.MapToSet
import ru.spbstu.wheels.NullableMonad.filter
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.geom.GeneralPath
import java.awt.geom.Point2D
import java.io.File
import java.math.BigInteger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

val million = BigInteger.valueOf(1_000_000)
val Long.big get() = BigInteger.valueOf(this)
val Int.big get() = BigInteger.valueOf(this.toLong())
val BigInteger.millions get() = times(million)

fun Boolean.toInt() = when {
    this -> 1
    else -> 0
}

enum class SolverMode {
    SINGLE, RANDOM, DUMMY_RANDOM, HOLE_FITTER
}

@OptIn(ExperimentalTime::class)
class OtherDummySolver(
    val allHolePoints: Set<Point>,
    val problem: Problem,
    val findAllSolutions: Boolean = false,
    val showGraphics: Boolean = false,
    val mode: SolverMode = SolverMode.SINGLE,
    val sufflePossibleEntries: Boolean = true,
    val optimizeFirstIteration: Boolean = true,
    val useRandomOtkats: Boolean = false
) {
    val timeout = Duration.minutes(10)
    val verifier = Verifier(problem)
    val canvas: TransformablePanel
    val overlays = ConcurrentHashMap<Drawable, Color>()
    val validEdges = hashSetOf<Edge>()
    val target: AtomicReference<Point?> = AtomicReference(null)
    val leftAssignments = AtomicInteger(problem.figure.vertices.size)
    val currentCtx: AtomicReference<VertexCtx?> = AtomicReference(null)
    lateinit var abstractSquares: Map<BigInteger, Set<Point>>

    private val solverIsRunning = AtomicBoolean(true)
    private var firstIteration = true

    fun Set<Int>.randomBest(): Int? = randomOrNull()

    fun Set<Int>.bestLongestPaths(): Int? = maxByOrNull {
        verticesToEdges[it].sumOf { edge -> edge.calculate().squaredLength }
    }

    fun Set<Int>.bestSmallestEdges(): Int? = minByOrNull {
        verticesToEdges[it].size
    }

    fun Set<Int>.best(): Int? = bestSmallestEdges()

    init {

        val figure = problem.figure
        val hole2DVertices = problem.hole.map { it.to2D() }
        canvas = dumbCanvas(1000, 1000) {
            withPaint(Color.GRAY.brighter().brighter()) {
                val hole2D = GeneralPath()
                hole2D.moveTo(hole2DVertices.first())
                for (point in hole2DVertices.drop(1)) hole2D.lineTo(point)
                hole2D.closePath()
                fill(hole2D)
            }

            for (edge in figure.calculatedEdges) {
                withPaint(Color.BLUE) {
                    val line = java.awt.geom.Line2D.Double(edge.start, edge.end)
                    draw(Drawable.Shape(BasicStroke(0.2f).createStrokedShape(line)))
                }
            }

            for ((index, point) in figure.vertices.withIndex()) {
                val color = if (verifier.isOutOfBounds(point)) Color.RED else Color.BLUE
                withPaint(color) {
                    fill(Ellipse2D(point, 2.0))
                    withFont(Font.decode("Fira-Mono-Bold-8")) {
                        drawString("$index", point.x, point.y)
                    }
                }
            }

            for (b in problem.bonuses.orEmpty()) {
                val color = when (b.bonus) {
                    BonusType.GLOBALIST -> Color.YELLOW
                    BonusType.BREAK_A_LEG -> Color.MAGENTA
                    BonusType.WALLHACK -> Color.ORANGE
                    BonusType.SUPERFLEX -> Color.CYAN
                }
                withPaint(color) {
                    fill(Ellipse2D(b.position, 2.0))
                }
                withPaint(Color.BLACK) {
                    draw(Ellipse2D(b.position, 2.0))
                }
            }


            for ((overlay, color) in overlays) {
                withPaint(color) {
                    draw(overlay)
                }
            }

            absolute {
                withPaint(Color.ORANGE) {
                    withFont(Font.decode("Fira-Mono-Bold-20")) {
                        drawString("left assignments: ${leftAssignments.get()}", 20.0f, 30.0f)
                    }
                }
            }

        }
        canvas.scale(10.0)
        canvas.translate(20.0, 20.0)
        canvas.onKey("DOWN") {
            val ty = 20.0 / canvas.transform.scaleY
            canvas.translate(0.0, ty)
        }
        canvas.onKey("UP") {
            val ty = -20.0 / canvas.transform.scaleY
            canvas.translate(0.0, ty)
        }
        canvas.onKey("LEFT") {
            val tx = -20.0 / canvas.transform.scaleX
            canvas.translate(tx, 0.0)
        }
        canvas.onKey("RIGHT") {
            val tx = 20.0 / canvas.transform.scaleX
            canvas.translate(tx, 0.0)
        }
        canvas.onKey("control shift S") {
            val ctx = currentCtx.get()
            ctx?.let {
                problem.figure.copy(vertices = problem.figure.vertices.withIndex().zip(it.assigment) { a, b ->
                    if (b == null) it.possiblePoints[a.index].randomOrNull() ?: a.value
                    else b
                })
            }?.let {
                saveInvalidResult(problem, it)
            }
        }
        canvas.onKey(KeyStroke.getKeyStroke('+', 0)) {
            canvas.scale(1.1)
        }
        canvas.onKey(KeyStroke.getKeyStroke('-', 0)) {
            canvas.scale(0.9)
        }
        canvas.onKey("control C") {
            solverIsRunning.set(false)
        }
        canvas.onKey("control L") {
            target.set(null)
        }
        canvas.onMouseWheel { e ->
            val rot = e.preciseWheelRotation
            val point = e.canvasPoint
            val scale = 1.0 - rot * 0.1
            canvas.zoomTo(point, scale)
        }
        canvas.onMousePan(filter = { SwingUtilities.isRightMouseButton(it) }) { _, prev, e ->
            val st = prev.canvasPoint
            val end = e.canvasPoint
            canvas.translate(end.x - st.x, end.y - st.y)
        }
        canvas.onMousePan(filter = { SwingUtilities.isLeftMouseButton(it) }) { _, prev, e ->
            target.set(prev.canvasPoint.round())
        }
        if (showGraphics) {
            dumbFrame(canvas).defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        }
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

        abstractSquares = problem.figure.calculatedEdges.mapTo(mutableSetOf()) { it.squaredLength.big }
            .associateWith { realDistance ->
                val result = mutableSetOf<Point>()
                val delta = problem.epsilon.big * realDistance
                val distance = realDistance.millions

                val range = (distance - delta)..(distance + delta)

                val outerRadius = ceil(sqrt(((delta + distance) / million).toDouble())).toInt() + 10
                val innerRadius =
                    floor(sqrt((((distance - delta).toDouble() / sqrt(2.0)) / million.toDouble()))).toInt() - 10

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

        println("Start search")
        return when (mode) {
            SolverMode.SINGLE -> singleTryMode()
            SolverMode.RANDOM -> randomTriesMode()
            SolverMode.DUMMY_RANDOM -> dummyRandom()
            SolverMode.HOLE_FITTER -> holeFitterMode()
        }
    }

    private fun dummyRandom(): Figure {
        val retries = 100
        val timer = Timer()
        return run {
            var result: Figure? = currentBestSolutionFigure()
            var tryIdx = 0
            while (tryIdx++ < retries) {
                val score = result?.let { dislikes(problem.hole, it.currentPose) }
                if (score != null && result != null && score == 0L) return result
                println("Start try $tryIdx | ${score}}")
                val vctx = VertexCtx(
                    MutableList(problem.figure.vertices.size) { allHolePoints }.toPersistentList(),
                    MutableList(problem.figure.vertices.size) { null }.toPersistentList(),
                    (0 until problem.figure.vertices.size).toPersistentSet()
                )

                val startingIdx = vctx.vertices.randomBest() ?: return problem.figure
                firstIteration = true
                val (tryResult, _) = timer.solveWithTimeout(timeout) {
                    searchVertex(startingIdx, vctx.withVertex(startingIdx))
                }
                tryResult ?: continue
                val tryFig = problem.figure.copy(vertices = tryResult.assigment.filterNotNull())
                if (saveResult(problem, tryFig) || result == null) {
                    result = tryFig
                }
            }
            result
        } ?: error("No solution found")
    }


    private fun randomTriesMode(): Figure {
        val retries = 10000
        val timer = Timer()
        return run {
            var result: Figure? = currentBestSolutionFigure()
            var tryIdx = 0
            while (tryIdx++ < retries) {
                val score = result?.let { dislikes(problem.hole, it.currentPose) }
                if (score != null && result != null && score == 0L) return result
                println("Start try $tryIdx | ${score}}")

                val initialCtx = randomInitialSeed()
                val randomInitialVertex = initialCtx.possiblePoints.withIndex()
                    .minByOrNull { it.value.size }?.index
                val startingIdx = randomInitialVertex ?: initialCtx.vertices.best() ?: error("No initial index")
                firstIteration = true
                val (tryResult, _) = timer.solveWithTimeout(timeout) {
                    searchVertex(
                        startingIdx,
                        initialCtx.withVertex(startingIdx)
                    )
                }
                tryResult ?: continue
                val tryFig = problem.figure.copy(vertices = tryResult.assigment.filterNotNull())
                if (saveResult(problem, tryFig) || result == null) {
                    result = tryFig
                }
            }
            result
        } ?: error("No solution found")
    }


    private fun holeFitterMode(): Figure {
        val retries = 10000
        val timer = Timer()
        return run {
            var result: Figure? = currentBestSolutionFigure()
            var tryIdx = 0
            val seedSequence = holeFitterInitialSeed().iterator()
            if (!seedSequence.hasNext()) error("Empty hole fitting paths")
            while (tryIdx++ < retries) {
                val score = result?.let { dislikes(problem.hole, it.currentPose) }
                if (score != null && result != null && score == 0L) return result
                println("Start try $tryIdx | ${score}}")
                val initialCtx = seedSequence.next()
                val randomInitialVertex = initialCtx.possiblePoints.withIndex()
                    .minByOrNull { it.value.size }?.index
                val startingIdx = randomInitialVertex ?: initialCtx.vertices.best() ?: error("No initial index")
                firstIteration = true
                val (tryResult, _) = timer.solveWithTimeout(timeout) {
                    searchVertex(
                        startingIdx,
                        initialCtx.withVertex(startingIdx)
                    )
                }
                tryResult ?: continue
                val tryFig = problem.figure.copy(vertices = tryResult.assigment.filterNotNull())
                if (saveResult(problem, tryFig) || result == null) {
                    result = tryFig
                } else {
                    saveInvalidResult(problem, tryFig)
                }
            }
            result
        } ?: error("No solution found")
    }

    private inline fun <reified T> Timer.solveWithTimeout(duration: Duration, computation: () -> T): T {
        solverIsRunning.set(true)
        val cancelation = object : TimerTask() {
            override fun run() {
                solverIsRunning.set(false)
            }
        }
        schedule(cancelation, duration.inWholeMilliseconds)
        val result = computation()
        cancelation.cancel()
        return result
    }

    private fun currentBestSolutionFigure(): Figure? {
        val resultFile = File("solutions/${problem.number}.sol").also {
            it.parentFile?.mkdirs()
        }
        val previousSolution = try {
            readValue<Pose>(resultFile)
        } catch (e: Throwable) {
            println("Could not read previous solution: ${e.message}")
            null
        }
        return previousSolution?.let { problem.figure.copy(vertices = it.vertices) }
    }

    private fun holeFitterInitialSeed() = sequence {
        val vertexAmount = problem.figure.vertices.size
        val hf = HoleFitter(problem)
        val fit = hf.fit()
        if (!fit.iterator().hasNext()) return@sequence
        while (true) {
            for (path in fit) {
                val assignments = MutableList<Point?>(vertexAmount) { null }
                for ((de, e) in path) {
                    assignments[de.startIndex] = e.start
                    assignments[de.endIndex] = e.end
                }
                val possiblePoints = MutableList(vertexAmount) { i ->
                    val assigment = assignments[i]
                    when {
                        assigment != null -> setOf(assigment)
                        else -> allHolePoints
                    }
                }
                val ctx = VertexCtx(
                    possiblePoints.toPersistentList(),
                    MutableList(vertexAmount) { null }.toPersistentList(),
                    (0 until vertexAmount).toPersistentSet()
                )
                yield(ctx)
            }
        }
    }

    private fun randomInitialSeed(): VertexCtx {
        val vertexAmount = problem.figure.vertices.size
        val holeVertices = problem.hole.toMutableSet()
        val assignments = MutableList<Point?>(vertexAmount) { null }
        val bound = minOf(vertexAmount, holeVertices.size, 5)

        for (i in 0..Random.nextInt(1, bound)) {
            val emptyVertices =
                assignments.withIndex().filter { it: IndexedValue<Point?> -> it.value == null }.map { it.index }
            val vertex = emptyVertices.random()
            val point = holeVertices.random()
            holeVertices -= point
            assignments[vertex] = point
        }

        val possiblePoints = MutableList(vertexAmount) { i ->
            val assigment = assignments[i]
            when {
                assigment != null -> setOf(assigment)
                else -> allHolePoints//randomlyReducePointSet(allHolePoints, Random.nextDouble(0.1, 1.0))
            }
        }
        return VertexCtx(
            possiblePoints.toPersistentList(),
            MutableList(vertexAmount) { null }.toPersistentList(),
            (0 until vertexAmount).toPersistentSet()
        )
    }

    private fun manualInvalidSeed(): VertexCtx {
        val vertexAmount = problem.figure.vertices.size
        val invalidResult = readValue<Pose>(File("solutions/${problem.number}.invalid.sol"))
        val assignments = problem.figure.vertices.zip(invalidResult.vertices) { a, b ->
            if (a == b) null else b
        }.toPersistentList()

        val possiblePoints = MutableList(vertexAmount) { i ->
            val assigment = assignments[i]
            when {
                assigment != null -> setOf(assigment)
                else -> allHolePoints//randomlyReducePointSet(allHolePoints, Random.nextDouble(0.1, 1.0))
            }
        }
        return VertexCtx(
            possiblePoints.toPersistentList(),
            assignments.toPersistentList(),
            (0 until vertexAmount).toPersistentSet()
        )
    }


    private fun randomlyReducePointSet(original: Set<Point>, percent: Double): Set<Point> {
        val pointSource = original.toList()
        val points = hashSetOf<Point>()
        val originalSize = original.size.toDouble()
        while (points.size / originalSize < percent) {
            points += pointSource.random()
        }
        return points
    }

    private fun singleTryMode(): Figure {
        val vctx = VertexCtx(
            MutableList(problem.figure.vertices.size) { allHolePoints }.toPersistentList(),
            MutableList(problem.figure.vertices.size) { null }.toPersistentList(),
            (0 until problem.figure.vertices.size).toPersistentSet()
        )

        val startingIdx = vctx.vertices.best() ?: return problem.figure

        val (result, _) = searchVertex(startingIdx, vctx.withVertex(startingIdx)) ?: error("No solution found")
        result ?: error("No solution found")
        return problem.figure.copy(vertices = result.assigment.toList() as List<Point>)
    }

    private fun DataEdge.calculate() =
        let { Edge(problem.figure.vertices[it.startIndex], problem.figure.vertices[it.endIndex]) }

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

    private fun checkCanceled(): Boolean = !solverIsRunning.get()

    fun searchVertex(vid: Int, ctx: VertexCtx): Pair<VertexCtx?, Int> {
        if (checkCanceled()) return null to 9999
        val allAbstractEdges = verticesToEdges[vid]
        val allConcreteGroups = mutableMapOf<Point, MutableMap<DataEdge, Set<Edge>>>()
        var currentVertexPossiblePoints = ctx.possiblePoints[vid]
        // this is fucked up, but it really increases first iteration speed
        if (firstIteration && optimizeFirstIteration) {
            firstIteration = false
            currentVertexPossiblePoints = currentVertexPossiblePoints.filter { it: Point -> it in problem.hole }.toSet()
        }
        leftAssignments.set(problem.figure.vertices.size - ctx.assigment.filterNotNull().size)
        currentCtx.set(ctx)
        // THIS IS FOR NEW YEAR!!!!!!!!!
        if (showGraphics) {
            overlays.clear()
            for (possiblePoint in currentVertexPossiblePoints) {
                overlays += Drawable.Shape(Ellipse2D(possiblePoint, 0.5)) to Color.PINK.darker().darker()
            }
            for (assignment in ctx.assigment) {
                if (assignment != null) {
                    overlays += Drawable.Shape(Ellipse2D(assignment, 2.0)) to Color.CYAN
                }
            }
            canvas.invokeRepaint()
        }
        for (abstractEdge in allAbstractEdges) {
            if (checkCanceled()) return null to 9999
            val otherVertexPossiblePoints = ctx.possiblePoints[abstractEdge.oppositeVertex(vid)]
            val possibleDistances = (abstractSquares[abstractEdge.calculate().squaredLength.big] ?: emptySet())
            val groupedConcreteEdges = currentVertexPossiblePoints.associateWith { startPoint ->
                possibleDistances.mapNotNull { distance ->
                    val endPoint = startPoint + distance
                    if (endPoint !in otherVertexPossiblePoints) return@mapNotNull null
                    val edge = when {
                        abstractEdge.isReversed(vid) -> Edge(endPoint, startPoint)
                        else -> Edge(startPoint, endPoint)
                    }
                    if (checkCanceled()) return null to 9999
                    when {
                        edge in validEdges -> edge
                        verifier.check(edge) == Verifier.Status.OK -> edge.also { validEdges += it }
                        else -> null
                    }
                }
            }.filterValues { it.isNotEmpty() }

            for ((keyPoint, edges) in groupedConcreteEdges) {
                val vertexEdges = allConcreteGroups.getOrPut(keyPoint) { mutableMapOf() }
                vertexEdges[abstractEdge] = edges.toSet()
            }
        }
        if (checkCanceled()) return null to 9999
        val possibleConcreteGroups = allConcreteGroups.filterValues { edges -> edges.keys == allAbstractEdges }
        val validAssignments = ctx.assigment.filterNotNull()
        val holeVertices = problem.hole.toSet() - validAssignments
        val bonusVertices = problem.bonuses.orEmpty().toSet() - validAssignments
        val currentTarget = target.get()
        val mostRemoteVertices = holeVertices.sortedByDescending { v ->
            validAssignments.sumOf { it.distance(v.to2D()) }
        }.take(5)
        val possibleConcreteGroupsWithHolePriority = when {
            sufflePossibleEntries -> possibleConcreteGroups.entries.shuffled()
            else -> possibleConcreteGroups.entries
        }.sortedWith(
            compareBy<Map.Entry<Point, *>> { (key, _) ->
                (key in holeVertices).toInt()
            }
                .thenBy { (key, _) ->
                    -(currentTarget?.let {
                        key.distance(it.to2D())
                    } ?: 0.0)

                }
//            .thenBy {
//                validAssignments.sumOf { assignment -> Edge(it.key, assignment).squaredLength }
//            }
//                .thenBy {
//                it in bonusVertices
//            }
                .reversed()
        )

        val otkat = if (useRandomOtkats) 1 + java.util.Random().nextInt(5) else 0
        for ((vertexPoint, edges) in possibleConcreteGroupsWithHolePriority) {
            if (checkCanceled()) return null to 9999
            var newCtx = ctx
            for ((abstractEdge, concreteEdges) in edges) {
                val vertex = abstractEdge.oppositeVertex(vid)
                val points = concreteEdges.map { it.vertexPoint(vertex, abstractEdge) }.toSet()
                val newPoints = points.intersect(newCtx.possiblePoints[vertex])
                newCtx = newCtx.vertexPoints(vertex, newPoints)
            }
            newCtx = newCtx.vertexPoints(vid, setOf(vertexPoint))
            newCtx = newCtx.assignVertex(vid, vertexPoint)
            val nextVertex = newCtx.vertices.minByOrNull { newCtx.possiblePoints[it].size }
            if (nextVertex == null) {
                val assigmentIsComplete = newCtx.assigment.all { it != null }
                if (!assigmentIsComplete)
                    error("Not connected vertices")
                val fig = problem.figure.copy(vertices = newCtx.assigment.toList() as List<Point>)
                if (!checkCorrect(problem.figure, fig, problem.epsilon)) {
                    println("Found incorrect assigment")
                    return null to otkat
                }
                if (findAllSolutions) {
                    saveResult(problem, fig)
                    return null to otkat
                }
                return newCtx to 9999
            }
            val (res, xotkat) = searchVertex(nextVertex, newCtx.withVertex(nextVertex))
            if (res != null) return res to 9999
            if (xotkat > 0) {
                return null to xotkat - 1
            }
        }
        return null to otkat
    }

}