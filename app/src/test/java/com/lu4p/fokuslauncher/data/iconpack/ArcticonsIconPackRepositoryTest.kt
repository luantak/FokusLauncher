package com.lu4p.fokuslauncher.data.iconpack

import android.content.ComponentName
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.lu4p.fokuslauncher.data.model.AppInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArcticonsIconPackRepositoryTest {

    private lateinit var repository: ArcticonsIconPackRepository

    @Before
    fun setUp() {
        repository = ArcticonsIconPackRepository(RuntimeEnvironment.getApplication())
        repository.resetCountersForTest()
    }

    @Test
    fun getIcon_reusesCachedDrawableWithoutRedecode() = runBlocking {
        val drawable = ColorDrawable(Color.WHITE)
        repository.seedLoadedPackForTest(
                packageName = "com.donnnno.arcticons",
                componentToDrawable =
                        mapOf(
                                "ComponentInfo{com.example.app/com.example.app.Main}" to "example",
                        ),
                drawableByName = mapOf("example" to drawable),
        )
        repository.resetCountersForTest()

        val app =
                AppInfo(
                        packageName = "com.example.app",
                        label = "Example",
                        icon = null,
                        componentName = ComponentName("com.example.app", "com.example.app.Main"),
                )

        val first = repository.getIcon(app)
        val second = repository.getIcon(app)

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(0, repository.appfilterParseCountForTest())
        assertEquals(0, repository.drawableDecodeCountForTest())
    }

    @Test
    fun getIcon_unmappedApp_usesArcticonsOutlinedCirclePlaceholder() = runBlocking {
        val placeholder = ColorDrawable(Color.WHITE)
        repository.seedLoadedPackForTest(
                packageName = "com.donnnno.arcticons",
                componentToDrawable = emptyMap(),
                drawableByName =
                        mapOf(ArcticonsIconPackRepository.PLACEHOLDER_DRAWABLE_NAME to placeholder),
        )
        repository.resetCountersForTest()

        val app =
                AppInfo(
                        packageName = "com.missing.app",
                        label = "Missing",
                        icon = null,
                        componentName = ComponentName("com.missing.app", "com.missing.app.Main"),
                )

        val first = repository.getIcon(app)
        val second = repository.getIcon(app)

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(0, repository.appfilterParseCountForTest())
        assertEquals(0, repository.drawableDecodeCountForTest())
    }

    @Test
    fun getIcon_returnsNullWhenPlaceholderAlsoMissing() = runBlocking {
        repository.seedLoadedPackForTest(
                packageName = "com.donnnno.arcticons",
                componentToDrawable = emptyMap(),
                drawableByName = emptyMap(),
        )
        repository.resetCountersForTest()

        val app =
                AppInfo(
                        packageName = "com.missing.app",
                        label = "Missing",
                        icon = null,
                        componentName = ComponentName("com.missing.app", "com.missing.app.Main"),
                )

        assertNull(repository.getIcon(app))
        assertNull(repository.getIcon(app))
        assertEquals(0, repository.appfilterParseCountForTest())
        assertEquals(0, repository.drawableDecodeCountForTest())
    }

    @Test
    fun refreshInstalledPackage_keepsCachesWhenPackageUnchanged() = runBlocking {
        val drawable = ColorDrawable(Color.BLACK)
        repository.seedLoadedPackForTest(
                packageName = "com.donnnno.arcticons",
                componentToDrawable =
                        mapOf(
                                "ComponentInfo{com.example.app/com.example.app.Main}" to "example",
                        ),
                drawableByName = mapOf("example" to drawable),
        )
        val app =
                AppInfo(
                        packageName = "com.example.app",
                        label = "Example",
                        icon = null,
                        componentName = ComponentName("com.example.app", "com.example.app.Main"),
                )
        assertNotNull(repository.getIcon(app))
        repository.resetCountersForTest()

        // No Arcticons installed in Robolectric → package becomes null and clears.
        // Seed again and verify warmUp does not re-parse when pack already loaded.
        repository.seedLoadedPackForTest(
                packageName = "com.donnnno.arcticons",
                componentToDrawable =
                        mapOf(
                                "ComponentInfo{com.example.app/com.example.app.Main}" to "example",
                        ),
                drawableByName = mapOf("example" to drawable),
        )
        assertEquals(1, repository.appfilterParseCountForTest())
        repository.resetCountersForTest()

        repository.warmUp()
        assertNotNull(repository.getIcon(app))
        assertEquals(0, repository.appfilterParseCountForTest())
        assertTrue(repository.drawableDecodeCountForTest() == 0)
    }

    @Test
    fun invalidate_clearsIconCache() = runBlocking {
        val drawable = ColorDrawable(Color.WHITE)
        repository.seedLoadedPackForTest(
                packageName = "com.donnnno.arcticons",
                componentToDrawable =
                        mapOf(
                                "ComponentInfo{com.example.app/com.example.app.Main}" to "example",
                        ),
                drawableByName = mapOf("example" to drawable),
        )
        val app =
                AppInfo(
                        packageName = "com.example.app",
                        label = "Example",
                        icon = null,
                        componentName = ComponentName("com.example.app", "com.example.app.Main"),
                )
        assertNotNull(repository.getIcon(app))

        repository.invalidate()

        // After invalidate with no installed Arcticons package, icons resolve to null.
        assertNull(repository.getIcon(app))
    }

    @Test
    fun placeholderDrawableName_isArcticonsOutlinedCircle() {
        assertEquals("circle", ArcticonsIconPackRepository.PLACEHOLDER_DRAWABLE_NAME)
    }
}
