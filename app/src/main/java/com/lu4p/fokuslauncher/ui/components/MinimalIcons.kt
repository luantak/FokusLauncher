package com.lu4p.fokuslauncher.ui.components

import androidx.compose.material.icons.Icons
import androidx.annotation.StringRes
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.ui.components.generated.MaterialOutlinedIconCategories
import com.lu4p.fokuslauncher.ui.components.generated.MaterialShippedOutlinedIcons
import java.util.Locale

/**
 * Home-screen and drawer icons using Material **Outlined** symbols (same family as
 * [Google Fonts Material Symbols](https://fonts.google.com/icons) “outlined” style), exposed
 * as Compose vectors via `material-icons-extended`.
 *
 * Only a **subset** of extended outlined glyphs is shipped ([MaterialShippedOutlinedIcons]): every
 * icon allowed in pickers plus [legacyAliases] Outlined targets, so R8 can strip unused library
 * icons. Regenerate that file with `scripts/gen_shipped_outlined_icons.py` when the full index or
 * picker rules change. Picker **sections** use [MaterialOutlinedIconCategories] (Google metadata).
 * Unknown keys still resolve to [Icons.Outlined.Circle]. Legacy `send` uses
 * [Icons.AutoMirrored.Outlined.Send].
 */
object MinimalIcons {

    data class IconPickerSection(
            @param:StringRes val titleRes: Int,
            /**
             * If set, shown instead of [titleRes] (e.g. a Google category label we have not added to
             * strings.xml yet).
             */
            val titleLiteral: String? = null,
            val names: List<String>,
    )

    /**
     * Icons whose Google Symbols metadata category is one of these are omitted from [names] /
     * pickers (see [isOmittedFromIconPickers]). Icons with **no** metadata row are omitted as well.
     */
    private val pickerOmittedGoogleCategories: Set<String> =
            setOf(
                    "Text",
                    "Android",
                    "UI actions",
                    "Actions",
                    "Home",
            )

    /** Display order for [MaterialOutlinedIconCategories] labels (aligned roughly with fonts.google.com). */
    private val googleCategorySectionOrder: List<String> =
            listOf(
                    "Communicate",
                    "Social",
                    "Maps",
                    "Travel",
                    "Transit",
                    "Activities",
                    "Business",
                    "Hardware",
                    "Household",
                    "Privacy",
                    "Images",
                    "Audio&Video",
            )
 
    private fun isOmittedFromIconPickers(name: String): Boolean {
        val cat = MaterialOutlinedIconCategories.GOOGLE_CATEGORY_BY_ICON_NAME[name] ?: return true
        return cat in pickerOmittedGoogleCategories
    }

    /** Picker section bucket: Google category, or Communicate for legacy `send` (not in metadata). */
    private fun pickerSectionCategoryForName(name: String): String? {
        MaterialOutlinedIconCategories.GOOGLE_CATEGORY_BY_ICON_NAME[name]?.let { return it }
        return if (name == "send") "Communicate" else null
    }

    /**
     * Shipped outlined glyphs only ([MaterialShippedOutlinedIcons]); keys not included here fall
     * through [iconFor] to [Icons.Outlined.Circle] unless matched by [legacyAliases].
     */
    private val allOutlinedIcons: Map<String, ImageVector> = MaterialShippedOutlinedIcons.byName

    /** Vectors that appear in [names] / pickers (at least one non-omitted outlined name). */
    private val pickerOutlinedIconVectorSet: Set<ImageVector> by lazy {
        allOutlinedIcons.mapNotNullTo(mutableSetOf()) { (name, vec) ->
            if (isOmittedFromIconPickers(name)) null else vec
        }
    }

