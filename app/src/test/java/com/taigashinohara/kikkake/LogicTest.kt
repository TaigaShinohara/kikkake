package com.taigashinohara.kikkake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LogicTest {
    @Test fun autoName_single() {
        assertEquals("時刻でマナーモード", Logic.autoName(TriggerType.TIME, listOf(ActionItem(ActionType.SILENT))))
    }
    @Test fun autoName_multi() {
        val n = Logic.autoName(TriggerType.TIME, listOf(ActionItem(ActionType.SILENT), ActionItem(ActionType.BRIGHTNESS_DOWN)))
        assertTrue(n.contains("など2件"))
    }
    @Test fun templates_areWellFormed() {
        // テンプレ全件の健全性（v2.9.0で51件）：名前の重複なし、動作が空でない、ひとことがある
        assertEquals(51, Templates.size)
        assertEquals(Templates.size, Templates.map { it.macroName }.toSet().size)
        Templates.forEach { t ->
            assertTrue("${t.title} has actions", t.actions.isNotEmpty())
            assertTrue("${t.title} has note", t.note.isNotBlank())
            // v2.15.0：結果の一文（カードの主役）が全件にあること。長すぎると主役にならないので上限も見る
            assertTrue("${t.title} has benefit", t.benefit.isNotBlank())
            assertTrue("${t.title} benefit too long", t.benefit.length <= 32)
        }
    }
    @Test fun sceneOf_byTriggerAndTime() {
        fun t(trigger: TriggerType, param: String = "") = Template("x", "x", trigger, param, listOf(ActionItem(ActionType.NOTIFICATION)))
        assertEquals("夜", Logic.sceneOf(t(TriggerType.TIME, "22:00")).second)
        assertEquals("朝", Logic.sceneOf(t(TriggerType.TIME, "07:00")).second)
        assertEquals("夕方", Logic.sceneOf(t(TriggerType.TIME, "18:45")).second)
        assertEquals("日の入り", Logic.sceneOf(t(TriggerType.SUN_EVENT, "sunset@34.6,135.5")).second)
        assertEquals("日の出", Logic.sceneOf(t(TriggerType.SUN_EVENT, "sunrise@34.6,135.5")).second)
        assertEquals("電池", Logic.sceneOf(t(TriggerType.BATTERY_LOW, "20%")).second)
        assertEquals("充電", Logic.sceneOf(t(TriggerType.CHARGING_START)).second)
        assertEquals("帰宅", Logic.sceneOf(t(TriggerType.LOCATION, "arrive@34.6,135.5")).second)
        assertEquals("連絡", Logic.sceneOf(t(TriggerType.NOTIFICATION_POSTED, "a|b")).second)
        assertEquals("玄関", Logic.sceneOf(t(TriggerType.NFC)).second)
        // 全テンプレに場面が付く（未対応トリガーの混入防止）
        Templates.forEach { assertTrue("${it.title} scene", Logic.sceneOf(it).second.isNotBlank()) }
    }
    @Test fun allPatternsHaveIcons() {
        TriggerType.values().forEach { assertTrue(it.icon.isNotBlank()) }
        ActionType.values().forEach { assertTrue(it.icon.isNotBlank()) }
    }
    @Test fun allActionsBelongToKnownGroup() {
        ActionType.values().forEach { assertTrue("group of ${it.name}", ActionGroups.contains(it.group)) }
        ActionGroups.forEach { g -> assertTrue(ActionType.values().any { it.group == g }) }
    }
    @Test fun dndHasOffCounterpart() {
        assertEquals("音", ActionType.DND_OFF.group)
        assertTrue(ActionType.DND_OFF.label.contains("解除"))
    }
    @Test fun chargingDoneLabelMatchesBehavior() {
        assertTrue(TriggerType.CHARGING_DONE.label.contains("ケーブル"))
        assertTrue(TriggerType.HEADSET.detail.contains("有線"))
    }

    @Test fun summary_includesParamAndConstraint() {
        val m = Macro(1, "test", TriggerType.TIME, "22:00", listOf(ActionItem(ActionType.SILENT), ActionItem(ActionType.BRIGHTNESS_DOWN)), ConstraintType.WEEKDAY)
        val s = Logic.summary(m)
        assertTrue(s.contains("22:00")); assertTrue(s.contains("マナーモード")); assertTrue(s.contains("明るさを下げる")); assertTrue(s.contains("平日だけ"))
    }
    @Test fun missingPermKeys_pure() {
        // スナップショット版は純粋判定：全部そろっていれば空、通知だけ欠ければ notif だけ
        val all = Engine.PermState(notif = true, dnd = true, write = true, exact = true, usage = true, location = true, calendar = true, bt = true, notiflisten = true)
        val m = Macro(1, "test", TriggerType.TIME, "22:00", listOf(ActionItem(ActionType.NOTIFICATION)), ConstraintType.NONE)
        assertTrue(missingPermKeys(listOf(m), all).isEmpty())
        val noNotif = all.copy(notif = false)
        assertTrue(missingPermKeys(listOf(m), noNotif) == setOf("notif"))
    }
    @Test fun tileSummary_isShort() {
        // タイル要約は「きっかけ → 最初の動作 ほかN件」だけ。動作の文面（薬を飲む等）は出さない
        val m = Macro(1, "test", TriggerType.CHARGING_START, "", listOf(ActionItem(ActionType.NOTIFICATION, "薬を飲む"), ActionItem(ActionType.VIBRATE), ActionItem(ActionType.FLASHLIGHT)), ConstraintType.NONE)
        val s = Logic.tileSummary(m)
        assertTrue(s.contains("充電開始")); assertTrue(s.contains("通知を出す")); assertTrue(s.contains("ほか2件"))
        assertFalse(s.contains("薬を飲む"))
    }
    @Test fun summary_includesMessageParam() {
        val m = Macro(1, "test", TriggerType.TIME, "07:00", listOf(ActionItem(ActionType.NOTIFICATION, "薬を飲む")))
        assertTrue(Logic.summary(m).contains("薬を飲む"))
        val m2 = Macro(2, "test", TriggerType.TIME, "07:00", listOf(ActionItem(ActionType.SPEAK, "おはよう")))
        assertTrue(Logic.summary(m2).contains("おはよう"))
    }
    @Test fun triggerGroups() {
        TriggerType.values().forEach { assertTrue("group of ${it.name}", TriggerGroups.contains(it.group)) }
        TriggerGroups.forEach { g -> assertTrue(TriggerType.values().any { it.group == g }) }
    }

    @Test fun batteryRisen() {
        assertTrue(Logic.batteryRisen(79, 80, 80))
        assertTrue(Logic.batteryRisen(50, 100, 80))
        assertTrue(!Logic.batteryRisen(80, 90, 80))
        assertTrue(!Logic.batteryRisen(50, 79, 80))
    }

    @Test fun timeDays() {
        assertEquals(null, Logic.timeDaysOf("22:00"))
        assertEquals(null, Logic.timeDaysOf("22:00@1234567"))
        assertEquals(setOf(2, 3, 4, 5, 6), Logic.timeDaysOf("22:00@23456"))
        assertEquals("平日", Logic.daysLabel(setOf(2, 3, 4, 5, 6)))
        assertEquals("週末", Logic.daysLabel(setOf(1, 7)))
    }

    @Test fun expandVarsReplaces() {
        val out = Logic.expandVars("{date} {time} {weekday} 電池{battery}%", "2026/11/22", "07:30", "日", 85)
        val withAi = Logic.expandVars("答えは{ai}です", "2026/11/22", "07:30", "日", 85, "晴れ")
        assertEquals("答えは晴れです", withAi)
        assertEquals("2026/11/22 07:30 日 電池85%", out)
    }

    @Test fun expandRepeatsDuplicatesPrevious() {
        val input = listOf(ActionItem(ActionType.VIBRATE), ActionItem(ActionType.REPEAT, "3回"), ActionItem(ActionType.RING))
        val out = Logic.expandRepeats(input)
        assertEquals(5, out.size)
        assertEquals(ActionType.VIBRATE, out[3].type)
        assertEquals(ActionType.RING, out[4].type)
        assertEquals(1, Logic.expandRepeats(listOf(ActionItem(ActionType.REPEAT, "2回"), ActionItem(ActionType.RING))).size)
    }

    @Test fun sunEventTokyo() {
        val sr = Logic.sunEventMinutes(35.68, 139.69, 326, 540, true)
        val ss = Logic.sunEventMinutes(35.68, 139.69, 326, 540, false)
        assertTrue("sunrise=$sr", sr in 378..398)
        assertTrue("sunset=$ss", ss in 979..999)
    }

    @Test fun batteryCrossed_onlyOnCrossing() {
        assertTrue(Logic.batteryCrossed(21, 20, 20))
        assertTrue(Logic.batteryCrossed(50, 10, 20))
        assertFalse(Logic.batteryCrossed(20, 19, 20))
        assertFalse(Logic.batteryCrossed(10, 5, 20))
        assertFalse(Logic.batteryCrossed(30, 25, 20))
    }
    @Test fun wifiEdge_onlyOnConnect() {
        assertTrue(Logic.wifiConnectedEdge(false, true))
        assertFalse(Logic.wifiConnectedEdge(true, true))
        assertFalse(Logic.wifiConnectedEdge(true, false))
    }
    @Test fun constraint_weekdayAndNight() {
        assertTrue(Logic.constraintOk(ConstraintType.WEEKDAY, Calendar.MONDAY, 10))
        assertFalse(Logic.constraintOk(ConstraintType.WEEKDAY, Calendar.SATURDAY, 10))
        assertTrue(Logic.constraintOk(ConstraintType.NIGHT, Calendar.MONDAY, 22))
        assertTrue(Logic.constraintOk(ConstraintType.NIGHT, Calendar.MONDAY, 3))
        assertFalse(Logic.constraintOk(ConstraintType.NIGHT, Calendar.MONDAY, 12))
        assertTrue(Logic.constraintOk(ConstraintType.NONE, Calendar.SUNDAY, 12))
    }
    // 「今日の顔」（v2.14.0）：予定の時刻と参加者の有無だけで判定する
    private val day0 = 1_731_000_000_000L
    private fun span(h1: Int, h2: Int, att: Boolean = false) = DayEventSpan(day0 + h1 * 3600_000L, day0 + h2 * 3600_000L, att)
    @Test fun dayFace_togetherWins() {
        assertEquals(DayFace.TOGETHER, DayLogic.faceFor(listOf(span(10, 11, att = true)), day0))
        // 忙しい日でも、誰かと一緒の予定があれば TOGETHER が優先
        assertEquals(DayFace.TOGETHER, DayLogic.faceFor(listOf(span(9, 10), span(11, 12), span(13, 14, att = true)), day0))
    }
    @Test fun dayFace_busyByCountAndMinutes() {
        assertEquals(DayFace.BUSY, DayLogic.faceFor(listOf(span(9, 10), span(11, 12), span(13, 14)), day0))
        assertEquals(DayFace.BUSY, DayLogic.faceFor(listOf(span(9, 14)), day0))
    }
    @Test fun dayFace_eveningAndOpen() {
        assertEquals(DayFace.EVENING, DayLogic.faceFor(listOf(span(19, 20)), day0))
        assertEquals(DayFace.OPEN, DayLogic.faceFor(listOf(span(9, 10)), day0))
        assertEquals(DayFace.OPEN, DayLogic.faceFor(emptyList(), day0))
    }
    @Test fun dayFace_fallbackByWeekdayAndHour() {
        assertEquals(DayFace.OPEN, DayLogic.fallbackFace(Calendar.SATURDAY, 10))
        assertEquals(DayFace.OPEN, DayLogic.fallbackFace(Calendar.SUNDAY, 20))
        assertEquals(DayFace.BUSY, DayLogic.fallbackFace(Calendar.MONDAY, 10))
        assertEquals(DayFace.EVENING, DayLogic.fallbackFace(Calendar.MONDAY, 19))
    }
    @Test fun dayRecommended_picksTaggedAndSkipsAdded() {
        val rec = DayLogic.recommendedFor(DayFace.BUSY, emptySet())
        assertEquals(3, rec.size)
        assertTrue(rec.all { Situation.QUICK in it.situations })
        val rec2 = DayLogic.recommendedFor(DayFace.BUSY, setOf(rec[0].macroName))
        assertEquals(3, rec2.size)
        assertFalse(rec2.any { it.macroName == rec[0].macroName })
        // どの顔でも3件は候補がある（場面タグの付け忘れ防止）
        DayFace.values().forEach { f -> assertEquals("face $f", 3, DayLogic.recommendedFor(f, emptySet()).size) }
    }
    @Test fun dayStrip_fallbackWording() {
        assertTrue(DayLogic.stripText(DayFace.BUSY, fromCalendar = false).contains("曜日と時間帯"))
        assertTrue(DayLogic.stripText(DayFace.OPEN, fromCalendar = true).contains("予定が少なめ"))
    }
}
