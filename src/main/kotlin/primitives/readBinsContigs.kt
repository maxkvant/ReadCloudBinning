package primitives

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.samtools.reference.ReferenceSequence
import java.io.File

fun readBins(binsDir: String): Map<String, String> {
    val colors = listOf("red", "green", "blue", "purple", "cyan")
    val binFiles = File(binsDir).listFiles().filter { it.name.endsWith(".fasta") }
    val contigColor = mutableMapOf<String, String>()
    for (i in binFiles.indices) {
        val sequences = readFasta(binFiles[i].absolutePath)
        sequences.forEach { contigColor[it.name] = colors[i] }
    }
    return contigColor
}