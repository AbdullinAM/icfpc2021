package ru.spbstu.icpfc2021.gui

import ru.spbstu.icpfc2021.model.*
import ru.spbstu.icpfc2021.model.Point
import ru.spbstu.icpfc2021.result.loadSolution
import ru.spbstu.icpfc2021.result.saveResult
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

interface Drawable {
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


fun drawFigure(problem: Problem, initialFigure: Figure? = null) {
    val (hole, startingFigure) = problem

    val verifier = Verifier(problem)
    val holePoints = verifier.getHolePoints().toSet()
    var validPoints = emptyList<Point>()

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
    val canvas = dumbCanvas {
        withPaint(Color.GRAY.brighter().brighter()) {
            val hole2D = GeneralPath()
            hole2D.moveTo(holeVertices.first())
            for (point in holeVertices.drop(1)) hole2D.lineTo(point)
            hole2D.closePath()
            fill(hole2D)
        }

        val graphEdges = figure.calculatedEdges

        for ((edge, oldEdge) in graphEdges.zip(startingFigure.calculatedEdges)) {
            val color = when {
                checkCorrect(oldEdge, edge, problem.epsilon) -> Color.BLUE
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

        for (point in validPoints) {
            withPaint(Color.GREEN) {
                fill(Ellipse2D(point, 1.5))
            }
        }

        absolute {
            withPaint(Color.ORANGE) {
                withFont(Font.decode("Fira-Mono-Bold-20")) {
                    drawString("dislikes: ${dislikes(hole, figure.currentPose)}", 20.0f, 20.0f)
                    if (currentCoordinates != null) {
                        drawString("current coordinates: x=${currentCoordinates!!.x}, y=${currentCoordinates!!.y}", 20.0f, 40.0f)
                    }
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
    canvas.onKey("control R") {
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
            validPoints = emptyList()
            canvas.invokeRepaint()
        }
    ) { start, prev, e ->
        val startPoint = (startFigure ?: figure).vertices.withIndex().minByOrNull { (_, v) -> v.distance(start.canvasPoint) }
        if (startPoint == null) return@onMousePan
        if (startingPoint == null || startingPoint != startPoint) {
            startingPoint = startPoint
            startFigure = figure
            val pointEdges = startFigure!!.edges.filter { it.startIndex == startPoint.index || it.endIndex == startPoint.index }
            validPoints = holePoints.filter {
                val newFigure = figure.copy(vertices = figure.vertices.toMutableList().apply {
                    this[startPoint.index] = it
                })

                pointEdges.all { dataEdge ->
                    val oldEdge = Edge(problem.figure.vertices[dataEdge.startIndex], problem.figure.vertices[dataEdge.endIndex])
                    val newEdge = Edge(newFigure.vertices[dataEdge.startIndex], newFigure.vertices[dataEdge.endIndex])

                    checkCorrect(oldEdge, newEdge, problem.epsilon)
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
    dumbFrame(canvas).defaultCloseOperation = JFrame.EXIT_ON_CLOSE
}


fun Point.to2D() = Point2D(x.toDouble(), y.toDouble())


fun testHole(): List<Point2D> =
    """
        [0,4],[11,0],[21,12],[27,0],[41,1],[56,0],[104,0],[104,25],[93,29],[97,41],[104,53],[82,57],[67,57],[58,49],[40,57],[25,57],[12,53],[0,56]
    """.trimIndent().split("],").map {
        val (x, y) = it.trim().removePrefix("[").removeSuffix("]").split(",")
        Point2D(x.trim().toInt(), y.trim().toInt())
    }

fun testFigure(): List<Pair<Point2D, Point2D>> {
    val vertices = """
        [26,37],[22,12],[2,20],[4,21],[5,0],[24,29],[25,12],[2,19],[5,23],[20,34],[0,46],[20,40]
    """.trimIndent().split("],").map {
        val (x, y) = it.trim().removePrefix("[").removeSuffix("]").split(",")
        Point2D(x.trim().toInt(), y.trim().toInt())
    }
    val edges = """
        [0,1],[0,3],[1,2],[2,4],[2,5],[3,5],[4,6],[5,7],[6,7],[6,8],[7,9],[8,10],[8,11],[9,10],[10,11]
    """.trimIndent().split("],").map {
        val (x, y) = it.trim().removePrefix("[").removeSuffix("]").split(",")
        x.trim().toInt() to y.trim().toInt()
    }
    return edges.map { vertices[it.first] to vertices[it.second] }
}

