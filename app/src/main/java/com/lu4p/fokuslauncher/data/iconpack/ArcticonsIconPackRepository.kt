package com.lu4p.fokuslauncher.data.iconpack

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.appListStableKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
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
 * Caches the parsed appfilter, per-app component resolution, and drawable constant states so
 * scrolling / recomposition reuse results instead of re-parsing or re-hitting PackageManager.
 */
@Singleton
class ArcticonsIconPackRepository
@Inject
constructor(@param:ApplicationContext private val context: Context) {
    private val loadMutex = Mutex()
    private val loadedPack = AtomicReference<LoadedPack?>(null)
    private val _installedPackage = MutableStateFlow<String?>(null)
    val installedPackage: StateFlow<String?> = _installedPackage.asStateFlow()

    /** appListStableKey → resolved component (or [COMPONENT_MISS] sentinel). */
    private val componentCache = ConcurrentHashMap<String, ComponentName>()

    /** appListStableKey → drawable ConstantState (mapped icon or pack placeholder). */
    private val iconConstantStateCache = ConcurrentHashMap<String, Drawable.ConstantState>()
    /** Apps that cannot resolve even the pack placeholder (pack missing / placeholder missing). */
    private val iconMissCache = ConcurrentHashMap.newKeySet<String>()

    /** Cached Arcticons outlined-circle placeholder ConstantState for the current pack. */
    private val cachedPlaceholderConstantState = AtomicReference<Drawable.ConstantState?>(null)

    private val appfilterParseCount = AtomicInteger(0)
    private val drawableDecodeCount = AtomicInteger(0)

    init {
        refreshInstalledPackage()
    }

    /**
     * Updates which Arcticons package is considered installed. Does **not** drop the appfilter /
     * icon caches unless the selected package identity changes.
     */
    fun refreshInstalledPackage() {
        val installed = ArcticonsPackages.findInstalledPackage(context.packageManager)
        val previous = _installedPackage.value
        _installedPackage.value = installed
        if (previous != installed) {
            clearCaches()
        }
    }

    fun isArcticonsInstalled(): Boolean = installedPackage.value != null

    /**
     * Drops appfilter + icon caches. Call only when the pack may have changed (install/update /
     * uninstall), not on ordinary drawer open/scroll.
     */
    fun invalidate() {
        clearCaches()
        refreshInstalledPackage()
    }

    /**
     * Ensures the appfilter for the current installed pack is loaded. Safe to call when enabling
     * the feature; no-ops when already warm.
     */
    suspend fun warmUp() {
        withContext(Dispatchers.IO) { ensureLoaded() }
    }

    suspend fun getIcon(app: AppInfo): Drawable? =
            withContext(Dispatchers.IO) {
                val appKey = appListStableKey(app)
                if (appKey in iconMissCache) return@withContext null
                iconConstantStateCache[appKey]?.let { return@withContext it.newDrawable().mutate() }

                val pack = ensureLoaded()
                if (pack == null) {
                    iconMissCache.add(appKey)
                    return@withContext null
                }

                val component = resolveComponentNameCached(app, appKey)
                val drawableName =
                        if (component != null) {
                            val componentKey =
                                    ArcticonsAppfilterParser.componentKey(
                                            component.packageName,
                                            component.className,
                                    )
                            pack.componentToDrawable[componentKey]
                                    ?: pack.packageToDrawable[component.packageName]
                        } else {
                            null
                        }

                val mappedState =
                        if (drawableName != null) pack.constantStateFor(drawableName) else null
                // Unmapped / decode-failed apps use Arcticons' outlined circle drawable.
                val constantState = mappedState ?: resolvePlaceholderConstantState(pack)
                if (constantState == null) {
                    iconMissCache.add(appKey)
                    return@withContext null
                }
                iconConstantStateCache[appKey] = constantState
                constantState.newDrawable().mutate()
            }

    /**
     * Arcticons ships [PLACEHOLDER_DRAWABLE_NAME] as a stroke-only outlined circle (also referenced
     * from appfilter for a few apps). Used when an installed app has no pack mapping.
     */
    private fun resolvePlaceholderConstantState(pack: LoadedPack): Drawable.ConstantState? {
        cachedPlaceholderConstantState.get()?.let { return it }
        val resolved = pack.constantStateFor(PLACEHOLDER_DRAWABLE_NAME) ?: return null
        cachedPlaceholderConstantState.compareAndSet(null, resolved)
        return cachedPlaceholderConstantState.get() ?: resolved
    }

    private suspend fun ensureLoaded(): LoadedPack? {
        loadedPack.get()?.let { return it }
        return loadMutex.withLock {
            loadedPack.get()?.let { return@withLock it }
            val installed =
                    _installedPackage.value
                            ?: ArcticonsPackages.findInstalledPackage(context.packageManager).also {
                                _installedPackage.value = it
                            }
            if (installed == null) {
                clearCachesLocked()
                return@withLock null
            }
            val parsed = loadPack(installed) ?: return@withLock null
            loadedPack.set(parsed)
            parsed
        }
    }

    private fun loadPack(packageName: String): LoadedPack? {
        return try {
            val resources = context.packageManager.getResourcesForApplication(packageName)
            val componentMap = parseAppfilter(resources, packageName) ?: return null
            appfilterParseCount.incrementAndGet()
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

    private fun LoadedPack.constantStateFor(drawableName: String): Drawable.ConstantState? {
        drawableConstantStates[drawableName]?.let { return it }
        if (drawableName in drawableMisses) return null
        val decoded = decodeConstantState(this, drawableName)
        if (decoded == null) {
            drawableMisses.add(drawableName)
            return null
        }
        drawableConstantStates[drawableName] = decoded
        return decoded
    }

    private fun decodeConstantState(pack: LoadedPack, drawableName: String): Drawable.ConstantState? {
        val id = pack.resources.getIdentifier(drawableName, "drawable", pack.packageName)
        if (id == 0) return null
        return try {
            drawableDecodeCount.incrementAndGet()
            ResourcesCompat.getDrawable(pack.resources, id, null)?.constantState
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveComponentNameCached(app: AppInfo, appKey: String): ComponentName? {
        componentCache[appKey]?.let { cached ->
            return if (cached === COMPONENT_MISS) null else cached
        }
        val resolved = resolveComponentName(app)
        componentCache[appKey] = resolved ?: COMPONENT_MISS
        return resolved
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

    private fun clearCaches() {
        loadedPack.set(null)
        componentCache.clear()
        iconConstantStateCache.clear()
        iconMissCache.clear()
        cachedPlaceholderConstantState.set(null)
    }

    private fun clearCachesLocked() {
        clearCaches()
    }

    private data class LoadedPack(
            val packageName: String,
            val resources: Resources,
            val componentToDrawable: Map<String, String>,
            val packageToDrawable: Map<String, String>,
            /** Successful drawable ConstantStates only (ConcurrentHashMap forbids null values). */
            val drawableConstantStates: ConcurrentHashMap<String, Drawable.ConstantState> =
                    ConcurrentHashMap(),
            val drawableMisses: MutableSet<String> = ConcurrentHashMap.newKeySet(),
    )

    @VisibleForTesting
    internal fun appfilterParseCountForTest(): Int = appfilterParseCount.get()

    @VisibleForTesting
    internal fun drawableDecodeCountForTest(): Int = drawableDecodeCount.get()

    @VisibleForTesting
    internal fun resetCountersForTest() {
        appfilterParseCount.set(0)
        drawableDecodeCount.set(0)
    }

    @VisibleForTesting
    internal fun seedLoadedPackForTest(
            packageName: String,
            componentToDrawable: Map<String, String>,
            drawableByName: Map<String, Drawable>,
    ) {
        clearCaches()
        _installedPackage.value = packageName
        val resources = context.resources
        val states = ConcurrentHashMap<String, Drawable.ConstantState>()
        for ((name, drawable) in drawableByName) {
            drawable.constantState?.let { states[name] = it }
        }
        val packageMap = HashMap<String, String>()
        for ((componentKey, drawableName) in componentToDrawable) {
            packageNameFromComponentKey(componentKey)?.let { packageMap.putIfAbsent(it, drawableName) }
        }
        loadedPack.set(
                LoadedPack(
                        packageName = packageName,
                        resources = resources,
                        componentToDrawable = componentToDrawable,
                        packageToDrawable = packageMap,
                        drawableConstantStates = states,
                )
        )
        // Count as one "parse" for tests that seed a pack without going through parseAppfilter.
        appfilterParseCount.set(1)
        drawableDecodeCount.set(0)
    }

    companion object {
        private const val TAG = "ArcticonsIconPack"

        /**
         * Verified in Arcticons APK resources (`com.donnnno.arcticons:drawable/circle`): a 48×48
         * stroke-only circle (`fill:none`, `r=21.5`). Source: `icons/white/circle.svg`.
         */
        internal const val PLACEHOLDER_DRAWABLE_NAME = "circle"

        /** Sentinel stored in [componentCache] when resolution failed. */
        private val COMPONENT_MISS = ComponentName("\u0000.miss", "\u0000.miss")

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
