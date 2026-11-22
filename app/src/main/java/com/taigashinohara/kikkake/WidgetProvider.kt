package com.taigashinohara.kikkake

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

// ホーム画面ウィジェット：「ウィジェットに表示」アクションで文字を差し替えられる
class KikkakeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
    }

    companion object {
        fun buildViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_kikkake)
            val text = StateStore(context).widgetText.ifBlank { "「ウィジェットに表示」で\nここに文字を出せます" }
            views.setTextViewText(R.id.widget_text, text)
            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            return views
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, KikkakeWidgetProvider::class.java))
            ids.forEach { manager.updateAppWidget(it, buildViews(context)) }
        }
    }
}
