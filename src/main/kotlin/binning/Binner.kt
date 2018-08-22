package binning

import java.io.File

interface Binner {
    val outdir: String
    val contigsFile: File
    fun getBins(): Map<String, Int>
}