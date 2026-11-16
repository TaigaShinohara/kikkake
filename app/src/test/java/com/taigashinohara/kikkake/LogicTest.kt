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

    @Test fun summary_includesParamAndConstraint() {
        val m = Macro(1, "test", TriggerType.TIME, "22:00", listOf(ActionItem(ActionType.SILENT), ActionItem(ActionType.BRIGHTNESS_DOWN)), ConstraintType.WEEKDAY)
        val s = Logic.summary(m)
        assertTrue(s.contains("22:00")); assertTrue(s.contains("マナーモード")); assertTrue(s.contains("明るさを下げる")); assertTrue(s.contains("平日だけ"))
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
