package app.kikkake

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Engine.ensureChannel(this)
        MonitorWorker.enqueue(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Palette.Pink, background = Palette.Paper, surface = Palette.CardWhite)) {
                Surface(Modifier.fillMaxSize(), color = Palette.Paper) { KikkakeApp() }
            }
        }
    }
}

data class AppEntry(val label: String, val pkg: String)

@Composable
fun KikkakeApp() {
    val context = LocalContext.current
    val store = remember { MacroStore(context) }
    var macros by remember { mutableStateOf(store.load()) }
    var screen by rememberSaveable { mutableStateOf("home") }
    var resumeTick by remember { mutableIntStateOf(0) }

    // 画面に戻るたびに権限状態とマクロを読み直す
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { resumeTick++; macros = store.load() } }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // 通知許可リクエスト（初回のみ自動で出す）
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Engine.hasNotificationPermission(context)) {
            permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // wizard state
    var editingId by rememberSaveable { mutableStateOf(-1L) }
    var wTrigger by rememberSaveable { mutableStateOf(TriggerType.TIME.name) }
    var wParam by rememberSaveable { mutableStateOf("22:00") }
    var wAction by rememberSaveable { mutableStateOf(ActionType.NOTIFICATION.name) }
    var wActionParam by rememberSaveable { mutableStateOf("") }
    var wConstraint by rememberSaveable { mutableStateOf(ConstraintType.NONE.name) }
    var wName by rememberSaveable { mutableStateOf("") }
    var wStep by rememberSaveable { mutableIntStateOf(1) }

    fun reload() { macros = store.load() }
    fun saveMacros(list: List<Macro>) { store.save(list); macros = list }
    fun startWizard(m: Macro?) {
        if (m != null) {
            editingId = m.id; wTrigger = m.trigger.name; wParam = m.triggerParam.ifBlank { "22:00" }
            wAction = m.action.name; wActionParam = m.actionParam; wConstraint = m.constraintType.name; wName = m.name
        } else {
            editingId = -1L; wTrigger = TriggerType.TIME.name; wParam = "22:00"
            wAction = ActionType.NOTIFICATION.name; wActionParam = ""; wConstraint = ConstraintType.NONE.name; wName = ""
        }
        wStep = 1; screen = "wizard"
    }

    if (screen == "wizard") {
        BackHandler { if (wStep > 1) wStep-- else screen = "home" }
        WizardScreen(
            step = wStep, triggerName = wTrigger, param = wParam, actionName = wAction, actionParam = wActionParam,
            constraintName = wConstraint, name = wName, isEditing = editingId > 0,
            onTrigger = { wTrigger = it; wParam = when (it) { TriggerType.TIME.name -> "22:00"; TriggerType.BATTERY_LOW.name -> "20%"; else -> "" } },
            onParam = { wParam = it }, onAction = { wAction = it }, onActionParam = { wActionParam = it },
            onConstraint = { wConstraint = it }, onName = { wName = it },
            onNext = { if (wStep < 3) wStep++ },
            onBack = { if (wStep > 1) wStep-- else screen = "home" },
            onSave = {
                val t = TriggerType.valueOf(wTrigger); val a = ActionType.valueOf(wAction)
                val nm = wName.ifBlank { Logic.autoName(t, a) }
                if (editingId > 0) {
                    val old = macros.find { it.id == editingId }
                    val updated = Macro(id = editingId, name = nm, trigger = t, triggerParam = wParam, action = a, actionParam = if (a == ActionType.LAUNCH_APP) wActionParam else "", constraintType = ConstraintType.valueOf(wConstraint), enabled = old?.enabled ?: true, lastRunAt = old?.lastRunAt ?: 0L)
                    if (old != null) Engine.cancelTimeAlarm(context, old)
                    saveMacros(macros.map { if (it.id == editingId) updated else it })
                    Engine.scheduleTimeAlarm(context, updated)
                } else {
                    val m = Macro(id = System.currentTimeMillis(), name = nm, trigger = t, triggerParam = wParam, action = a, actionParam = if (a == ActionType.LAUNCH_APP) wActionParam else "", constraintType = ConstraintType.valueOf(wConstraint), enabled = true)
                    saveMacros(macros + m)
                    Engine.scheduleTimeAlarm(context, m)
                }
                screen = "home"
            }
        )
        return
    }

    Scaffold(
        containerColor = Palette.Paper,
        floatingActionButton = {
            FloatingActionButton(onClick = { startWizard(null) }, containerColor = Palette.Pink, contentColor = Color.White) {
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
            item { PermissionSection(resumeTick) }
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
                    onToggle = {
                        val updated = m.copy(enabled = !m.enabled)
                        saveMacros(macros.map { if (it.id == m.id) updated else it })
                        if (updated.enabled) Engine.scheduleTimeAlarm(context, updated) else Engine.cancelTimeAlarm(context, updated)
                    },
                    onTest = { Engine.execute(context, m); reload() },
                    onEdit = { startWizard(m) },
                    onDelete = { Engine.cancelTimeAlarm(context, m); saveMacros(macros.filter { it.id != m.id }) }
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
fun PermissionSection(tick: Int) {
    val context = LocalContext.current
    // tick が変わるたび再評価される
    val notifOk = remember(tick) { Engine.hasNotificationPermission(context) }
    val dndOk = remember(tick) { Engine.hasDndAccess(context) }
    val writeOk = remember(tick) { Engine.canWriteSettings(context) }
    if (notifOk && dndOk && writeOk) return
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.SkyBg).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("権限チェック", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Palette.Sky)
            Text("足りない許可があると、その動作は動きません。使う動作の分だけ許可してね。", fontSize = 13.sp, color = Palette.Ink)
            if (!notifOk) PermRow("通知", "「通知を出す」に必要") {
                val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }
                context.startActivity(i)
            }
            if (!dndOk) PermRow("マナーモードの許可", "着信音の変更に必要") {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
            if (!writeOk) PermRow("明るさ変更の許可", "画面の明るさ変更に必要") {
                val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { data = Uri.parse("package:" + context.packageName) }
                context.startActivity(i)
            }
        }
    }
}

@Composable
fun PermRow(title: String, desc: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, color = Palette.InkSoft, fontSize = 12.sp)
        }
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Palette.Sky), shape = RoundedCornerShape(999.dp)) { Text("許可する", fontSize = 13.sp) }
    }
}

