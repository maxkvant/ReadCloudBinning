package binning

import debruijn_graph.rc
import debruijn_graph.writeFastaElement
import primitives.readBins
import scaffold_graph.ScoreGraph
import scaffold_graph.ScoreGraphVertex
import scaffold_graph.genScoreGraph
import utils.execCmd
import java.io.File
import kotlin.math.max

const val gapString = "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"

class ScoreBinner(contigsFile: String, samFile: String, val outdir: String) {
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

        var clusters = contigsLong.values.mapIndexed { i, contig -> ContigsCluster("CLUSTER_$i", listOf(contig)) }
        var clustersPrev = clusters
        var bins = mapOf<String, Int>()

        repeat(2) { _ ->
            outputCoverages(coverageFile, clusters)
            outputClusters(clustesFile, clusters)
            bins = Maxbin("$outdir/maxbin", clustesFile, coverageFile).getBins()
            val contigsRs = estimateContigRadius(clusters, bins)

            val edges = scoreGraph.filteredEdges().filter { edge ->
                val seqFrom = contigsByName[edge.from.contigName]!!.seq
                val seqTo = contigsByName[edge.to.contigName]!!.seq
                val kmerDist = kmerProfileOf(listOf(seqFrom)).dist(kmerProfileOf(listOf(seqTo)))
                val maxR = max(contigsRs[edge.from.contigName]!!, contigsRs[edge.to.contigName]!!)
                println("$kmerDist ${maxR * 3}")
                        kmerDist < maxR * 3
            }

            val components = connectedComponents(edges)
            clustersPrev = clusters
            clusters = components.mapIndexed { i, contigNames ->
                val contigs = contigNames.map { contigsByName[it]!! }
                ContigsCluster("CLUSTER_$i", contigs)
            }
        }
        val contigToBin = clustersPrev
                .flatMap { cluster -> cluster.contigs.map { Pair(it, bins[cluster.name]) } }
                .groupBy { it.second }
                .mapValues { (_, contigs) -> contigs.map { it.first } }

        val dir = "$outdir/bins"
        File(dir).deleteRecursively()
        File(dir).mkdirs()
        contigToBin.forEach { (bin, contigs) ->
            File("$dir/bin_$bin.fasta").printWriter().use {
                contigs.forEach { contig ->
                    it.writeFastaElement(contig.name, contig.seq)

                }
            }
        }
    }

    private fun connectedComponents(edges: List<ScoreGraph.Edge>): List<List<String>> {
        val edgesFrom = edges.groupBy { it.from.contigName }
        val component = mutableMapOf<String, Int>()

        fun dfs(v: String, c: Int) {
            if (component.containsKey(v)) {
                return
            }
            component[v] = c
            for (e in edgesFrom[v] ?: emptyList()) {
                dfs(e.to.contigName, c)
            }
        }
        var i = 0
        for (v in scoreGraph.vertices) {
            if (!component.containsKey(v.contigName)) {
                dfs(v.contigName, i)
                i += 1
            }
        }
        return scoreGraph.vertices.map { it.contigName }.distinct().groupBy { component[it] }.values.toList()
    }

    private fun outputCoverages(file: File, clusters: List<ContigsCluster>) {
        file.delete()
        file.printWriter().use { printWriter ->
            clusters.forEach {
                printWriter.println("${it.name} ${it.coverage}")
            }
        }
    }

    private fun outputClusters(file: File, clusters: List<ContigsCluster>) {
        file.delete()
        file.printWriter().use { printWriter ->
            clusters.forEach {
                printWriter.writeFastaElement(it.name, it.seq)
            }
        }
    }

    private fun estimateContigRadius(clusters: List<ContigsCluster>, bins: Map<String,Int>): Map<String, Double> {
        val binsRadius = clusters
                .filter { bins.containsKey(it.name) }
                .groupBy { bins[it.name]!! }
                .mapValues { (_, clustersByBin) ->
                    val centerProfile = kmerProfileOf(clustersByBin.map { it.seq })
                    val contigs = clustersByBin.flatMap { it.contigs }
                    Pair(contigs, contigs.map { it.kmerProfile.dist(centerProfile) }.max()!!)
                }
        println(binsRadius.values.map { it.second }.toList())
        val averageR = binsRadius.values.map { it.second }.average()
        val contigRs = binsRadius.values.flatMap { (contigs, r) -> contigs.map { Pair(it.name, r) }  }.toMap()
        return contigRs.withDefault { averageR }
    }

    fun runMaxbin(fastaFile: File, coveragesFile: File): String {
        val dir = "$outdir/maxbin"
        File(dir).deleteRecursively()
        File(dir).mkdirs()
        execCmd("$maxbinPath -contig ${fastaFile.absolutePath} -abund ${coveragesFile.absolutePath} -out $dir/bin")
        return dir
    }

    class ContigsCluster(val name: String, val contigs: List<ContigInfo>) {
        val seq = contigs.map { it.seq }.joinToString(gapString)
        val coverage = contigs.map { it.seq.length * it.coverage }.sum() / contigs.map { it.seq.length }.sum().toDouble()
    }
}