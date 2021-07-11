package ru.spbstu.icpfc2021


import com.jme3.app.SimpleApplication
import com.jme3.asset.AssetManager
import com.jme3.bullet.BulletAppState
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape
import com.jme3.bullet.control.RigidBodyControl
import com.jme3.bullet.joints.ConeJoint
import com.jme3.bullet.joints.PhysicsJoint
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.MouseButtonTrigger
import com.jme3.light.AmbientLight
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.FastMath
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.VertexBuffer
import com.jme3.scene.shape.Box
import com.jme3.util.BufferUtils
import ru.spbstu.icpfc2021.model.*
import ru.spbstu.icpfc2021.solver.cartesian
import java.io.File
import kotlin.math.sqrt


fun main(args: Array<String>) {
//    val index = args[0].toInt()
    val index = 90
    val json = File("problems/$index.problem").readText()
    println("$index.problem")
    val problem = readProblem(index, json)
    println(problem)
    val app = TestRagDoll(problem)
    app.start()
}

class TestRagDoll(val problem: Problem) : SimpleApplication(), ActionListener {
    private var bulletAppState = BulletAppState()
    private val ragDoll = Node()
    private val upforce = Vector3f(0f, 200f, 0f)
    val edgeLimbs = mutableMapOf<DataEdge, Node>()
    private var applyForce = true
    override fun simpleInitApp() {
        bulletAppState = BulletAppState()
        stateManager.attach(bulletAppState)
        bulletAppState.isDebugEnabled = true
        inputManager.addMapping("Pull ragdoll up", MouseButtonTrigger(0))
        inputManager.addListener(this, "Pull ragdoll up")
        createPhysicsTestWorld(rootNode, assetManager, bulletAppState.physicsSpace)
        createRagDoll()
    }

    private fun Point.vec() = Vector3f(x.toFloat(), y.toFloat(), 0f)
    private fun DataEdge.reverted() = DataEdge(endIndex, startIndex)

    private fun DataEdge.calculate() =
        let { Edge(problem.figure.vertices[it.startIndex], problem.figure.vertices[it.endIndex]) }

    private fun createRagDoll() {

        for (edge in problem.figure.edges) {
            edgeLimbs[edge] = createLimb(
                sqrt(edge.calculate().squaredLength.toFloat()),
                0.1f, //                sqrt(edge.calculate().squaredLength.toFloat()),
                edge.calculate().start.vec(),
                false
            )
        }
        val joints = mutableListOf<PhysicsJoint>()
        for ((i, vertex) in problem.figure.vertices.withIndex()) {
            val startsHere = problem.figure.edges.filter { it.startIndex == i }
            val endsHere = problem.figure.edges.filter { it.endIndex == i }.map { it.reverted() }
            val allEdges = (startsHere + endsHere).toSet()
            val allEdgePairs = listOf(allEdges, allEdges).cartesian().toSet().filter { it.toSet().size != it.size }
            val connectionPoint = vertex.vec()
            for ((edgea, edgeb) in allEdgePairs) {
                val nodeA = edgeLimbs[edgea] ?: edgeLimbs[edgea.reverted()] ?: error("No limb for edge")
                val nodeB = edgeLimbs[edgeb] ?: edgeLimbs[edgeb.reverted()] ?: error("No limb for edge")
                joints += join(nodeA, nodeB, connectionPoint)
            }
        }

        edgeLimbs.forEach { ragDoll.attachChild(it.value) }
        rootNode.attachChild(ragDoll)
        bulletAppState.physicsSpace.addAll(ragDoll)
    }

    private fun createLimb(width: Float, height: Float, location: Vector3f, rotate: Boolean): Node {
        val axis = if (rotate) PhysicsSpace.AXIS_X else PhysicsSpace.AXIS_Y
        val shape = CapsuleCollisionShape(width, height, axis)
        val node = Node("Limb")
        val rigidBodyControl = RigidBodyControl(shape, 1f)
        node.localTranslation = location
        node.addControl(rigidBodyControl)
        return node
    }

    private fun join(A: Node, B: Node, connectionPoint: Vector3f): PhysicsJoint {
        val pivotA = A.worldToLocal(connectionPoint, Vector3f())
        val pivotB = B.worldToLocal(connectionPoint, Vector3f())
        val joint = ConeJoint(
            A.getControl(RigidBodyControl::class.java),
            B.getControl(RigidBodyControl::class.java),
            pivotA, pivotB
        )
        joint.setLimit(1f, 1f, 0f)
        return joint
    }