@Composable
fun MacroCard(macro: Macro, onToggle: () -> Unit, onTest: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
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
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(999.dp)) { Text("編集") }
                OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(999.dp)) { Text("削除") }
            }
        }
    }
}

@Composable
fun WizardScreen(step: Int, triggerName: String, param: String, actionName: String, actionParam: String, constraintName: String, name: String, isEditing: Boolean, onTrigger: (String) -> Unit, onParam: (String) -> Unit, onAction: (String) -> Unit, onActionParam: (String) -> Unit, onConstraint: (String) -> Unit, onName: (String) -> Unit, onNext: () -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    val context = LocalContext.current
    val apps = remember {
        try {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }
                .map { AppEntry(pm.getApplicationLabel(it).toString(), it.packageName) }
                .sortedBy { it.label }
                .take(60)
        } catch (e: Exception) { emptyList() }
    }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(if (isEditing) "自動化を編集" else "自動化をつくる", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("① きっかけ" to Palette.Pink, "② やること" to Palette.Orange, "③ 仕上げ" to Palette.Purple).forEachIndexed { idx, (label, color) ->
                val active = idx + 1 == step
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (active) color else Palette.Line).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(label, color = if (active) Color.White else Palette.InkSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        when (step) {
            1 -> {
                Text("いつ動かす？", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Pink)
                TriggerType.values().forEach { t ->
                    val selected = triggerName == t.name
                    Box(Modifier.fillMaxWidth().alpha(if (t.available) 1f else 0.55f).clip(RoundedCornerShape(20.dp)).background(if (selected) Palette.PinkBg else Palette.CardWhite).then(if (t.available) Modifier.clickable { onTrigger(t.name) } else Modifier).padding(16.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(t.label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (selected) Palette.Pink else Palette.Ink, modifier = Modifier.weight(1f))
                                if (!t.available) Box(Modifier.clip(RoundedCornerShape(999.dp)).background(Palette.Orange).padding(horizontal = 8.dp, vertical = 2.dp)) { Text("準備中", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                            Text(t.detail, color = Palette.InkSoft, fontSize = 13.sp)
                        }
                    }
                }
                if (triggerName == TriggerType.TIME.name) {
                    val parts = param.split(":"); val curH = parts.getOrNull(0)?.toIntOrNull() ?: 22; val curM = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    Text("時刻：${"%02d:%02d".format(curH, curM)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..23).forEach { h ->
                            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (curH == h) Palette.Pink else Palette.Line).clickable { onParam("%02d:%02d".format(h, curM)) }.padding(horizontal = 12.dp, vertical = 7.dp)) { Text("${h}時", color = if (curH == h) Color.White else Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 15, 30, 45).forEach { m ->
                            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (curM == m) Palette.Pink else Palette.Line).clickable { onParam("%02d:%02d".format(curH, m)) }.padding(horizontal = 12.dp, vertical = 7.dp)) { Text("${m}分", color = if (curM == m) Color.White else Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        }
                    }
                }
                if (triggerName == TriggerType.BATTERY_LOW.name) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("10%", "20%", "30%").forEach { v ->
                            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (param == v) Palette.Pink else Palette.Line).clickable { onParam(v) }.padding(horizontal = 14.dp, vertical = 8.dp)) { Text(v, color = if (param == v) Color.White else Palette.Ink, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Text("電池・Wi-Fiは、アプリを閉じていても約15分ごとにチェックされます。充電開始はその場で動きます。", color = Palette.InkSoft, fontSize = 12.sp)
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
                if (actionName == ActionType.LAUNCH_APP.name) {
                    Text("どのアプリを開く？", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Palette.Orange)
                    LazyColumn(Modifier.heightIn(max = 260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(apps, key = { it.pkg }) { app ->
                            val sel = actionParam == app.pkg
                            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (sel) Palette.OrangeBg else Palette.CardWhite).clickable { onActionParam(app.pkg) }.padding(12.dp)) {
                                Text(app.label, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Palette.Orange else Palette.Ink, fontSize = 14.sp)
                            }
                        }
                    }
                    if (apps.isEmpty()) Text("アプリ一覧が取得できませんでした", color = Palette.InkSoft, fontSize = 13.sp)
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
                if (a == ActionType.LAUNCH_APP && actionParam.isBlank()) Text("注意：「アプリを開く」には、前のステップでアプリを選んでね。", color = Palette.Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("もどる") }
            if (step < 3) Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = Palette.Pink), shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("次へ") }
            else Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Palette.Pink), shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("保存する") }
        }
    }
}
