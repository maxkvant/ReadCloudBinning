package debruijn_graph

import ch.qos.logback.classic.turbo.DynamicThresholdFilter
import org.apache.xmlgraphics.util.dijkstra.Edge
import kotlin.math.abs
import kotlin.math.min

class PairedBarcodeIndex(private val graph: DebruijnGraph) {
    private val edges = mutableMapOf<Int, MutableMap<Int, Edge>>()
    private val edgeBarcodes = mutableMapOf<Int, MutableSet<Int>>()

    fun add(edgeId1: Int, edgeId2: Int, barcode: Int) {
        val e1 = abs(edgeId1)
        val e2 = abs(edgeId2)
        if (e1 != e2) {
            edges.putIfAbsent(e1, mutableMapOf())
            edges.putIfAbsent(e2, mutableMapOf())
            edges[e1]!!.putIfAbsent(e2, Edge(e1, e2))
            edges[e2]!!.putIfAbsent(e1, Edge(e2, e1))

            edges[e1]!![e2]!!.barcodes.add(barcode)
        }
        edgeBarcodes.putIfAbsent(e1, mutableSetOf())
        edgeBarcodes[e1]!!.add(barcode)
        edgeBarcodes.putIfAbsent(e2, mutableSetOf())
        edgeBarcodes[e2]!!.add(barcode)
    }

    fun getEdges(edgeId: Int): List<Edge> = edges[edgeId]?.values?.toList() ?: emptyList()

    class Edge(val from: Int, val to: Int) {
        val barcodes = mutableSetOf<Int>()
    }

    fun checkPath(barcodes: Set<Int>, from1: Int, to1: Int, lenThreshold: Int = 30000, intersectionThreshold: Int = 1): Boolean {
        val from = abs(from1)
        val to = abs(to1)
        val queue = sortedMapOf<Int, MutableList<Int> >()
        val used = mutableSetOf<Int>()
        queue[0] = mutableListOf(from)

        while (!queue.isEmpty() && queue.firstKey() < lenThreshold) {
            val d = queue.firstKey()
            println(d)
            val vs = queue[d]!!
            queue.remove(d)
            for (v in vs) {
                if (v == to) {
                    println("found !")
                    return true
                }

                if (used.contains(v)) {
                    continue
                }
                used.add(v)

                for (e in getEdges(v)) {
                    val u = e.to
                    if (used.contains(u) || (edgeBarcodes[u]?.size ?: 0 > 100
                                    && barcodes.intersect(edgeBarcodes[u] ?: emptySet()).size < intersectionThreshold)) {
                        continue
                    }
                    val vLen = (graph.getEdge(v) ?: graph.getEdge(-v)!!).seq.length
                    val d2 = d + vLen + 200
                    queue.putIfAbsent(d2, mutableListOf())
                    queue[d2]!!.add(u)
                }
            }
        }
        return false
    }
}