    /** Legacy snake_case keys from earlier versions; values may differ from the PascalCase outlined name. */
    private val legacyAliases: Map<String, ImageVector> =
            mapOf(
                    "circle" to Icons.Outlined.Circle,
                    "dot" to Icons.Outlined.Circle,
                    "apps" to Icons.Outlined.Apps,
                    "menu" to Icons.Outlined.Menu,
                    "category" to Icons.Outlined.Category,
                    "chat" to Icons.Outlined.Email,
                    "mail" to Icons.Outlined.Mail,
                    "send" to Icons.AutoMirrored.Outlined.Send,
                    "call" to Icons.Outlined.Call,
                    "phone" to Icons.Outlined.Phone,
                    "contacts" to Icons.Outlined.Contacts,
                    "link" to Icons.Outlined.Link,
                    "share" to Icons.Outlined.Share,
                    "music" to Icons.Outlined.MusicNote,
                    "video" to Icons.Outlined.Videocam,
                    "camera" to Icons.Outlined.CameraAlt,
                    "gallery" to Icons.Outlined.Photo,
                    "image" to Icons.Outlined.Image,
                    "play" to Icons.Outlined.PlayArrow,
                    "art" to Icons.Outlined.Palette,
                    "book" to Icons.Outlined.Book,
                    "read" to Icons.Outlined.Book,
                    "news" to Icons.Outlined.Newspaper,
                    "headset" to Icons.Outlined.Headset,
                    "home" to Icons.Outlined.Home,
                    "map" to Icons.Outlined.Map,
                    "place" to Icons.Outlined.Place,
                    "location" to Icons.Outlined.LocationOn,
                    "directions" to Icons.Outlined.Directions,
                    "travel" to Icons.Outlined.Flight,
                    "alarm" to Icons.Outlined.Alarm,
                    "timer" to Icons.Outlined.Timer,
                    "calendar" to Icons.Outlined.CalendarMonth,
                    "event" to Icons.Outlined.Event,
                    "person" to Icons.Outlined.Person,
                    "favorite" to Icons.Outlined.Favorite,
                    "favorite_border" to Icons.Outlined.FavoriteBorder,
                    "bookmark" to Icons.Outlined.Bookmark,
                    "bookmark_border" to Icons.Outlined.BookmarkBorder,
                    "volunteer" to Icons.Outlined.VolunteerActivism,
                    "work" to Icons.Outlined.Work,
                    "folder" to Icons.Outlined.Folder,
                    "code" to Icons.Outlined.Code,
                    "settings" to Icons.Outlined.Settings,
                    "finance" to Icons.Outlined.AccountBalance,
                    "shop" to Icons.Outlined.ShoppingCart,
                    "bag" to Icons.Outlined.ShoppingBag,
                    "food" to Icons.Outlined.Restaurant,
                    "game" to Icons.Outlined.Gamepad,
                    "star" to Icons.Outlined.Star,
                    "star_border" to Icons.Outlined.StarBorder,
                    "cloud" to Icons.Outlined.Cloud,
                    "search" to Icons.Outlined.Search,
                    "notifications" to Icons.Outlined.Notifications,
                    "lock" to Icons.Outlined.Lock,
                    "dark" to Icons.Outlined.DarkMode,
                    "language" to Icons.Outlined.Language,
                    "translate" to Icons.Outlined.Translate,
            )

    /** Valid storage keys: shipped outlined names plus all legacy aliases (see [iconFor]). */
    val all: Map<String, ImageVector> by lazy {
        buildMap {
            putAll(allOutlinedIcons)
            putAll(legacyAliases)
        }
    }

    /**
     * Keys for icon pickers: **one entry per distinct** [ImageVector]. Outlined glyph names (e.g.
     * `Call`) win over legacy aliases (`call`) for the same shape. Order is A–Z (case-insensitive,
     * then exact spelling). Glyphs catalogued under [pickerOmittedGoogleCategories], or with no
     * metadata row, are excluded.
     */
    val names: List<String> by lazy {
        val iconByVector = LinkedHashMap<ImageVector, String>()
        val pickerVectors = pickerOutlinedIconVectorSet
        for (name in allOutlinedIcons.keys) {
            if (isOmittedFromIconPickers(name)) continue
            val vector = allOutlinedIcons.getValue(name)
            iconByVector.putIfAbsent(vector, name)
        }
        for (name in legacyAliases.keys.sorted()) {
            val vector = legacyAliases.getValue(name)
            if (name != "send" && vector !in pickerVectors) continue
            iconByVector.putIfAbsent(vector, name)
        }
        val sort = compareBy<String> { it.lowercase(Locale.ROOT) }.thenBy { it }
        iconByVector.values.sortedWith(sort)
    }

