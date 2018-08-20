package primitives

import kotlin.math.min

fun readContigsBarcodes(samFile: String): List<ContigBarcodes> {
    fun tryLoad(): List<ContigBarcodes>? {
        val res: Any? = load("kotlin_saves", samFile)
        if (res == null) {
            return res
        } else {
            return res as List<ContigBarcodes>
        }
    }

    val fromSave = tryLoad()
    if (fromSave != null) {
        return fromSave
    }

    val contigBarcodes = mutableMapOf<String, ContigBarcodes>()

    forSamRecordsPaired(samFile) { left, right ->
        if (insertSize(left, right) < insertSizeThreshold
                && (left.alignmentLen() + right.alignmentLen()) >= 0.9 * (left.readLength + right.readLength)
                && left.contig == right.contig) {
            val barcode = left.barcode()!!
            val pos = min(left.alignmentStart, right.alignmentStart)
            contigBarcodes.putIfAbsent(left.contig, ContigBarcodes(left.contig))
            contigBarcodes[left.contig]!!.addBarcode(pos, barcode)
        }
    }

    val res = contigBarcodes.values.toList()
    save("kotlin_saves", samFile, ArrayList(res))
    return res
}