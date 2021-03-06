package ru.spbstu.icpfc2021.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ru.spbstu.icpfc2021.solver.big
import ru.spbstu.icpfc2021.solver.millions
import ru.spbstu.ktuples.jackson.registerKTuplesModule
import ru.spbstu.ktuples.zip
import java.awt.geom.Point2D
import java.io.File
import java.io.InputStream
import java.io.Writer
import java.math.BigInteger
import kotlin.math.abs

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class Point(
    @get:JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    val x: Int,
    @get:JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    val y: Int
) : Point2D() {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    fun rotate90() = Point(y, -x)

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    override fun getX(): kotlin.Double = x.toDouble()

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    override fun getY(): kotlin.Double = y.toDouble()

    override fun setLocation(p0: kotlin.Double, p1: kotlin.Double) {
        TODO("Not yet implemented")
    }
}

fun Int.sqr() = this.toLong() * this.toLong()

infix fun Point.squaredDistance(other: Point) = (x - other.x).sqr() + (y - other.y).sqr()

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class DataEdge(
    val startIndex: Int, val endIndex: Int
)

data class Edge(val start: Point, val end: Point) {
    val squaredLength get() = start.squaredDistance(end)
}

private val million = BigInteger.valueOf(1_000_000)
val Long.big get() = BigInteger.valueOf(this)
val Int.big get() = BigInteger.valueOf(this.toLong())

fun areWithinEpsilon(f: Long, t: Long, epsilon: Int): Boolean {
    return when {
        t == 0L -> false
        f == t -> true
        f > t -> {
            million * f.big - million * t.big <= epsilon.big * f.big
        }
        else -> {
            million * t.big - million * f.big <= epsilon.big * f.big
        }
    }
}

fun checkCorrect(from: Edge, to: Edge, epsilon: Int): Boolean {
    val f = from.squaredLength
    val t = to.squaredLength
    return areWithinEpsilon(f, t, epsilon)
}

fun checkCorrect(from: Figure, to: Figure, epsilon: Int) =
    zip(from.calculatedEdges, to.calculatedEdges).all { (a, b) -> checkCorrect(a, b, epsilon) }

fun checkCorrectGlobalist(from: Figure, to: Figure, epsilon: Int): Boolean {
    val edgeDeltas = zip(from.calculatedEdges, to.calculatedEdges)
        .map { (f, t) -> f.squaredLength.toDouble() to t.squaredLength.toDouble() }
        .map { (f, t) -> abs((t / f) - 1.0) }
    val possibleDelta = from.edges.size * epsilon.toDouble() / 1_000_000
    println("Globalist: ${edgeDeltas.sum()} <= $possibleDelta")
    return edgeDeltas.sum() <= possibleDelta
}

typealias Hole = List<Point>

enum class Axis { X, Y }

data class Figure(
    val vertices: List<Point>,
    val edges: List<DataEdge>
) {
    fun calculateEdge(data: DataEdge): Edge = Edge(vertices[data.startIndex], vertices[data.endIndex])

    @get:JsonIgnore
    val calculatedEdges by lazy { edges.map { calculateEdge(it) } }

    fun moveAll(dp: Point) = copy(vertices = vertices.map { it + dp })
    fun rotate90() = copy(vertices = vertices.map { it.rotate90() })
    fun mirror(axis: Axis) = when (axis) {
        Axis.X -> copy(vertices = vertices.map { Point(it.x, -it.y) })
        Axis.Y -> copy(vertices = vertices.map { Point(-it.x, it.y) })
    }

    @get:JsonIgnore
    val currentPose
        get() = Pose(vertices, listOf())


    fun currentPoseWithBonus(bonus: BonusUse) = Pose(vertices, listOf(bonus))
}

data class Problem(
    val hole: Hole,
    val figure: Figure,
    val epsilon: Int,
    val bonuses: List<Bonus>? = null,

    @JsonIgnore
    val number: Int = 0
)

fun Problem.distanceToMillionsRange(realDistance: BigInteger): ClosedRange<BigInteger> {
    val delta = epsilon.big * realDistance
    val distance = realDistance.millions
    return (distance - delta)..(distance + delta)
}

data class Pose(
    val vertices: List<Point>,
    val bonuses: List<BonusUse>?
)

fun dislikes(hole: Hole, pose: Pose) =
    hole.sumOf { hp -> pose.vertices.minOf { vp -> hp.squaredDistance(vp) } }

@PublishedApi
internal val om = ObjectMapper().registerKotlinModule().registerKTuplesModule()

inline fun <reified T> readValue(file: File): T = om.readValue(file)
inline fun <reified T> readValue(file: String): T = om.readValue(file)
inline fun <reified T> readValue(file: InputStream): T = om.readValue(file)

fun readProblem(number: Int, json: String): Problem = readValue<Problem>(json).copy(number = number)

inline fun <reified T> writeValue(w: Writer, value: T) = om.writeValue(w, value)
inline fun <reified T> T.toJsonString() = om.writeValueAsString(this)

enum class BonusType { GLOBALIST, BREAK_A_LEG, WALLHACK, SUPERFLEX }

data class Bonus(
    val position: Point,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val bonus: BonusType,
    val problem: Int
)

data class BonusUse(
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val bonus: BonusType,
    val problem: Int
)

data class BonusInfo(
    val data: List<BonusUse>
) {
    @get:JsonIgnore
    val processed: Map<Int, List<BonusType>> by lazy {
        data.groupBy({ it.problem }) { it.bonus }
    }
}

fun DataEdge.oppositeVertex(vertex: Int) = if (startIndex == vertex) endIndex else startIndex
fun DataEdge.isReversed(vertex: Int) = startIndex != vertex
fun Edge.vertexPoint(vertex: Int, abstractEdge: DataEdge) =
    if (abstractEdge.startIndex == vertex) start else end
