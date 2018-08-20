package primitives

import java.io.*
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.abs


fun save(dir: String, name: String, o: Serializable) {
    val hash = name.hashCode()
    val path = Paths.get(dir, hash.toString())
    if (!File(dir).exists()) {
        File(dir).mkdirs()
    }
    FileOutputStream(path.toFile()).use { fos ->
        ObjectOutputStream(fos).use {
            it.writeObject(o)
        }
    }
}

fun load(dir: String, name: String): Any? {
    val hash = name.hashCode()
    val path = Paths.get(dir, hash.toString())
    if (path.toFile().exists() && path.toFile().isFile) {
        FileInputStream(path.toFile()).use { fis ->
            ObjectInputStream(fis).use {
                return it.readObject()
            }
        }
    } else {
        return null
    }
}