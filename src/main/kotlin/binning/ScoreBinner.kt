package binning

import debruijn_graph.writeFastaElement
import scaffold_graph.genScoreGraph
import java.io.File

const val gapString = "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"

class ScoreBinner(val contigsFile: String, val samFile: String, val outdir: String) {
    private val contigs = readContigs(contigsFile, samFile)
    private val contigsByName = contigs.groupBy { it.name }.mapValues { it.value.first() }
    private val scoreGraph = genScoreGraph(contigs.map { it.barcodes })

    fun binContigs() {
        if (!File(outdir).exists()) {
            File(outdir).mkdirs()
        }
        val longNames = scoreGraph.vertices.map { it.contigName }.toSet()
        val contigsLong = contigsByName.filter { longNames.contains(it.key) }
        val clustesFile = File("$outdir/clusters.fasta")
        val coverageFile = File("$outdir/coverages.abund")

        val clusters = contigsLong.values.mapIndexed { i, contig -> Pair("CLUSTER_$i", listOf(contig)) }
        outputCoverages(coverageFile, clusters)
        outputClusters(clustesFile, clusters)
    }

    private fun outputCoverages(file: File, clusters: List<Pair<String, List<ContigInfo>>>) {
        file.delete()
        file.printWriter().use { printWriter ->
            clusters.forEach { cluster ->
                val (name, contigs) = cluster
                val len = contigs.map { it.seq.length }.sum()
                val coverageSum = contigs.map { it.seq.length * it.coverage }.sum()
                printWriter.println("$name ${coverageSum / len}")
            }
        }
    }

    private fun outputClusters(file: File, clusters: List<Pair<String, List<ContigInfo>>>) {
        file.delete()
        file.printWriter().use { printWriter ->
            clusters.forEach { cluster ->
                val (name, contigs) = cluster
                val seq = contigs.map { it.seq }.joinToString(gapString)
                printWriter.writeFastaElement(name, seq)
            }
        }
    }
}