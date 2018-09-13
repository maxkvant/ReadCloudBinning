package binning

import debruijn_graph.writeFastaElement
import scaffold_graph.genScoreGraph
import java.io.File
import kotlin.math.max

const val gapString = "NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN"

class ScoreBinner(override val contigsFile: File, samFile: String, override val outdir: String): Binner {
    private val contigs = readContigs(contigsFile.absolutePath, samFile)
    private val contigsByName = contigs.groupBy { it.name }.mapValues { it.value.first() }
    private val scoreGraph = genScoreGraph(contigs.map { it.barcodes })
    private val tmpDir = "$outdir/scoreBinnerTmp"

    override fun getBins(): Map<String, Int> {
        File(outdir).deleteRecursively()
        File(tmpDir).deleteRecursively()

        File(outdir).mkdirs()
        File(tmpDir).mkdirs()

        val longNames = scoreGraph.vertices.map { it.contigName }.toSet()
        val contigsLong = contigsByName.filter { longNames.contains(it.key) }

        var clusters = contigsLong.values.mapIndexed { i, contig -> ContigsCluster("CLUSTER_$i", listOf(contig)) }
        var resBins: Map<String, Int> = mapOf()

        repeat(3) { iteration ->
            val clustesFile = File("$tmpDir/clusters_$iteration.fasta")
            val coverageFile = File("$tmpDir/coverages_$iteration.abund")

            outputCoverages(coverageFile, clusters)
            outputClusters(clustesFile, clusters)
            val bins = Maxbin("$tmpDir/maxbin", clustesFile, coverageFile).getBins()

            resBins = toContigBins(clusters, bins)

            saveBins("$tmpDir/bins_$iteration", contigsLong.values.toList(), resBins)

            val contigsRs = estimateContigRadius(clusters, bins)
            val rAverage = contigsRs.values.distinct().average()

            val edges = scoreGraph.filteredEdges().filter { edge ->
                val seqFrom = contigsByName[edge.from.contigName]!!.seq
                val seqTo = contigsByName[edge.to.contigName]!!.seq
                val kmerDist = kmerProfileOf(seqFrom).dist(kmerProfileOf(seqTo))
                val maxR = max(contigsRs[edge.from.contigName] ?: rAverage, contigsRs[edge.to.contigName] ?: rAverage)
                kmerDist < maxR * 4
            }.toList()

            val components = scoreGraph.connectedComponentsContigs(edges)
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