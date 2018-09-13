package binning

import debruijn_graph.writeFastaElement
import java.io.File

interface Binner {
    val outdir: String
    val contigsFile: File
    fun getBins(): Map<String, Int>

    fun saveBins(dir: String, contigs: List<ContigInfo>, bins: Map<String, Int>) {
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
}