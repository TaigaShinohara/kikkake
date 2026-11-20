package com.taigashinohara.kikkake

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Palette {
    var Paper = Color(0xFFFFFBF7)
    var Ink = Color(0xFF26221F)
    var InkSoft = Color(0xFF554D44)
    var Pink = Color(0xFFC2185B); var PinkBg = Color(0xFFFCE7F1)
    var Orange = Color(0xFFE65100); var OrangeBg = Color(0xFFFFEDD9)
    var Purple = Color(0xFF6A1B9A); var PurpleBg = Color(0xFFF1E6F7)
    var Sky = Color(0xFF0277BD); var SkyBg = Color(0xFFE0F2FB)
    var CardWhite = Color(0xFFFFFFFF)
    var Line = Color(0xFFEDE6DA)
    var GlassHi = Color.White.copy(alpha = 0.75f)
    var GlassLo = Color.White.copy(alpha = 0.15f)
    var GlassCard = Color.White.copy(alpha = 0.58f)
    // Simple Glass v1.1「やんわりグラデーション」：読み物ガラスは上62%→下54%（平均58%）
    var GlassCardTop = Color.White.copy(alpha = 0.62f)
    var GlassCardBottom = Color.White.copy(alpha = 0.54f)
    var InputFace = Color.White.copy(alpha = 0.84f)
    var BlobA = Color(0xFFF8BBD0)
    var BlobB = Color(0xFFFFCC80)
    var BlobC = Color(0xFF81D4FA)

    fun applyDark(dark: Boolean) {
        if (dark) {
            Paper = Color(0xFF1B1713); Ink = Color(0xFFF3EDE6); InkSoft = Color(0xFFD8CFC2)
            Pink = Color(0xFFF78FB8); PinkBg = Color(0xFF3D2230)
            Orange = Color(0xFFFFAB7A); OrangeBg = Color(0xFF402A1C)
            Purple = Color(0xFFCE9BE8); PurpleBg = Color(0xFF2F2140)
            Sky = Color(0xFF7CC7F0); SkyBg = Color(0xFF1D3242)
            CardWhite = Color(0xFF292219); Line = Color(0xFF3A322A)
            GlassHi = Color.White.copy(alpha = 0.38f); GlassLo = Color.White.copy(alpha = 0.06f)
            GlassCard = Color.White.copy(alpha = 0.16f)
            GlassCardTop = Color.White.copy(alpha = 0.19f)
            GlassCardBottom = Color.White.copy(alpha = 0.13f)
            InputFace = Color.White.copy(alpha = 0.20f)
            BlobA = Color(0xFF4E2438); BlobB = Color(0xFF4E2E1C); BlobC = Color(0xFF16303F)
        }
    }
}

// Liquid Glass風：半透明の面＋上辺のハイライトふち＋ぼかした色の光
// 読み物ガラス（GlassCard）は Simple Glass v1.1 のやんわりグラデーション（上→下・同系色・段差約8pt）で塗る
fun Modifier.glass(radius: Int, color: Color): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    val base = if (color == Palette.GlassCard) {
        this.clip(shape).background(Brush.verticalGradient(listOf(Palette.GlassCardTop, Palette.GlassCardBottom)))
    } else if (color.alpha < 1f) {
        this.clip(shape).background(Brush.verticalGradient(listOf(color.copy(alpha = minOf(1f, color.alpha + 0.04f)), color.copy(alpha = maxOf(0f, color.alpha - 0.04f)))))
    } else {
        this.clip(shape).background(tintBrush(color))
    }
    return base.border(1.dp, Brush.verticalGradient(listOf(Palette.GlassHi, Palette.GlassLo)), shape)
}

fun Color.lerpWhite(f: Float): Color = androidx.compose.ui.graphics.lerp(this, Color.White, f)
fun Color.lerpBlack(f: Float): Color = androidx.compose.ui.graphics.lerp(this, Color.Black, f)
// 役割色ベタのやんわりグラデーション（Simple Glass v1.1：白+8%→黒-4%）
fun solidBrush(color: Color) = Brush.verticalGradient(listOf(color.lerpWhite(0.08f), color.lerpBlack(0.04f)))
// 淡色ティント面のやんわりグラデーション（上に白6%→下はそのまま）
fun tintBrush(color: Color) = Brush.verticalGradient(listOf(color.lerpWhite(0.06f), color))

// 押した瞬間に少し沈むフィードバック（状態の完全性・SG-8）。面より前に置いて全体を縮める
@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    return this.scale(if (pressed && enabled) 0.98f else 1f)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

// 主ボタン：役割色ベタ＋やんわりグラデーション（ガラス化しない）
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(999.dp))
            .background(if (enabled) solidBrush(Palette.Pink) else tintBrush(Palette.Line))
            .pressable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (enabled) Color.White else Palette.InkSoft, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
fun AppBackground() {
    // 光は放射グラデーション（中心→透明）で描く。blur はレイヤーの端で切れて硬い継ぎ目になるため使わない（v1.5.2）
    val base = Brush.verticalGradient(listOf(Palette.Paper, androidx.compose.ui.graphics.lerp(Palette.Paper, Palette.PinkBg, 0.38f), androidx.compose.ui.graphics.lerp(Palette.Paper, Palette.SkyBg, 0.32f)))
    Box(Modifier.fillMaxSize().background(base)) {
        Box(Modifier.size(430.dp).offset(x = (-110).dp, y = (-90).dp).background(Brush.radialGradient(listOf(Palette.BlobA, Color.Transparent))))
        Box(Modifier.size(380.dp).offset(x = 230.dp, y = 590.dp).background(Brush.radialGradient(listOf(Palette.BlobB, Color.Transparent))))
        Box(Modifier.size(400.dp).offset(x = 110.dp, y = 430.dp).background(Brush.radialGradient(listOf(Palette.BlobC, Color.Transparent))))
        Box(Modifier.size(360.dp).offset(x = (-90).dp, y = 670.dp).background(Brush.radialGradient(listOf(Palette.BlobA, Color.Transparent))))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Engine.ensureChannel(this)
        MonitorWorker.enqueue(this)
        setContent {
            val dark = isSystemInDarkTheme()
            Palette.applyDark(dark)
            MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Palette.Pink, background = Palette.Paper, surface = Palette.CardWhite) else lightColorScheme(primary = Palette.Pink, background = Palette.Paper, surface = Palette.CardWhite)) {
                Surface(Modifier.fillMaxSize(), color = Color.Transparent) { Box(Modifier.fillMaxSize()) { AppBackground(); KikkakeApp() } }
            }
        }
    }
}

