package binning

import debruijn_graph.writeFastaElement
import scaffold_graph.ScoreGraph
import scaffold_graph.genScoreGraph
import utils.execCmd
import java.io.File
import java.util.concurrent.ForkJoinPool
import kotlin.math.max
import kotlin.streams.toList

const val gapString = "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"

class ScoreBinner(override val contigsFile: File, samFile: String, override val outdir: String): Binner {
    private val contigs = readContigs(contigsFile.absolutePath, samFile)
    private val contigsByName = contigs.groupBy { it.name }.mapValues { it.value.first() }
    private val scoreGraph = genScoreGraph(contigs.map { it.barcodes })
    private val tmpDir = "$outdir/scoreBinnerTmp"
    private val threads = 16

    override fun getBins(): Map<String, Int> {
        File(outdir).deleteRecursively()
        File(tmpDir).deleteRecursively()

        File(outdir).mkdirs()
        File(tmpDir).mkdirs()

        val longNames = scoreGraph.vertices.map { it.contigName }.toSet()
        val contigsLong = contigsByName.filter { longNames.contains(it.key) }
        val clustesFile = File("$tmpDir/clusters.fasta")
        val coverageFile = File("$tmpDir/coverages.abund")

        var clusters = contigsLong.values.mapIndexed { i, contig -> ContigsCluster("CLUSTER_$i", listOf(contig)) }
        var resBins: Map<String, Int> = mapOf()

        repeat(4) { iteration ->
            outputCoverages(coverageFile, clusters)
            outputClusters(clustesFile, clusters)
            val bins = Maxbin("$tmpDir/maxbin", clustesFile, coverageFile).getBins()
            val contigsRs = estimateContigRadius(clusters, bins)

            val edges = scoreGraph.filteredEdges().filter { edge ->
                val seqFrom = contigsByName[edge.from.contigName]!!.seq
                val seqTo = contigsByName[edge.to.contigName]!!.seq
                val kmerDist = kmerProfileOf(listOf(seqFrom)).dist(kmerProfileOf(listOf(seqTo)))
                val maxR = max(contigsRs[edge.from.contigName]!!, contigsRs[edge.to.contigName]!!)
                        kmerDist < maxR * 3
            }.toList()

            resBins = toContigBins(clusters, bins)

            saveBins("$tmpDir/bins_$iteration", contigsLong.values.toList(), resBins)


            val components = connectedComponents(edges)
            clusters = components.mapIndexed { i, contigNames ->
                val contigs = contigNames.map { contigsByName[it]!! }
                ContigsCluster("CLUSTER_$i", contigs)
            }
        }
        saveBins(outdir, contigsLong.values.toList(), resBins)
        return resBins
    }

    fun toContigBins(clusters: List<ContigsCluster>, clusterBins: Map<String, Int>): Map<String, Int> {
        return clusters.flatMap { cluster -> cluster.contigs
                    .filter { clusterBins[cluster.name] != null }
                    .map { Pair(it.name, clusterBins[cluster.name]!!) }
        }.toMap()


    }

    private fun saveBins(dir: String, contigs: List<ContigInfo>, bins: Map<String, Int>) {
        if (!File(dir).exists()) {
            File(dir).mkdirs()
        }

        contigs.groupBy { bins[it.name] }.forEach { (bin, contigs) ->
            val binName = if (bin != null) "$dir/bin_$bin.fasta" else "$dir/bin_noclass"
            File(binName).printWriter().use {
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

    class ContigsCluster(val name: String, val contigs: List<ContigInfo>) {
        val seq = contigs.map { it.seq }.joinToString(gapString)
        val coverage = contigs.map { it.seq.length * it.coverage }.sum() / contigs.map { it.seq.length }.sum().toDouble()
    }
}