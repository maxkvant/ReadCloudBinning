package binning

import primitives.ContigBarcodes
import primitives.readContigsBarcodes
import primitives.readFasta
import java.io.File

fun readContigs(contigsFasta: String, samFile: String): List<ContigInfo> {
    val contigBarcodes = readContigsBarcodes(samFile).groupBy { it.name }.mapValues { it.value.first() }
    val res = mutableListOf<ContigInfo>()
    for (sequence in readFasta(contigsFasta)) {
        val coverage = sequence.name.split('_')[5].toDouble()
        val barcodes = contigBarcodes[sequence.name] ?: ContigBarcodes(sequence.name)
        res.add(ContigInfo(sequence.name, sequence.baseString, coverage, barcodes))
    }
    return res
}

fun outputSpadesCoverages(contigsFile: String, coveragesFile: String) {
    File(coveragesFile).printWriter().use {
        for (contig in readFasta(contigsFile)) {
            it.println("${contig.name} ${contig.name.split("_")[5]}")
        }
    }
}