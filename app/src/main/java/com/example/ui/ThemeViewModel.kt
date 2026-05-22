package com.example.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.ThemeDatabase
import com.example.data.ThemeRepository
import com.example.data.WidgetConfig
import com.example.ui.theme.ThemePresets
import com.example.widget.AestheticWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(private val repository: ThemeRepository) : ViewModel() {

    // Active App Settings (System wide configuration)
    val appSettings: StateFlow<AppSettings> = repository.appSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    // Current lists of configurations saved to database
    val savedWidgets: StateFlow<List<WidgetConfig>> = repository.allWidgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // active page: 0 = Themes, 1 = Widget Lab, 2 = Homescreen Simulator
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Temporary scratchpad configuration for Custom Widget Builder
    private val _editingWidget = MutableStateFlow(createDefaultWidgetScratchpad())
    val editingWidget: StateFlow<WidgetConfig> = _editingWidget.asStateFlow()

    fun setActiveTab(index: Int) {
        _activeTab.value = index
    }

    private fun createDefaultWidgetScratchpad(): WidgetConfig {
        return WidgetConfig(
            widgetId = 9999, // 9999 denotes our scratchpad editing widget
            styleType = "digital",
            backgroundColor = "#121325",
            textColor = "#FFFFFF",
            accentColor = "#00E5FF",
            opacity = 0.85f,
            customText = "Design is intelligence made visible.",
            fontStyle = "sans",
            showBattery = true,
            showDate = true
        )
    }

    // Themes page actions
    fun selectThemePreset(context: Context, themeId: String) {
        viewModelScope.launch {
            val preset = ThemePresets.get(themeId)
            val currentSettings = repository.getAppSettings()
            repository.saveAppSettings(
                currentSettings.copy(
                    activeThemeId = themeId,
                    activeWallpaperPresetIndex = ThemePresets.list.indexOf(preset)
                )
            )

            // Auto-align the scratchpad widget builder to selected theme presets!
            _editingWidget.value = WidgetConfig(
                widgetId = 9999,
                styleType = preset.styleType,
                backgroundColor = preset.widgetBgColor,
                textColor = preset.textColor,
                accentColor = preset.accentColor,
                opacity = preset.widgetOpacity,
                customText = preset.defaultText,
                fontStyle = "sans",
                showBattery = true,
                showDate = true
            )
        }
    }

    // Apply entire Theme Wallpaper to user's phone
    fun setWallpaperOnPhone(context: Context, themeId: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                ThemePresets.applySystemWallpaper(context.applicationContext, themeId)
            }
            if (success) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    // Widget Lab Scratchpad Actions
    fun updateStyleType(type: String) {
        _editingWidget.value = _editingWidget.value.copy(styleType = type)
    }

    fun updateBgColor(color: String) {
        _editingWidget.value = _editingWidget.value.copy(backgroundColor = color)
    }

    fun updateTextColor(color: String) {
        _editingWidget.value = _editingWidget.value.copy(textColor = color)
    }

    fun updateAccentColor(color: String) {
        _editingWidget.value = _editingWidget.value.copy(accentColor = color)
    }

    fun updateOpacity(value: Float) {
        _editingWidget.value = _editingWidget.value.copy(opacity = value)
    }

    fun updateCustomText(text: String) {
        _editingWidget.value = _editingWidget.value.copy(customText = text)
    }

    fun updateFontStyle(font: String) {
        _editingWidget.value = _editingWidget.value.copy(fontStyle = font)
    }

    fun toggleBattery(show: Boolean) {
        _editingWidget.value = _editingWidget.value.copy(showBattery = show)
    }

    fun toggleDate(show: Boolean) {
        _editingWidget.value = _editingWidget.value.copy(showDate = show)
    }

    // Pin custom designed widget directly to real phone's homescreen
    fun pinWidgetToHomeScreen(context: Context, onPinnableSupported: () -> Unit, onNotSupported: () -> Unit) {
        viewModelScope.launch {
            val widgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AestheticWidgetProvider::class.java)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (widgetManager.isRequestPinAppWidgetSupported) {
                    onPinnableSupported()
                    
                    // Generate a unique ID for the saving configuration
                    // We save under 1001, 1002, or map it using a counter to avoid conflicts
                    val randomId = (1000..9999).random()
                    val finalConfig = _editingWidget.value.copy(widgetId = randomId)
                    repository.saveWidgetConfig(finalConfig)

                    // Pin Widget
                    val pinIntent = widgetManager.requestPinAppWidget(component, null, null)
                    
                    // Flush update to widgets
                    AestheticWidgetProvider.triggerWidgetUpdate(context)
                } else {
                    onNotSupported()
                }
            } else {
                onNotSupported()
            }
        }
    }

    // Manual Save Config to local Room DB list
    fun saveWidgetToStorage(context: Context) {
        viewModelScope.launch {
            val randomId = (1000..100000).random()
            repository.saveWidgetConfig(_editingWidget.value.copy(widgetId = randomId))
            AestheticWidgetProvider.triggerWidgetUpdate(context)
        }
    }

    fun deleteSavedWidget(widgetId: Int) {
        viewModelScope.launch {
            repository.deleteWidgetConfig(widgetId)
        }
    }
}

// ViewModel Factory
class ThemeViewModelFactory(private val repository: ThemeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThemeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
