package com.lu4p.fokuslauncher.data.iconpack

import android.content.ComponentName
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import com.lu4p.fokuslauncher.R
import com.lu4p.fokuslauncher.data.model.AppInfo
import com.lu4p.fokuslauncher.data.model.appListStableKey
import com.google.android.material.color.DynamicColors
import com.lu4p.fokuslauncher.data.local.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
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
 * Loads icons from the user-selected icon pack via its `appfilter.xml`.
 *
 * Any installed pack is supported: Arcticons variants (historical default, auto-selected),
 * Delta, or any package advertising the standard ADW/Nova icon-pack discovery actions.
 *
 * Caches the parsed appfilter, per-app component resolution, and drawable constant states so
 * scrolling / recomposition reuse results instead of re-parsing or re-hitting PackageManager.
 */
@Singleton
class ArcticonsIconPackRepository
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val preferencesManager: PreferencesManager,
) {
    private val loadMutex = Mutex()

    /**
     * Context used to inflate pack drawables. Wrapped in a MaterialComponents theme with
     * Material You dynamic colors applied: packs like Arcticons You reference "?attr"
     * theme colors at inflate time; without a themed context those vectors render blank.
     */
    private val themedInflationContext: Context by lazy {
        val wrapped = ContextThemeWrapper(context, R.style.IconPackThemedInflation)
        try {
            DynamicColors::class.java
                    .getMethod("applyContextIfAvailable", Context::class.java)
                    .invoke(null, wrapped)
        } catch (_: Exception) {
            // Material lib without dynamic colors support: static theme attrs still resolve.
        }
        wrapped
    }
    private val loadedPack = AtomicReference<LoadedPack?>(null)
    private val _installedPackage = MutableStateFlow<String?>(null)
    val installedPackage: StateFlow<String?> = _installedPackage.asStateFlow()

    /** appListStableKey → resolved component (or [COMPONENT_MISS] sentinel). */
    private val componentCache = ConcurrentHashMap<String, ComponentName>()

    /** appListStableKey → drawable ConstantState (mapped icon or pack placeholder). */
    private val iconConstantStateCache = ConcurrentHashMap<String, Drawable.ConstantState>()
    /** Apps that cannot resolve even the pack placeholder (pack missing / placeholder missing). */
    private val iconMissCache = ConcurrentHashMap.newKeySet<String>()

    /** Cached placeholder ConstantState for the current pack. */
    private val cachedPlaceholderConstantState = AtomicReference<Drawable.ConstantState?>(null)

    private val appfilterParseCount = AtomicInteger(0)
    private val drawableDecodeCount = AtomicInteger(0)

    /** Marks the selected-pack preference read at least once after process start. */
    private val preferencePrimed = AtomicReference<String?>(null)

    /**
     * Whether third-party packs may become the active pack. Starts false each process;
     * flipped true once the user accepts the disclaimer. Consent gates third-party icon
     * rendering itself, not merely picker clicks, so auto-selection can never silently
     * apply a third-party pack (issue #295 maintainer request).
     */
    private val thirdPartyAllowed = AtomicBoolean(false)

    /** Application-scoped collector for persisted consent + selection (see init). */
    private val preferenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Re-resolves the active pack whenever any package changes on device. */
    private val packageChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Data races with the resolution path are harmless: worst case an extra reload.
            refreshInstalledPackage()
        }
    }

    init {
        refreshInstalledPackage()
        // Track pack installs/uninstalls so the active pack reflects reality without a
        // settings visit.
        val packageFilter = IntentFilter()
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED)
        packageFilter.addDataScheme("package")
        try {
            context.registerReceiver(
                    packageChangedReceiver,
                    packageFilter,
                    Context.RECEIVER_NOT_EXPORTED,
            )
        } catch (_: Exception) {
            // Registration refused (restricted environment); settings-visit refresh
            // still works as fallback.
        }
        // Restore persisted state independent of any ViewModel lifecycle: the drawer and
        // home read installedPackage long before the settings screen exists.
        preferenceScope.launch {
            preferencesManager.iconPackDisclaimerAcceptedFlow.collect { accepted ->
                if (accepted) setThirdPartyPacksAllowed()
            }
        }
        preferenceScope.launch {
            preferencesManager.selectedIconPackFlow.collect { selected ->
                setSelectedPackage(selected)
            }
        }
    }

    /**
     * Resolves the active pack: the user-selected package when installed, otherwise automatic
     * selection. Does **not** drop the appfilter / icon caches unless the selected package
     * identity changes.
     */
    fun refreshInstalledPackage() {
        val selected = preferencePrimed.get()
        val installed = resolveActivePack(selected)
        val previous = _installedPackage.value
        _installedPackage.value = installed
        if (previous != installed) {
            clearCaches()
        }
    }

    /**
     * The pack actually used for icons. A user selection always wins when installed.
     * Automatic selection only ever resolves to Arcticons variants unless third-party
     * packs are allowed; unconsented third-party packs are never auto-applied.
     */
    private fun resolveActivePack(selected: String?): String? {
        if (selected != null && ArcticonsPackages.isPackageInstalled(context.packageManager, selected)) {
            return selected
        }
        return IconPackPackages.findPreferredInstalledPackage(
                context.packageManager,
                thirdPartyAllowed.get(),
        )
    }

    /** True when an icon pack (selected, Arcticons, or discovered) is installed. */
    fun isIconPackInstalled(): Boolean = installedPackage.value != null

    /**
     * True when the active pack draws monochrome glyphs meant to be tinted by the launcher
     * (Arcticons line icons). Colored packs (Delta etc.) render untinted.
     */
    fun isActivePackMonochrome(): Boolean =
            installedPackage.value?.let { ArcticonsPackages.isWhitelisted(it) } ?: false

    /**
     * Drops appfilter + icon caches. Call only when the pack may have changed (install/update /
     * uninstall / user selection), not on ordinary drawer open/scroll.
     */
    fun invalidate() {
        clearCaches()
        refreshInstalledPackage()
    }

    /**
     * Applies a user pack selection. Clears caches so the next icon lookup loads the new pack.
     * Safe to call with a package that is no longer installed; automatic selection applies.
     */
    fun setSelectedPackage(packageName: String) {
        if (preferencePrimed.get() == packageName) return
        preferencePrimed.set(packageName)
        invalidate()
    }

    /** Records disclaimer acceptance; lets third-party packs become the active pack. */
    fun setThirdPartyPacksAllowed() {
        if (thirdPartyAllowed.getAndSet(true)) return
        refreshInstalledPackage()
    }

    /**
     * The pack that would resolve as active if third-party packs were permitted
     * (whether or not the disclaimer was accepted). Pure query: mutates nothing. Used by
     * the enable-toggle consent gate, which must know whether a third-party pack *would*
     * become active � the actually-active pack is null until consent is given.
     */
    fun wouldResolveToThirdPartyPack(): Boolean {
        val selected = preferencePrimed.get()
        if (selected != null &&
                    ArcticonsPackages.isPackageInstalled(context.packageManager, selected) &&
                    !ArcticonsPackages.isWhitelisted(selected)
        ) {
            return true
        }
        val candidate = IconPackPackages.findPreferredInstalledPackage(
                context.packageManager,
                thirdPartyAllowed = true,
        )
        return candidate != null && !ArcticonsPackages.isWhitelisted(candidate)
    }

    /**
     * Ensures the appfilter for the current installed pack is loaded. Safe to call when enabling
     * the feature; no-ops when already warm.
     */
    suspend fun warmUp() {
        withContext(Dispatchers.IO) { ensureLoaded() }
    }

    /**
     * Parses the appfilter (when needed) and pre-decodes pack icons for [apps], populating
     * the same caches [getIcon] reads, so drawer scrolling does not decode on demand.
     * Cheap after the first pass: every hit short-circuits on cache.
     */
    suspend fun prefetchIcons(apps: List<AppInfo>) {
        if (apps.isEmpty()) return
        if (installedPackage.value == null) return
        withContext(Dispatchers.IO) {
            apps.forEach { app -> getIcon(app) }
        }
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
                // Unmapped / decode-failed apps use the pack placeholder when one exists.
                val constantState = mappedState ?: resolvePlaceholderConstantState(pack)
                if (constantState == null) {
                    iconMissCache.add(appKey)
                    return@withContext null
                }
                iconConstantStateCache[appKey] = constantState
                constantState.newDrawable().mutate()
            }

    /**
     * Packs commonly ship a fallback drawable referenced from appfilter (Arcticons ships
     * [PLACEHOLDER_DRAWABLE_NAME], a stroke-only outlined circle). Used when an installed app
     * has no pack mapping. Returns null when the pack has no such drawable; callers then treat
     * the app as unmapped.
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
                            ?: IconPackPackages.findPreferredInstalledPackage(
                                    context.packageManager,
                                    thirdPartyAllowed.get(),
                            ).also {
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
            Log.w(TAG, "Failed to load appfilter from icon pack $packageName", e)
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
            // Inflate against the launcher theme. Dynamic-color packs (Arcticons You,
            // day-night) reference ?attr colors that resolve against the launcher's
            // Material You palette; a null theme leaves them blank white.
            ResourcesCompat.getDrawable(pack.resources, id, themedInflationContext.theme)?.constantState
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
         * Placeholder drawable shipped by Arcticons (`com.donnnno.arcticons:drawable/circle`):
         * a 48×48 stroke-only circle (`fill:none`, `r=21.5`). Packs without this drawable have
         * no placeholder; unmapped apps then surface as misses.
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
