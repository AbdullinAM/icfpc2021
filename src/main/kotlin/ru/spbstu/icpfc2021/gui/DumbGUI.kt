package ru.spbstu.icpfc2021.gui

import ru.spbstu.icpfc2021.model.*
import ru.spbstu.icpfc2021.model.Point
import ru.spbstu.icpfc2021.result.loadSolution
import ru.spbstu.icpfc2021.result.saveInvalidResult
import ru.spbstu.icpfc2021.result.saveResult
import ru.spbstu.wheels.Stack
import ru.spbstu.wheels.stack
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.*
import javax.swing.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


fun Action(body: (ActionEvent) -> Unit) = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
        return body(e)
    }
}

operator fun Point2D.component1() = x
operator fun Point2D.component2() = y
fun Point2D(x: Double, y: Double): Point2D = Point2D.Double(x, y)
fun Point2D(x: Int, y: Int): Point2D = Point2D.Double(x.toDouble(), y.toDouble())
fun Point2D.copy(x: Double = this.x, y: Double = this.y) = Point2D(x, y)
operator fun Point2D.plus(rhv: Point2D) = Point2D(x + rhv.x, y + rhv.y)
operator fun Point2D.minus(other: Point2D) = Point2D(x - other.x, y - other.y)
operator fun Point2D.plusAssign(rhv: Point2D) = setLocation(x + rhv.x, y + rhv.y)
infix fun Point2D.distance(that: Point2D) = this.distance(that)
infix fun Point2D.manhattanDistance(that: Point2D) = maxOf(abs(x - that.x), abs(y - that.y))
infix fun Point2D.chebyshevDistance(that: Point2D) = abs(x - that.x) + abs(y - that.y)

val Rectangle2D.center get() = Point2D(centerX, centerY)
fun Rectangle2D(x: Double, y: Double, w: Double, h: Double = w): Rectangle2D =
    Rectangle2D.Double(x, y, w, h)

fun Rectangle2D(p: Point2D, w: Double, h: Double = w): Rectangle2D =
    Rectangle2D.Double(p.x, p.y, w, h)

fun Rectangle2D(p1: Point2D, p2: Point2D): Rectangle2D =
    Rectangle2D.Double(p1.x, p2.y, 0.0, 0.0).apply { add(p2) }

fun Rectangle2D.copy(
    x: Double = this.x,
    y: Double = this.y,
    w: Double = this.width,
    h: Double = this.height
) = Rectangle2D(x, y, w, h)

operator fun Rectangle2D.plusAssign(point: Point2D) = add(point)
operator fun Rectangle2D.plusAssign(rectangle2D: Rectangle2D) = add(rectangle2D)

fun Ellipse2D(x: Double, y: Double, w: Double, h: Double): Ellipse2D =
    Ellipse2D.Double(x, y, w, h)

fun Ellipse2D(center: Point2D, w: Double, h: Double = w): Ellipse2D =
    Ellipse2D.Double(center.x - w / 2, center.y - h / 2, w, h)

fun Line2D(p1: Point2D, p2: Point2D): Line2D = Line2D.Double(p1, p2)
operator fun Line2D.component1() = p1
operator fun Line2D.component2() = p2

fun Color.transparent() = Color(this.red, this.green, this.blue, this.alpha - 25)

fun dumbCanvas(width: Int = 800, height: Int = 600, draw: Graphics2D.() -> Unit) = object : TransformablePanel() {
    override val transform: AffineTransform = AffineTransform()

    override fun getPreferredSize(): Dimension = Dimension(width, height)
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g as Graphics2D
        val oldTransform = g.transform
        g.transform(transform)
        g.stroke = BasicStroke((1.0 / maxOf(transform.scaleX, transform.scaleY)).toFloat())
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.draw()
        g.transform = oldTransform
    }

}

fun dumbFrame(panel: JPanel, title: String = "") = JFrame(title).apply {
    add(panel)
    pack()
    isVisible = true
}


