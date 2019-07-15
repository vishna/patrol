package dev.vishna.patrol

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.StringReader

/**
 * Holds current working directory as path.
 */
val pwd: FilePath by lazy { System.getProperty("user.dir") }

typealias FilePath = String
fun FilePath.asFile() : File {
    val file = File(this)
    return if (file.exists()) {
        file
    } else {
        File(pwd, this)
    }
}

internal fun String.asYamlArray() :  ArrayList<*> = Yaml().load(StringReader(this)) as ArrayList<*>

fun String.saveAs(file: File) {
    file.writeText(text = this)
}