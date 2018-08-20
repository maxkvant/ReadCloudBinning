package primitives

import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

const val step = 100

class ContigBarcodes(val name: String): Serializable {
    private val barcodeSets: MutableList<MutableSet<Int>> = mutableListOf()

    fun addBarcode(pos: Int, barcode: Int) {
        val pos1 = pos / step
        while (barcodeSets.size <= pos1) {
            barcodeSets.add(mutableSetOf())
        }
        require(0 <= pos1 && pos1 < barcodeSets.size)
        barcodeSets[pos1].add(barcode)
    }

    fun beginBarcodes(len: Int): List<Set<Int>> {
        return barcodeSets
                .toTypedArray()
                .copyOfRange(0, min(barcodeSets.size - 1, len / step)).toList()
    }

    fun endBarcodes(len: Int): List<Set<Int>> {
        return barcodeSets.asReversed()
                .toTypedArray()
                .copyOfRange(0, min(barcodeSets.size - 1, len / step)).toList()
    }

    val length
            get() = step * barcodeSets.size

    fun barcodes(): List<Set<Int>> {
        return barcodeSets
    }
}