fun dumbFrame(title: String = "", width: Int = 800, height: Int = 600, draw: Graphics2D.() -> Unit) =
    dumbFrame(title = title, panel = dumbCanvas(width, height, draw))


abstract class TransformablePanel : JPanel() {
    abstract val transform: AffineTransform

    init {
        background = Color.GRAY.darker().darker()
    }

    fun scale(scaleX: Double, scaleY: Double = scaleX) {
        SwingUtilities.invokeLater {
            transform.scale(scaleX, scaleY)
            repaint()
        }
    }

    fun translate(tx: Double, ty: Double) {
        SwingUtilities.invokeLater {
            transform.translate(tx, ty)
            repaint()
        }
    }

    fun zoomTo(point: Point2D, scaleX: Double, scaleY: Double = scaleX) {
        SwingUtilities.invokeLater {
            transform.apply {
                translate(point.x, point.y)
                scale(scaleX, scaleY)
                translate(-point.x, -point.y)
            }
            repaint()
        }
    }

    fun invokeRepaint() = SwingUtilities.invokeLater { repaint() }

    val canvasMousePosition: Point2D? get() = this.mousePosition?.let { transform.inverseTransform(it) }
    val MouseEvent.canvasPoint: Point2D get() = transform.inverseTransform(point, Point2D.Double())
}

inline fun Graphics2D.withPaint(p: Paint, body: Graphics2D.() -> Unit) {
    val oldPaint = paint
    paint = p
    body()
    paint = oldPaint
}

inline fun Graphics2D.withFont(f: Font, body: Graphics2D.() -> Unit) {
    val oldFont = font
    font = f
    body()
    font = oldFont
}

inline fun Graphics2D.withStroke(s: Stroke, body: Graphics2D.() -> Unit) {
    val oldStroke = stroke
    stroke = s
    body()
    stroke = oldStroke
}

inline fun Graphics2D.absolute(body: Graphics2D.() -> Unit) {
    val oldTransform = transform
    transform = AffineTransform()
    body()
    transform = oldTransform
}

inline fun TransformablePanel.onMouseClick(crossinline body: TransformablePanel.(e: MouseEvent) -> Unit) {
    addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            body(e)
        }
    })
}

inline fun TransformablePanel.onMouseWheel(crossinline body: TransformablePanel.(e: MouseWheelEvent) -> Unit) {
    addMouseWheelListener { e -> body(e) }
}

fun sameButton(start: MouseEvent, e: MouseEvent) =
    start.modifiersEx == e.modifiersEx

