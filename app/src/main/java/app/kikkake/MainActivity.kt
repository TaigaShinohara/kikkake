package app.kikkake

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Palette {
    val Paper = Color(0xFFFFFBF7)
    val Ink = Color(0xFF26221F)
    val InkSoft = Color(0xFF6F6659)
    val Pink = Color(0xFFC2185B); val PinkBg = Color(0xFFFCE7F1)
    val Orange = Color(0xFFE65100); val OrangeBg = Color(0xFFFFEDD9)
    val Purple = Color(0xFF6A1B9A); val PurpleBg = Color(0xFFF1E6F7)
    val Sky = Color(0xFF0277BD); val SkyBg = Color(0xFFE0F2FB)
    val CardWhite = Color(0xFFFFFFFF)
    val Line = Color(0xFFEDE6DA)
}

sealed interface Screen { data object Home : Screen; data object Wizard : Screen }

class MainActivity : ComponentActivity() {
    private var powerReceiver: PowerReceiver? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Engine.ensureChannel(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Palette.Pink, background = Palette.Paper, surface = Palette.CardWhite)) {
                Surface(Modifier.fillMaxSize(), color = Palette.Paper) { KikkakeApp() }
            }
        }
    }
    override fun onStart() {
        super.onStart()
        if (powerReceiver == null) {
            powerReceiver = PowerReceiver()
            val f = IntentFilter().apply { addAction(Intent.ACTION_POWER_CONNECTED); addAction(Intent.ACTION_BATTERY_CHANGED) }
            try { registerReceiver(powerReceiver, f) } catch (e: Exception) {}
        }
    }
    override fun onStop() {
        super.onStop()
        powerReceiver?.let { try { unregisterReceiver(it) } catch (e: Exception) {} }
        powerReceiver = null
    }
}

