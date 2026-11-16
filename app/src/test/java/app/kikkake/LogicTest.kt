package app.kikkake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LogicTest {
    @Test fun autoName_combinesTriggerAndAction() {
        assertEquals("時刻で通知を出す", Logic.autoName(TriggerType.TIME, ActionType.NOTIFICATION))
        assertEquals("充電開始でライト", Logic.autoName(TriggerType.CHARGING_START, ActionType.FLASHLIGHT))
    }
    @Test fun summary_includesParamAndConstraint() {
        val m = Macro(1, "test", TriggerType.TIME, "22:00", ActionType.SILENT, ConstraintType.WEEKDAY)
        assertEquals("時刻（22:00） → マナーモード / 平日だけ", Logic.summary(m))
    }
    @Test fun constraint_weekdayAndNight() {
        assertTrue(Logic.constraintOk(ConstraintType.NONE, Calendar.SUNDAY, 12))
        assertTrue(Logic.constraintOk(ConstraintType.WEEKDAY, Calendar.MONDAY, 12))
        assertFalse(Logic.constraintOk(ConstraintType.WEEKDAY, Calendar.SUNDAY, 12))
        assertTrue(Logic.constraintOk(ConstraintType.NIGHT, Calendar.MONDAY, 22))
        assertFalse(Logic.constraintOk(ConstraintType.NIGHT, Calendar.MONDAY, 12))
    }
}
