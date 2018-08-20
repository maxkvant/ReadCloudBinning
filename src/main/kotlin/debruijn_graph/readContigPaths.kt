import debruijn_graph.ContigPath
import debruijn_graph.DebruijnGraph
import java.io.File

fun readContigPaths(graph: DebruijnGraph, pathsFile: String): List<ContigPath> {
    File(pathsFile).useLines { lines ->
        val res = mutableListOf<ContigPath>()
        var name = ""
        val ids = mutableListOf<List<DebruijnGraph.Edge>>()
        for (line in lines) {
            if (line.startsWith("NODE")) {
                if (ids.size != 0) {
                    res.add(ContigPath(name, ids.toList()))
                    ids.clear()
                }
                name = line.trim()
            } else {
                val path = line.split(",", ";").filter { it.length >= 2 }.map {
                    val rc = it.endsWith("-")
                    val id = it.substring(0, it.length - 1).toInt()
                    val idSigned = if (rc) -id else id
                    graph.getEdge(idSigned)!!
                }
                ids.add(path)
            }
        }
        res.add(ContigPath(name, ids.toList()))
        return res
    }
}