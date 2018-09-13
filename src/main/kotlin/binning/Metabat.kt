package binning

import primitives.readFasta
import utils.execCmd
import java.io.File

const val condaBin = "/home/mvinnichenko/anaconda3/bin"
const val metabatPath = "$condaBin/metabat"

class Metabat(override val outdir: String, override val contigsFile: File, val depthFile: File?) : Binner {
    override fun getBins(): Map<String, Int> {
        File(outdir).deleteRecursively()
        File(outdir).mkdirs()
        if (depthFile != null) {
            execCmd("$metabatPath -i ${contigsFile.absolutePath} -o $outdir/bin -a ${depthFile.absolutePath}")
        } else {
            execCmd("$metabatPath -i ${contigsFile.absolutePath} -o $outdir/bin")
        }
        val binsFiles = File(outdir).listFiles().filter { it.extension == "fa" }

        val res = mutableMapOf<String, Int>()
        for (i in binsFiles.indices) {
            readFasta(binsFiles[i].absolutePath).forEach {
                res[it.name] = i + 1
            }
        }
        return res
    }
}