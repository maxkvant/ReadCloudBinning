package binning

import primitives.ContigBarcodes

class ContigInfo(val name: String, val seq: String, val coverage: Double, val barcodes: ContigBarcodes) {
    val kmerProfile = kmerProfileOf(seq)
}