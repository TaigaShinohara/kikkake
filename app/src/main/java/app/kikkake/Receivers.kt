package app.kikkake

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** 充電開始：Manifest登録なのでアプリを閉じていても届く */
class PowerConnectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) Engine.onChargingStarted(context)
    }
}

/** 時刻アラーム */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("macroId", -1L)
        if (id < 0) return
        val macro = MacroStore(context).load().find { it.id == id && it.enabled } ?: return
        Engine.execute(context, macro, showToast = false)
        Engine.scheduleTimeAlarm(context, macro)
    }
}

/** 再起動後：アラーム再登録と定期チェック再開 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Engine.rescheduleAll(context)
            MonitorWorker.enqueue(context)
        }
    }
}

/** 電池・Wi-Fiを定期チェック（最短15分間隔）。アプリを閉じていてもWorkManagerが起こす */
class MonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        // 電池レベルは sticky broadcast から取得
        val batteryIntent = ctx.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (scale > 0) Engine.checkBattery(ctx, level * 100 / scale)
        }
        // Wi-Fi接続状態
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        Engine.checkWifi(ctx, wifi)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val req = PeriodicWorkRequestBuilder<MonitorWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("kikkake-monitor", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
