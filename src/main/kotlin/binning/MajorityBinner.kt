package binning

import scaffold_graph.genScoreGraph
import java.io.File
import kotlin.math.max

const val kmerDistThreshold = 0.05
private const val leaderThreshold = 1.5

class MajorityBinner(override val contigsFile: File, samFile: String, override val outdir: String, val depthFile: File?): Binner {
    private val contigs = readContigs(contigsFile.absolutePath, samFile)
    private val contigsByName = contigs.groupBy { it.name }.mapValues { it.value.first() }
    private val scoreGraph = genScoreGraph(contigs.map { it.barcodes })
    private val tmpDir = "$outdir/majourityBinnerTmp"

    override fun getBins(): Map<String, Int> {
        File(outdir).deleteRecursively()
        File(tmpDir).deleteRecursively()

        File(outdir).mkdirs()
        File(tmpDir).mkdirs()

        val longNames = scoreGraph.vertices.map { it.contigName }.toSet()
        val contigsLong = contigsByName.filter { longNames.contains(it.key) }

        val bins = Metabat("$tmpDir/metabat", contigsFile, depthFile).getBins()

        val edges = scoreGraph.filteredEdges().filter { edge ->
            val seqFrom = contigsByName[edge.from.contigName]!!.seq
            val seqTo = contigsByName[edge.to.contigName]!!.seq
            val kmerDist = kmerProfileOf(seqFrom).dist(kmerProfileOf(seqTo))
            kmerDist < kmerDistThreshold
        }.toList()

        val resBins = bins.toMutableMap()

        val components = scoreGraph.connectedComponentsContigs(edges)
        for (component in components) {
            val szBefore = component.size
            val binSizes = component.map { contigsLong[it]!! }
                    .filter { bins[it.name] != null }
                    .groupBy { bins[it.name]!! }
                    .mapValues { (_, contigs) ->
                        contigs.map { it.seq.length }.sum()
                    }
            if (binSizes.isEmpty()) {
                continue
            }
            val bySize = binSizes.toList().sortedBy { it.second }
            println("componentSize: $szBefore -> ${bySize.size}")
            if (bySize.size == 1 || bySize[0].second > leaderThreshold * bySize[1].second) {
                component.forEach { resBins[it] = bySize[0].first }
            }
        }

        saveBins(outdir, contigs, resBins)
        return resBins
    }


}