package primitives

import htsjdk.samtools.SAMRecord
import htsjdk.samtools.SamReaderFactory
import java.io.File
import kotlin.math.max
import kotlin.math.min

const val insertSizeThreshold = 1000
const val barcodeTag = "BX"

fun SAMRecord.barcode(): Int? {
    val barcode: String? = this.getStringAttribute(primitives.barcodeTag)
    if (barcode != null) {

        var barcodeInt = 0
        val nucleotides = "ACGT"
        barcode.forEach {
            if (nucleotides.contains(it)) {
                barcodeInt = 4 * barcodeInt + nucleotides.indexOf(it)
            }
        }
        return barcodeInt
    } else {
        return null
    }
}

fun insertSize(left: SAMRecord, right: SAMRecord): Int {
    return max(left.alignmentEnd, right.alignmentEnd) - min(left.alignmentStart, right.alignmentStart)
}

fun SAMRecord.alignmentLen() = this.alignmentEnd - this.alignmentStart

fun getSamIterator(samFile: String) = SamReaderFactory.makeDefault().open(File(samFile)).iterator()

fun forSamRecordsPaired(samFile: String, f: (SAMRecord, SAMRecord) -> Unit) {
    val samRecordIterator = getSamIterator(samFile)
    var iterations = 0

    while (samRecordIterator.hasNext()) {
        iterations += 1
        if (iterations % 1000000 == 0) {
            println("iteration $iterations")
        }

        val left = samRecordIterator.next()
        if (!samRecordIterator.hasNext()) {
            continue
        }
        val right = samRecordIterator.next()

        if (left.barcode() != null && right.barcode() != null
                && left.barcode() == right.barcode()) {
            f(left, right)
        }
    }
}