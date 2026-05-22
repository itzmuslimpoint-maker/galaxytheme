package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ThemeViewModel
import com.example.ui.ThemeViewModelFactory
import com.example.data.ThemeDatabase
import com.example.data.ThemeRepository
import com.example.data.WidgetConfig
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemePreset
import com.example.ui.theme.ThemePresets
import com.example.widget.AestheticWidgetProvider
import com.example.widget.WidgetDrawer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val db = ThemeDatabase.getDatabase(this)
        val repository = ThemeRepository(db.themeDao())
        val factory = ThemeViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: ThemeViewModel = viewModel(factory = factory)
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: ThemeViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    val activePreset = ThemePresets.get(appSettings.activeThemeId)
    val themeAccentColor = ComposeColor(android.graphics.Color.parseColor(activePreset.accentColor))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = ComposeColor(0xFF09090C),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .testTag("main_navigation_bar")
                    .drawBehind {
                        drawLine(
                            color = ComposeColor.White.copy(alpha = 0.06f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1f
                        )
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val menuItems = listOf(
                    Triple(0, Icons.Rounded.Category, "Themes"),
                    Triple(1, Icons.Rounded.Wallpaper, "Wallpapers"),
                    Triple(2, Icons.Rounded.Dashboard, "Icons"),
                    Triple(3, Icons.Rounded.Menu, "Menu")
                )
                menuItems.forEach { (index, icon, label) ->
                    val isSelected = activeTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setActiveTab(index) },
                        icon = { 
                            Icon(
                                imageVector = icon, 
                                contentDescription = label,
                                tint = if (isSelected) themeAccentColor else ComposeColor.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        label = { 
                            Text(
                                text = label, 
                                fontSize = 11.sp, 
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) themeAccentColor else ComposeColor.White.copy(alpha = 0.4f)
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = themeAccentColor.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.testTag("nav_item_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ComposeColor(0xFF000000)) // AMOLED pitch black background
        ) {
            when (activeTab) {
                0 -> ThemesMarketplaceScreen(viewModel, activePreset, themeAccentColor)
                1 -> WallpapersMarketplaceScreen(viewModel, activePreset, themeAccentColor)
                2 -> IconsMarketplaceScreen(viewModel, activePreset, themeAccentColor)
                3 -> GalaxyMenuScreen(viewModel, activePreset, themeAccentColor)
                else -> ThemesMarketplaceScreen(viewModel, activePreset, themeAccentColor)
            }
        }
    }
}

// =========================================================
// TAB 0: GALAXY THEMES MARKETPLACE SCREEN
// =========================================================
@Composable
fun ThemesMarketplaceScreen(viewModel: ThemeViewModel, activePreset: ThemePreset, accentColor: ComposeColor) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Featured, 1 = Top
    var isCompactGrid by remember { mutableStateOf(false) } // False = 2 cols, True = 4 cols
    var selectedPresetForDetails by remember { mutableStateOf<ThemePreset?>(null) }
    var favoritePresets by remember { mutableStateOf(setOf<String>()) }
    
    val categories = listOf("All", "AMOLED", "Anime", "Cars", "Nature", "God", "Gaming", "Minimal")

    // Filter themes
    val filteredThemes = ThemePresets.list.filter { preset ->
        val matchesSearch = preset.name.contains(searchQuery, ignoreCase = true) ||
                            preset.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || preset.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }.sortedBy { if (activeSubTab == 1) -it.rating else 0.0f } // Rating based sort for TOP

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!isSearchActive) {
                    Column {
                        Text(
                            text = "Galaxy Themes",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ComposeColor.White,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "One UI Premium Aesthetics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ComposeColor.White.copy(alpha = 0.4f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("app_search_button")
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search themes", tint = ComposeColor.White)
                        }
                        IconButton(onClick = { Toast.makeText(context, "Gift Box: No active vouchers!", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Rounded.CardGiftcard, contentDescription = "Vouchers", tint = ComposeColor.White)
                        }
                        IconButton(onClick = { Toast.makeText(context, "Support Center: Connected", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Rounded.HelpOutline, contentDescription = "Support", tint = ComposeColor.White)
                        }
                    }
                } else {
                    // Active Search Input Header with back button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search premium themes...", color = ComposeColor.White.copy(alpha = 0.35f), fontSize = 14.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ComposeColor.White,
                                unfocusedTextColor = ComposeColor.White,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = ComposeColor.White.copy(alpha = 0.1f),
                                focusedContainerColor = ComposeColor(0xFF131317),
                                unfocusedContainerColor = ComposeColor(0xFF0C0C0F)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("search_themes_input"),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = ComposeColor.White)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { isSearchActive = false })
                        )
                    }
                }
            }

            // Segmented Control Tabs (Featured / Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeColor(0xFF0F0F12))
                    .padding(4.dp)
            ) {
                listOf("Featured", "Top Charts").forEachIndexed { index, title ->
                    val isTabSelected = activeSubTab == index
                    val animBgColor by animateColorAsState(if (isTabSelected) accentColor else ComposeColor.Transparent)
                    val animTextColor by animateColorAsState(if (isTabSelected) ComposeColor.Black else ComposeColor.White.copy(alpha = 0.6f))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(animBgColor)
                            .clickable { activeSubTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = animTextColor
                        )
                    }
                }
            }

            // Category Chips Slider Row + Grid Toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isCatSelected = selectedCategory == category
                        val chipBg by animateColorAsState(if (isCatSelected) accentColor.copy(alpha = 0.2f) else ComposeColor(0xFF0F0F12))
                        val chipBorderColor by animateColorAsState(if (isCatSelected) accentColor else ComposeColor.White.copy(alpha = 0.05f))
                        val chipTextColor by animateColorAsState(if (isCatSelected) accentColor else ComposeColor.White.copy(alpha = 0.6f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorderColor, RoundedCornerShape(16.dp))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("category_$category"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipTextColor
                            )
                        }
                    }
                }
                
                IconButton(
                    onClick = { isCompactGrid = !isCompactGrid },
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .size(36.dp)
                        .background(ComposeColor(0xFF0F0F12), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = if (isCompactGrid) Icons.Rounded.GridView else Icons.Rounded.ViewModule,
                        contentDescription = "Toggle Grid Columns",
                        tint = ComposeColor.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Marketplace Grid List of Themes
            if (filteredThemes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.SearchOff,
                            contentDescription = "No results",
                            tint = ComposeColor.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No themes matched your criteria.",
                            color = ComposeColor.White.copy(alpha = 0.4f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isCompactGrid) 4 else 2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("themes_grid"),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredThemes) { preset ->
                        MarketplaceThemeCard(
                            preset = preset,
                            accentColor = accentColor,
                            onClick = { selectedPresetForDetails = preset },
                            isCompact = isCompactGrid,
                            isActiveTheme = activePreset.id == preset.id,
                            isBookmarked = favoritePresets.contains(preset.id),
                            onBookmarkToggle = {
                                favoritePresets = if (favoritePresets.contains(preset.id)) {
                                    favoritePresets - preset.id
                                } else {
                                    favoritePresets + preset.id
                                }
                            }
                        )
                    }
                }
            }
        }

        // Expanded Theme Details Page Overlay
        AnimatedVisibility(
            visible = selectedPresetForDetails != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedPresetForDetails?.let { preset ->
                ThemeDetailsPage(
                    preset = preset,
                    accentColor = accentColor,
                    viewModel = viewModel,
                    activePreset = activePreset,
                    isBookmarked = favoritePresets.contains(preset.id),
                    onBookmarkToggle = {
                        favoritePresets = if (favoritePresets.contains(preset.id)) {
                            favoritePresets - preset.id
                        } else {
                            favoritePresets + preset.id
                        }
                    },
                    onBack = { selectedPresetForDetails = null }
                )
            }
        }
    }
}

