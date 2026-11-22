package com.taigashinohara.kikkake

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MacroStore(context: Context) {
    private val prefs = context.getSharedPreferences("macros", Context.MODE_PRIVATE)

    fun load(): List<Macro> {
        val raw = prefs.getString("list", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<Macro>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val actions = mutableListOf<ActionItem>()
            val aArr = o.optJSONArray("actions")
            if (aArr != null) {
                for (j in 0 until aArr.length()) {
                    val ao = aArr.getJSONObject(j)
                    actions.add(ActionItem(ActionType.valueOf(ao.getString("type")), ao.optString("param", "")))
                }
            } else if (o.has("action")) {
                // v0.2以前の単一アクション形式から移行
                actions.add(ActionItem(ActionType.valueOf(o.getString("action")), o.optString("actionParam", "")))
            }
            if (actions.isEmpty()) actions.add(ActionItem(ActionType.NOTIFICATION))
            out.add(
                Macro(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    trigger = TriggerType.valueOf(o.getString("trigger")),
                    triggerParam = o.optString("param", ""),
                    actions = actions,
                    constraintType = ConstraintType.valueOf(o.optString("constraint", "NONE")),
                    constraintParam = o.optString("constraintParam", ""),
                    enabled = o.optBoolean("enabled", true),
                    lastRunAt = o.optLong("lastRunAt", 0L)
                )
            )
        }
        return out
    }

    fun save(list: List<Macro>) {
        val arr = JSONArray()
        list.forEach { m ->
            val aArr = JSONArray()
            m.actions.forEach { a -> aArr.put(JSONObject().apply { put("type", a.type.name); put("param", a.param) }) }
            arr.put(JSONObject().apply {
                put("id", m.id); put("name", m.name); put("trigger", m.trigger.name)
                put("param", m.triggerParam); put("actions", aArr)
                put("constraint", m.constraintType.name); put("constraintParam", m.constraintParam); put("enabled", m.enabled); put("lastRunAt", m.lastRunAt)
            })
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}

// 書き出し（JSON文字列）。読み込みは id を振り直して返す
fun exportJson(list: List<Macro>): String {
    val arr = JSONArray()
    list.forEach { m ->
        val aArr = JSONArray()
        m.actions.forEach { a -> aArr.put(JSONObject().apply { put("type", a.type.name); put("param", a.param) }) }
        arr.put(JSONObject().apply {
            put("id", m.id); put("name", m.name); put("trigger", m.trigger.name)
            put("param", m.triggerParam); put("actions", aArr)
            put("constraint", m.constraintType.name); put("constraintParam", m.constraintParam); put("enabled", m.enabled); put("lastRunAt", m.lastRunAt)
        })
    }
    return arr.toString()
}

fun importJson(raw: String): List<Macro>? {
    return try {
        val arr = JSONArray(raw)
        val out = mutableListOf<Macro>()
        val base = System.currentTimeMillis()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val actions = mutableListOf<ActionItem>()
            val aArr = o.optJSONArray("actions")
            if (aArr != null) for (j in 0 until aArr.length()) {
                val ao = aArr.getJSONObject(j)
                actions.add(ActionItem(ActionType.valueOf(ao.getString("type")), ao.optString("param", "")))
            }
            if (actions.isEmpty()) actions.add(ActionItem(ActionType.NOTIFICATION))
            out.add(Macro(
                id = base + i,
                name = o.optString("name", "読み込んだ自動化"),
                trigger = TriggerType.valueOf(o.getString("trigger")),
                triggerParam = o.optString("param", ""),
                actions = actions,
                constraintType = ConstraintType.valueOf(o.optString("constraint", "NONE")),
                constraintParam = o.optString("constraintParam", ""),
                enabled = true, lastRunAt = 0L
            ))
        }
        out
    } catch (e: Exception) { null }
}

class StateStore(context: Context) {
    private val prefs = context.getSharedPreferences("state", Context.MODE_PRIVATE)
    var lastBattery: Int
        get() = prefs.getInt("lastBattery", 100)
        set(v) = prefs.edit().putInt("lastBattery", v).apply()
    var lastWifi: Boolean
        get() = prefs.getBoolean("lastWifi", false)
        set(v) = prefs.edit().putBoolean("lastWifi", v).apply()
    var torchOn: Boolean
        get() = prefs.getBoolean("torchOn", false)
        set(v) = prefs.edit().putBoolean("torchOn", v).apply()
    var lastUsageCheck: Long
        get() = prefs.getLong("lastUsageCheck", 0L)
        set(v) = prefs.edit().putLong("lastUsageCheck", v).apply()
    var runLog: String
        get() = prefs.getString("runLog", "") ?: ""
        set(v) = prefs.edit().putString("runLog", v).apply()
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboardingDone", false)
        set(v) = prefs.edit().putBoolean("onboardingDone", v).apply()
    // 端末状態のエッジ検出用（-1=未観測, 0=OFF, 1=ON）
    var stateDnd: Int
        get() = prefs.getInt("stateDnd", -1)
        set(v) = prefs.edit().putInt("stateDnd", v).apply()
    var statePowerSave: Int
        get() = prefs.getInt("statePowerSave", -1)
        set(v) = prefs.edit().putInt("statePowerSave", v).apply()
    var stateAirplane: Int
        get() = prefs.getInt("stateAirplane", -1)
        set(v) = prefs.edit().putInt("stateAirplane", v).apply()
    // 場所きっかけの内外状態（"マクロID=1,..." 形式、1=範囲内）
    var placeState: String
        get() = prefs.getString("placeState", "") ?: ""
        set(v) = prefs.edit().putString("placeState", v).apply()
    var lastFgPkg: String
        get() = prefs.getString("lastFgPkg", "") ?: ""
        set(v) = prefs.edit().putString("lastFgPkg", v).apply()
    // AIに聞く：APIキー（端末内AIが使えない時の予備）と直近の答え（{ai} 変数用）
    var aiApiKey: String
        get() = prefs.getString("aiApiKey", "") ?: ""
        set(v) = prefs.edit().putString("aiApiKey", v).apply()
    var lastAiResponse: String
        get() = prefs.getString("lastAiResponse", "") ?: ""
        set(v) = prefs.edit().putString("lastAiResponse", v).apply()
    // ホーム画面ウィジェットに表示する文字
    var widgetText: String
        get() = prefs.getString("widgetText", "") ?: ""
        set(v) = prefs.edit().putString("widgetText", v).apply()
    // 画面を消さない の解除時に戻す元の消灯時間（ミリ秒、-1=未保存）
    var screenTimeoutPrev: Int
        get() = prefs.getInt("screenTimeoutPrev", -1)
        set(v) = prefs.edit().putInt("screenTimeoutPrev", v).apply()
}
