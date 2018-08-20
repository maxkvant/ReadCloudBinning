package debruijn_graph

import sun.security.util.Length
import kotlin.math.abs

typealias Kmer = String

private val nucleotides = arrayOf('A', 'C', 'G', 'T')

class DebruijnGraph(val K: Int) {
    private val edges = mutableMapOf<Kmer, Array<Edge?>>()
    val idToEdge = mutableMapOf<Int, Edge>()

    private fun addVertex(kmer: Kmer) {
        require(kmer.length == K)
        edges.putIfAbsent(kmer, Array(nucleotides.size, { null }))
    }

    fun addEdgeAndRc(edge: Edge) {
        fun addEdge(edge: Edge) {
            addVertex(edge.from)
            addVertex(edge.to)
            edges[edge.from]!![edge.charId] = edge
            idToEdge[edge.id] = edge
        }
        addEdge(edge)
        addEdge(edge.rc())
    }

    fun getEdge(id: Int): Edge? = idToEdge[id]

    fun edgesFrom(kmer: Kmer): List<Edge> = edges[kmer]?.filterNotNull() ?: emptyList()

    fun edges(): List<Edge> = edges.values.flatMap { it.filterNotNull() }

    fun buildEdge(seq: String, name: String, id: Int, rc: Boolean): Edge {
        return Edge(seq, name, if (rc) -id else id)
    }

    inner class Edge(val seq: String, val name: String, val id: Int) {
        init {
            require(seq.length >= K + 1)
        }

        val from: Kmer
            get() = seq.substring(0, K)

        val to: Kmer
            get() = seq.substring(seq.length - K, seq.length)

        val charId: Int
            get() = nucleotides.indexOf(seq[K])

        val graph: DebruijnGraph
            get() = this@DebruijnGraph
    }

    fun checkPath(edgeId1: Int, edgeId2: Int, lenThreshold: Int = 15000): Boolean {
        val edge1 = getEdge(edgeId1)
        val edge2 = getEdge(edgeId2)
        if (edge1 == null || edge2 == null) {
            return false
        }
        val from = mutableListOf(edge1.to, edge1.from, edge1.rc().from, edge1.rc().to)
        val to = mutableListOf(edge2.from, edge2.to, edge2.rc().from, edge2.rc().to)

        val queue = sortedMapOf<Int, MutableList<Pair<Kmer, Int>> >()
        val used = mutableSetOf<Pair<Kmer, Int>>()
        queue[0] = from.map { Pair(it, 0) }.toMutableList()

        while (!queue.isEmpty() && queue.firstKey() < lenThreshold) {
            val d = queue.firstKey()
            val vs = queue[d]!!
            queue.remove(d)
            for (v in vs) {
                if (used.contains(v) || v.second > 4) {
                    continue
                }
                if (to.contains(v.first)) {
                    return true
                }
                used.add(v)
                for (e in edgesFrom(v.first)) {
                    val d2 = d + e.seq.length - K
                    queue.putIfAbsent(d2, mutableListOf())
                    val u = e.to
                    queue[d2]!!.add(Pair(u, v.second))
                }
            }
        }
        return false
    }

    fun Edge.rc(): Edge {
        return Edge(seq.rc(), this.name, -this.id)
    }
}

fun String.rc(): String {
    val resChars = this.toCharArray().map { c ->
        when (c) {
            'A' -> 'T'
            'C' -> 'G'
            'G' -> 'C'
            'T' -> 'A'
            else -> 'N'
        }
    }.reversed().toCharArray()
    return String(resChars)
}