@Composable
fun KikkakeApp() {
    val context = LocalContext.current
    val store = remember { MacroStore(context) }
    var macros by remember { mutableStateOf(store.load()) }
    var screen by rememberSaveable { mutableStateOf("home") }
    // wizard state
    var wTrigger by rememberSaveable { mutableStateOf(TriggerType.TIME.name) }
    var wParam by rememberSaveable { mutableStateOf("22:00") }
    var wAction by rememberSaveable { mutableStateOf(ActionType.NOTIFICATION.name) }
    var wConstraint by rememberSaveable { mutableStateOf(ConstraintType.NONE.name) }
    var wName by rememberSaveable { mutableStateOf("") }
    var wStep by rememberSaveable { mutableIntStateOf(1) }

    fun reload() { macros = store.load() }
    fun saveMacros(list: List<Macro>) { store.save(list); macros = list }

    if (screen == "wizard") {
        BackHandler { if (wStep > 1) wStep-- else screen = "home" }
        WizardScreen(
            step = wStep,
            triggerName = wTrigger, param = wParam, actionName = wAction, constraintName = wConstraint, name = wName,
            onTrigger = { wTrigger = it; wParam = when (it) { TriggerType.TIME.name -> "22:00"; TriggerType.BATTERY_LOW.name -> "20%"; else -> "" } },
            onParam = { wParam = it },
            onAction = { wAction = it },
            onConstraint = { wConstraint = it },
            onName = { wName = it },
            onNext = { if (wStep < 3) wStep++ },
            onBack = { if (wStep > 1) wStep-- else screen = "home" },
            onSave = {
                val t = TriggerType.valueOf(wTrigger); val a = ActionType.valueOf(wAction)
                val nm = wName.ifBlank { Logic.autoName(t, a) }
                val m = Macro(id = System.currentTimeMillis(), name = nm, trigger = t, triggerParam = wParam, action = a, constraintType = ConstraintType.valueOf(wConstraint), enabled = true)
                saveMacros(macros + m)
                Engine.scheduleTimeAlarm(context, m)
                wStep = 1; wName = ""; screen = "home"
            }
        )
        return
    }

    // Home
    Scaffold(
        containerColor = Palette.Paper,
        floatingActionButton = {
            FloatingActionButton(onClick = { wStep = 1; screen = "wizard" }, containerColor = Palette.Pink, contentColor = Color.White) {
                Text("＋ つくる", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Spacer(Modifier.height(10.dp))
                Text("きっかけ", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                Text("きっかけを置くだけで、スマホが動く。", color = Palette.InkSoft, fontSize = 15.sp)
            }
            if (macros.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.PinkBg).padding(20.dp)) {
                        Column {
                            Text("まだ自動化がありません", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Pink)
                            Text("下のテンプレから1つ足すか、「＋ つくる」で3ステップで作れます。自分にも作れるよ。", color = Palette.Ink, fontSize = 14.sp)
                        }
                    }
                }
            }
            items(macros, key = { it.id }) { m ->
                MacroCard(
                    macro = m,
                    onToggle = { saveMacros(macros.map { if (it.id == m.id) it.copy(enabled = !it.enabled) else it }) },
                    onTest = { Engine.execute(context, m); reload() },
                    onDelete = { saveMacros(macros.filter { it.id != m.id }) }
                )
            }
            item { Text("テンプレから始める", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Purple, modifier = Modifier.padding(top = 8.dp)) }
            items(Templates) { t ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.CardWhite).clickable {
                    val m = Macro(id = System.currentTimeMillis() + t.title.hashCode(), name = t.macroName, trigger = t.trigger, triggerParam = t.param, action = t.action, constraintType = t.constraintType, enabled = true)
                    saveMacros(macros + m); Engine.scheduleTimeAlarm(context, m)
                }.padding(16.dp)) {
                    Column {
                        Text(t.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(Logic.summary(t.trigger, t.action, t.param), color = Palette.InkSoft, fontSize = 13.sp)
                        Text("タップで追加", color = Palette.Sky, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
fun MacroCard(macro: Macro, onToggle: () -> Unit, onTest: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.CardWhite).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(macro.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Switch(checked = macro.enabled, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Pink))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(Palette.PinkBg).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(macro.trigger.label, color = Palette.Pink, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Text("→", color = Palette.InkSoft)
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(Palette.OrangeBg).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(macro.action.label, color = Palette.Orange, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                if (macro.constraintType != ConstraintType.NONE) {
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(Palette.PurpleBg).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(macro.constraintType.label, color = Palette.Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
            Text(Logic.summary(macro), color = Palette.InkSoft, fontSize = 13.sp)
            if (macro.lastRunAt > 0) Text("最終実行：${SimpleDateFormat("M月d日 HH:mm", Locale.JAPAN).format(Date(macro.lastRunAt))}", color = Palette.Sky, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTest, colors = ButtonDefaults.buttonColors(containerColor = Palette.Pink), shape = RoundedCornerShape(999.dp)) { Text("テスト実行") }
                OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(999.dp)) { Text("削除") }
            }
        }
    }
}

@Composable
fun WizardScreen(step: Int, triggerName: String, param: String, actionName: String, constraintName: String, name: String, onTrigger: (String) -> Unit, onParam: (String) -> Unit, onAction: (String) -> Unit, onConstraint: (String) -> Unit, onName: (String) -> Unit, onNext: () -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("自動化をつくる", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        // StepHeader
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("① きっかけ" to Palette.Pink, "② やること" to Palette.Orange, "③ 仕上げ" to Palette.Purple).forEachIndexed { idx, (label, color) ->
                val active = idx + 1 == step
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (active) color else Palette.Line).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(label, color = if (active) Color.White else Palette.InkSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        when (step) {
            1 -> {
                Text("いつ動かす？", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Pink)
                TriggerType.values().forEach { t ->
                    val selected = triggerName == t.name
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (selected) Palette.PinkBg else Palette.CardWhite).clickable { onTrigger(t.name) }.padding(16.dp)) {
                        Column { Text(t.label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (selected) Palette.Pink else Palette.Ink); Text(t.detail, color = Palette.InkSoft, fontSize = 13.sp) }
                    }
                }
                if (triggerName == TriggerType.TIME.name) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("07:00", "12:00", "22:00", "23:00").forEach { v ->
                            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (param == v) Palette.Pink else Palette.Line).clickable { onParam(v) }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text(v, color = if (param == v) Color.White else Palette.Ink, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                if (triggerName == TriggerType.BATTERY_LOW.name) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("10%", "20%", "30%").forEach { v ->
                            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (param == v) Palette.Pink else Palette.Line).clickable { onParam(v) }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text(v, color = if (param == v) Color.White else Palette.Ink, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
            2 -> {
                Text("何をする？", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Orange)
                ActionType.values().forEach { a ->
                    val selected = actionName == a.name
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(if (selected) Palette.OrangeBg else Palette.CardWhite).clickable { onAction(a.name) }.padding(16.dp)) {
                        Column { Text(a.label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (selected) Palette.Orange else Palette.Ink); Text(a.detail, color = Palette.InkSoft, fontSize = 13.sp) }
                    }
                }
            }
            else -> {
                Text("名前と条件", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Purple)
                OutlinedTextField(value = name, onValueChange = onName, label = { Text("名前（空なら自動でつきます）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("条件（任意）", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConstraintType.values().forEach { c ->
                        val selected = constraintName == c.name
                        Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (selected) Palette.Purple else Palette.Line).clickable { onConstraint(c.name) }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text(c.label, color = if (selected) Color.White else Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    }
                }
                val t = TriggerType.valueOf(triggerName); val a = ActionType.valueOf(actionName)
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.SkyBg).padding(16.dp)) {
                    Text("プレビュー：${Logic.summary(t, a, param)}", color = Palette.Sky, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("もどる") }
            if (step < 3) Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = Palette.Pink), shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("次へ") }
            else Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Palette.Pink), shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("保存する") }
        }
    }
}
