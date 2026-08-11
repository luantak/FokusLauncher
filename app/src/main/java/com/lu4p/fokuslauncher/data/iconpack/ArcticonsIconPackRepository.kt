package com.lu4p.fokuslauncher.data.iconpack

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.lu4p.fokuslauncher.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads icons exclusively from a whitelisted Arcticons package via its `appfilter.xml`.
 *
 * Does not scan or accept arbitrary third-party icon packs.
 */
@Singleton
class ArcticonsIconPackRepository
@Inject
constructor(@param:ApplicationContext private val context: Context) {
    private val mutex = Mutex()
    private val loadedPack = AtomicReference<LoadedPack?>(null)
    private val _installedPackage = MutableStateFlow<String?>(null)
    val installedPackage: StateFlow<String?> = _installedPackage.asStateFlow()

    init {
        refreshInstalledPackage()
    }

    fun refreshInstalledPackage() {
        _installedPackage.value = ArcticonsPackages.findInstalledPackage(context.packageManager)
    }

    fun isArcticonsInstalled(): Boolean = installedPackage.value != null

    /** Drops the cached appfilter so the next lookup reloads from the installed pack. */
    fun invalidate() {
        loadedPack.set(null)
        refreshInstalledPackage()
    }

    suspend fun getIcon(app: AppInfo): Drawable? =
            withContext(Dispatchers.IO) {
                val pack = ensureLoaded() ?: return@withContext null
                val component = resolveComponentName(app) ?: return@withContext null
                val key =
                        ArcticonsAppfilterParser.componentKey(
                                component.packageName,
                                component.className,
                        )
                val drawableName =
                        pack.componentToDrawable[key]
                                ?: pack.packageToDrawable[component.packageName]
                                ?: return@withContext null
                loadDrawable(pack, drawableName)
            }

    private suspend fun ensureLoaded(): LoadedPack? =
            mutex.withLock {
                val installed = ArcticonsPackages.findInstalledPackage(context.packageManager)
                _installedPackage.value = installed
                if (installed == null) {
                    loadedPack.set(null)
                    return@withLock null
                }
                val current = loadedPack.get()
                if (current != null && current.packageName == installed) return@withLock current

                val parsed = loadPack(installed) ?: return@withLock null
                loadedPack.set(parsed)
                parsed
            }

    private fun loadPack(packageName: String): LoadedPack? {
        return try {
            val resources = context.packageManager.getResourcesForApplication(packageName)
            val componentMap = parseAppfilter(resources, packageName) ?: return null
            val packageMap = HashMap<String, String>(componentMap.size / 4)
            for ((componentKey, drawable) in componentMap) {
                val pkg = packageNameFromComponentKey(componentKey) ?: continue
                packageMap.putIfAbsent(pkg, drawable)
            }
            LoadedPack(
                    packageName = packageName,
                    resources = resources,
                    componentToDrawable = componentMap,
                    packageToDrawable = packageMap,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load Arcticons appfilter from $packageName", e)
            null
        }
    }

    private fun parseAppfilter(resources: Resources, packageName: String): Map<String, String>? {
        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlId != 0) {
            return try {
                ArcticonsAppfilterParser.parse(resources.getXml(xmlId))
            } catch (e: Exception) {
                Log.w(TAG, "Failed parsing appfilter.xml resource", e)
                null
            }
        }
        return try {
            resources.assets.open("appfilter.xml").use { ArcticonsAppfilterParser.parse(it) }
        } catch (e: Exception) {
            Log.w(TAG, "No appfilter.xml in $packageName", e)
            null
        }
    }

    private fun loadDrawable(pack: LoadedPack, drawableName: String): Drawable? {
        val id = pack.resources.getIdentifier(drawableName, "drawable", pack.packageName)
        if (id == 0) return null
        return try {
            ResourcesCompat.getDrawable(pack.resources, id, null)
                    ?.constantState
                    ?.newDrawable()
                    ?.mutate()
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveComponentName(app: AppInfo): ComponentName? {
        app.componentName?.let { return it }
        val user: UserHandle = app.userHandle ?: Process.myUserHandle()
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        if (launcherApps != null) {
            try {
                val activities = launcherApps.getActivityList(app.packageName, user)
                activities.firstOrNull()?.componentName?.let { return it }
            } catch (_: Exception) {
                // Fall through to intent lookup.
            }
        }
        return try {
            context.packageManager.getLaunchIntentForPackage(app.packageName)?.component
        } catch (_: Exception) {
            null
        }
    }

    private data class LoadedPack(
            val packageName: String,
            val resources: Resources,
            val componentToDrawable: Map<String, String>,
            val packageToDrawable: Map<String, String>,
    )

    companion object {
        private const val TAG = "ArcticonsIconPack"

        internal fun packageNameFromComponentKey(componentKey: String): String? {
            // ComponentInfo{package/class}
            if (!componentKey.startsWith("ComponentInfo{") || !componentKey.endsWith("}")) {
                return null
            }
            val inner = componentKey.removePrefix("ComponentInfo{").removeSuffix("}")
            val slash = inner.indexOf('/')
            if (slash <= 0) return null
            return inner.substring(0, slash)
        }
    }
}