data class AppEntry(val label: String, val pkg: String)

@Composable
fun rememberApps(): List<AppEntry> {
    val context = LocalContext.current
    return remember {
        try {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }
                .map { AppEntry(pm.getApplicationLabel(it).toString(), it.packageName) }
                .sortedBy { it.label }
                .take(200)
        } catch (e: Exception) { emptyList() }
    }
}

@Composable
fun KikkakeApp() {
    val context = LocalContext.current
    val store = remember { MacroStore(context) }
    val stateStore = remember { StateStore(context) }
    var macros by remember { mutableStateOf(store.load()) }
    var screen by rememberSaveable { mutableStateOf(if (stateStore.onboardingDone) "home" else "onboarding") }
    var resumeTick by remember { mutableIntStateOf(0) }
    var highlightId by remember { mutableStateOf(-1L) }
    var pickerTarget by rememberSaveable { mutableStateOf("") }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { resumeTick++; macros = store.load() } }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // 通知許可は、最初の自動化ができてから頼む（UX-15）
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumeTick++ }
    LaunchedEffect(screen, macros.size) {
        if (screen == "home" && macros.isNotEmpty() && Build.VERSION.SDK_INT >= 33 && !Engine.hasNotificationPermission(context)) {
            permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var editingId by rememberSaveable { mutableStateOf(-1L) }
    var wTrigger by rememberSaveable { mutableStateOf(TriggerType.TIME.name) }
    var wParam by rememberSaveable { mutableStateOf("22:00") }
    var wActions by remember { mutableStateOf(setOf(ActionType.NOTIFICATION.name)) }
    var wLaunchParam by rememberSaveable { mutableStateOf("") }
    var wNotifParam by rememberSaveable { mutableStateOf("") }
    var wSpeakParam by rememberSaveable { mutableStateOf("") }
    var permGuide by remember { mutableStateOf<Macro?>(null) }
    var wConstraint by rememberSaveable { mutableStateOf(ConstraintType.NONE.name) }
    var wName by rememberSaveable { mutableStateOf("") }
    var wStep by rememberSaveable { mutableIntStateOf(1) }

    fun reload() { macros = store.load() }
    fun saveMacros(list: List<Macro>) { store.save(list); macros = list }
    fun startWizard(m: Macro?) {
        if (m != null) {
            editingId = m.id; wTrigger = m.trigger.name; wParam = m.triggerParam.ifBlank { if (m.trigger == TriggerType.TIME) "22:00" else "" }
            wActions = m.actions.map { it.type.name }.toSet()
            wLaunchParam = m.actions.firstOrNull { it.type == ActionType.LAUNCH_APP }?.param ?: ""
            wNotifParam = m.actions.firstOrNull { it.type == ActionType.NOTIFICATION }?.param ?: ""
            wSpeakParam = m.actions.firstOrNull { it.type == ActionType.SPEAK }?.param ?: ""
            wConstraint = m.constraintType.name; wName = m.name
        } else {
            editingId = -1L; wTrigger = TriggerType.TIME.name; wParam = "22:00"
            wActions = setOf(ActionType.NOTIFICATION.name); wLaunchParam = ""; wNotifParam = ""; wSpeakParam = ""; wConstraint = ConstraintType.NONE.name; wName = ""
        }
        wStep = 1; screen = "wizard"
    }

    fun addMacro(m: Macro, message: String) {
        saveMacros(macros + m)
        Engine.scheduleTimeAlarm(context, m)
        if (missingPermKeys(context, listOf(m)).isNotEmpty()) permGuide = m
        highlightId = m.id
        scope.launch {
            val idx = macros.indexOfFirst { it.id == m.id }
            // ヘッダー分を概算でずらして新規カードへ寄せる
            listState.animateScrollToItem(if (idx >= 0) idx + 3 else 3)
            snackbarHost.showSnackbar(message)
        }
    }

    if (screen == "onboarding") {
        OnboardingScreen(onFinish = { stateStore.onboardingDone = true; reload(); screen = "home" })
        return
    }

    if (pickerTarget.isNotEmpty()) {
        BackHandler { pickerTarget = "" }
        AppPickerScreen(
            title = if (pickerTarget == "trigger") "どのアプリを開いたら？" else "どのアプリを起動する？",
            selected = if (pickerTarget == "trigger") wParam else wLaunchParam,
            onSelect = { pkg -> if (pickerTarget == "trigger") wParam = pkg else wLaunchParam = pkg; pickerTarget = "" },
            onBack = { pickerTarget = "" }
        )
        return
    }

    if (screen == "wizard") {
        BackHandler { if (wStep > 1) wStep-- else screen = "home" }
        WizardScreen(
            step = wStep, triggerName = wTrigger, param = wParam, actionNames = wActions, launchParam = wLaunchParam,
            constraintName = wConstraint, name = wName, isEditing = editingId > 0,
            onTrigger = { wTrigger = it; wParam = when (it) { TriggerType.TIME.name -> "22:00"; TriggerType.BATTERY_LOW.name -> "20%"; else -> "" } },
            onParam = { wParam = it },
            onToggleAction = { n -> wActions = if (wActions.contains(n)) { if (wActions.size > 1) wActions - n else wActions } else wActions + n },
            onLaunchParam = { wLaunchParam = it },
            notifParam = wNotifParam, speakParam = wSpeakParam, onNotifParam = { wNotifParam = it }, onSpeakParam = { wSpeakParam = it },
            onOpenPicker = { pickerTarget = it },
            onConstraint = { wConstraint = it }, onName = { wName = it },
            onNext = { if (wStep < 3) wStep++ },
            onBack = { if (wStep > 1) wStep-- else screen = "home" },
            onSave = {
                val t = TriggerType.valueOf(wTrigger)
                val items = ActionType.values().filter { wActions.contains(it.name) }.map { at -> ActionItem(at, when (at) { ActionType.LAUNCH_APP -> wLaunchParam; ActionType.NOTIFICATION -> wNotifParam; ActionType.SPEAK -> wSpeakParam; else -> "" }) }
                val nm = wName.ifBlank { Logic.autoName(t, items) }
                if (editingId > 0) {
                    val old = macros.find { it.id == editingId }
                    val updated = Macro(id = editingId, name = nm, trigger = t, triggerParam = wParam, actions = items, constraintType = ConstraintType.valueOf(wConstraint), enabled = old?.enabled ?: true, lastRunAt = old?.lastRunAt ?: 0L)
                    if (old != null) Engine.cancelTimeAlarm(context, old)
                    saveMacros(macros.map { if (it.id == editingId) updated else it })
                    Engine.scheduleTimeAlarm(context, updated)
                    if (missingPermKeys(context, listOf(updated)).isNotEmpty()) permGuide = updated
                    Toast.makeText(context, "直したよ", Toast.LENGTH_SHORT).show()
                } else {
                    val m = Macro(id = System.currentTimeMillis(), name = nm, trigger = t, triggerParam = wParam, actions = items, constraintType = ConstraintType.valueOf(wConstraint), enabled = true)
                    addMacro(m, "作れたよ！「${nm}」")
                }
                screen = "home"
            }
        )
        return
    }

    if (screen == "help") {
        BackHandler { screen = "home" }
        HelpScreen(onBack = { screen = "home" })
        return
    }

    permGuide?.let { g -> PermGuideDialog(macro = g, tick = resumeTick, onClose = { permGuide = null }) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(solidBrush(Palette.Pink)).pressable { startWizard(null) }.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("＋ つくる", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    ) { pad ->
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 96.dp), modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("きっかけ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
                    Spacer(Modifier.weight(1f))
                    Text("使い方", color = Palette.Sky, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.clickable { screen = "help" }.padding(8.dp))
                }
            }
            if (macros.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().glass(20, Palette.PinkBg).padding(20.dp)) {
                        Column {
                            Text("まだ自動化がありません", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Pink)
                            Text("下のテンプレから1つ足すか、「＋ つくる」で3ステップで作れます。自分にも作れるよ。", color = Palette.Ink, fontSize = 15.sp, lineHeight = 24.sp)
                        }
                    }
                }
            }
            itemsIndexed(macros, key = { _, m -> m.id }) { _, m ->
                MacroCard(
                    macro = m, highlighted = m.id == highlightId,
                    onToggle = {
                        val updated = m.copy(enabled = !m.enabled)
                        saveMacros(macros.map { if (it.id == m.id) updated else it })
                        if (updated.enabled) Engine.scheduleTimeAlarm(context, updated) else Engine.cancelTimeAlarm(context, updated)
                    },
                    onTest = {
                        // 声で知らせる等の完了待ちがあるため、テストはバックグラウンドで実行する
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { Engine.executeTest(context, m) }
                            val msg = when (r) {
                                Engine.ExecResult.OK -> "動いたよ ✓"
                                Engine.ExecResult.PARTIAL_OK -> "一部の動作は許可が足りなくて動かなかったよ。下の「権限チェック」を見てね"
                                Engine.ExecResult.BLOCKED_BY_PERMISSION -> "許可が足りない動作があったよ。下の「権限チェック」を見てね"
                                else -> "今は動かせる動作がなかったよ"
                            }
                            snackbarHost.showSnackbar(msg)
                            reload()
                        }
                    },
                    onEdit = { startWizard(m) },
                    onDelete = {
                        Engine.cancelTimeAlarm(context, m)
                        val removed = m
                        saveMacros(macros.filter { it.id != m.id })
                        scope.launch {
                            val res = snackbarHost.showSnackbar("「${removed.name}」を消したよ", actionLabel = "元に戻す", duration = SnackbarDuration.Short)
                            if (res == SnackbarResult.ActionPerformed) {
                                saveMacros(store.load() + removed)
                                Engine.scheduleTimeAlarm(context, removed)
                            }
                        }
                    }
                )
            }
            item { RunLogSection(stateStore.runLog) }
            item { PermissionSection(resumeTick, macros) }
            item {
                var expanded by remember { mutableStateOf(false) }
                val showAll = macros.isEmpty() || expanded
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("テンプレから始める", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Ink, modifier = Modifier.weight(1f))
                        if (macros.isNotEmpty()) Text(if (expanded) "閉じる" else "開く", color = Palette.Sky, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.clickable { expanded = !expanded }.padding(8.dp))
                    }
                    if (showAll) Templates.forEach { t ->
                        Box(Modifier.fillMaxWidth().pressable {
                            if (macros.any { it.name == t.macroName }) {
                                scope.launch { snackbarHost.showSnackbar("「${t.macroName}」はもうあるよ") }
                            } else {
                                val m = Macro(id = System.currentTimeMillis() + t.title.hashCode(), name = t.macroName, trigger = t.trigger, triggerParam = t.param, actions = t.actions, constraintType = t.constraintType, enabled = true)
                                addMacro(m, "追加したよ！「${t.macroName}」")
                            }
                        }.glass(20, Palette.GlassCard).padding(16.dp)) {
                            Column {
                                Text(t.title, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(Logic.summary(Macro(0, t.macroName, t.trigger, t.param, t.actions, t.constraintType)), color = Palette.InkSoft, fontSize = 15.sp, lineHeight = 24.sp)
                                if (t.trigger == TriggerType.BATTERY_LOW || t.trigger == TriggerType.WIFI_CONNECTED || t.trigger == TriggerType.APP_LAUNCH) Text("※ 約15分ごとにチェックするので、少し遅れて動きます", color = Palette.InkSoft, fontSize = 13.sp)
                                Text("タップで追加", color = Palette.Sky, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
fun RunLogSection(runLog: String) {
    data class Entry(val name: String, val at: Long, val failReason: String?)
    val entries = runLog.split("\n").filter { it.isNotBlank() }.take(5).mapNotNull {
        val parts = it.split("|")
        if (parts.size >= 2) Entry(parts[0], parts[1].toLongOrNull() ?: 0L, if (parts.size >= 3 && parts[2].startsWith("F:")) parts[2].removePrefix("F:") else null) else null
    }
    if (entries.isEmpty()) return
    Box(Modifier.fillMaxWidth().glass(20, Palette.GlassCard).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("最近の実行", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Ink)
            entries.forEach { e ->
                val time = SimpleDateFormat("M月d日 HH:mm", Locale.JAPAN).format(Date(e.at))
                if (e.failReason == null) {
                    Text("✓ ${e.name}（$time）", fontSize = 15.sp, color = Palette.Ink, lineHeight = 24.sp)
                } else {
                    Text("⚠ ${e.name}（$time）${e.failReason}ので動かなかった", fontSize = 15.sp, color = Palette.Orange, lineHeight = 24.sp)
                }
            }
        }
    }
}

fun hapticFeedback(view: View, strong: Boolean) {
    val c = if (strong && Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    view.performHapticFeedback(c)
}

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    var page by rememberSaveable { mutableIntStateOf(0) }
    var trouble by rememberSaveable { mutableStateOf("") }
    var advancing by remember { mutableStateOf(false) }
    var trialPlaced by remember { mutableStateOf(false) }
    var permTick by remember { mutableIntStateOf(0) }
    val store = remember { MacroStore(context) }

    val lifecycleOwner2 = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner2) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) permTick++ }
        lifecycleOwner2.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner2.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(advancing) {
        if (advancing) { delay(950); page = 2; advancing = false }
    }

    // 完了の返し（大）：試しを置いて最後のページに着いたら、成功の触覚を1回だけ返す
    LaunchedEffect(page) {
        if (page == 7 && trialPlaced) hapticFeedback(view, strong = true)
    }

    // 戻る操作でも前のページへ（離れる操作に称賛・触覚は付けない）
    BackHandler(enabled = page > 0) { page-- }

    val trialTemplate = when (trouble) {
        "battery" -> Templates[2]
        else -> Templates[0]
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 進捗は窓ドット：最大5つ、現在を中心に窓が動き、総ページ数を数えさせない
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val totalPages = 8
                val winStart = (page - 2).coerceIn(0, totalPages - 5)
                (winStart until winStart + 5).forEach { i ->
                    val dotSize = when { i == page -> 10.dp; i == winStart || i == winStart + 4 -> 5.dp; else -> 8.dp }
                    Box(Modifier.size(dotSize).clip(RoundedCornerShape(999.dp)).background(if (i <= page) Palette.Pink else Palette.Line))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        when (page) {
            0 -> {
                Text("スマホの操作、\n覚えなくていい", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, lineHeight = 36.sp)
                Text("きっかけを置くだけで、スマホが勝手に動いてくれる。", fontSize = 15.sp, color = Palette.InkSoft, lineHeight = 24.sp)
                BigEmoji("🌱")
            }
            1 -> {
                Text("どんなことで\n困ってる？", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, lineHeight = 36.sp)
                listOf("night" to "夜に音が鳴って困る", "battery" to "電池の減りが気になる", "unknown" to "まだわからない").forEach { (key, label) ->
                    Box(Modifier.fillMaxWidth().pressable { if (!advancing) { trouble = key; advancing = true; hapticFeedback(view, strong = false) } }.glass(20, if (trouble == key) Palette.PinkBg else Palette.GlassCard).padding(20.dp)) {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (trouble == key) Palette.Pink else Palette.Ink)
                    }
                }
                if (advancing) {
                    val praise = when (trouble) {
                        "night" -> "ありがとう。夜の静けさ、大事だよね"
                        "battery" -> "ありがとう。電池の心配、減らしていこう"
                        else -> "ありがとう。正直がいちばん助かる"
                    }
                    Text(praise, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Palette.Pink, lineHeight = 24.sp)
                }
            }
            2 -> {
                val echo = when (trouble) {
                    "night" -> "「夜に音が鳴って困る」——その悩み、受け取ったよ"
                    "battery" -> "「電池の減りが気になる」——その心配、受け取ったよ"
                    else -> "「まだわからない」でも大丈夫"
                }
                Text(echo, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Pink, lineHeight = 36.sp)
                val msg = when (trouble) {
                    "night" -> "22時に自動で静かにできるよ"
                    "battery" -> "電池が減ったら、自動で節約できるよ"
                    else -> "大丈夫。テンプレから気軽に試せるよ"
                }
                Text("そんなあなたに、きっかけはぴったり。$msg", fontSize = 15.sp, color = Palette.InkSoft, lineHeight = 24.sp)
                BigEmoji("✨")
            }
            3 -> { StepPage("ステップ 1 / 3", "きっかけを選ぶ", "「22時になったら」「電池が減ったら」みたいな、始まりの合図。", "①", Palette.Pink, Palette.PinkBg) }
            4 -> { StepPage("ステップ 2 / 3", "やることを選ぶ", "静かにする、通知する…。組み合わせもOK。", "②", Palette.Orange, Palette.OrangeBg) }
            5 -> { StepPage("ステップ 3 / 3", "保存するだけ", "あとはスマホがやってくれる。", "③", Palette.Purple, Palette.PurpleBg) }
            6 -> {
                Text("試しに1つ、\n置いてみよう", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Ink, lineHeight = 36.sp)
                Box(Modifier.fillMaxWidth().glass(20, Palette.GlassCard).padding(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(trialTemplate.title, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(Logic.summary(Macro(0, trialTemplate.macroName, trialTemplate.trigger, trialTemplate.param, trialTemplate.actions, trialTemplate.constraintType)), color = Palette.InkSoft, fontSize = 15.sp, lineHeight = 24.sp)
                    }
                }
                Text("※ この自動化には、端末の許可が要る動作が含まれます。置いたあとの画面で案内します。", color = Palette.InkSoft, fontSize = 13.sp)
                PrimaryButton("これを置く", modifier = Modifier.fillMaxWidth(), onClick = {
                    val t = trialTemplate
                    if (store.load().none { it.name == t.macroName }) {
                        val m = Macro(id = System.currentTimeMillis(), name = t.macroName, trigger = t.trigger, triggerParam = t.param, actions = t.actions, constraintType = t.constraintType, enabled = true)
                        store.save(store.load() + m); Engine.scheduleTimeAlarm(context, m)
                    }
                    hapticFeedback(view, strong = true)
                    trialPlaced = true
                    page = 7
                })
                OutlinedButton(onClick = { onFinish() }, shape = RoundedCornerShape(999.dp), modifier = Modifier.fillMaxWidth()) { Text("あとで試す", fontSize = 15.sp, color = Palette.Pink) }
            }
            else -> {
                Text(if (trialPlaced) "置けたよ。最初の自動化、完成" else "準備できたよ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Pink, lineHeight = 36.sp)
                Text(if (trialPlaced) "「始める」を押すとホームが開いて、「${trialTemplate.macroName}」が待ってる。あとはスマホにまかせよう。" else "ホームの「テンプレから始める」から、いつでも気軽に試せるよ。気が向いたら1つ置いてみてね。", fontSize = 15.sp, color = Palette.InkSoft, lineHeight = 24.sp)
                // 試しの自動化に必要な許可が足りなければ、その場で案内する（UX2-4）
                if (trialPlaced) {
                    val t = trialTemplate
                    val needDnd = t.actions.any { it.type == ActionType.SILENT || it.type == ActionType.DND_ON || it.type == ActionType.DND_OFF }
                    val needWrite = t.actions.any { it.type == ActionType.BRIGHTNESS_DOWN || it.type == ActionType.BRIGHTNESS_UP }
                    val needNotif = t.actions.any { it.type == ActionType.NOTIFICATION }
                    val dndMissing = remember(permTick) { needDnd && !Engine.hasDndAccess(context) }
                    val writeMissing = remember(permTick) { needWrite && !Engine.canWriteSettings(context) }
                    val notifMissing = remember(permTick) { needNotif && !Engine.hasNotificationPermission(context) }
                    if (dndMissing || writeMissing || notifMissing) {
                        Box(Modifier.fillMaxWidth().glass(20, Palette.SkyBg).padding(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("あと1つだけ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Sky)
                                Text("置いた自動化を動かすには、この許可が要ります。今ここで済ませると、最初の実行が失敗しません。", fontSize = 15.sp, color = Palette.Ink, lineHeight = 24.sp)
                                if (dndMissing) PermRow("マナーモードの許可", "着信音の変更に必要", "開いた一覧で『きっかけ』を選んでオンにしてね") {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                                }
                                if (writeMissing) PermRow("明るさ変更の許可", "画面の明るさ変更に必要", "開いた画面でスイッチをオンにしてね") {
                                    val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { data = Uri.parse("package:" + context.packageName) }
                                    context.startActivity(i)
                                }
                                if (notifMissing) PermRow("通知", "「通知を出す」に必要", "開いた画面で「通知」をオンにしてね") {
                                    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }
                                    context.startActivity(i)
                                }
                                Text("あとからホームの「権限チェック」でもできます。", fontSize = 13.sp, color = Palette.InkSoft)
                            }
                        }
                    }
                }
                BigEmoji("🎉")
            }
        }
        Spacer(Modifier.weight(1f))
        if (page != 1 && page != 6) {
            PrimaryButton(if (page >= 7) "始める" else "次へ", modifier = Modifier.fillMaxWidth().height(54.dp)) { hapticFeedback(view, strong = false); if (page >= 7) onFinish() else page++ }
        }
        Row(Modifier.fillMaxWidth()) {
            if (page > 0) Text("← もどる", color = Palette.InkSoft, fontSize = 13.sp, modifier = Modifier.clickable { page-- }.padding(4.dp))
            Spacer(Modifier.weight(1f))
            if (page != 6 && page != 7) Text("スキップ", color = Palette.InkSoft, fontSize = 13.sp, modifier = Modifier.clickable { onFinish() }.padding(4.dp))
        }
    }
}

@Composable
fun BigEmoji(e: String) {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) { Text(e, fontSize = 84.sp) }
}

@Composable
fun StepPage(kicker: String, title: String, desc: String, num: String, color: Color, bg: Color) {
    Text(kicker, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
    Text(desc, fontSize = 15.sp, color = Palette.InkSoft, lineHeight = 24.sp)
    Box(Modifier.size(110.dp).glass(28, bg), contentAlignment = Alignment.Center) { Text(num, fontSize = 52.sp, color = color, fontWeight = FontWeight.Bold) }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← もどる", color = Palette.Sky, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(8.dp))
        }
        Text("使い方", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Palette.Ink)
        Text("自動化は3ステップで作れます。ホームの「＋ つくる」から始められます。", fontSize = 15.sp, color = Palette.InkSoft, lineHeight = 24.sp)
        StepPage("ステップ 1 / 3", "きっかけを選ぶ", "「22時になったら」「電池が減ったら」みたいな、始まりの合図。", "①", Palette.Pink, Palette.PinkBg)
        StepPage("ステップ 2 / 3", "やることを選ぶ", "静かにする、通知する…。組み合わせもOK。", "②", Palette.Orange, Palette.OrangeBg)
        StepPage("ステップ 3 / 3", "保存するだけ", "あとはスマホがやってくれる。", "③", Palette.Purple, Palette.PurpleBg)
        Box(Modifier.fillMaxWidth().glass(20, Palette.GlassCard).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("困ったときは", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Ink)
                Text("・動かない時は、ホーム下の「権限チェック」を見てね。足りない許可があると動きません。", color = Palette.Ink, fontSize = 15.sp, lineHeight = 24.sp)
                Text("・電池・Wi-Fi・アプリ起動のきっかけは、約15分ごとにチェックするので少し遅れて動きます。", color = Palette.Ink, fontSize = 15.sp, lineHeight = 24.sp)
                Text("・作った自動化は「▶ テスト実行」ですぐに試せます。結果は「最近の実行」にも残ります。", color = Palette.Ink, fontSize = 15.sp, lineHeight = 24.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun PermissionSection(tick: Int, macros: List<Macro>) {
    val context = LocalContext.current
    val keys = remember(tick, macros) { missingPermKeys(context, macros) }
    if (keys.isEmpty()) return
    Box(Modifier.fillMaxWidth().glass(20, Palette.SkyBg).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("権限チェック", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Sky)
            Text("あなたの自動化に必要な許可だけを表示しています。足りない許可があると、その動作は動きません。", fontSize = 15.sp, color = Palette.Ink, lineHeight = 24.sp)
            MissingPermRows(macros, tick)
        }
    }
}

// 足りない許可のキー一覧（保存直後の案内と共通で使う）
fun missingPermKeys(context: Context, macros: List<Macro>): Set<String> {
    val keys = mutableSetOf<String>()
    if (macros.any { m -> m.actions.any { it.type == ActionType.NOTIFICATION } } && !Engine.hasNotificationPermission(context)) keys += "notif"
    if (macros.any { m -> m.actions.any { it.type == ActionType.SILENT || it.type == ActionType.SILENT_OFF || it.type == ActionType.DND_ON || it.type == ActionType.DND_OFF } } && !Engine.hasDndAccess(context)) keys += "dnd"
    if (macros.any { m -> m.actions.any { it.type == ActionType.BRIGHTNESS_DOWN || it.type == ActionType.BRIGHTNESS_UP } } && !Engine.canWriteSettings(context)) keys += "write"
    if (macros.any { it.trigger == TriggerType.TIME } && !Engine.canExactAlarm(context)) keys += "exact"
    if (macros.any { it.trigger == TriggerType.APP_LAUNCH } && !Engine.hasUsageAccess(context)) keys += "usage"
    return keys
}

// 足りない許可だけを行で出す（行き先で何をするかの一言つき、B-3）
@Composable
fun MissingPermRows(macros: List<Macro>, tick: Int) {
    val context = LocalContext.current
    val keys = remember(tick, macros) { missingPermKeys(context, macros) }
    if ("notif" in keys) PermRow("通知", "「通知を出す」に必要", "開いた画面で「通知」をオンにしてね") {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }
        context.startActivity(i)
    }
    if ("dnd" in keys) PermRow("マナーモードの許可", "着信音の変更に必要", "開いた一覧で『きっかけ』を選んでオンにしてね") {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }
    if ("write" in keys) PermRow("明るさ変更の許可", "画面の明るさ変更に必要", "開いた画面でスイッチをオンにしてね") {
        val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { data = Uri.parse("package:" + context.packageName) }
        context.startActivity(i)
    }
    if ("exact" in keys) PermRow("正確なアラームの許可", "時刻きっかけを正確に動かすために必要", "開いた画面で許可してね") {
        if (Build.VERSION.SDK_INT >= 31) {
            val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply { data = Uri.parse("package:" + context.packageName) }
            context.startActivity(i)
        }
    }
    if ("usage" in keys) PermRow("使用状況へのアクセス", "「アプリを開いたら」きっかけに必要", "開いた一覧で『きっかけ』を選んで許可してね") {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

// 保存・追加の直後に出す許可案内（B-2）。そろったら完了を伝える
@Composable
fun PermGuideDialog(macro: Macro, tick: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val keys = remember(tick, macro) { missingPermKeys(context, listOf(macro)) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("あと1つだけ", color = Palette.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (keys.isEmpty()) {
                    Text("全部そろったよ。これで「${macro.name}」が動きます。", color = Palette.Ink, fontSize = 15.sp, lineHeight = 24.sp)
                } else {
                    Text("「${macro.name}」を動かすには、この許可が要ります。今ここで済ませると、最初の実行が失敗しません。", color = Palette.Ink, fontSize = 15.sp, lineHeight = 24.sp)
                    MissingPermRows(listOf(macro), tick)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(if (keys.isEmpty()) "閉じる" else "あとで", color = Palette.Pink) } }
    )
}

@Composable
fun PermRow(title: String, desc: String, howto: String = "", onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(desc, color = Palette.InkSoft, fontSize = 13.sp)
            if (howto.isNotBlank()) Text(howto, color = Palette.Sky, fontSize = 13.sp)
        }
        Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Palette.Sky), shape = RoundedCornerShape(999.dp)) { Text("許可する", fontSize = 13.sp, color = Color.White) }
    }
}

@Composable
fun MacroCard(macro: Macro, highlighted: Boolean, onToggle: () -> Unit, onTest: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val apps = rememberApps()
    val triggerChip = when {
        macro.trigger == TriggerType.APP_LAUNCH && macro.triggerParam.isNotBlank() -> "アプリを開いたら（${apps.firstOrNull { it.pkg == macro.triggerParam }?.label ?: macro.triggerParam.substringAfterLast('.')}）"
        macro.triggerParam.isNotBlank() -> "${macro.trigger.label}（${macro.triggerParam}）"
        else -> macro.trigger.label
    }
    Box(Modifier.fillMaxWidth().glass(20, if (highlighted) Palette.PinkBg else Palette.GlassCard).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(macro.name, color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Text(if (macro.enabled) "オン" else "オフ", color = if (macro.enabled) Palette.Pink else Palette.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Switch(checked = macro.enabled, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = Palette.Pink, checkedThumbColor = Color.White, uncheckedThumbColor = Color.White, uncheckedTrackColor = Palette.GlassCard, uncheckedBorderColor = Palette.Line))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(tintBrush(Palette.PinkBg)).padding(horizontal = 12.dp, vertical = 4.dp)) { Text("${macro.trigger.icon} $triggerChip", color = Palette.Pink, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Text("→", color = Palette.InkSoft)
                macro.actions.forEach { item ->
                    val label = "${item.type.icon} " + (if (item.type == ActionType.LAUNCH_APP && item.param.isNotBlank()) "${item.type.label}（${apps.firstOrNull { it.pkg == item.param }?.label ?: item.param.substringAfterLast('.')}）" else item.type.label)
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(tintBrush(Palette.OrangeBg)).padding(horizontal = 12.dp, vertical = 4.dp)) { Text(label, color = Palette.Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
                if (macro.constraintType != ConstraintType.NONE) {
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(tintBrush(Palette.PurpleBg)).padding(horizontal = 12.dp, vertical = 4.dp)) { Text(macro.constraintType.label, color = Palette.Purple, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
            if (macro.trigger == TriggerType.BATTERY_LOW || macro.trigger == TriggerType.WIFI_CONNECTED || macro.trigger == TriggerType.APP_LAUNCH) Text("約15分ごとにチェックしています（少し遅れて動くことがあります）", color = Palette.InkSoft, fontSize = 13.sp)
            if (macro.lastRunAt > 0) Text("最終実行：${SimpleDateFormat("M月d日 HH:mm", Locale.JAPAN).format(Date(macro.lastRunAt))}", color = Palette.Sky, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTest, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1.5f)) { Text("▶ テスト実行", fontSize = 15.sp, color = Palette.Pink, maxLines = 1) }
                OutlinedButton(onClick = onEdit, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("編集", fontSize = 15.sp, color = Palette.Pink, maxLines = 1) }
                OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("削除", fontSize = 15.sp, color = Palette.Pink, maxLines = 1) }
            }
        }
    }
}

@Composable
fun AppPickerScreen(title: String, selected: String, onSelect: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val apps = rememberApps()
    var query by remember { mutableStateOf("") }
    val shown = if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← もどる", color = Palette.Sky, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }.padding(8.dp))
        }
        Text(title, color = Palette.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("アプリをさがす") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Palette.InputFace, unfocusedContainerColor = Palette.InputFace, focusedBorderColor = Palette.Pink, unfocusedBorderColor = Palette.Line, focusedLabelColor = Palette.InkSoft, unfocusedLabelColor = Palette.InkSoft, focusedTextColor = Palette.Ink, unfocusedTextColor = Palette.Ink))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown, key = { it.pkg }) { app ->
                val sel = selected == app.pkg
                val iconBmp = remember(app.pkg) {
                    try {
                        val d = context.packageManager.getApplicationIcon(app.pkg)
                        val bmp = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        d.setBounds(0, 0, 72, 72); d.draw(canvas)
                        bmp.asImageBitmap()
                    } catch (e: Exception) { null }
                }
                Box(Modifier.fillMaxWidth().pressable { onSelect(app.pkg) }.glass(16, if (sel) Palette.PinkBg else Palette.GlassCard).padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (iconBmp != null) Image(iconBmp, contentDescription = null, modifier = Modifier.size(32.dp))
                        Text(app.label, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Palette.Pink else Palette.Ink, fontSize = 15.sp)
                    }
                }
            }
        }
        if (apps.isEmpty()) Text("アプリ一覧が取得できませんでした", color = Palette.InkSoft, fontSize = 13.sp)
        else if (shown.isEmpty()) Text("「$query」に合うアプリが見つかりませんでした", color = Palette.InkSoft, fontSize = 13.sp)
    }
}

