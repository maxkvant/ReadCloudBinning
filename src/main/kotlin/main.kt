import binning.ScoreBinner
import com.github.sh0nk.matplotlib4j.Plot
import debruijn_graph.readPairedBarcodeIndex
import debruijn_graph.readSpadesGraph
import primitives.*
import scaffold_graph.ScoreGraph
import scaffold_graph.ScoreGraphVertex
import scaffold_graph.genScoreGraph
import scaffold_graph.toScgRecord
import java.io.File
import java.io.PrintWriter
import kotlin.math.min
import kotlin.math.roundToInt

fun main(args: Array<String>) {
    val samFile1 = "/media/maxim/DATA/binning/output/simulated_dataset_1/alignments/aln_contigs.bam"
    val bins1 = "/media/maxim/DATA/binning/output/simulated_dataset_1/maxbin3"
    val pafFile1 = "/media/maxim/DATA/binning/output/simulated_dataset_1/tmp_read_cloud_binning/aln_contigs.paf"
    val contigs1 = "/media/maxim/DATA/binning/output/simulated_dataset_1_master/contigs.fasta"

    val samFile2 = "/Iceking/mvinnichenko/binning/mock/alignments/aln_contigs.sam"
    val bins2 = "/Iceking/mvinnichenko/binning/mock/maxbin_spades_scaffolds"
    val samFile3 = "/Iceking/mvinnichenko/binning/human_gut_other/alignments/aln_contigs.bam"


    val binner = ScoreBinner(contigs1, samFile1, "scoreBinner")
    binner.binContigs()

    val contigBarcodes = readContigsBarcodes(samFile1)
    println(contigBarcodes.size)
    val contigColors = readBins(bins1)


    val debruijnGraph = readSpadesGraph(55, "/media/maxim/DATA/binning/output/simulated_dataset_1_master/assembly_graph.fastg")

    val pathsFile1 = "/media/maxim/DATA/binning/output/simulated_dataset_1_master/contigs.paths"
    val contigPaths = readContigPaths(debruijnGraph, pathsFile1)
            .filter { !it.contig.endsWith("'") }
            .groupBy { it.contig }
            .mapValues { it.value.first() }

    print(contigPaths.size)

    val graph = genScoreGraph(contigBarcodes)

    val pafRecords = readPafRecords(pafFile1)
    val genomePath = genGenomePath(pafRecords)

    val graphsDir = "graphs"
    File(graphsDir).mkdirs()

    fun ScoreGraphVertex.pathStart(): Int {
        val contigPath = contigPaths[this.contigName]!!
        return if (!this.rc) contigPath.begin.id else contigPath.end.id
    }

    fun ScoreGraphVertex.pathEnd(): Int {
        val contigPath = contigPaths[this.contigName]!!
        return if (!this.rc) contigPath.end.id else contigPath.begin.id
    }

    val thresholds = listOf(0.15, 0.12, 0.1)

    for (threshold in thresholds) {
        println(threshold)
        val dotFile = File("$graphsDir/sim_1_score_${threshold}.dot")
        dotFile.printWriter().use { printWriter ->
            val edges = graph.edges().filter {
                it.score > threshold
            }
            outputToDot(edges, printWriter, contigColors = contigColors)
        }
    }

    val scgFile = File("$graphsDir/scoreGraph.scg")
    scgFile.printWriter().use { printWriter ->
        graph.edges().forEach { e ->
            printWriter.println(e.toScgRecord())
        }
    }
}

fun genGenomePath(pafRecords: List<PafRecord>): MutableSet<Pair<String, String>> {
    val byRef = pafRecords.groupBy { it.refAlignment.name }
    val res = mutableSetOf<Pair<String, String>>()
    byRef.forEach { _, records ->
        val sortedRecords = records.filter { it.mapQual >= 20 }.sortedBy { (it.refAlignment.start + it.refAlignment.end) / 2 }.toList()
        for (i in sortedRecords.indices) {
            val j = (i + 1) % sortedRecords.size
            val vName = sortedRecords[i].qAlignment.name
            val uName = sortedRecords[j].qAlignment.name
            res.add(Pair(vName, uName))
            //res.add(Pair(uName, vName))
        }
    }
    return res
}

fun outputToDot(edges: List<ScoreGraph.Edge>,
                printWriter: PrintWriter,
                contigColors: Map<String, String> = mapOf()
) {
    val vertices = edges.map { it.from }.toSet()

    printWriter.println("digraph score {")
    fun fixName(v: ScoreGraphVertex): String {
        return v.contigName.split("_", ".").take(6).joinToString("_")
    }

    vertices.groupBy { it.contigName }.forEach { name, vs ->
        vs.forEach {v ->
            printWriter.println("${fixName(v)} [style=<filled>, fillcolor=<${contigColors[v.contigName] ?: "white"}>]")
        }
    }
    printWriter.println()
    edges.forEach { edge ->
        //val color: String = if (isSiblings) "blue" else "black"
        fun color(v: ScoreGraphVertex) = if (v.rc) "blue" else "red"
        printWriter.println("${fixName(edge.from)} -> ${fixName(edge.to)} [label=\"${(1000 * edge.score).roundToInt()}\", color=\"${color(edge.from)};0.5:${color(edge.to)}\"]")
    }
    printWriter.println("}")
}

