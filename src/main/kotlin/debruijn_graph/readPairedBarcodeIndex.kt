package debruijn_graph

import htsjdk.samtools.SAMRecord
import primitives.barcode
import primitives.forSamRecordsPaired

fun readPairedBarcodeIndex(graph: DebruijnGraph, samFile: String): PairedBarcodeIndex {
    fun SAMRecord.id(): Int = this.referenceName.split("_")[1].toInt()
    val index = PairedBarcodeIndex(graph)

    forSamRecordsPaired(samFile) { left, right ->
        index.add(left.id(), right.id(), left.barcode()!!)
    }

    return index
}