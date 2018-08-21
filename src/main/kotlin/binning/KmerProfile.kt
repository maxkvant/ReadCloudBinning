package binning

import debruijn_graph.rc
import kotlin.math.sqrt

const val profileK = 4
const val profileKmers = 1 shl (2 * profileK)

private val nucleotides = arrayOf('A', 'C', 'G', 'T')

class KmerProfile(val frequences: DoubleArray) {
    init {
        require(frequences.size == profileKmers)
    }

    fun dist(other: KmerProfile): Double {
        var sum = 0.0
        for (i in frequences.indices) {
            val diff = frequences[i] - other.frequences[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }
}

fun kmerProfileOf(sequences: List<String>): KmerProfile {
    val frequences = DoubleArray(profileKmers, { 0.0 })
    for (sequence in sequences) {
        for (seq in listOf(sequence, sequence.rc())) {
            forKmers@ for (i in 0..seq.length - profileK) {
                val kmer = seq.substring(i, i + profileK)
                var kmerI = 0
                for (c in kmer) {
                    if (!nucleotides.contains(c)) {
                        continue@forKmers
                    }
                    kmerI = kmerI * 4 + nucleotides.indexOf(c)
                }
                frequences[kmerI] += 1.0
            }
        }
    }
    val sum = frequences.sum()
    for (i in frequences.indices) {
        frequences[i] /= sum
    }
    return KmerProfile(frequences)
}