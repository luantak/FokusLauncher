package com.lu4p.fokuslauncher.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NoteDocuments {
    suspend fun create(folder: String): String
    suspend fun write(document: String, text: String)
}

fun Context.persistNoteFolderAccess(folder: Uri) {
    contentResolver.takePersistableUriPermission(
            folder,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
}

fun noteFolderLabel(folder: String): String =
        Uri.parse(folder).lastPathSegment?.substringAfter(':').orEmpty().ifEmpty { folder }

class SafNoteDocuments(private val context: Context) : NoteDocuments {
    override suspend fun create(folder: String): String = withContext(Dispatchers.IO) {
        val tree = Uri.parse(folder)
        if (tree.scheme != "content" || !DocumentsContract.isTreeUri(tree)) {
            throw IOException("Invalid note folder")
        }
        val parent = DocumentsContract.buildDocumentUriUsingTree(
                tree, DocumentsContract.getTreeDocumentId(tree),
        )
        // The random suffix avoids collisions across rapid clears and other apps writing here.
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"))
        val name = "Note-$timestamp-${UUID.randomUUID()}.md"
        val document = DocumentsContract.createDocument(
                context.contentResolver, parent, "text/markdown", name,
        ) ?: throw IOException("Could not create a note in the selected folder")
        write(document.toString(), "")
        document.toString()
    }

    override suspend fun write(document: String, text: String) = withContext(Dispatchers.IO) {
        // Do not fall back to "w": providers may leave trailing bytes when a note becomes shorter.
        val stream = context.contentResolver.openOutputStream(Uri.parse(document), "wt")
                ?: throw IOException("Could not open the note file")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }
}