    override fun onAction(string: String, bln: Boolean, tpf: Float) {
        if ("Pull ragdoll up" == string) {
            applyForce = when {
                bln -> {
                    edgeLimbs.values.random().getControl(RigidBodyControl::class.java).activate()
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    override fun simpleUpdate(tpf: Float) {
        if (applyForce) {
            edgeLimbs.values.random().getControl(RigidBodyControl::class.java).applyForce(upforce, Vector3f.ZERO)
        }
    }


    /**
     * creates a simple physics test world with a floor, an obstacle and some test boxes
     *
     * @param rootNode where lights and geometries should be added
     * @param assetManager for loading assets
     * @param space where collision objects should be added
     */
    fun createPhysicsTestWorld(rootNode: Node, assetManager: AssetManager, space: PhysicsSpace) {
        val light = AmbientLight()
        light.color = ColorRGBA.LightGray
        rootNode.addLight(light)
        val material = Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
//        material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"))

//        val floorBox = createFloorMesh(5, 2f)
        val floorBox = Box(140f, 0.25f, 140f)
        val floorGeometry = Geometry("Floor", floorBox)
        floorGeometry.material = material
        floorGeometry.setLocalTranslation(0f, -5f, 0f)
        //        Plane plane = new Plane();
//        plane.setOriginNormal(new Vector3f(0, 0.25f, 0), Vector3f.UNIT_Y);
//        floorGeometry.addControl(new RigidBodyControl(new PlaneCollisionShape(plane), 0));
        floorGeometry.addControl(RigidBodyControl(0f))
        rootNode.attachChild(floorGeometry)
        space.add(floorGeometry)

    }


    private fun createFloorMesh(meshDetail: Int, floorDimensions: Float): Mesh {
        var meshDetail = meshDetail
        if (meshDetail < 10) {
            meshDetail = 10
        }
        val numVertices = meshDetail * meshDetail * 2 * 3 //width * depth * two tris * 3 verts per tri
        val indexBuf = IntArray(numVertices)
        var i = 0
        for (x in 0 until meshDetail) {
            for (z in 0 until meshDetail) {
                indexBuf[i] = i++
                indexBuf[i] = i++
                indexBuf[i] = i++
                indexBuf[i] = i++
                indexBuf[i] = i++
                indexBuf[i] = i++
            }
        }
        val vertBuf = FloatArray(numVertices * 3)
        val xIncrement = floorDimensions / meshDetail
        val zIncrement = floorDimensions / meshDetail
        var j = 0
        for (x in 0 until meshDetail) {
            val xPos = x * xIncrement
            for (z in 0 until meshDetail) {
                val zPos = z * zIncrement
                //First tri
                vertBuf[j++] = xPos
                vertBuf[j++] = getY(xPos, zPos, floorDimensions)
                vertBuf[j++] = zPos
                vertBuf[j++] = xPos
                vertBuf[j++] = getY(xPos, zPos + zIncrement, floorDimensions)
                vertBuf[j++] = zPos + zIncrement
                vertBuf[j++] = xPos + xIncrement
                vertBuf[j++] = getY(xPos + xIncrement, zPos, floorDimensions)
                vertBuf[j++] = zPos
                //Second tri
                vertBuf[j++] = xPos
                vertBuf[j++] = getY(xPos, zPos + zIncrement, floorDimensions)
                vertBuf[j++] = zPos + zIncrement
                vertBuf[j++] = xPos + xIncrement
                vertBuf[j++] = getY(xPos + xIncrement, zPos + zIncrement, floorDimensions)
                vertBuf[j++] = zPos + zIncrement
                vertBuf[j++] = xPos + xIncrement
                vertBuf[j++] = getY(xPos + xIncrement, zPos, floorDimensions)
                vertBuf[j++] = zPos
            }
        }
        val m = Mesh()
        m.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(*indexBuf))
        m.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(*vertBuf))
        m.updateBound()
        return m
    }

    private fun getY(x: Float, z: Float, max: Float): Float {
        val yMaxHeight = 8f
        val xv = FastMath.unInterpolateLinear(FastMath.abs(x - max / 2), 0f, max) * FastMath.TWO_PI
        val zv = FastMath.unInterpolateLinear(FastMath.abs(z - max / 2), 0f, max) * FastMath.TWO_PI
        val xComp = (FastMath.sin(xv) + 1) * 0.5f
        val zComp = (FastMath.sin(zv) + 1) * 0.5f
        return -yMaxHeight * xComp * zComp
    }
}