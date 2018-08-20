package primitives

import primitives.PafRecord.Companion.parse
import java.io.File

class PafRecord(val qAlignment: PafAlignment, val refAlignment: PafAlignment, val matches: Int, val strand: Boolean, val mapQual: Int) {
    class PafAlignment(val name: String, val len: Int, val start: Int, val end: Int)

    companion object {
        fun parse(line: String): PafRecord {
            val pafElements = line.split("\\s+".toRegex()).take(12).toTypedArray()
            fun toPafAlignment(strings: Array<String>): PafAlignment {
                val (name, len, start, end) = strings
                return PafAlignment(name, len.toInt(), start.toInt(), end.toInt())
            }


            val qAlignment = toPafAlignment(pafElements.copyOfRange(0, 4))
            val refAlignment = toPafAlignment(pafElements.copyOfRange(5, 9))

            val strand = pafElements[4]
            val matches = pafElements[9]
            val mapQual = pafElements[11]
            return PafRecord(qAlignment, refAlignment, matches.toInt(), strand == "+", mapQual.toInt())
        }
    }
}

fun readPafRecords(pafFile: String): List<PafRecord> {
    File(pafFile).useLines { lines ->
        return lines.map { PafRecord.parse(it) }.toList()
    }
}