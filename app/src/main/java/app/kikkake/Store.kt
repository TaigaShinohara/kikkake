package app.kikkake

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
            out.add(
                Macro(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    trigger = TriggerType.valueOf(o.getString("trigger")),
                    triggerParam = o.optString("param", ""),
                    action = ActionType.valueOf(o.getString("action")),
                    actionParam = o.optString("actionParam", ""),
                    constraintType = ConstraintType.valueOf(o.optString("constraint", "NONE")),
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
            arr.put(JSONObject().apply {
                put("id", m.id); put("name", m.name); put("trigger", m.trigger.name)
                put("param", m.triggerParam); put("action", m.action.name); put("actionParam", m.actionParam)
                put("constraint", m.constraintType.name); put("enabled", m.enabled); put("lastRunAt", m.lastRunAt)
            })
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}

/** エッジ検出などの状態保持 */
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
}
