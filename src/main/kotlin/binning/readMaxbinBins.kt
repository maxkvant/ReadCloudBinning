package binning

import primitives.readFasta
import java.io.File

fun readMaxbinBins(dir: String): Map<String, Int> {
    require(File(dir).isDirectory)
    val binsFiles = File(dir).listFiles().filter { it.extension == "fasta" }

    val res = mutableMapOf<String, Int>()
    for (i in binsFiles.indices) {
        readFasta(binsFiles[i].absolutePath).forEach {
            res[it.name] = i + 1
        }
    }
    return res.withDefault { 0 }
}