package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeRepository(private val themeDao: ThemeDao) {

    val allWidgets: Flow<List<WidgetConfig>> = themeDao.getAllWidgetConfigs()
    
    val appSettings: Flow<AppSettings> = themeDao.getAppSettingsFlow().map { 
        it ?: AppSettings() // Fallback to default set of settings if null
    }

    suspend fun getWidgetConfig(widgetId: Int): WidgetConfig? {
        return themeDao.getWidgetConfig(widgetId)
    }

    suspend fun saveWidgetConfig(config: WidgetConfig) {
        themeDao.insertWidgetConfig(config)
    }

    suspend fun deleteWidgetConfig(widgetId: Int) {
        themeDao.deleteWidgetConfig(widgetId)
    }

    suspend fun getAppSettings(): AppSettings {
        return themeDao.getAppSettings() ?: AppSettings()
    }

    suspend fun saveAppSettings(settings: AppSettings) {
        themeDao.saveAppSettings(settings)
    }
}