    /**
     * Icon pickers: sections follow **Google Material Symbols** category metadata; see
     * [pickerSectionCategoryForName]. Order within a section is A–Z (case-insensitive).
     */
    val iconPickerSections: List<IconPickerSection> by lazy { buildIconPickerSections() }

    /** Single section for search-filtered results (shortcut picker). */
    fun iconPickerSearchSections(matches: List<String>): List<IconPickerSection> {
        if (matches.isEmpty()) return emptyList()
        return listOf(IconPickerSection(R.string.icon_cat_search_results, names = matches))
    }

    @StringRes
    private fun stringResForGoogleCategory(googleLabel: String): Int =
            when (googleLabel) {
                "Actions" -> R.string.icon_cat_actions
                "Activities" -> R.string.icon_cat_activities
                "Android" -> R.string.icon_cat_android
                "Audio&Video" -> R.string.icon_cat_audio_video
                "Business" -> R.string.icon_cat_business
                "Communicate" -> R.string.icon_cat_communicate
                "Hardware" -> R.string.icon_cat_hardware
                "Home" -> R.string.icon_cat_home
                "Household" -> R.string.icon_cat_household
                "Images" -> R.string.icon_cat_images
                "Maps" -> R.string.icon_cat_maps
                "Privacy" -> R.string.icon_cat_privacy
                "Social" -> R.string.icon_cat_social
                "Text" -> R.string.icon_cat_text
                "Transit" -> R.string.icon_cat_transit
                "Travel" -> R.string.icon_cat_travel
                "UI actions" -> R.string.icon_cat_ui_actions
                else -> R.string.icon_cat_other
            }

    private fun buildIconPickerSections(): List<IconPickerSection> {
        val labels = names.toSet()
        val sort = compareBy<String> { it.lowercase(Locale.ROOT) }.thenBy { it }
        val byCategory = mutableMapOf<String, MutableList<String>>()
        for (name in labels) {
            val cat = pickerSectionCategoryForName(name) ?: continue
            byCategory.getOrPut(cat) { mutableListOf() }.add(name)
        }
        val sections = mutableListOf<IconPickerSection>()
        val ordered = googleCategorySectionOrder.toSet()
        for (cat in googleCategorySectionOrder) {
            val list = byCategory[cat]?.sortedWith(sort) ?: continue
            if (list.isNotEmpty()) {
                sections += IconPickerSection(stringResForGoogleCategory(cat), names = list)
            }
        }
        val extras = (byCategory.keys - ordered).sorted()
        for (cat in extras) {
            val list = byCategory.getValue(cat).sortedWith(sort)
            val res = stringResForGoogleCategory(cat)
            sections +=
                    if (res == R.string.icon_cat_other) {
                        IconPickerSection(res, titleLiteral = cat, names = list)
                    } else {
                        IconPickerSection(res, names = list)
                    }
        }
        return sections
    }

    /**
     * Keys used when hashing a custom category name to a default rail icon. Kept to the legacy
     * alias set for stable defaults (resolved with [iconFor]).
     */
    val namesForDefaultCategoryHash: List<String> = legacyAliases.keys.sorted()

    fun iconFor(name: String): ImageVector {
        legacyAliases[name]?.let { return it }
        allOutlinedIcons[name]?.let { return it }
        val pascal = snakeToPascalCase(name)
        if (pascal != name) allOutlinedIcons[pascal]?.let { return it }
        return Icons.Outlined.Circle
    }

    fun iconKeyMatchesStoredIcon(pickerKey: String, storedKey: String): Boolean =
            iconFor(pickerKey) == iconFor(storedKey)

    /** Text used when filtering icons in a picker (glyph name + split words, e.g. `WifiCalling3`). */
    fun materialOutlinedSearchHaystack(propertyName: String): String =
            buildString {
                append(propertyName)
                append(' ')
                append(
                        propertyName
                                .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                                .replace(Regex("([A-Za-z])([0-9])"), "$1 $2")
                                .lowercase(Locale.getDefault())
                )
            }

    private fun snakeToPascalCase(snake: String): String {
        if ('_' !in snake) return snake
        return snake.split('_').joinToString("") { part ->
            part.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
    }
}
