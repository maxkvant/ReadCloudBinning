package debruijn_graph

class ContigPath(val contig: String, val blocsEdges: List<List<DebruijnGraph.Edge>>) {
    init {
        blocsEdges.forEach { edges ->
            for (i in 0 until edges.size - 1) {

                if (setOf(edges[i].from, edges[i].to, edges[i].from.rc(), edges[i].to.rc())
                                .intersect(setOf(edges[i + 1].from, edges[i + 1].to)).isEmpty()) {
                    println("${edges[i].id} ${edges[i + 1].id}")
                    println(edges[i].from)
                    println(edges[i].to)
                    println(edges[i + 1].from)
                    println(edges[i + 1].to)
                    println()
                }
//                require(edges[i].to == edges[i + 1].from || edges[i].from == edges[i + 1].to)
            }
        }
    }

    val begin: DebruijnGraph.Edge
        get() = blocsEdges.first().first()

    val end: DebruijnGraph.Edge
        get()= blocsEdges.last().last()
}