inline fun TransformablePanel.onMousePan(
    crossinline filter: TransformablePanel.(e: MouseEvent) -> Boolean = { true },
    crossinline destructor: TransformablePanel.(start: MouseEvent, prev: MouseEvent) -> Unit = { _, _ -> },
    crossinline body: TransformablePanel.(start: MouseEvent, prev: MouseEvent, e: MouseEvent) -> Unit
) {

    val panner = object : MouseAdapter() {
        private var start: MouseEvent? = null
        private var prev: MouseEvent? = null

        override fun mousePressed(e: MouseEvent) {
            if (filter(e)) {
                start = e
                prev = e
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            val start = start
            val prev = prev
            if (start != null && prev != null && e.button == start.button) {
                body(start, prev, e)
                destructor(start, e)
                this.start = null
                this.prev = null
            }
        }

        override fun mouseDragged(e: MouseEvent) {
            val start = start
            val prev = prev
            if (start != null && prev != null && sameButton(start, e)) {
                body(start, prev, e)
                this.prev = e
            }
        }
    }

    addMouseListener(panner)
    addMouseMotionListener(panner)
}

inline fun JComponent.onKey(stroke: KeyStroke, crossinline body: () -> Unit) {
    val skey = "$stroke".replace(" ", "+")
    inputMap.put(stroke, skey)
    actionMap.put(skey, Action { body() })
}

inline fun JComponent.onKey(stroke: String, crossinline body: () -> Unit) {
    val skey = stroke.replace(" ", "+")
    inputMap.put(KeyStroke.getKeyStroke(stroke), skey)
    actionMap.put(skey, Action { body() })
}


operator fun AffineTransform.invoke(p: Point2D) = transform(p, Point2D.Double())
fun AffineTransform.inverseTransform(p: Point2D) = inverseTransform(p, Point2D.Double())

enum class CellState {
    Free, NotFree, Start, Finish;

    val invert: CellState
        get() = when (this) {
            Free -> NotFree
            NotFree -> Free
            Start -> Free
            Finish -> Free
        }
}

data class Cell(var state: CellState, val x: Int, val y: Int)

fun interface Drawable {
    fun drawOn(graphics2D: Graphics2D)

    object Nil : Drawable {
        override fun drawOn(graphics2D: Graphics2D) {}
    }

    data class Shape(val inner: java.awt.Shape) : Drawable {
        override fun drawOn(graphics2D: Graphics2D) {
            graphics2D.fill(inner)
        }
    }

    class Multi(vararg val inner: Drawable) : Drawable {
        constructor(vararg shapes: java.awt.Shape) : this(*shapes.map { Shape(it) }.toTypedArray<Drawable>())

        override fun drawOn(graphics2D: Graphics2D) {
            for (e in inner) e.drawOn(graphics2D)
        }
    }
}

fun Graphics2D.draw(drawable: Drawable) = drawable.drawOn(this)
fun GeneralPath.moveTo(point: Point2D) = moveTo(point.x, point.y)
fun GeneralPath.lineTo(point: Point2D) = lineTo(point.x, point.y)
fun Graphics2D.drawLine(start: Point2D, end: Point2D) =
    drawLine(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt())

fun Point2D.round() = Point(x.roundToInt(), y.roundToInt())

data class GetterAndSetterForLocalPropertyBitch<T>(
    val getter: () -> T,
    val setter: (T) -> Unit
) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getter()
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = setter(value)
}

fun Figure.center(): Point = this.vertices.toArea().bounds.center.round()

fun Figure.rotate(theta: Double, around: Point = center()): Figure {
    val transform = AffineTransform.getRotateInstance(theta, around.getX(), around.getY())
    return copy(vertices = vertices.map {
        val res = Point2D.Double()
        transform.transform(it, res)
        res.round()
    })
}

data class GUIController(
    val figureStack: Stack<Figure>,
    val canvas: TransformablePanel
) {
    fun setFigure(figure: Figure){
        figureStack.push(figure)
    }

    fun invokeRepaint() {
        canvas.invokeRepaint()
    }
}

