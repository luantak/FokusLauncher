package com.lu4p.fokuslauncher.data.model

import org.json.JSONArray
import org.json.JSONObject

data class HostedWidget(
        val id: String,
        val appWidgetId: Int,
        val providerPackageName: String,
        val providerClassName: String,
        val label: String,
        val heightDp: Int,
        val position: Int,
)

const val HOSTED_WIDGET_MIN_HEIGHT_DP = 96
const val HOSTED_WIDGET_MAX_HEIGHT_DP = 640
const val HOSTED_WIDGET_DEFAULT_HEIGHT_DP = 180

fun clampHostedWidgetHeightDp(heightDp: Int): Int =
        heightDp.coerceIn(HOSTED_WIDGET_MIN_HEIGHT_DP, HOSTED_WIDGET_MAX_HEIGHT_DP)

fun serializeHostedWidgets(widgets: List<HostedWidget>): String {
    if (widgets.isEmpty()) return ""
    val array = JSONArray()
    widgets.sortedBy { it.position }.forEach { widget ->
        array.put(
                JSONObject()
                        .put("id", widget.id)
                        .put("appWidgetId", widget.appWidgetId)
                        .put("providerPackageName", widget.providerPackageName)
                        .put("providerClassName", widget.providerClassName)
                        .put("label", widget.label)
                        .put("heightDp", clampHostedWidgetHeightDp(widget.heightDp))
                        .put("position", widget.position)
        )
    }
    return array.toString()
}

fun parseHostedWidgets(raw: String): List<HostedWidget> {
    if (raw.isBlank()) return emptyList()
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id").trim()
                val appWidgetId = item.optInt("appWidgetId", -1)
                val providerPackageName = item.optString("providerPackageName").trim()
                val providerClassName = item.optString("providerClassName").trim()
                if (id.isBlank() ||
                                appWidgetId <= 0 ||
                                providerPackageName.isBlank() ||
                                providerClassName.isBlank()
                ) {
                    continue
                }
                add(
                        HostedWidget(
                                id = id,
                                appWidgetId = appWidgetId,
                                providerPackageName = providerPackageName,
                                providerClassName = providerClassName,
                                label = item.optString("label").trim(),
                                heightDp =
                                        clampHostedWidgetHeightDp(
                                                item.optInt(
                                                        "heightDp",
                                                        HOSTED_WIDGET_DEFAULT_HEIGHT_DP,
                                                )
                                        ),
                                position = item.optInt("position", i),
                        )
                )
            }
        }.sortedBy { it.position }
    } catch (_: Exception) {
        emptyList()
    }
}
