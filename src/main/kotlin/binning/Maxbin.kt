package binning

import primitives.readFasta
import utils.execCmd
import java.io.File

const val maxbinPath = "/home/mvinnichenko/software/MaxBin-2.2.5/run_MaxBin.pl" //"/media/maxim/DATA/software/MaxBin-2.2.5/run_MaxBin.pl"

class Maxbin(override val outdir: String, override val contigsFile: File, val coveragesFile: File): Binner {
    private var result: Map<String, Int>? = null

    override fun getBins(): Map<String, Int> {
        val res = result ?: run()
        result = res
        return res
    }

    private fun run(): MutableMap<String, Int> {
        File(outdir).deleteRecursively()
        File(outdir).mkdirs()
        execCmd("$maxbinPath -contig ${contigsFile.absolutePath} -abund ${coveragesFile.absolutePath} -out $outdir/bin")

        val binsFiles = File(outdir).listFiles().filter { it.extension == "fasta" }

        val res = mutableMapOf<String, Int>()
        for (i in binsFiles.indices) {
            readFasta(binsFiles[i].absolutePath).forEach {
                res[it.name] = i + 1
            }
        }
        return res
    }
}