@Composable
fun WizardScreen(step: Int, triggerName: String, param: String, actionNames: Set<String>, launchParam: String, constraintName: String, name: String, isEditing: Boolean, onTrigger: (String) -> Unit, onParam: (String) -> Unit, onToggleAction: (String) -> Unit, onLaunchParam: (String) -> Unit, notifParam: String, speakParam: String, onNotifParam: (String) -> Unit, onSpeakParam: (String) -> Unit, onOpenPicker: (String) -> Unit, onConstraint: (String) -> Unit, onName: (String) -> Unit, onNext: () -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    val apps = rememberApps()
    val saveBlocked = (triggerName == TriggerType.APP_LAUNCH.name && param.isBlank()) || (actionNames.contains(ActionType.LAUNCH_APP.name) && launchParam.isBlank())
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (isEditing) "自動化を編集" else "自動化をつくる", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Palette.InkSoft)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("① きっかけ" to Palette.Pink, "② やること" to Palette.Orange, "③ 仕上げ" to Palette.Purple).forEachIndexed { idx, (label, color) ->
                val active = idx + 1 == step
                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (active) solidBrush(color) else tintBrush(Palette.Line)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(label, color = if (active) Color.White else Palette.InkSoft, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (step) {
                1 -> {
                    Text("いつ動かす？", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Pink)
                    TriggerType.values().forEach { t ->
                        val selected = triggerName == t.name
                        Box(Modifier.fillMaxWidth().pressable { onTrigger(t.name) }.glass(20, if (selected) Palette.PinkBg else Palette.GlassCard).padding(16.dp)) {
                            Column { Text("${t.icon} ${t.label}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (selected) Palette.Pink else Palette.Ink); Text(t.detail, color = Palette.InkSoft, fontSize = 15.sp, lineHeight = 24.sp) }
                        }
                    }
                    if (triggerName == TriggerType.TIME.name) {
                        val parts = param.split(":"); val curH = parts.getOrNull(0)?.toIntOrNull() ?: 22; val curM = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        Text("時刻：${"%02d:%02d".format(curH, curM)}", color = Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..23).forEach { h ->
                                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (curH == h) solidBrush(Palette.Pink) else tintBrush(Palette.Line)).clickable { onParam("%02d:%02d".format(h, curM)) }.padding(horizontal = 12.dp, vertical = 8.dp)) { Text("${h}時", color = if (curH == h) Color.White else Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0..55 step 5).forEach { m ->
                                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (curM == m) solidBrush(Palette.Pink) else tintBrush(Palette.Line)).clickable { onParam("%02d:%02d".format(curH, m)) }.padding(horizontal = 12.dp, vertical = 8.dp)) { Text("${m}分", color = if (curM == m) Color.White else Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            }
                        }
                    }
                    if (triggerName == TriggerType.BATTERY_LOW.name) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("10%", "20%", "30%").forEach { v ->
                                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (param == v) solidBrush(Palette.Pink) else tintBrush(Palette.Line)).clickable { onParam(v) }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text(v, color = if (param == v) Color.White else Palette.Ink, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Text("電池の残りは、アプリを閉じていても約15分ごとにチェックされます。少し遅れて動くことがあります。", color = Palette.InkSoft, fontSize = 13.sp)
                    }
                    if (triggerName == TriggerType.WIFI_CONNECTED.name) {
                        Text("Wi-Fiの接続は、アプリを閉じていても約15分ごとにチェックされます。少し遅れて動くことがあります。", color = Palette.InkSoft, fontSize = 13.sp)
                    }
                    if (triggerName == TriggerType.APP_LAUNCH.name) {
                        val selLabel = apps.firstOrNull { it.pkg == param }?.label
                        Box(Modifier.fillMaxWidth().pressable { onOpenPicker("trigger") }.glass(20, Palette.GlassCard).padding(16.dp)) {
                            Text(if (selLabel != null) "対象のアプリ：$selLabel（タップで変更）" else "アプリを選ぶ（必須）", fontWeight = FontWeight.Bold, color = Palette.Pink, fontSize = 15.sp)
                        }
                        Text("アプリを開いたかどうかは、約15分ごとにチェックされます。少し遅れて動くことがあります。", color = Palette.InkSoft, fontSize = 13.sp)
                    }
                }
                2 -> {
                    Text("何をする？（複数えらべる）", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Orange)
                    ActionGroups.forEach { g ->
                        Text(g, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Palette.InkSoft)
                        if (g == "音") Text("マナーモード＝着信音を消すこと。おやすみモード＝通知をぜんぶ止めること。", color = Palette.InkSoft, fontSize = 13.sp)
                        ActionType.values().filter { it.group == g }.forEach { a ->
                            val selected = actionNames.contains(a.name)
                            Box(Modifier.fillMaxWidth().pressable { onToggleAction(a.name) }.glass(20, if (selected) Palette.OrangeBg else Palette.GlassCard).padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { Text("${a.icon} ${a.label}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (selected) Palette.Orange else Palette.Ink); Text(a.detail, color = Palette.InkSoft, fontSize = 15.sp, lineHeight = 24.sp) }
                                    if (selected) Text("✓", color = Palette.Orange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    if (actionNames.contains(ActionType.NOTIFICATION.name)) {
                        OutlinedTextField(value = notifParam, onValueChange = onNotifParam, label = { Text("通知の文（空なら「実行しました」）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Palette.InputFace, unfocusedContainerColor = Palette.InputFace, focusedBorderColor = Palette.Pink, unfocusedBorderColor = Palette.Line, focusedLabelColor = Palette.InkSoft, unfocusedLabelColor = Palette.InkSoft, focusedTextColor = Palette.Ink, unfocusedTextColor = Palette.Ink))
                    }
                    if (actionNames.contains(ActionType.SPEAK.name)) {
                        OutlinedTextField(value = speakParam, onValueChange = onSpeakParam, label = { Text("読み上げる文（空なら「実行しました」）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Palette.InputFace, unfocusedContainerColor = Palette.InputFace, focusedBorderColor = Palette.Pink, unfocusedBorderColor = Palette.Line, focusedLabelColor = Palette.InkSoft, unfocusedLabelColor = Palette.InkSoft, focusedTextColor = Palette.Ink, unfocusedTextColor = Palette.Ink))
                    }
                    if (actionNames.contains(ActionType.LAUNCH_APP.name)) {
                        val selLabel = apps.firstOrNull { it.pkg == launchParam }?.label
                        Box(Modifier.fillMaxWidth().pressable { onOpenPicker("action") }.glass(20, Palette.GlassCard).padding(16.dp)) {
                            Text(if (selLabel != null) "起動するアプリ：$selLabel（タップで変更）" else "起動するアプリを選ぶ（必須）", fontWeight = FontWeight.Bold, color = Palette.Orange, fontSize = 15.sp)
                        }
                    }
                }
                else -> {
                    Text("名前と条件", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Purple)
                    OutlinedTextField(value = name, onValueChange = onName, label = { Text("名前（空なら自動でつきます）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Palette.InputFace, unfocusedContainerColor = Palette.InputFace, focusedBorderColor = Palette.Pink, unfocusedBorderColor = Palette.Line, focusedLabelColor = Palette.InkSoft, unfocusedLabelColor = Palette.InkSoft, focusedTextColor = Palette.Ink, unfocusedTextColor = Palette.Ink))
                    Text("条件（任意）", color = Palette.Ink, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConstraintType.values().forEach { c ->
                            val selected = constraintName == c.name
                            Box(Modifier.clip(RoundedCornerShape(999.dp)).background(if (selected) solidBrush(Palette.Purple) else tintBrush(Palette.Line)).clickable { onConstraint(c.name) }.padding(horizontal = 16.dp, vertical = 8.dp)) { Text(c.label, color = if (selected) Color.White else Palette.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        }
                    }
                    Text("平日だけ＝月〜金／夜だけ＝21時〜朝6時", color = Palette.InkSoft, fontSize = 13.sp)
                    val t = TriggerType.valueOf(triggerName)
                    val items = ActionType.values().filter { actionNames.contains(it.name) }.map { at -> ActionItem(at, if (at == ActionType.LAUNCH_APP) launchParam else "") }
                    Box(Modifier.fillMaxWidth().glass(20, Palette.GlassCard).padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("できあがり", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Palette.InkSoft)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                val trigLabel = "${t.icon} ${t.label}" + (if (param.isNotBlank() && t != TriggerType.APP_LAUNCH) "（$param）" else "")
                                Box(Modifier.clip(RoundedCornerShape(999.dp)).background(tintBrush(Palette.PinkBg)).padding(horizontal = 12.dp, vertical = 4.dp)) { Text(trigLabel, color = Palette.Pink, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                Text("→", color = Palette.InkSoft)
                                items.forEach { item ->
                                    val actLabel = "${item.type.icon} " + (if (item.type == ActionType.LAUNCH_APP && item.param.isNotBlank()) "${item.type.label}（${apps.firstOrNull { it.pkg == item.param }?.label ?: item.param.substringAfterLast('.')}）" else item.type.label)
                                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(tintBrush(Palette.OrangeBg)).padding(horizontal = 12.dp, vertical = 4.dp)) { Text(actLabel, color = Palette.Orange, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                }
                                if (ConstraintType.valueOf(constraintName) != ConstraintType.NONE) {
                                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(tintBrush(Palette.PurpleBg)).padding(horizontal = 12.dp, vertical = 4.dp)) { Text(ConstraintType.valueOf(constraintName).label, color = Palette.Purple, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                            Text(Logic.summary(Macro(0, "", t, param, items, ConstraintType.valueOf(constraintName))), color = Palette.Ink, fontSize = 15.sp, lineHeight = 24.sp)
                        }
                    }
                    if (saveBlocked) Text("アプリの選択が終わっていないので、まだ保存できません。もどって選んでね。", color = Palette.Orange, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(999.dp), modifier = Modifier.weight(1f)) { Text("もどる", fontSize = 15.sp, color = Palette.Pink) }
            if (step < 3) PrimaryButton("次へ", modifier = Modifier.weight(1f)) { onNext() }
            else PrimaryButton("保存する", modifier = Modifier.weight(1f), enabled = !saveBlocked) { onSave() }
        }
    }
}
