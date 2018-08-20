package scaffold_graph

class ScoreGraphVertex(val contigName: String, beginBarcodes: List<Set<Int>>, endBarcodes: List<Set<Int>>, val rc: Boolean) {
    val endInfo = BarcodeInfo(endBarcodes)
    val beginInfo = BarcodeInfo(beginBarcodes)

    val frequentNum = (beginBarcodes.frequentBarcodesNum() + endBarcodes.frequentBarcodesNum()) / 2.0

    class BarcodeInfo(val barcodesList: List<Set<Int>>) {
        val set = barcodesList.joinToSet()
    }

    override fun equals(other: Any?): Boolean {
        if (other != null && other is ScoreGraphVertex) {
            return contigName == other.contigName && rc == other.rc
        } else {
            return false
        }
    }

    override fun hashCode(): Int = 31 * contigName.hashCode() + rc.hashCode()

    private fun List<Set<Int>>.frequentBarcodesNum(): Int {
        val counts = mutableMapOf<Int, Int>()
        var res = 0
        this.forEach { barcodeSet ->
            barcodeSet.forEach {
                counts[it] = (counts[it] ?: 0) + 1
                if (counts[it] == 2) {
                    res += 1
                }
            }
        }
        return res
    }
}

private fun <E> List<Set<E>>.joinToSet(): Set<E> {
    val res = mutableSetOf<E>()
    for (s in this) {
        res.addAll(s)
    }
    return res
}