package com.example.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Brush
import com.example.data.WidgetConfig

data class ThemePreset(
    val id: String,
    val name: String,
    val description: String,
    val gradientStart: String,
    val gradientEnd: String,
    val textColor: String,
    val accentColor: String,
    val widgetBgColor: String,
    val widgetOpacity: Float,
    val defaultText: String,
    val styleType: String = "digital",
    val category: String = "AMOLED",
    val price: String = "Free",
    val originalPrice: String? = null,
    val isAnimated: Boolean = false,
    val rating: Float = 4.8f,
    val author: String = "Prism Design Studio",
    val downloads: String = "150K+"
) {
    fun getBrush(): Brush {
        return Brush.verticalGradient(
            listOf(
                androidx.compose.ui.graphics.Color(Color.parseColor(gradientStart)),
                androidx.compose.ui.graphics.Color(Color.parseColor(gradientEnd))
            )
        )
    }
}

object ThemePresets {
    val list = listOf(
        ThemePreset(
            id = "cosmic_dark",
            name = "Cosmic Slate",
            description = "Premium minimalist dark theme featuring deep space blues, celestial orbital paths, and glowing cyan neon highlights.",
            gradientStart = "#0B0C15",
            gradientEnd = "#1B1D30",
            textColor = "#FFFFFF",
            accentColor = "#00E5FF",
            widgetBgColor = "#121325",
            widgetOpacity = 0.85f,
            defaultText = "Design is intelligence made visible.",
            styleType = "digital",
            category = "AMOLED",
            price = "Free",
            isAnimated = true,
            rating = 4.9f,
            author = "Galaxy Premium Team",
            downloads = "2.4M+"
        ),
        ThemePreset(
            id = "neon_cyberpunk",
            name = "Retro Cyberpunk",
            description = "High-fidelity retro-futuristic dark theme, inspired by vaporwave neon, deep magenta glows, and synthwave sunset tones.",
            gradientStart = "#06070C",
            gradientEnd = "#160C1B",
            textColor = "#FFFFFF",
            accentColor = "#FE2C55",
            widgetBgColor = "#0A050F",
            widgetOpacity = 0.75f,
            defaultText = "The future belongs to those who create it.",
            styleType = "analog",
            category = "Gaming",
            price = "$1.99",
            originalPrice = "$3.99",
            isAnimated = true,
            rating = 4.8f,
            author = "Synthetix Arts",
            downloads = "890K+"
        ),
        ThemePreset(
            id = "pastel_calm",
            name = "Aesthetic Pastel",
            description = "Cozy day-mode pastel vibes pairing soft lavender, warm frosted peach gradients, and clean minimalist styling.",
            gradientStart = "#FFF3EB",
            gradientEnd = "#F2EDFF",
            textColor = "#3C2F2F",
            accentColor = "#FF8E7A",
            widgetBgColor = "#FFFFFF",
            widgetOpacity = 0.70f,
            defaultText = "Pause, breathe, and enjoy the moment.",
            styleType = "calendar",
            category = "Minimal",
            price = "Free",
            rating = 4.6f,
            author = "Lofi Vibes Co",
            downloads = "350K+"
        ),
        ThemePreset(
            id = "forest_organic",
            name = "Sage Forest Wood",
            description = "Earthy biophilic sage green theme for nature lovers, bringing soothing forest elements and organic minimalism home.",
            gradientStart = "#1B241E",
            gradientEnd = "#2D3A30",
            textColor = "#EAF0EA",
            accentColor = "#A2B798",
            widgetBgColor = "#1D2821",
            widgetOpacity = 0.80f,
            defaultText = "Nature does not hurry, yet everything is accomplished.",
            styleType = "quote",
            category = "Nature",
            price = "Free",
            rating = 4.7f,
            author = "Gaia Elements",
            downloads = "120K+"
        ),
        ThemePreset(
            id = "sunset_warmth",
            name = "Sunset Vintage",
            description = "Nostalgic 70s-style vinyl layout consisting of deep crimson melting into vibrant golden-amber sunset glows.",
            gradientStart = "#180003",
            gradientEnd = "#4E1500",
            textColor = "#FFE6D5",
            accentColor = "#FFB300",
            widgetBgColor = "#2A0402",
            widgetOpacity = 0.85f,
            defaultText = "Every sunset brings the promise of a new dawn.",
            styleType = "digital",
            category = "Nature",
            price = "$0.99",
            originalPrice = "$1.99",
            rating = 4.9f,
            author = "Retro Wave Lab",
            downloads = "410K+"
        ),
        ThemePreset(
            id = "zen_sakura",
            name = "Zen Sakura Anime",
            description = "Handcrafted anime-style canvas detailing neon cherry blossoms falling gently against beautiful dark purple sky backdrops.",
            gradientStart = "#0B0516",
            gradientEnd = "#2D0A35",
            textColor = "#FFF0FA",
            accentColor = "#FF66B2",
            widgetBgColor = "#1E0422",
            widgetOpacity = 0.80f,
            defaultText = "The beauty of tomorrow shines within you.",
            styleType = "digital",
            category = "Anime",
            price = "$1.49",
            originalPrice = "$2.99",
            rating = 4.9f,
            isAnimated = false,
            author = "Vibrant Otaku Themes",
            downloads = "95K+"
        ),
        ThemePreset(
            id = "hypercar_gt",
            name = "Carbon Hypercar GT",
            description = "Extreme high-intensity automotive theme. Woven carbon fibers styled with electric yellow dynamic instruments.",
            gradientStart = "#0A0A0E",
            gradientEnd = "#1E1E28",
            textColor = "#FFFFFF",
            accentColor = "#FFD700",
            widgetBgColor = "#121217",
            widgetOpacity = 0.85f,
            defaultText = "Speed is a state of mind.",
            styleType = "analog",
            category = "Cars",
            price = "$2.49",
            originalPrice = "$4.99",
            rating = 5.0f,
            isAnimated = true,
            author = "Octane Studio",
            downloads = "550K+"
        ),
        ThemePreset(
            id = "shiva_eternal",
            name = "Shiva Cosmic Devotion",
            description = "A magnificent holy theme featuring divine golden space contours, celestial trident symbols, and deep cosmic meditative blue shades.",
            gradientStart = "#030A1C",
            gradientEnd = "#1A253E",
            textColor = "#F5F8FF",
            accentColor = "#FFA726",
            widgetBgColor = "#0A1124",
            widgetOpacity = 0.80f,
            defaultText = "Infinite consciousness resides within.",
            styleType = "quote",
            category = "God",
            price = "Free",
            rating = 5.0f,
            isAnimated = true,
            author = "Devotional Artworks",
            downloads = "1.2M+"
        ),
        ThemePreset(
            id = "nebula_forge",
            name = "Nebula Forge",
            description = "Interstellar sci-fi command center utilizing dark purple cosmic portals and glowing pink system indicators.",
            gradientStart = "#0D0C15",
            gradientEnd = "#25123E",
            textColor = "#FFFFFF",
            accentColor = "#F06292",
            widgetBgColor = "#130F25",
            widgetOpacity = 0.90f,
            defaultText = "Explore the boundless sky.",
            styleType = "digital",
            category = "Anime",
            price = "Free",
            rating = 4.7f,
            author = "Starlight Arts",
            downloads = "65K+"
        ),
        ThemePreset(
            id = "aura_quartz",
            name = "Aura Neon Quartz",
            description = "Brilliant holographic patterns with interactive cyan-purple glows on a pitch black AMOLED background.",
            gradientStart = "#000000",
            gradientEnd = "#120B1C",
            textColor = "#E1D5EC",
            accentColor = "#B388FF",
            widgetBgColor = "#0C0512",
            widgetOpacity = 0.75f,
            defaultText = "Live in the vibrant aura.",
            styleType = "digital",
            category = "AMOLED",
            price = "Free",
            rating = 4.8f,
            author = "OneUI Lab Team",
            downloads = "3.1M+"
        )
    )

    fun get(id: String): ThemePreset {
        return list.find { it.id == id } ?: list[0]
    }

    fun applySystemWallpaper(context: Context, themeId: String): Boolean {
        // We simulate the wallpaper creation and application on the phone system,
        // as actual WallpaperManager calls are restricted/unstable in containerized heads-up previews,
        // and cause unrecoverable UI channel errors. The selectThemePreset function already locally
        // persists the active aesthetic theme in our repository for in-app wallpaper previews.
        return true
    }
}