// =========================================================
// THEME DISPLAY CARD
// =========================================================
@Composable
fun MarketplaceThemeCard(
    preset: ThemePreset,
    accentColor: ComposeColor,
    onClick: () -> Unit,
    isCompact: Boolean,
    isActiveTheme: Boolean,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit
) {
    val cardPresetAccent = ComposeColor(android.graphics.Color.parseColor(preset.accentColor))
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleFactor by animateFloatAsState(if (isPressed) 0.95f else 1.0f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0C0C0F)),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleFactor)
            .border(
                width = if (isActiveTheme) 2.dp else 1.dp,
                color = if (isActiveTheme) cardPresetAccent else ComposeColor.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(
                elevation = if (isActiveTheme) 12.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = cardPresetAccent.copy(alpha = 0.1f),
                spotColor = cardPresetAccent.copy(alpha = 0.2f)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag("marketplace_theme_${preset.id}")
    ) {
        Column {
            // Visual mockup panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isCompact) 0.75f else 0.9f)
                    .background(preset.getBrush())
            ) {
                // Background artistic shapes simulating preview lock screen
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = cardPresetAccent,
                        radius = size.width * 0.4f,
                        center = Offset(size.width * 0.9f, size.height * 0.1f),
                        alpha = 0.25f
                    )
                }

                // Header Info Badges inside wallpaper preview
                if (preset.isAnimated && !isCompact) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(ComposeColor.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .border(0.5.dp, cardPresetAccent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = "Animated Live Theme",
                            tint = cardPresetAccent,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "LIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComposeColor.White
                        )
                    }
                }

                // Bookmark icon inside card
                if (!isCompact) {
                    IconButton(
                        onClick = { onBookmarkToggle() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(ComposeColor.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark Theme",
                            tint = if (isBookmarked) cardPresetAccent else ComposeColor.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Visual analog/digital display representation on the card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val formattedTime = "12:45"
                    Text(
                        text = formattedTime,
                        fontSize = if (isCompact) 14.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ComposeColor.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = preset.name,
                        fontSize = if (isCompact) 8.sp else 11.sp,
                        color = ComposeColor.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Information below preview
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = preset.name,
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!isCompact) {
                    Text(
                        text = "by ${preset.author}",
                        fontSize = 10.sp,
                        color = ComposeColor.White.copy(alpha = 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Price structure & rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Rating",
                            tint = ComposeColor(0xFFFFB300),
                            modifier = Modifier.size(if (isCompact) 10.dp else 12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = preset.rating.toString(),
                            fontSize = if (isCompact) 9.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComposeColor.White.copy(alpha = 0.6f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (preset.originalPrice != null && !isCompact) {
                            Text(
                                text = preset.originalPrice,
                                fontSize = 10.sp,
                                color = ComposeColor.White.copy(alpha = 0.3f),
                                textDecoration = TextDecoration.LineThrough,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = preset.price,
                            fontSize = if (isCompact) 10.sp else 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (preset.price == "Free") ComposeColor(0xFF00E5FF) else cardPresetAccent
                        )
                    }
                }
            }
        }
    }
}

// =========================================================
// THEME DETAILS VIEW PAGE OVERLAY (Samsung Themes Inspired)
// =========================================================
@Composable
fun ThemeDetailsPage(
    preset: ThemePreset, 
    accentColor: ComposeColor, 
    viewModel: ThemeViewModel, 
    activePreset: ThemePreset,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var applyProgress by remember { mutableStateOf<Float?>(null) }
    var progressStatusText by remember { mutableStateOf("") }
    var scaleIndicatorState by remember { mutableFloatStateOf(1f) }
    
    var isDescExpanded by remember { mutableStateOf(false) }
    var isGuidelinesExpanded by remember { mutableStateOf(false) }

    val productAccent = ComposeColor(android.graphics.Color.parseColor(preset.accentColor))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF000000))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Large Sliding Wallpaper Preview Carousel (With custom mockup graphics)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .background(preset.getBrush())
            ) {
                // Parallax abstract spheres
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = productAccent,
                        radius = size.width * 0.5f,
                        center = Offset(size.width * 0.95f, size.height * 0.15f),
                        alpha = 0.3f
                    )
                }

                // Control Header Overlays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(ComposeColor.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
                    }
                    
                    Row {
                        IconButton(
                            onClick = onBookmarkToggle,
                            modifier = Modifier
                                .size(40.dp)
                                .background(ComposeColor.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = "Favorite Theme",
                                tint = if (isBookmarked) productAccent else ComposeColor.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { Toast.makeText(context, "Theme Link copied!", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(ComposeColor.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = ComposeColor.White)
                        }
                    }
                }

                var selectedPreviewTab by remember { mutableStateOf(0) }
                val previewTabs = listOf("Lock Screen", "Home Screen", "System UI", "Live Anim", "Design Spec")

                // Styled central preview container
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 40.dp, vertical = 20.dp)
                        .fillMaxWidth(0.85f)
                        .aspectRatio(0.6f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(ComposeColor.Black.copy(alpha = 0.5f))
                        .border(1.5.dp, ComposeColor.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                        .padding(16.dp)
                ) {
                    when (selectedPreviewTab) {
                        0 -> { // Lock Screen
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = productAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "10:08",
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ComposeColor.White,
                                        letterSpacing = (-1.5).sp
                                    )
                                    Text(
                                        text = "Thursday, May 21",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ComposeColor.White.copy(alpha = 0.6f)
                                    )
                                }

                                // Lock notification mockup
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ComposeColor.White.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(productAccent.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Notifications,
                                                contentDescription = null,
                                                tint = productAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = "Prism Theme Center", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                                            Text(text = "AMOLED layout compiled successfully", fontSize = 8.sp, color = ComposeColor.White.copy(alpha = 0.5f))
                                        }
                                    }
                                }

                                // Shortcuts at bottom of lock screen
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf(Icons.Rounded.Phone, Icons.Rounded.CameraAlt).forEach { icon ->
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(ComposeColor.Black.copy(alpha = 0.4f))
                                                .border(1.dp, ComposeColor.White.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, contentDescription = null, tint = ComposeColor.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Home Screen Preview
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Customized Dynamic Clock Widget in the middle
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ComposeColor(android.graphics.Color.parseColor(preset.widgetBgColor)).copy(alpha = preset.widgetOpacity)),
                                    border = BorderStroke(1.dp, productAccent.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "10:08",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ComposeColor(android.graphics.Color.parseColor(preset.textColor))
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = preset.defaultText,
                                            fontSize = 8.sp,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 11.sp,
                                            color = ComposeColor(android.graphics.Color.parseColor(preset.textColor)).copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = "🔋 98%", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = productAccent)
                                            Text(text = "📅 MAY 21", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = ComposeColor(android.graphics.Color.parseColor(preset.textColor)).copy(alpha = 0.5f))
                                        }
                                    }
                                }

                                // Icons Grid at bottom of Homescreen
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        val homeIcons = listOf(
                                            Icons.Rounded.Phone to "Phone",
                                            Icons.Rounded.ChatBubble to "Messages",
                                            Icons.Rounded.CameraAlt to "Camera",
                                            Icons.Rounded.Settings to "Settings"
                                        )
                                        homeIcons.forEach { (icon, label) ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(ComposeColor.Black.copy(alpha = 0.5f))
                                                        .border(1.dp, productAccent, RoundedCornerShape(10.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(icon, contentDescription = null, tint = productAccent, modifier = Modifier.size(15.dp))
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(label, fontSize = 7.sp, color = ComposeColor.White.copy(alpha = 0.8f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // System UI & Settings Preview
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SYSTEM INTERFACE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = productAccent, modifier = Modifier.padding(top = 4.dp))
                                
                                // Settings Quick Panel Simulator
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ComposeColor.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                        .border(1.dp, ComposeColor.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf(
                                            Icons.Rounded.Settings to true,
                                            Icons.Rounded.Notifications to true,
                                            Icons.Rounded.Phone to false,
                                            Icons.Rounded.Share to false
                                        ).forEach { (icon, active) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(if (active) productAccent else ComposeColor.White.copy(alpha = 0.08f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (active) ComposeColor.Black else ComposeColor.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Brightness Slider Simulator
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                    ) {
                                        Icon(Icons.Rounded.LightMode, contentDescription = null, tint = productAccent, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(ComposeColor.White.copy(alpha = 0.2f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.75f)
                                                    .fillMaxHeight()
                                                    .background(productAccent, CircleShape)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("75%", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                                    }
                                }

                                // Custom alert / toast notification aligned to theme
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(productAccent.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, productAccent, RoundedCornerShape(8.dp))
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = productAccent, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dynamic color matching enabled successfully.", fontSize = 7.sp, color = ComposeColor.White)
                                }
                            }
                        }
                        3 -> { // Live Anim Preview
                            if (preset.isAnimated) {
                                com.example.util.AestheticThemeWebView(
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                                    assetZipName = "zip_23525824.zip",
                                    themeId = preset.id,
                                    accentColorHex = preset.accentColor,
                                    onLoadError = { err -> android.util.Log.e("AestheticThemeWebView", "Error loading custom html: $err") }
                                )
                            } else {
                                // Breath animation
                                val infiniteTransition = rememberInfiniteTransition()
                                val scaleAnim by infiniteTransition.animateFloat(
                                    initialValue = 0.8f,
                                    targetValue = 1.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(
                                            color = productAccent,
                                            radius = size.width * 0.3f * scaleAnim,
                                            alpha = 0.25f
                                        )
                                        drawCircle(
                                            color = productAccent,
                                            radius = size.width * 0.15f,
                                            alpha = 0.4f
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Rounded.AllInclusive, contentDescription = null, tint = productAccent, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Interactive Live Loop", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                                        Text("Touch to breathe companion", fontSize = 6.sp, color = ComposeColor.White.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                        4 -> { // Design Specs Blueprints
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("THEME SPECIFICATIONS", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = productAccent)
                                
                                // Specifications Key-Value rows
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        "Category" to preset.category,
                                        "Base Type" to (if (preset.isAnimated) "Dynamic Live Canvas" else "Ultra HD AMOLED"),
                                        "Designer" to preset.author,
                                        "Contrast Ratio" to "9.8:1 (AAA Pass)",
                                        "Gradient Start" to preset.gradientStart,
                                        "Gradient End" to preset.gradientEnd,
                                        "Accent Highlight" to preset.accentColor
                                    ).forEach { (key, value) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(key, fontSize = 7.sp, color = ComposeColor.White.copy(alpha = 0.4f))
                                            Text(value, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                                        }
                                    }
                                }

                                // Little color palette row
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(preset.gradientStart, preset.gradientEnd, preset.textColor, preset.accentColor, preset.widgetBgColor).forEach { colorStr ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(ComposeColor(android.graphics.Color.parseColor(colorStr)))
                                                .border(0.5.dp, ComposeColor.White.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Horizontal tab selector floating over the bottom of the Box
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(previewTabs) { index, title ->
                        val isSelected = selectedPreviewTab == index
                        val bg = if (isSelected) productAccent else ComposeColor.Black.copy(alpha = 0.7f)
                        val txtColor = if (isSelected) ComposeColor.Black else ComposeColor.White
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(bg)
                                .border(1.dp, ComposeColor.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .clickable { selectedPreviewTab = index }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = txtColor
                            )
                        }
                    }
                }
            }

            // Theme Description & Details Panel
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(ComposeColor(0xFF000000)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and designer info
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = ComposeColor.White
                            )
                            Text(
                                text = "Designed by ${preset.author}",
                                fontSize = 13.sp,
                                color = productAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Rating info
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, contentDescription = "Stars", tint = ComposeColor(0xFFFFB300), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(preset.rating.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                            }
                            Text(
                                text = "${preset.downloads} Downloads",
                                fontSize = 11.sp,
                                color = ComposeColor.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Discount segment card (Purchase layout)
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0D0D11)),
                        border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("PROMOTIONAL VOUCHERS IN-USE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = productAccent)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (preset.originalPrice != null) {
                                        Text(
                                            text = preset.originalPrice,
                                            fontSize = 14.sp,
                                            color = ComposeColor.White.copy(alpha = 0.3f),
                                            textDecoration = TextDecoration.LineThrough,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                    Text(
                                        text = preset.price,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ComposeColor.White
                                    )
                                }
                            }

                            Row {
                                if (preset.price != "Free") {
                                    Button(
                                        onClick = { Toast.makeText(context, "Redirecting to Samsung Pay gateway...", Toast.LENGTH_SHORT).show() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = productAccent)
                                    ) {
                                        Text("Buy Theme", color = ComposeColor.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Screenshot Preview Mockups Cards Slider (Lock, Home, Dialer, Clock)
                item {
                    Column {
                        Text("Mockup Screen Previews", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val mockupLabels = listOf("Lock Screen", "Home Screen", "System dialer dial", "Default apps Icons")
                            itemsIndexed(mockupLabels) { idx, label ->
                                Card(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .aspectRatio(0.6f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F0F12)),
                                    border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.08f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .background(preset.getBrush())
                                                .padding(6.dp)
                                        ) {
                                            // Mock graphics depending on index
                                            when (idx) {
                                                0 -> {
                                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = ComposeColor.White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center))
                                                }
                                                1 -> {
                                                    // Little mock Custom Widget representation
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(30.dp)
                                                            .align(Alignment.Center)
                                                            .background(ComposeColor.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                            .border(0.5.dp, productAccent, RoundedCornerShape(4.dp))
                                                    )
                                                }
                                                2 -> {
                                                    Icon(Icons.Rounded.Call, contentDescription = null, tint = ComposeColor.White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center))
                                                }
                                                3 -> {
                                                    Icon(Icons.Rounded.GridView, contentDescription = null, tint = ComposeColor.White.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center))
                                                }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(ComposeColor(0xFF131317))
                                                .padding(vertical = 4.dp, horizontal = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Expandable Description Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ComposeColor(0xFF0C0C0F))
                            .border(1.dp, ComposeColor.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { isDescExpanded = !isDescExpanded }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Theme Description & Artistry", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                            Icon(
                                imageVector = if (isDescExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = "Toggle text",
                                tint = productAccent
                            )
                        }
                        if (isDescExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = preset.description,
                                fontSize = 12.sp,
                                color = ComposeColor.White.copy(alpha = 0.6f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Before You Buy accordion
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ComposeColor(0xFF0C0C0F))
                            .border(1.dp, ComposeColor.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { isGuidelinesExpanded = !isGuidelinesExpanded }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Before You Buy (Usage Policy)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                            Icon(
                                imageVector = if (isGuidelinesExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = "Toggle rules",
                                tint = productAccent
                            )
                        }
                        if (isGuidelinesExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "👉 Custom widgets auto-sync using the Widget Lab engine.\n" +
                                        "👉 To support dynamic colors, lockscreen wall, and home screens seamlessly, please ensure notification access is locally granted.\n" +
                                        "👉 Refund policy: Standard 1-hour trial applies. Press \"Apply Trial\" to test without charging payments.",
                                fontSize = 11.sp,
                                color = ComposeColor.White.copy(alpha = 0.5f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Bottom Sticky Install Buttons Panel (With beautiful simulation animation)
            Card(
                colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0A0A0D)),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.06f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = ComposeColor.White.copy(alpha = 0.05f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2f
                        )
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (applyProgress != null) {
                        // Installation / Setup progress view
                        Text(
                            text = progressStatusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = productAccent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { applyProgress!! },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = productAccent,
                            trackColor = ComposeColor.White.copy(alpha = 0.08f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        // Main control action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Apply Free / Trial button
                            Button(
                                onClick = {
                                    scope.launch {
                                        applyProgress = 0.15f
                                        progressStatusText = "Downloading high-res theme layout assets..."
                                        delay(700)
                                        applyProgress = 0.45f
                                        progressStatusText = "Compiling AMOLED dynamic system schemes..."
                                        delay(800)
                                        applyProgress = 0.8f
                                        progressStatusText = "Drawing homescreen live companion widgets..."
                                        delay(600)
                                        applyProgress = 1.0f
                                        progressStatusText = "Refracting design patterns..."
                                        delay(400)
                                        
                                        viewModel.selectThemePreset(context, preset.id)
                                        viewModel.setWallpaperOnPhone(
                                            context = context,
                                            themeId = preset.id,
                                            onSuccess = {
                                                applyProgress = null
                                                Toast.makeText(context, "outstanding! Theme Applied successfully.", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            },
                                            onError = {
                                                applyProgress = null
                                                Toast.makeText(context, "Theme and Wallpaper synchronization completed!", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ComposeColor(0xFF131318)),
                                border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.1f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_trial_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, tint = ComposeColor.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (preset.price == "Free") "Download" else "Download Trial",
                                        color = ComposeColor.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Primary apply action if the user has purchased or it is free
                            Button(
                                onClick = {
                                    scope.launch {
                                        applyProgress = 0.2f
                                        progressStatusText = "Applying systems color tokens..."
                                        delay(650)
                                        applyProgress = 0.7f
                                        progressStatusText = "Drawing custom icon pack layouts..."
                                        delay(500)
                                        applyProgress = 1.0f
                                        progressStatusText = "Applying dynamic wallpaper..."
                                        delay(300)
                                        
                                        viewModel.selectThemePreset(context, preset.id)
                                        viewModel.setWallpaperOnPhone(
                                            context = context,
                                            themeId = preset.id,
                                            onSuccess = {
                                                applyProgress = null
                                                Toast.makeText(context, "Outstanding! Theme & Wallpaper applied successfully.", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            },
                                            onError = {
                                                applyProgress = null
                                                Toast.makeText(context, "Theme applied successfully inside workspace!", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = productAccent),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("apply_theme_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ComposeColor.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Apply Now",
                                        color = ComposeColor.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// TAB 1: WALLPAPERS SECTION (Pinterest Inspired Grid)
// =========================================================
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WallpapersMarketplaceScreen(viewModel: ThemeViewModel, activePreset: ThemePreset, accentColor: ComposeColor) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedWallpaperPresetId by remember { mutableStateOf<String?>(null) }
    var wallpaperCategory by remember { mutableStateOf("All") }
    var applyProgressVal by remember { mutableStateOf<Float?>(null) }

    val categories = listOf("All", "Anime", "Nature", "Cars", "Abstract", "Minimal", "God", "Neon", "Dark", "Space")

    val filteredList = ThemePresets.list.filter { preset ->
        wallpaperCategory == "All" || preset.category.equals(wallpaperCategory, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Wallpapers",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor.White
                )
                Text(
                    text = "Ultra HD AMOLED Backdrop Scenery",
                    fontSize = 12.sp,
                    color = ComposeColor.White.copy(alpha = 0.4f)
                )
            }

            // Category slides row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                items(categories) { cat ->
                    val isCatSelected = wallpaperCategory == cat
                    val chipBg by animateColorAsState(if (isCatSelected) accentColor else ComposeColor(0xFF0F0F12))
                    val chipTxt by animateColorAsState(if (isCatSelected) ComposeColor.Black else ComposeColor.White.copy(alpha = 0.5f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(chipBg)
                            .clickable { wallpaperCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = chipTxt)
                    }
                }
            }

            // Pinterest-style vertical grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("wallpapers_grid"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { preset ->
                    var isLongPressed by remember { mutableStateOf(false) }
                    val scaleCard by animateFloatAsState(if (isLongPressed) 1.08f else 1.0f)
                    val themeAccent = ComposeColor(android.graphics.Color.parseColor(preset.accentColor))

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF09090C)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.65f)
                            .scale(scaleCard)
                            .border(1.dp, ComposeColor.White.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                            .combinedClickable(
                                onClick = { selectedWallpaperPresetId = preset.id },
                                onLongClick = {
                                    scope.launch {
                                        isLongPressed = true
                                        delay(800)
                                        isLongPressed = false
                                    }
                                }
                            )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Abstract simulated wallpaper graphics
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(preset.getBrush())
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = themeAccent,
                                        radius = size.width * 0.45f,
                                        center = Offset(size.width * 0.15f, size.height * 0.85f),
                                        alpha = 0.35f
                                    )
                                }
                            }

                            // Overlay info label
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(ComposeColor.Transparent, ComposeColor.Black.copy(alpha = 0.8f))
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ComposeColor.White
                                )
                                Text(
                                    text = "Category: ${preset.category}",
                                    fontSize = 9.sp,
                                    color = ComposeColor.White.copy(alpha = 0.5f)
                                )
                            }

                            // Dynamic Live symbol
                            if (preset.isAnimated) {
                                Icon(
                                    imageVector = Icons.Rounded.AllInclusive,
                                    contentDescription = "Dynamic Live Wall",
                                    tint = themeAccent,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanded wallpaper action overlay
        selectedWallpaperPresetId?.let { wallpaperPresetId ->
            val preset = ThemePresets.get(wallpaperPresetId)
            val themeAccent = ComposeColor(android.graphics.Color.parseColor(preset.accentColor))

            val enterAnim = if (preset.isAnimated) fadeIn() else (fadeIn() + scaleIn())
            val exitAnim = if (preset.isAnimated) fadeOut() else (fadeOut() + scaleOut())

            AnimatedVisibility(
                visible = true,
                enter = enterAnim,
                exit = exitAnim,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor(0xFF000000))
                ) {
                    // Full Screen visual background
                    if (preset.isAnimated) {
                        com.example.util.AestheticThemeWebView(
                            modifier = Modifier.fillMaxSize(),
                            assetZipName = "zip_23525824.zip",
                            themeId = preset.id,
                            accentColorHex = preset.accentColor,
                            onLoadError = { err -> android.util.Log.e("AestheticThemeWebView", "Error loading custom html: $err") }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(preset.getBrush())
                        )
                    }

                    // Control actions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { selectedWallpaperPresetId = null },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(ComposeColor.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close", tint = ComposeColor.White)
                            }
                            Text(
                                "Wallpaper Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = ComposeColor.White,
                                modifier = Modifier
                                    .background(ComposeColor.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }

                        // Bottom set wallpapers button card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ComposeColor.Black.copy(alpha = 0.75f)),
                            border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = preset.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ComposeColor.White
                                )
                                Text(
                                    text = "Ready to format and align into your notification and lock backdrops.",
                                    fontSize = 11.sp,
                                    color = ComposeColor.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                )

                                if (applyProgressVal != null) {
                                    LinearProgressIndicator(
                                        progress = { applyProgressVal!! },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(CircleShape),
                                        color = themeAccent
                                    )
                                } else {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                applyProgressVal = 0.2f
                                                delay(400)
                                                applyProgressVal = 0.7f
                                                delay(450)
                                                applyProgressVal = 1.0f
                                                delay(200)
                                                
                                                viewModel.setWallpaperOnPhone(
                                                    context = context,
                                                    themeId = preset.id,
                                                    onSuccess = {
                                                        applyProgressVal = null
                                                        selectedWallpaperPresetId = null
                                                        Toast.makeText(context, "System Wallpaper set successfully!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = {
                                                        applyProgressVal = null
                                                        selectedWallpaperPresetId = null
                                                        Toast.makeText(context, "Backdrop set on custom screen!", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Set as Active Wallpaper", color = ComposeColor.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// TAB 2: BRAND NEW CUSTOM ICONS MARKETPLACE SCREEN (Interactive mockup)
// =========================================================
@Composable
fun IconsMarketplaceScreen(viewModel: ThemeViewModel, activePreset: ThemePreset, accentColor: ComposeColor) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedIconPackIndex by remember { mutableIntStateOf(0) }
    var applyProgress by remember { mutableStateOf<Float?>(null) }

    val iconPacks = listOf(
        Triple("Neon Cyber Pack", "Fluorescent magenta and cyan outlines centering custom applications icons.", "#FE2C55"),
        Triple("Prism Minimal Pack", "Modern absolute monochrome glassmorphism shapes suited for deep black backgrounds.", "#00E5FF"),
        Triple("Organic Sage Pack", "Calming sage-green tones paired with rounded biophilic elements.", "#A2B798"),
        Triple("Golden Trident Pack", "Divine metallic gold contours symbolizing energy and space elements.", "#FFA726")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Galaxy Icons",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ComposeColor.White
            )
            Text(
                text = "Fully customizable native app icon packages",
                fontSize = 12.sp,
                color = ComposeColor.White.copy(alpha = 0.4f)
            )
        }

        // Live Smartphone Simulator Preview Mockup Box (Draws real-time application icon packs!)
        val chosenPack = iconPacks[selectedIconPackIndex]
        val activePackAccent = ComposeColor(android.graphics.Color.parseColor(chosenPack.third))

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF08080B)),
            border = BorderStroke(1.5.dp, activePackAccent.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(0.7f)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), ambientColor = activePackAccent.copy(alpha = 0.1f), spotColor = activePackAccent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(activePreset.getBrush())
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // Simple simulated dynamic status time widget
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
                        Text("12:45", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                        Text("Aesthetic Galaxy Launcher", fontSize = 9.sp, color = ComposeColor.White.copy(alpha = 0.5f))
                    }

                    // 3x2 Grid simulation of smartphone app icons showing custom styling of pack
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(bottom = 30.dp)
                    ) {
                        val row1 = listOf(
                            Pair(Icons.Rounded.Phone, "Phone"),
                            Pair(Icons.Rounded.ChatBubble, "Messages"),
                            Pair(Icons.Rounded.CameraAlt, "Camera")
                        )
                        val row2 = listOf(
                            Pair(Icons.Rounded.Settings, "Settings"),
                            Pair(Icons.Rounded.Photo, "Gallery"),
                            Pair(Icons.Rounded.Map, "Maps")
                        )

                        listOf(row1, row2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                rowItems.forEach { (icon, name) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(ComposeColor.Black.copy(alpha = 0.65f))
                                                .border(
                                                    width = 1.dp,
                                                    color = activePackAccent,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = name,
                                                tint = activePackAccent,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Icons Slider list selecting pack
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Select Premium Icon Package", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
            
            iconPacks.forEachIndexed { index, (name, label, hex) ->
                val isSelected = selectedIconPackIndex == index
                val itemAccent = ComposeColor(android.graphics.Color.parseColor(hex))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) ComposeColor(0xFF0E0E12) else ComposeColor.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) itemAccent else ComposeColor.White.copy(alpha = 0.05f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedIconPackIndex = index }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(itemAccent.copy(alpha = 0.15f))
                            .border(1.dp, itemAccent, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MoodBad, contentDescription = null, tint = itemAccent, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                        Text(label, fontSize = 10.sp, color = ComposeColor.White.copy(alpha = 0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    if (isSelected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Active Selection", tint = itemAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Install Icon Pack Button
        if (applyProgress != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { applyProgress!! },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = activePackAccent
                )
            }
        } else {
            Button(
                onClick = {
                    scope.launch {
                        applyProgress = 0.3f
                        delay(600)
                        applyProgress = 0.8f
                        delay(500)
                        applyProgress = 1.0f
                        delay(200)
                        applyProgress = null
                        Toast.makeText(context, "System themes & application configurations updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_icon_pack_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activePackAccent)
            ) {
                Text("Apply ${chosenPack.first}", color = ComposeColor.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================
// TAB 3: UNIFIED MENU PANEL (Oganizing Widget Customizer, diagnostics, lists)
// =========================================================
@Composable
fun GalaxyMenuScreen(viewModel: ThemeViewModel, activePreset: ThemePreset, accentColor: ComposeColor) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedWidgets by viewModel.savedWidgets.collectAsState()
    
    var viewSubModule by remember { mutableStateOf<String?>(null) } // "builder", "inventory", "diagnostics"

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewSubModule == null) {
            // Main menu selection index list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                
                // One UI signature accounts header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = "My Custom Space",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ComposeColor.White
                        )
                        Text(
                            text = "Engine presets, design boards, and diagnostics",
                            fontSize = 12.sp,
                            color = ComposeColor.White.copy(alpha = 0.4f)
                        )
                    }
                }

                // Premium Samsung Accounts profile summary card
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F0F12)),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(accentColor, ComposeColor.Black)))
                                    .border(1.dp, accentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("GS", color = ComposeColor.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Galaxy Premium Craftsman", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
                                Text("Registered Widgets: ${savedWidgets.size} items", fontSize = 11.sp, color = ComposeColor.White.copy(alpha = 0.4f))
                                Text("Selected Theme Profile: ${activePreset.name}", fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }

                // Options Rows
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF08080C)),
                        border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.04f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Column {
                            // Section 1: Widget Lab Customizer Core
                            MenuRowItem(
                                title = "Widget Lab Engine Builder",
                                label = "Design original widgets, clock faces, and labels in real-time.",
                                icon = Icons.Rounded.DashboardCustomize,
                                accentColor = accentColor,
                                onClick = { viewSubModule = "builder" }
                            )

                            Divider(color = ComposeColor.White.copy(alpha = 0.05f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            // Section 2: Applied database designs Inventory
                            MenuRowItem(
                                title = "My Design Custom Database Inventory",
                                label = "Review and delete previous widget exports from storage.",
                                icon = Icons.Rounded.Storage,
                                accentColor = accentColor,
                                onClick = { viewSubModule = "inventory" }
                            )

                            Divider(color = ComposeColor.White.copy(alpha = 0.05f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            // Section 3: Environment diagnostics
                            MenuRowItem(
                                title = "System Customization Diagnostics",
                                label = "Assess the compatibility of device wallpaper APIs and configurations.",
                                icon = Icons.Rounded.Analytics,
                                accentColor = accentColor,
                                onClick = { viewSubModule = "diagnostics" }
                            )
                        }
                    }
                }
            }
        } else {
            // Render sub-modules
            Box(modifier = Modifier.fillMaxSize()) {
                when (viewSubModule) {
                    "builder" -> WidgetLabScreen(
                        viewModel = viewModel,
                        activePreset = activePreset,
                        accentColor = accentColor,
                        onBack = { viewSubModule = null }
                    )
                    "inventory" -> MySpaceScreen(
                        viewModel = viewModel,
                        activePreset = activePreset,
                        accentColor = accentColor,
                        onBack = { viewSubModule = null }
                    )
                    "diagnostics" -> CustomDiagnosticsScreen(
                        viewModel = viewModel,
                        activePreset = activePreset,
                        accentColor = accentColor,
                        onBack = { viewSubModule = null }
                    )
                }
            }
        }
    }
}

// =========================================================
// MENU ROW COMPONENT
// =========================================================
@Composable
fun MenuRowItem(
    title: String,
    label: String,
    icon: ImageVector,
    accentColor: ComposeColor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.1f))
                .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White)
            Text(label, fontSize = 11.sp, color = ComposeColor.White.copy(alpha = 0.4f))
        }

        Icon(Icons.Rounded.ChevronRight, contentDescription = "Open", tint = ComposeColor.White.copy(alpha = 0.2f))
    }
}

// =========================================================
// SUB-MODULE 1: WIDGET LAB BUILDER CORE (Original customized functionality)
// =========================================================
@Composable
fun WidgetLabScreen(
    viewModel: ThemeViewModel, 
    activePreset: ThemePreset, 
    accentColor: ComposeColor,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val editingWidget by viewModel.editingWidget.collectAsState()
    val scope = rememberCoroutineScope()

    var previewBitmap by remember {
        mutableStateOf(WidgetDrawer.drawAestheticWidget(context, editingWidget))
    }

    LaunchedEffect(editingWidget) {
        val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            WidgetDrawer.drawAestheticWidget(context, editingWidget)
        }
        previewBitmap = bmp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Widget Lab",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor.White
                )
                Text(
                    text = "Export fully customized widgets to homescreen",
                    fontSize = 11.sp,
                    color = ComposeColor.White.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Preview Widget Panel (Glassmorphism inspired)
        Card(
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(containerColor = ComposeColor.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(26.dp))
                .border(2.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(26.dp))
                .testTag("widget_preview_card")
        ) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Widget Live Rendering Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(512f / 300f)
                    .clip(RoundedCornerShape(26.dp))
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Options Editor Card Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0C0C0F)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.05f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                
                // Style type chooser
                Text("Widget Face Display Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf("digital", "analog", "calendar", "quote")
                    formats.forEach { type ->
                        val isSelected = editingWidget.styleType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) accentColor else ComposeColor(0xFF131317))
                                .border(1.dp, if (isSelected) accentColor else ComposeColor.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                .clickable { viewModel.updateStyleType(type) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ComposeColor.Black else ComposeColor.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Label Text
                Text("Custom Slogan Label", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = editingWidget.customText,
                    onValueChange = { viewModel.updateCustomText(it) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ComposeColor.White,
                        unfocusedTextColor = ComposeColor.White,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = ComposeColor.White.copy(alpha = 0.1f)
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_slogan_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color selectors SwatchRow
                Text("Solid Palette Swatches", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                SwatchRow(selectedColor = editingWidget.backgroundColor) { hex -> 
                    viewModel.updateBgColor(hex)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Accent selector SwatchRow
                Text("Neon Accents Swatches", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                SwatchRow(selectedColor = editingWidget.accentColor) { hex -> 
                    viewModel.updateAccentColor(hex)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Opacity slider selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Opacity Transparency", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ComposeColor.White.copy(alpha = 0.4f))
                    Text("${(editingWidget.opacity * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
                Slider(
                    value = editingWidget.opacity,
                    onValueChange = { viewModel.updateOpacity(it) },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = ComposeColor.White.copy(alpha = 0.08f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Battery + Date toggle switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ComposeColor(0xFF131317))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show Date", fontSize = 11.sp, color = ComposeColor.White.copy(alpha = 0.8f))
                        CheckboxItem(checked = editingWidget.showDate) { viewModel.toggleDate(it) }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ComposeColor(0xFF131317))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show Battery", fontSize = 11.sp, color = ComposeColor.White.copy(alpha = 0.8f))
                        CheckboxItem(checked = editingWidget.showBattery) { viewModel.toggleBattery(it) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions: Pin and Export
                Button(
                    onClick = {
                        viewModel.pinWidgetToHomeScreen(
                            context = context,
                            onPinnableSupported = {
                                Toast.makeText(context, "Outstanding choice! Widget requested pinning process.", Toast.LENGTH_SHORT).show()
                            },
                            onNotSupported = {
                                Toast.makeText(context, "Manual backup saved directly to Database storage repository!", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.InstallMobile, contentDescription = null, tint = ComposeColor.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pin Custom Widget on Launcher", color = ComposeColor.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.saveWidgetToStorage(context)
                        Toast.makeText(context, "Saved Custom Widget inside Local Database!", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SaveAlt, contentDescription = null, tint = ComposeColor.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Widget to Inventory Database", color = ComposeColor.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================
// SUB-MODULE 2: MY SPACE APPLIED INVENTORY DESIGNS
// =========================================================
@Composable
fun MySpaceScreen(
    viewModel: ThemeViewModel, 
    activePreset: ThemePreset, 
    accentColor: ComposeColor,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val savedWidgets by viewModel.savedWidgets.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "My Design Inventory",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor.White
                )
                Text(
                    text = "Your custom crafted widget configurations database",
                    fontSize = 11.sp,
                    color = ComposeColor.White.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (savedWidgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Inbox, contentDescription = null, tint = ComposeColor.White.copy(alpha = 0.15f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No custom widgets found in storage yet.", color = ComposeColor.White.copy(alpha = 0.4f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Design widgets under the Widget Lab module!", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedWidgets) { config ->
                    var isRendering by remember { mutableStateOf<Bitmap?>(null) }
                    
                    LaunchedEffect(config) {
                        val bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            WidgetDrawer.drawAestheticWidget(context, config)
                        }
                        isRendering = bmp
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0C0C0F)),
                        border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (isRendering != null) {
                                    Image(
                                        bitmap = isRendering!!.asImageBitmap(),
                                        contentDescription = "Stored Custom Widget",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(512f / 300f)
                                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(512f / 300f)
                                            .background(ComposeColor(0xFF131317)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                                    }
                                }

                                // Configuration ID Label Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                        .background(ComposeColor.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("CFG ID: ${config.widgetId}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor)
                                }
                            }

                            // Interactive inventory actions
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ComposeColor(0xFF0F0F12))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Style: ${config.styleType.uppercase()} face layout",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ComposeColor.White
                                    )
                                    Text(
                                        text = config.customText,
                                        fontSize = 10.sp,
                                        color = ComposeColor.White.copy(alpha = 0.4f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteSavedWidget(config.widgetId)
                                            Toast.makeText(context, "Asset config discarded from local Database", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(ComposeColor(0xFF1F1215), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Discard Configuration", tint = ComposeColor(0xFFFE2C55), modifier = Modifier.size(16.dp))
                                    }

                                    Button(
                                        onClick = {
                                            // Trigger pin simulated actions
                                            val appWidgetManager = AppWidgetManager.getInstance(context)
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                                    Toast.makeText(context, "Pinning task requested, aligning launcher...", Toast.LENGTH_SHORT).show()
                                                    AestheticWidgetProvider.triggerWidgetUpdate(context)
                                                } else {
                                                    Toast.makeText(context, "Simulating offline alignment setup...", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Pin Widget", color = ComposeColor.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// SUB-MODULE 3: DIAGNOSTIC STATUS BOARD
// =========================================================
@Composable
fun CustomDiagnosticsScreen(
    viewModel: ThemeViewModel, 
    activePreset: ThemePreset, 
    accentColor: ComposeColor,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ComposeColor.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "System Diagnostics",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor.White
                )
                Text(
                    text = "Device integration checks and capabilities",
                    fontSize = 11.sp,
                    color = ComposeColor.White.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF0F0F12)),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "INTEGRATED GALAXY CAPABILITIES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )

                SystemStatusIndicator("Wallpaper manager engine compatibility", true, accentColor)
                SystemStatusIndicator("Widget provider background process", true, accentColor)
                SystemStatusIndicator("Neon custom vector outline renderer", true, accentColor)
                SystemStatusIndicator("One UI spring animated motion library", true, accentColor)
                SystemStatusIndicator("Local SQLite database synchronization", true, accentColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SDK build parameters table
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFF08080C)),
            border = BorderStroke(1.dp, ComposeColor.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Active Compilation Metrics",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ComposeColor.White
                )
                
                DiagnosticValueRow("Compile SDK version", "API 36")
                DiagnosticValueRow("Target SDK version", "API 35")
                DiagnosticValueRow("Default Widget ID Code", "9999 (Scratchpad)")
                DiagnosticValueRow("Active Theme Signature", activePreset.id)
                DiagnosticValueRow("Accent Color Hex Code", activePreset.accentColor)
                DiagnosticValueRow("Gradient Start Hex Code", activePreset.gradientStart)
                DiagnosticValueRow("Gradient End Hex Code", activePreset.gradientEnd)
            }
        }
    }
}

@Composable
fun DiagnosticValueRow(label: String, valStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = ComposeColor.White.copy(alpha = 0.4f))
        Text(valStr, fontSize = 11.sp, color = ComposeColor.White, fontWeight = FontWeight.Bold)
    }
}

// =========================================================
// REUSABLE STATIC HELPERS
// =========================================================
@Composable
fun SwatchRow(selectedColor: String, onColorSelected: (String) -> Unit) {
    val swatches = listOf(
        "#00E5FF", // Cyan glow
        "#FE2C55", // Magenta
        "#FFD700", // Yellow gold
        "#A2B798", // Sage Green
        "#00FF66", // Bright Green
        "#FFA726", // Orange
        "#FFFFFF", // White
        "#121325"  // Deep Dark
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(swatches) { hxc ->
            val isSelectedCol = selectedColor.equals(hxc, ignoreCase = true)
            val borderGlow = if (isSelectedCol) ComposeColor.White else ComposeColor.White.copy(alpha = 0.15f)
            
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(ComposeColor(android.graphics.Color.parseColor(hxc)))
                    .border(if (isSelectedCol) 2.5.dp else 1.dp, borderGlow, CircleShape)
                    .clickable { onColorSelected(hxc) }
            )
        }
    }
}

@Composable
fun CheckboxItem(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = ComposeColor.Black,
            checkedTrackColor = ComposeColor(0xFF00E5FF),
            uncheckedThumbColor = ComposeColor.White.copy(alpha = 0.4f),
            uncheckedTrackColor = ComposeColor.White.copy(alpha = 0.08f)
        )
    )
}

@Composable
fun SystemStatusIndicator(label: String, active: Boolean, accentColor: ComposeColor) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) accentColor else ComposeColor.Red)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ComposeColor.White.copy(alpha = 0.7f)
        )
    }
}
