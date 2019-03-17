/*
 * Copyright (C) 2017-2019 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.hazuki.yuzubrowser.core.utility.utils

import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import jp.hazuki.yuzubrowser.core.utility.extensions.contains
import java.io.File

val externalUserDirectory: File
    get() = File(Environment.getExternalStorageDirectory().toString() + File.separator + "YuzuBrowser" + File.separator)

fun createUniqueFileName(root: DocumentFile, fileName: String, suffix: String): String {
    val children = root.listFiles()
    if (canUseName(children, fileName, suffix)) return fileName

    val checkName = CheckName(children)
    val parsedName = getParsedFileName(FileUtils.replaceProhibitionWord(fileName))

    var i = 1
    val builder = StringBuilder()
    val isSuffix = parsedName.suffix != null
    var newName: String
    var tmpName: String
    do {
        builder.append(parsedName.prefix).append('-').append(i++)
        if (isSuffix) {
            builder.append('.').append(parsedName.suffix)
        }
        newName = builder.toString()
        builder.append(suffix)
        tmpName = builder.toString()
        builder.delete(0, builder.length)
    } while (checkName.exists(newName) || children.contains { it.name == tmpName })

    return newName
}

private fun canUseName(fileList: Array<DocumentFile>, fileName: String, suffix: String?): Boolean {
    if (suffix.isNullOrEmpty()) {
        return !fileList.contains { it.name == fileName }
    }
    val tmpName = fileName + suffix
    fileList.forEach {
        val name = it.name
        if (name == fileName || name == tmpName) return false
    }
    return true
}

private fun Array<DocumentFile>.sortedFileName(): List<String> {
    return map { it.name ?: "" }
            .sortedWith(Comparator { s1, s2 ->
                val compared = compareLength(s1, s2)
                if (compared == 0) {
                    s1.compareTo(s2)
                } else {
                    compared
                }
            })
}

private class CheckName(items: Array<DocumentFile>) {
    private val files = items.sortedFileName()
    private val length = files.size
    private var index = 0

    fun exists(name: String): Boolean {
        while (length > index) {
            if (files[index] == name) return true
            index++
        }
        return false
    }
}

private fun compareLength(s1: String, s2: String): Int {
    return when {
        s1.length > s2.length -> 1
        s1.length < s2.length -> -1
        else -> 0
    }
}

class ParsedFileName internal constructor(var prefix: String, var suffix: String?)

fun getParsedFileName(filename: String): ParsedFileName {
    val point = filename.lastIndexOf(".")
    return if (point >= 0) {
        ParsedFileName(filename.substring(0, point), filename.substring(point + 1))
    } else {
        ParsedFileName(filename, null)
    }
}

fun getMimeType(fileName: String): String {
    val lastDot = fileName.lastIndexOf('.')
    if (lastDot >= 0) {
        val extension = fileName.substring(lastDot + 1).toLowerCase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        if (mime != null) {
            return mime
        }

        when (extension) {
            "mht", "mhtml" -> return "multipart/related"
            "js" -> return "application/javascript"
        }
    }
    return "application/octet-stream"
}