package scaffold_graph

import primitives.ContigBarcodes
import kotlin.math.min

class ScoreGraph(val vertices: List<ScoreGraphVertex>) {
    private val edgesFrom = mutableMapOf<ScoreGraphVertex, MutableList<Edge>>()
    private val edgesTo   = mutableMapOf<ScoreGraphVertex, MutableList<Edge>>()

    fun edges(): List<Edge> = edgesFrom.values.flatMap { it.toList() }

    fun addEdge(v: ScoreGraphVertex, u: ScoreGraphVertex, score: Double) {
        val e = Edge(v, u, score)
        edgesFrom.putIfAbsent(v, mutableListOf())
        edgesFrom[v]!!.add(e)

        edgesTo.putIfAbsent(u, mutableListOf())
        edgesTo[u]!!.add(e)
    }

    class Edge(val from: ScoreGraphVertex, val to: ScoreGraphVertex, val score: Double)
}

fun genScoreGraph(contigBarcodes: List<ContigBarcodes>, partLen: Int = 3000): ScoreGraph {
    val vertices: MutableList<ScoreGraphVertex> = mutableListOf()
    contigBarcodes.forEach {
        if (it.length > partLen) {
            vertices.add(ScoreGraphVertex(it.name, it.beginBarcodes(partLen), it.endBarcodes(partLen), false))
            vertices.add(ScoreGraphVertex(it.name, it.endBarcodes(partLen), it.beginBarcodes(partLen), true))
        }
    }

    val barcodeVertexBegins = mutableMapOf<Int, MutableList<Int>>()
    val barcodeVertexEnds = mutableMapOf<Int, MutableList<Int>>()


    for (i in vertices.indices) {
        for (barcode in vertices[i].beginInfo.set) {
            if (barcodeVertexBegins[barcode] == null) {
                barcodeVertexBegins[barcode] = mutableListOf(i)
            } else {
                barcodeVertexBegins[barcode]!!.add(i)
            }
        }

        for (barcode in vertices[i].endInfo.set) {
            if (barcodeVertexEnds[barcode] == null) {
                barcodeVertexEnds[barcode] = mutableListOf(i)
            } else {
                barcodeVertexEnds[barcode]!!.add(i)
            }
        }
    }

    val intersections = mutableMapOf<Pair<Int,Int>, Double>()

    for (barcode in barcodeVertexBegins.keys) {
        var cnt = 0
        barcodeVertexBegins[barcode]?.forEach { i ->
            barcodeVertexEnds[barcode]?.forEach { j ->
                if (vertices[i].contigName != vertices[j].contigName) {
                    intersections[Pair(i, j)] = (intersections[Pair(i, j)] ?: 0.0) + 1.0
                    cnt += 1
                }
            }
        }
    }

    val graph = ScoreGraph(vertices)
    intersections.forEach {
        val (i, j) = it.key
        val size = it.value
        val score = size / (0.5 * (vertices[i].beginInfo.set.size + vertices[j].endInfo.set.size))
        graph.addEdge(vertices[i], vertices[j], score)
    }

    return graph
}

