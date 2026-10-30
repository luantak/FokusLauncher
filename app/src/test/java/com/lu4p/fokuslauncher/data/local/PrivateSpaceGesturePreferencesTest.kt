package com.lu4p.fokuslauncher.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.lu4p.fokuslauncher.data.model.ShortcutTarget
import com.lu4p.fokuslauncher.data.model.WidgetTapTarget
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PrivateSpaceGesturePreferencesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private fun preferences(file: File, scope: CoroutineScope) = PreferencesManager(
        RuntimeEnvironment.getApplication(),
        mockk<NoteDocuments>(relaxed = true),
        PreferenceDataStoreFactory.create(scope = scope) { file },
    )

    @Test
    fun `all gesture bindings survive reopening the preferences store and can be cleared`() = runBlocking {
        val file = File(temporaryFolder.root, "gestures.preferences_pb")
        val writingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val target = ShortcutTarget.UnlockPrivateSpace
        try {
            val writer = preferences(file, writingScope)
            writer.setSwipeLeftTarget(target)
            writer.setSwipeRightTarget(target)
            TwoFingerDirection.entries.forEach { writer.setTwoFingerTarget(it, target) }
            writer.setDoubleTapEmptyTarget(WidgetTapTarget(target))
        } finally {
            writingScope.coroutineContext[Job]!!.cancelAndJoin()
        }

        val readingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reader = preferences(file, readingScope)
            assertEquals(target, reader.swipeLeftTargetFlow.first())
            assertEquals(target, reader.swipeRightTargetFlow.first())
            TwoFingerDirection.entries.forEach {
                assertEquals(target, reader.twoFingerTargetFlow(it).first())
            }
            assertEquals(WidgetTapTarget(target), reader.doubleTapEmptyTargetFlow.first())

            reader.setSwipeLeftTarget(null)
            reader.setSwipeRightTarget(null)
            TwoFingerDirection.entries.forEach { reader.setTwoFingerTarget(it, null) }
            reader.setDoubleTapEmptyTarget(null)
            assertNull(reader.swipeLeftTargetFlow.first())
            assertNull(reader.swipeRightTargetFlow.first())
            TwoFingerDirection.entries.forEach { assertNull(reader.twoFingerTargetFlow(it).first()) }
            assertNull(reader.doubleTapEmptyTargetFlow.first())
        } finally {
            readingScope.cancel()
        }
    }
}
