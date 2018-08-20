package debruijn_graph

import htsjdk.samtools.reference.ReferenceSequence
import primitives.readFasta
import java.io.File
import java.io.PrintWriter
import kotlin.math.abs

fun readSpadesGraph(K: Int, fastgFile: String): DebruijnGraph {
    val graph = DebruijnGraph(K)

    val fastaFile = fastgFile.replace(".fastg", ".fasta")
    File(fastaFile).delete()
    File(fastgFile).copyTo(File(fastaFile))
    fun toEdge(sequence: ReferenceSequence): DebruijnGraph.Edge {
        var name = sequence.name.substringBefore(":")
        val rc: Boolean = name.endsWith('\'')
        if (rc) {
            name = name.substring(0, name.length - 1)
        }
        val id = name.split('_')[1].toInt()
        return graph.buildEdge(sequence.baseString, name, id, rc)
    }

    val fastaFixed = fastaFile.replace(".fasta", "_fixed.fasta")

    if (File(fastaFixed).exists()) {
        File(fastaFixed).delete()
    }

    File(fastaFixed).delete()
    val fixedWriter = PrintWriter(File(fastaFixed))

    val sequences = readFasta(fastaFile)
    val used = mutableSetOf<String>()
    for (sequence in sequences) {
        val edge = toEdge(sequence)
        if (!used.contains(edge.name)) {
            graph.addEdgeAndRc(edge)
            used.add(edge.name)
        }
        fixedWriter.writeFastaElement(edge.name + (if (edge.id < 0) "'" else ""), edge.seq)
    }

    fixedWriter.close()

    graph.edges().sortedBy { it.seq.length }.forEach {
        println("${it.seq.length - it.graph.K} ${it.name}")
    }

    val shortIds = graph.edges().sortedBy { it.seq.length }.filter { it.seq.length < K + 10 }.distinct().map { abs(it.id) }.joinToString(",")
    println(shortIds)
    return graph
}

fun PrintWriter.writeFastaElement(header: String, sequence: String) {
    this.println(">" + header)
    sequence.chunked(80).forEach(this::println)
}