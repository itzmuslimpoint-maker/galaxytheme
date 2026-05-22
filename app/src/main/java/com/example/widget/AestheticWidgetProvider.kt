package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.ThemeDatabase
import com.example.data.WidgetConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AestheticWidgetProvider : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Build or update each active widget on the homescreen
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Check for theme updates or manual triggers
        if (intent.action == ACTION_THEME_UPDATED) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AestheticWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up DB on deletion
        coroutineScope.launch {
            val db = ThemeDatabase.getDatabase(context)
            for (id in appWidgetIds) {
                db.themeDao().deleteWidgetConfig(id)
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        coroutineScope.launch {
            val db = ThemeDatabase.getDatabase(context)
            val config = db.themeDao().getWidgetConfig(appWidgetId)

            // Render on background
            val renderedBitmap = WidgetDrawer.drawAestheticWidget(context, config)

            withContext(Dispatchers.Main) {
                val views = RemoteViews(context.packageName, R.layout.aesthetic_widget_layout)
                
                // Draw the dynamic canvas render onto Image View
                views.setImageViewBitmap(R.id.widget_image_view, renderedBitmap)

                // When clicked, open the customization suite inside our APP
                val intent = Intent(context, MainActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    context, 
                    appWidgetId, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_image_view, pendingIntent)

                // Push update
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    companion object {
        const val ACTION_THEME_UPDATED = "com.example.widget.ACTION_THEME_UPDATED"

        // Helper trigger to announce and force sync widget views
        fun triggerWidgetUpdate(context: Context) {
            val intent = Intent(context, AestheticWidgetProvider::class.java).apply {
                action = ACTION_THEME_UPDATED
            }
            context.sendBroadcast(intent)
        }
    }
}
