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
    class Dataset(val name: String, val contigsFile: String, val samFile: String, val dir: String)

    val dataset_sim_1 = Dataset(
            name = "sim_1",
            contigsFile = "/media/maxim/DATA/binning/output/simulated_dataset_1_master/contigs.fasta",
            samFile = "/media/maxim/DATA/binning/output/simulated_dataset_1/alignments/aln_contigs.bam",
            dir = "."
    )

    val dataset_mock = Dataset(
            name = "mock",
            contigsFile = "/Iceking/mvinnichenko/binning/mock/spades/baseline_scaffolds.fasta",
            samFile = "/Iceking/mvinnichenko/binning/mock/alignments/aln_contigs.sam",
            dir = "/Iceking/mvinnichenko/binning/mock"
    )

    val dataset_hmp_mock = Dataset(
            name = "hmp_mock",
            contigsFile = "/Iceking/mvinnichenko/binning/mash_refs_by_snurk/spades/contigs.fasta",
            samFile = "/Iceking/mvinnichenko/binning/mash_refs_by_snurk/alignments/aln_reads_refs.sam",
            dir = "/Iceking/mvinnichenko/binning/mash_refs_by_snurk"
    )

    val dataset = dataset_sim_1
    ScoreBinner(
            contigsFile = File(dataset.contigsFile),
            samFile = dataset.samFile,
            outdir = "${dataset.dir}/scoreBinner"
    ).getBins()
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