fun drawFigure(problem: Problem, initialFigure: Figure? = null): GUIController {
    val (hole, startingFigure) = problem

    val verifier = Verifier(problem)
    val holePoints = verifier.getHolePoints().toSet()
    val validPoints = mutableMapOf<Point, Double>()

    val figureStack = stack<Figure>()

    figureStack.push(startingFigure)
    if(initialFigure != null){
        figureStack.push(initialFigure)
    }
    var figure: Figure by GetterAndSetterForLocalPropertyBitch(
        getter = { figureStack.top!! },
        setter = { figureStack.push(it) }
    )
    val holeVertices = hole.map { it.to2D() }
    var currentCoordinates: Point? = null

    val overlays: MutableMap<String, Drawable> = mutableMapOf()

    val canvas = dumbCanvas {
        withPaint(Color.GRAY.brighter().brighter()) {
            val hole2D = GeneralPath()
            hole2D.moveTo(holeVertices.first())
            for (point in holeVertices.drop(1)) hole2D.lineTo(point)
            hole2D.closePath()
            fill(hole2D)
        }

        val graphEdges = figure.calculatedEdges

        for ((point, ratio) in validPoints) {
            val (color, radius) = when (ratio) {
                1.0 -> Color.GREEN to 1.5
                else -> Color.PINK to (0.25 + 1.0 * ratio)
            }
            withPaint(color) {
                fill(Ellipse2D(point, radius))
            }
        }

        for ((edge, oldEdge) in graphEdges.zip(startingFigure.calculatedEdges)) {
            val color = when {
                checkCorrect(oldEdge, edge, problem.epsilon) && verifier.check(edge) == Verifier.Status.OK -> Color.BLUE
                else -> Color.RED
            }
            withPaint(color) {
                val line = Line2D.Double(edge.start, edge.end)
                draw(Drawable.Shape(BasicStroke(0.2f).createStrokedShape(line)))
            }
        }

        for (point in figure.vertices) {
            val color = if (verifier.isOutOfBounds(point)) Color.RED else Color.BLUE
            withPaint(color) {
                fill(Ellipse2D(point, 2.0))
            }
        }

        for (b in problem.bonuses.orEmpty()) {
            val acquired = figure.vertices.any { it == b.position }
            var color = when(b.bonus) {
                BonusType.GLOBALIST -> Color.YELLOW
                BonusType.BREAK_A_LEG -> Color.MAGENTA
                BonusType.WALLHACK -> Color.ORANGE
                BonusType.SUPERFLEX -> Color.CYAN
            }
            color = when {
                acquired -> color.darker()
                else -> color.brighter()
            }
            withPaint(color) {
                fill(Ellipse2D(b.position, 2.0))
            }
            withPaint(Color.BLACK) {
                draw(Ellipse2D(b.position, 2.0))
            }
        }

        for (overlay in overlays.values) {
            overlay.drawOn(this)
        }

        absolute {
            withPaint(Color.ORANGE) {
                withFont(Font.decode("Fira-Mono-Bold-20")) {
                    drawString("dislikes: ${dislikes(hole, figure.currentPose)}", 20.0f, 30.0f)
                    if (currentCoordinates != null) {
                        drawString("current coordinates: x=${currentCoordinates!!.x}, y=${currentCoordinates!!.y}", 20.0f, 40.0f)
                    }
                    drawString("invalid edges: ${verifier.countInvalidEdges(figure)}", 20.0f, 60.0f)
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
    canvas.onKey("control S") {
        println(figure.currentPose.toJsonString())
        saveResult(problem, figure)
    }
    canvas.onKey("control shift S") {
        println(figure.currentPose.toJsonString())
        saveInvalidResult(problem, figure)
    }
    canvas.onKey("control L") {
        figure = loadSolution(problem)
        canvas.invokeRepaint()
    }
    canvas.onKey("control Z") {
        if (figureStack.size > 1) {
            figureStack.pop()
            canvas.invokeRepaint()
        }
    }
    canvas.onKey("control alt R") {
        while (figureStack.size > 1) {
            figureStack.pop()
        }
        canvas.invokeRepaint()
    }
    canvas.onKey("R") {
        val point = canvas.canvasMousePosition?.round() ?: figure.center()
        figureStack.push(figure.rotate(Math.PI / 4, point))
        canvas.invokeRepaint()
    }
    canvas.onKey("shift R") {
        val point = canvas.canvasMousePosition?.round() ?: figure.center()
        figureStack.push(figure.rotate(0.05, point))
        canvas.invokeRepaint()
    }
    canvas.onKey("shift L") {
        val point = canvas.canvasMousePosition?.round() ?: figure.center()
        figureStack.push(figure.rotate(-0.05, point))
        canvas.invokeRepaint()
    }
    canvas.onKey("M") {
        figureStack.push(figure.mirror(Axis.X))
        canvas.invokeRepaint()
    }
    canvas.onKey("N") {
        figureStack.push(figure.mirror(Axis.Y))
        canvas.invokeRepaint()
    }
    canvas.onKey("W") {
        figureStack.push(figure.moveAll(Point(0, -1)))
        canvas.invokeRepaint()
    }
    canvas.onKey("S") {
        figureStack.push(figure.moveAll(Point(0, 1)))
        canvas.invokeRepaint()
    }
    canvas.onKey("A") {
        figureStack.push(figure.moveAll(Point(-1, 0)))
        canvas.invokeRepaint()
    }
    canvas.onKey("D") {
        figureStack.push(figure.moveAll(Point(1, 0)))
        canvas.invokeRepaint()
    }
    canvas.onKey(KeyStroke.getKeyStroke('+', 0)) {
        canvas.scale(1.1)
    }
    canvas.onKey(KeyStroke.getKeyStroke('-', 0)) {
        canvas.scale(0.9)
    }
    canvas.onKey("control E") {
        val mouse = canvas.canvasMousePosition ?: return@onKey
        val startPoint = figure.vertices.withIndex().minByOrNull { (_, v) -> v.distance(mouse) }?.value ?: return@onKey

        val holeEdges = (problem.hole + problem.hole.first()).windowed(2).mapTo(mutableSetOf()) { (a, b) -> Edge(a, b) }

        val ourIndex = figure.vertices.indexOf(startPoint)
        val ourEdges = problem.figure.edges.filter { it.startIndex == ourIndex || it.endIndex == ourIndex }

        val candidates = holeEdges.flatMap { he ->
            ourEdges.filter { e ->
                checkCorrect(he, problem.figure.calculateEdge(e), problem.epsilon)
            }.map { it to he }
        }

        var hedges by overlays

        hedges = Drawable {
            with(it) {
                for (candidate in candidates) {
                    withPaint(Color.GREEN.darker().transparent().transparent().transparent()) {
                        //drawLine(candidate.start, candidate.end)
                        val line = Line2D.Double(candidate.second.start, candidate.second.end)
                        it.draw(Drawable.Shape(BasicStroke(0.8f).createStrokedShape(line)))
                    }
                }
            }
        }

        canvas.invokeRepaint()
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
    var startingPoint: IndexedValue<Point>? = null
    var startFigure: Figure? = null
    canvas.onMousePan(
        filter = { SwingUtilities.isLeftMouseButton(it) },
        destructor = { _, _ ->
            startingPoint = null
            startFigure = null
            currentCoordinates = null
            validPoints.clear()
            canvas.invokeRepaint()
        }
    ) { start, prev, e ->
        val startPoint = (startFigure ?: figure).vertices.withIndex().minByOrNull { (_, v) -> v.distance(start.canvasPoint) }
        if (startPoint == null) return@onMousePan
        if (startingPoint == null || startingPoint != startPoint) {
            startingPoint = startPoint
            startFigure = figure
            val pointEdges = startFigure!!.edges.filter { it.startIndex == startPoint.index || it.endIndex == startPoint.index }
            holePoints.forEach {
                val newFigure = figure.copy(vertices = figure.vertices.toMutableList().apply {
                    this[startPoint.index] = it
                })

                val countCorrect = pointEdges.count { dataEdge ->
                    val oldEdge = Edge(startingFigure.vertices[dataEdge.startIndex], startingFigure.vertices[dataEdge.endIndex])
                    val newEdge = Edge(newFigure.vertices[dataEdge.startIndex], newFigure.vertices[dataEdge.endIndex])

                    checkCorrect(oldEdge, newEdge, problem.epsilon)
                }
                if (countCorrect >= 1) {
                    validPoints[it] = countCorrect.toDouble() / pointEdges.size
                }
            }
        }
        val stt = figure.vertices.withIndex().minByOrNull { (_, v) -> v.distance(prev.canvasPoint) }
        if (stt == null) return@onMousePan
        currentCoordinates = stt.value
        if (prev.canvasPoint.round() == e.canvasPoint.round()) return@onMousePan
        val (ix, _) = stt

        figure = figure.copy(vertices = figure.vertices.toMutableList().apply {
            this[ix] = e.canvasPoint.round()
        })
        canvas.invokeRepaint()

    }
    dumbFrame(canvas, "TASK# ${problem.number}").defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    return GUIController(figureStack, canvas)
}


fun Point.to2D() = Point2D(x.toDouble(), y.toDouble())
