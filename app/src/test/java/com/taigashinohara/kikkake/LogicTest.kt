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
}
