package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "widget_configs")
data class WidgetConfig(
    @PrimaryKey val widgetId: Int,
    val styleType: String, // "analog", "digital", "quote", "calendar"
    val backgroundColor: String, // Hex e.g. "#121212"
    val textColor: String, // Hex e.g. "#FFFFFF"
    val accentColor: String, // Hex e.g. "#00E5FF"
    val opacity: Float, // 0f to 1f
    val customText: String, // Custom text or inspiration quotes
    val fontStyle: String, // "sans", "serif", "monospace"
    val showBattery: Boolean = true,
    val showDate: Boolean = true
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val activeThemeId: String = "cosmic_dark",
    val activeWallpaperPresetIndex: Int = 0,
    val customWallpaperGradientStart: String = "#1A1B2F",
    val customWallpaperGradientEnd: String = "#16161D"
)

// --- Room DAOs ---

@Dao
interface ThemeDao {
    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun getWidgetConfig(widgetId: Int): WidgetConfig?

    @Query("SELECT * FROM widget_configs")
    fun getAllWidgetConfigs(): Flow<List<WidgetConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidgetConfig(config: WidgetConfig)

    @Query("DELETE FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun deleteWidgetConfig(widgetId: Int)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getAppSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getAppSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppSettings(settings: AppSettings)
}

// --- Room Database ---

@Database(entities = [WidgetConfig::class, AppSettings::class], version = 1, exportSchema = false)
abstract class ThemeDatabase : RoomDatabase() {
    abstract fun themeDao(): ThemeDao

    companion object {
        @Volatile
        private var INSTANCE: ThemeDatabase? = null

        fun getDatabase(context: Context): ThemeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ThemeDatabase::class.java,
                    "theme_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
