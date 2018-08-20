package primitives

import htsjdk.samtools.reference.FastaSequenceFile
import htsjdk.samtools.reference.ReferenceSequence
import java.io.File

fun readFasta(filename: String): List<ReferenceSequence> {
    FastaSequenceFile(File(filename), false).use { fastaFile ->
        val res = mutableListOf<ReferenceSequence>()
        var sequence: ReferenceSequence? = fastaFile.nextSequence()
        while (sequence != null) {
            res.add(sequence)
            sequence = fastaFile.nextSequence()
        }
        return res
    }
}