package com.lu4p.fokuslauncher.data.model

import android.content.Context
import com.lu4p.fokuslauncher.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategoryAddValidationTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `empty field yields no inline failure`() {
        assertNull(categoryAddFieldFailure(context, "", emptyList()))
    }

    @Test
    fun `whitespace only yields Blank`() {
        assertEquals(
                AddCategoryResult.Failure.Blank,
                categoryAddFieldFailure(context, "   \t", emptyList())
        )
    }

    @Test
    fun `reserved uses drawer string comparison`() {
        assertEquals(
                AddCategoryResult.Failure.ReservedPrivate,
                categoryAddFieldFailure(context, context.getString(R.string.drawer_filter_private), emptyList())
        )
    }

    @Test
    fun `duplicate matches normalized definition names`() {
        assertEquals(
                AddCategoryResult.Failure.Duplicate("Games"),
                categoryAddFieldFailure(
                        context,
                        "Games",
                        listOf("Utilities", "Games", "Media")
                )
        )
    }

    @Test
    fun `no failure for new unique name`() {
        assertNull(
                categoryAddFieldFailure(context, "Reading", listOf("Utilities"))
        )
    }
}
