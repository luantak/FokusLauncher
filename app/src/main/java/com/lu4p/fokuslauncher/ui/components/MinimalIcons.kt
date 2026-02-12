package com.lu4p.fokuslauncher.ui.components

import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.outlined.Send
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

/**
 * A curated set of minimal, outline-style icons for home-screen shortcuts.
 * Uses Material Outlined icons for a clean, light look.
 */
object MinimalIcons {

    val all: Map<String, ImageVector> = linkedMapOf(
        // Default & basic
        "circle" to Icons.Outlined.Circle,
        "dot" to Icons.Outlined.Circle,
        "apps" to Icons.Outlined.Apps,
        "menu" to Icons.Outlined.Menu,
        "category" to Icons.Outlined.Category,
        // Communication
        "chat" to Icons.Outlined.Email,
        "mail" to Icons.Outlined.Mail,
        "send" to Icons.Outlined.Send,
        "call" to Icons.Outlined.Call,
        "phone" to Icons.Outlined.Phone,
        "contacts" to Icons.Outlined.Contacts,
        "link" to Icons.Outlined.Link,
        "share" to Icons.Outlined.Share,
        // Media & creativity
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
        // Place & travel
        "home" to Icons.Outlined.Home,
        "map" to Icons.Outlined.Map,
        "place" to Icons.Outlined.Place,
        "location" to Icons.Outlined.LocationOn,
        "directions" to Icons.Outlined.Directions,
        "travel" to Icons.Outlined.Flight,
        // Time & calendar
        "alarm" to Icons.Outlined.Alarm,
        "timer" to Icons.Outlined.Timer,
        "calendar" to Icons.Outlined.CalendarMonth,
        "event" to Icons.Outlined.Event,
        // People & preferences
        "person" to Icons.Outlined.Person,
        "favorite" to Icons.Outlined.Favorite,
        "favorite_border" to Icons.Outlined.FavoriteBorder,
        "bookmark" to Icons.Outlined.Bookmark,
        "bookmark_border" to Icons.Outlined.BookmarkBorder,
        "volunteer" to Icons.Outlined.VolunteerActivism,
        // Work & productivity
        "work" to Icons.Outlined.Work,
        "folder" to Icons.Outlined.Folder,
        "code" to Icons.Outlined.Code,
        "settings" to Icons.Outlined.Settings,
        // Commerce & finance
        "finance" to Icons.Outlined.AccountBalance,
        "shop" to Icons.Outlined.ShoppingCart,
        "bag" to Icons.Outlined.ShoppingBag,
        // Lifestyle
        "food" to Icons.Outlined.Restaurant,
        "game" to Icons.Outlined.Gamepad,
        "star" to Icons.Outlined.Star,
        "star_border" to Icons.Outlined.StarBorder,
        // System & misc
        "cloud" to Icons.Outlined.Cloud,
        "search" to Icons.Outlined.Search,
        "notifications" to Icons.Outlined.Notifications,
        "lock" to Icons.Outlined.Lock,
        "dark" to Icons.Outlined.DarkMode,
        "language" to Icons.Outlined.Language,
        "translate" to Icons.Outlined.Translate,
    )

    val names: List<String> = all.keys.toList()

    fun iconFor(name: String): ImageVector = all[name] ?: Icons.Outlined.Circle
}
