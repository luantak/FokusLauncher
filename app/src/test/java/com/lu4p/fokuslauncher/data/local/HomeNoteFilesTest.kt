package com.lu4p.fokuslauncher.data.local


import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test
import org.junit.After
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeNoteFilesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val documents = MemoryNoteDocuments()
    private val preferences by lazy {
        PreferencesManager(RuntimeEnvironment.getApplication(), documents,
                PreferenceDataStoreFactory.create(scope = scope) {
                    temporaryFolder.newFile("note.preferences_pb")
                })
    }
    @After fun tearDown() { scope.cancel() }

    @Test
    fun selectingFolderExportsExistingNote() = runBlocking {
        preferences.setHomeNoteText("# Existing")
        preferences.setHomeNoteFolder("folder-a")
        assertEquals("# Existing", documents.files.values.single())
        assertEquals("folder-a", preferences.homeNoteFolderFlow.first())
    }

    @Test
    fun editsReuseCurrentFileAndClearPreservesUnsavedDraft() = runBlocking {
        preferences.setHomeNoteFolder("folder-a")
        preferences.setHomeNoteText("saved")
        preferences.startNewHomeNote("unsaved draft")
        assertEquals(listOf("unsaved draft", ""), documents.files.values.toList())
        preferences.setHomeNoteText("next note")
        assertEquals(listOf("unsaved draft", "next note"), documents.files.values.toList())
        assertEquals("next note", preferences.homeNoteTextFlow.first())
    }

    @Test
    fun clearWithoutFolderRemainsLocal() = runBlocking {
        preferences.setHomeNoteText("old")
        preferences.startNewHomeNote("draft")
        assertEquals("", preferences.homeNoteTextFlow.first())
        assertTrue(documents.files.isEmpty())
    }

    @Test
    fun failedNewFileDoesNotClearCurrentNote() = runBlocking {
        preferences.setHomeNoteText("old")
        preferences.setHomeNoteFolder("folder-a")
        documents.failCreate = true
        try {
            preferences.startNewHomeNote("latest draft")
            fail("Expected storage failure")
        } catch (_: IOException) { }
        assertEquals("latest draft", preferences.homeNoteTextFlow.first())
        assertEquals("latest draft", documents.files.values.single())
    }

    @Test
    fun disablingFolderLeavesFilesAndStopsDiskWrites() = runBlocking {
        preferences.setHomeNoteText("old")
        preferences.setHomeNoteFolder("folder-a")
        preferences.setHomeNoteFolder("")
        preferences.setHomeNoteText("local")
        assertEquals("old", documents.files.values.single())
        assertEquals("local", preferences.homeNoteTextFlow.first())
    }

    @Test
    fun taskTogglesUpdateSameMarkdownFile() = runBlocking {
        preferences.setHomeNoteText("- [ ] Task")
        preferences.setHomeNoteFolder("folder-a")
        preferences.updateHomeNoteText { it.replace("[ ]", "[x]") }
        assertEquals("- [x] Task", documents.files.values.single())
    }

    @Test
    fun failedWriteKeepsLatestTextLocally() = runBlocking {
        preferences.setHomeNoteFolder("folder-a")
        val current = preferences.homeNoteDocumentFlow.first()
        documents.failWrite = true
        try {
            preferences.setHomeNoteText("new draft")
            fail("Expected storage failure")
        } catch (_: IOException) { }
        assertEquals("new draft", preferences.homeNoteTextFlow.first())
        assertEquals(current, preferences.homeNoteDocumentFlow.first())
    }

    @Test
    fun changingFolderCreatesNewFileWithoutTouchingPreviousFolder() = runBlocking {
        preferences.setHomeNoteText("note")
        preferences.setHomeNoteFolder("folder-a")
        preferences.setHomeNoteFolder("folder-b")
        assertEquals(listOf("note", "note"), documents.files.values.toList())
        preferences.setHomeNoteText("changed")
        assertEquals(listOf("note", "changed"), documents.files.values.toList())
    }

    @Test
    fun repeatedClearsAlwaysCreateDistinctSiblings() = runBlocking {
        preferences.setHomeNoteFolder("folder-a")
        preferences.startNewHomeNote("first")
        preferences.startNewHomeNote("second")
        assertEquals(listOf("first", "second", ""), documents.files.values.toList())
        assertEquals(3, documents.files.size)
    }

    @Test
    fun failedFolderChangeKeepsOriginalPointer() = runBlocking {
        preferences.setHomeNoteText("note")
        preferences.setHomeNoteFolder("folder-a")
        val current = preferences.homeNoteDocumentFlow.first()
        documents.failCreate = true
        try {
            preferences.setHomeNoteFolder("folder-b")
            fail("Expected storage failure")
        } catch (_: IOException) { }
        assertEquals("folder-a", preferences.homeNoteFolderFlow.first())
        assertEquals(current, preferences.homeNoteDocumentFlow.first())
    }

    @Test
    fun failedClearWriteKeepsDraftWithoutCreatingSibling() = runBlocking {
        preferences.setHomeNoteFolder("folder-a")
        val current = preferences.homeNoteDocumentFlow.first()
        documents.failWrite = true
        try {
            preferences.startNewHomeNote("latest draft")
            fail("Expected storage failure")
        } catch (_: IOException) { }
        assertEquals("latest draft", preferences.homeNoteTextFlow.first())
        assertEquals(current, preferences.homeNoteDocumentFlow.first())
        assertEquals(1, documents.files.size)
    }

    @Test
    fun concurrentUpdatesKeepAllChangesInTheSameFile() = runBlocking {
        preferences.setHomeNoteFolder("folder-a")
        coroutineScope {
            repeat(20) { launch { preferences.updateHomeNoteText { it + "x" } } }
        }
        assertEquals("x".repeat(20), preferences.homeNoteTextFlow.first())
        assertEquals("x".repeat(20), documents.files.values.single())
    }

    private class MemoryNoteDocuments : NoteDocuments {
        val files = linkedMapOf<String, String>()
        var failCreate = false
        var failWrite = false
        override suspend fun create(folder: String): String {
            if (failCreate) throw IOException("Folder unavailable")
            return "$folder/${files.size}.md".also { files[it] = "" }
        }
        override suspend fun write(document: String, text: String) {
            if (failWrite) throw IOException("Write failed")
            files[document] = text
        }
    }
}
