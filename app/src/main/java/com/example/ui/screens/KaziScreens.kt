package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.EventEntity
import com.example.data.model.GitHubConfigEntity
import com.example.data.model.LocalSettingsEntity
import com.example.data.model.StreamEntity
import com.example.ui.components.PremiumPlayer
import com.example.ui.theme.KaziTVTheme
import com.example.ui.viewmodel.KaziViewModel
import com.example.ui.viewmodel.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Navigation Routes
object KaziRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val FAVORITES = "favorites"
    const val WATCH_STREAM_SELECT = "watch_stream_select"
    const val PLAYER = "player"
    const val ADD_EVENT = "add_event"
    const val EDIT_EVENT = "edit_event"
    const val STREAM_MANAGER = "stream_manager"
    const val ADD_STREAM = "add_stream"
    const val EDIT_STREAM = "edit_stream"
    const val GITHUB_SETTINGS = "github_settings"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

@Composable
fun KaziAppMain() {
    val navController = rememberNavController()
    val viewModel: KaziViewModel = viewModel()
    
    val localSettings by viewModel.localSettings.collectAsStateWithLifecycle()
    val isDarkMode = localSettings?.isDarkMode ?: true

    KaziTVTheme(darkTheme = isDarkMode) {
        var showExitDialog by remember { mutableStateOf(false) }

        // Handles exit double checks
        BackHandler {
            showExitDialog = true
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = KaziRoutes.SPLASH
                ) {
                    composable(KaziRoutes.SPLASH) {
                        KaziSplashScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.HOME) {
                        KaziHomeScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.FAVORITES) {
                        KaziFavoritesScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.WATCH_STREAM_SELECT) {
                        KaziWatchStreamScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.PLAYER) {
                        KaziPlayerScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.ADD_EVENT) {
                        KaziAddEventScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.EDIT_EVENT) {
                        KaziEditEventScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.STREAM_MANAGER) {
                        KaziStreamManagerScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.ADD_STREAM) {
                        KaziAddStreamScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.EDIT_STREAM) {
                        KaziEditStreamScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.GITHUB_SETTINGS) {
                        KaziGitHubSettingsScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.SETTINGS) {
                        KaziSettingsScreen(navController, viewModel)
                    }
                    composable(KaziRoutes.ABOUT) {
                        KaziAboutScreen(navController, viewModel)
                    }
                }

                // Global Sync State Notifications Toast/Overlay
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route
                if (currentRoute != KaziRoutes.SPLASH) {
                    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
                    SyncStateNotification(syncState, onDismiss = { viewModel.clearSyncState() })
                }

                // Exit Confirmation Overlay
                if (showExitDialog) {
                    KaziExitDialog(
                        onDismiss = { showExitDialog = false },
                        onConfirm = {
                            (navController.context as? Activity)?.finish()
                        }
                    )
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 1: SPLASH SCREEN
// =========================================================================
@Composable
fun KaziSplashScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsStateWithLifecycle()

    var statusText by remember { mutableStateOf("Initializing KaziTV...") }

    LaunchedEffect(Unit) {
        viewModel.checkInternetConnection()
        delay(1000)

        if (!isInternetAvailable) {
            statusText = "Offline mode active."
            delay(1000)
            navController.navigate(KaziRoutes.HOME) {
                popUpTo(KaziRoutes.SPLASH) { inclusive = true }
            }
            return@LaunchedEffect
        }

        statusText = "Syncing live sports matches from GitHub..."
        viewModel.syncFromGitHub(silent = true)
        delay(1500)

        navController.navigate(KaziRoutes.HOME) {
            popUpTo(KaziRoutes.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .align(Alignment.Center)
        ) {
            // Visual Logo Header
            KaziLogoBadge(
                size = 140.dp,
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "KaziTV",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )

            Text(
                text = "PREMIUM LIVE SPORTS & TV",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = statusText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        // Version text pinned safely to the absolute bottom center
        Text(
            text = "Version 1.0.0 (Stable)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

// =========================================================================
// SCREEN 2: HOME SCREEN
// =========================================================================
@Composable
fun KaziHomeScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val filteredEvents by viewModel.filteredEvents.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()

    val categories = remember(allEvents) {
        val activeCategories = allEvents.map { it.category.trim() }
            .distinct()
            .filter { it.isNotEmpty() }
            .sorted()
        listOf("All") + activeCategories
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Action Bottom Sheet trigger state
    var selectedEventForBottomSheet by remember { mutableStateOf<EventEntity?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                // Header Row with Sleek Logo Badge & Styling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF3D5AFE), Color(0xFF00B0FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "K",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "KaziTV",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navController.navigate(KaziRoutes.FAVORITES) },
                            modifier = Modifier
                                .testTag("favorites_button")
                                .background(Color(0xFF1A1C1E), CircleShape)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                tint = Color(0xFFC4C7C5),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { navController.navigate(KaziRoutes.GITHUB_SETTINGS) },
                            modifier = Modifier
                                .testTag("github_button")
                                .background(Color(0xFF1A1C1E), CircleShape)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "GitHub AutoSync Admin",
                                tint = Color(0xFFC4C7C5),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { navController.navigate(KaziRoutes.SETTINGS) },
                            modifier = Modifier
                                .testTag("settings_button")
                                .background(Color(0xFF1A1C1E), CircleShape)
                                .size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFFC4C7C5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable categories selector tabs - Custom Sleek Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = (selectedCategory == "All" && cat == "All") || selectedCategory == cat
                        SleekCategoryChip(
                            category = if (cat == "All") "All Sports" else cat,
                            isSelected = isSelected,
                            onClick = { viewModel.setSelectedCategory(cat) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // ADMIN FEATURE - ADD MATCH (Styled as a Sleek Gradient button)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF3D5AFE), Color(0xFF00B0FF))
                        )
                    )
                    .clickable { navController.navigate(KaziRoutes.ADD_EVENT) }
                    .testTag("add_event_fab"),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Match Event", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (filteredEvents.isEmpty()) {
                // Empty state page Screen 18 (Styled with sleek typography and theme colors)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = "No events",
                        tint = Color(0xFF00B0FF).copy(alpha = 0.6f),
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Live Events Found",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try pulling to refresh or adjust your filter configuration.",
                        fontSize = 14.sp,
                        color = Color(0xFF9EA1A4),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                viewModel.checkInternetConnection()
                                viewModel.syncFromGitHub()
                                delay(1000)
                                isRefreshing = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Text("Refresh Sync", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Unified single Card containing all match row wraps, styled exactly like HTML
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("match_list_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                filteredEvents.forEachIndexed { index, event ->
                                    HtmlStyleMatchRow(
                                        event = event,
                                        onClick = { selectedEventForBottomSheet = event }
                                    )
                                    if (index < filteredEvents.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Simple pull to refresh helper
            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Selected action Bottom Sheet Screen 3
        if (selectedEventForBottomSheet != null) {
            val event = selectedEventForBottomSheet!!
            KaziEventActionBottomSheet(
                event = event,
                onDismiss = { selectedEventForBottomSheet = null },
                onWatchStream = {
                    viewModel.activeEventForStreams.value = event
                    selectedEventForBottomSheet = null
                    navController.navigate(KaziRoutes.WATCH_STREAM_SELECT)
                },
                onFullEdit = {
                    viewModel.activeEventForEdit.value = event
                    selectedEventForBottomSheet = null
                    navController.navigate(KaziRoutes.EDIT_EVENT)
                },
                onEditStreamsOnly = {
                    viewModel.activeEventForStreams.value = event
                    selectedEventForBottomSheet = null
                    navController.navigate(KaziRoutes.STREAM_MANAGER)
                },
                onDelete = {
                    viewModel.deleteEvent(event)
                    selectedEventForBottomSheet = null
                }
            )
        }
    }
}

@Composable
fun SleekCategoryChip(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFF00B0FF) else Color(0xFF1A1C1E)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else Color(0x0DFFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category,
            color = if (isSelected) Color.White else Color(0xFF9EA1A4),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun FeaturedEventCard(
    event: EventEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val bgImageUrl = when (event.category.lowercase()) {
        "football", "soccer" -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&q=80&w=800"
        "cricket" -> "https://images.unsplash.com/photo-1531415080290-bc9854503f37?auto=format&fit=crop&q=80&w=800"
        "basketball" -> "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&q=80&w=800"
        else -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&q=80&w=800"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image with opacity
            AsyncImage(
                model = bgImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.55f)
            )

            // Sleek Gradient Overlay to black out the bottom text area safely
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Top overlay layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RED LIVE Pill Badge
                if (event.status == "LIVE") {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFE50914), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                } else {
                    Text(
                        text = event.status.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // 4K Ultra HD badge
                Text(
                    text = "4K ULTRA HD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Translucent Round Favorite button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (event.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (event.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom Content info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "${event.category.uppercase()} • ${event.league.uppercase()}",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${event.team1Name} vs ${event.team2Name}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Score indicator
                    if (event.status == "LIVE") {
                        Text(
                            text = "LIVE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE50914),
                            letterSpacing = 1.sp
                        )
                    } else {
                        Text(
                            text = event.time,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: EventEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1C1E)
        ),
        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top tag bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = event.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // League tag
                Text(
                    text = event.league,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // LIVE Badge
                if (event.status == "LIVE") {
                    Box(
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = event.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // Favorite Toggle
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (event.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (event.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scoreboard and Flags center row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team 1
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamFlag(url = event.team1Flag, char = event.team1Name.firstOrNull() ?: '1')
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = event.team1Name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // VS details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "VS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.round,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }

                // Team 2
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    TeamFlag(url = event.team2Flag, char = event.team2Name.firstOrNull() ?: '2')
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = event.team2Name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Match timing info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = "Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = event.date,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Time",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = event.time,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TeamFlag(url: String, char: Char, size: androidx.compose.ui.unit.Dp = 60.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
            AsyncImage(
                model = url,
                contentDescription = "Team Flag",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // It could be an emoji (like 🇦🇷) or a fallback character
            val displayText = url.ifEmpty { char.toString().uppercase() }
            Text(
                text = displayText,
                fontSize = if (url.isNotEmpty()) (size.value * 0.45f).sp else (size.value * 0.45f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =========================================================================
// SCREEN 3: EVENT ACTION BOTTOM SHEET
// =========================================================================
@Composable
fun HtmlActionMenuItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(textColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// =========================================================================
// SCREEN 3: EVENT ACTION BOTTOM SHEET
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaziEventActionBottomSheet(
    event: EventEntity,
    onDismiss: () -> Unit,
    onWatchStream: () -> Unit,
    onFullEdit: () -> Unit,
    onEditStreamsOnly: () -> Unit,
    onDelete: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sheet Handle bar is drawn automatically by ModalBottomSheet

            // Action Match Header like HTML .action-match-header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSystemDark) Color(0xFF23262B) else Color(0xFFF9F9F9),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamFlag(url = event.team1Flag, char = event.team1Name.firstOrNull() ?: '1', size = 26.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${event.team1Name} vs ${event.team2Name}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(10.dp))
                TeamFlag(url = event.team2Flag, char = event.team2Name.firstOrNull() ?: '2', size = 26.dp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Option 1: Watch Stream (btn-stream)
            HtmlActionMenuItem(
                icon = { Text("📺", fontSize = 20.sp) },
                title = "Watch Stream",
                subtitle = "Open stream links",
                backgroundColor = if (isSystemDark) Color(0xFF10253C) else Color(0xFFF0F8FF),
                textColor = Color(0xFF0A84FF),
                onClick = onWatchStream
            )

            // Option 2: Full Edit (btn-edit)
            HtmlActionMenuItem(
                icon = { Text("✏️", fontSize = 20.sp) },
                title = "Full Edit",
                subtitle = "Edit all event details",
                backgroundColor = if (isSystemDark) Color(0xFF2C2F34) else Color(0xFFF5F5F7),
                textColor = if (isSystemDark) Color.White else Color(0xFF111111),
                onClick = onFullEdit
            )

            // Option 3: Edit Streams Only (btn-stream-edit)
            HtmlActionMenuItem(
                icon = { Text("🔗", fontSize = 20.sp) },
                title = "Edit Streams Only",
                subtitle = "Add, edit or remove links",
                backgroundColor = if (isSystemDark) Color(0xFF1C2C22) else Color(0xFFF0FFF4),
                textColor = Color(0xFF30B94D),
                onClick = onEditStreamsOnly
            )

            // Option 4: Delete List (btn-delete)
            HtmlActionMenuItem(
                icon = { Text("🗑️", fontSize = 20.sp) },
                title = "Delete List",
                subtitle = "Remove this event",
                backgroundColor = if (isSystemDark) Color(0xFF33191B) else Color(0xFFFFF0F0),
                textColor = Color(0xFFFF3B30),
                onClick = onDelete
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button like close-btn in HTML
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cancel_action"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSystemDark) Color(0xFF2C2F34) else Color(0xFFF2F2F7),
                    contentColor = Color(0xFFFF3B30)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// =========================================================================
// SCREEN 4: WATCH STREAM SELECT SCREEN (SERVERS LISTING)
// =========================================================================
@Composable
fun KaziWatchStreamScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val activeEvent by viewModel.activeEventForStreams.collectAsStateWithLifecycle()
    
    if (activeEvent == null) {
        LaunchedEffect(Unit) {
            navController.navigate(KaziRoutes.HOME) { popUpTo(KaziRoutes.HOME) { inclusive = true } }
        }
        return
    }

    val event = activeEvent!!
    val streamsFlow = remember(event.id) { viewModel.getStreamsForEventFlow(event.id) }
    val streamsList by streamsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Stream Server", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Match Header info Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamFlag(url = event.team1Flag, char = event.team1Name.firstOrNull() ?: '1')
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${event.team1Name} vs ${event.team2Name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(event.league, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    TeamFlag(url = event.team2Flag, char = event.team2Name.firstOrNull() ?: '2')
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("AVAILABLE SERVERS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(12.dp))

            if (streamsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, contentDescription = "No streams", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Stream Servers Configured", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(streamsList) { stream ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.activePlayingStream.value = stream
                                    navController.navigate(KaziRoutes.PLAYER)
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = "Server icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stream.serverName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Speed: High Speed Server", fontSize = 11.sp, color = Color.Green)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(stream.quality, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 5: VIDEO PLAYER CONTAINER
// =========================================================================
@Composable
fun KaziPlayerScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val activeStream by viewModel.activePlayingStream.collectAsStateWithLifecycle()

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        val window = activity?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    if (activeStream == null) {
        LaunchedEffect(Unit) {
            navController.navigate(KaziRoutes.HOME) { popUpTo(KaziRoutes.HOME) { inclusive = true } }
        }
        return
    }

    val stream = activeStream!!

    PremiumPlayer(
        streamUrl = stream.streamUrl,
        serverName = stream.serverName,
        quality = stream.quality,
        linkType = stream.linkType,
        clearKeyId = stream.clearKeyId,
        clearKey = stream.clearKey,
        onBack = {
            navController.navigateUp()
        }
    )
}

// =========================================================================
// SCREEN 6: ADD MATCH EVENT SCREEN
// =========================================================================
// Helper local data class for mutable stream details
data class StreamInput(
    val serverName: String = "",
    val streamUrl: String = "",
    val quality: String = "FHD",
    val linkType: String = "HLS",
    val clearKeyId: String = "",
    val clearKey: String = ""
)

@Composable
fun KaziAddEventScreen(navController: NavHostController, viewModel: KaziViewModel) {
    var team1Name by remember { mutableStateOf("") }
    var team1Flag by remember { mutableStateOf("") }
    var team2Name by remember { mutableStateOf("") }
    var team2Flag by remember { mutableStateOf("") }
    val status by remember { mutableStateOf("LIVE") }
    var date by remember { mutableStateOf("2026-07-12") }
    var time by remember { mutableStateOf("18:00") }
    var eventCategory by remember { mutableStateOf("Football") }
    var league by remember { mutableStateOf("") }
    var round by remember { mutableStateOf("Regular Season") }

    // Multiple streams list state
    val streamsList = remember {
        mutableStateListOf(
            StreamInput(
                serverName = "Main Stream Server",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                quality = "FHD"
            )
        )
    }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current

    val calendar = java.util.Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            date = formattedDate
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formattedTime = String.format("%02d:%02d", hourOfDay, minute)
            time = formattedTime
        },
        calendar.get(java.util.Calendar.HOUR_OF_DAY),
        calendar.get(java.util.Calendar.MINUTE),
        true
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Match Event", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("MATCH TEAMS DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

            OutlinedTextField(
                value = team1Name,
                onValueChange = { team1Name = it },
                label = { Text("Team 1 Name") },
                modifier = Modifier.fillMaxWidth().testTag("team1_input")
            )

            OutlinedTextField(
                value = team1Flag,
                onValueChange = { team1Flag = it },
                label = { Text("Team 1 Logo URL") },
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { team1Flag = it }
                    }) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = team2Name,
                onValueChange = { team2Name = it },
                label = { Text("Team 2 Name") },
                modifier = Modifier.fillMaxWidth().testTag("team2_input")
            )

            OutlinedTextField(
                value = team2Flag,
                onValueChange = { team2Flag = it },
                label = { Text("Team 2 Logo URL") },
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { team2Flag = it }
                    }) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Text("EVENT METADATA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

            OutlinedTextField(
                value = league,
                onValueChange = { league = it },
                label = { Text("League / Tournament Name") },
                modifier = Modifier.fillMaxWidth().testTag("league_input")
            )

            OutlinedTextField(
                value = round,
                onValueChange = { round = it },
                label = { Text("Round / Stage (e.g., Matchday 1)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Category choice
            var isCategoryExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isCategoryExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Category: $eventCategory")
                }
                DropdownMenu(expanded = isCategoryExpanded, onDismissRequest = { isCategoryExpanded = false }) {
                    listOf("Football", "Cricket", "Basketball", "Tennis", "Esports", "Others").forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                eventCategory = cat
                                isCategoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Date & Time pickers on the same line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { datePickerDialog.show() }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time") },
                        trailingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Select Time")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { timePickerDialog.show() }
                    )
                }
            }

            // Stream Channels section with multiple links support
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STREAM CHANNELS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )

                Button(
                    onClick = {
                        streamsList.add(StreamInput(serverName = "Stream Server ${streamsList.size + 1}", streamUrl = "", quality = "FHD"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Stream", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            streamsList.forEachIndexed { index, streamInput ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stream Link #${index + 1}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (streamsList.size > 1) {
                                IconButton(
                                    onClick = { streamsList.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Stream",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = streamInput.serverName,
                            onValueChange = { newValue ->
                                streamsList[index] = streamInput.copy(serverName = newValue)
                            },
                            label = { Text("Server Label") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = streamInput.streamUrl,
                            onValueChange = { newValue ->
                                streamsList[index] = streamInput.copy(streamUrl = newValue)
                            },
                            label = { Text("Stream URL (M3U8 / MP4)") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    clipboardManager.getText()?.text?.let { pastedText ->
                                        streamsList[index] = streamInput.copy(streamUrl = pastedText)
                                    }
                                }) {
                                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quality selection for this stream
                        var isQualityExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { isQualityExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Quality: ${streamInput.quality}")
                            }
                            DropdownMenu(expanded = isQualityExpanded, onDismissRequest = { isQualityExpanded = false }) {
                                listOf("FHD", "HD", "SD").forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q) },
                                        onClick = {
                                            streamsList[index] = streamInput.copy(quality = q)
                                            isQualityExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Link Type selection for this stream
                        var isTypeExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { isTypeExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Link Type: ${streamInput.linkType}")
                            }
                            DropdownMenu(expanded = isTypeExpanded, onDismissRequest = { isTypeExpanded = false }) {
                                listOf("HLS", "DRM").forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            streamsList[index] = streamInput.copy(linkType = t)
                                            isTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (streamInput.linkType == "DRM") {
                            OutlinedTextField(
                                value = streamInput.clearKeyId,
                                onValueChange = { newValue ->
                                    streamsList[index] = streamInput.copy(clearKeyId = newValue)
                                },
                                label = { Text("ক্লিয়ার কীড (Clear Key ID)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = streamInput.clearKey,
                                onValueChange = { newValue ->
                                    streamsList[index] = streamInput.copy(clearKey = newValue)
                                },
                                label = { Text("ক্লিয়ার কি (Clear Key)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Buttons
            Button(
                onClick = {
                    if (team1Name.isNotEmpty() && team2Name.isNotEmpty()) {
                        val event = EventEntity(
                            team1Name = team1Name,
                            team1Flag = team1Flag,
                            team2Name = team2Name,
                            team2Flag = team2Flag,
                            status = status,
                            date = date,
                            time = time,
                            category = eventCategory,
                            league = league,
                            round = round
                        )
                        val streamEntities = streamsList.map { streamInput ->
                            StreamEntity(
                                eventId = 0, // Assigned later in VM
                                quality = streamInput.quality,
                                serverName = streamInput.serverName,
                                streamUrl = streamInput.streamUrl,
                                linkType = streamInput.linkType,
                                clearKeyId = streamInput.clearKeyId,
                                clearKey = streamInput.clearKey
                            )
                        }
                        viewModel.addEvent(event, streamEntities)
                        navController.navigateUp()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SAVE MATCH EVENT", fontWeight = FontWeight.Black)
            }

            OutlinedButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("CANCEL")
            }
        }
    }
}

// =========================================================================
// SCREEN 7: EDIT MATCH EVENT DETAILS SCREEN
// =========================================================================
@Composable
fun KaziEditEventScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val activeEvent by viewModel.activeEventForEdit.collectAsStateWithLifecycle()

    if (activeEvent == null) {
        LaunchedEffect(Unit) {
            navController.navigate(KaziRoutes.HOME) { popUpTo(KaziRoutes.HOME) { inclusive = true } }
        }
        return
    }

    val event = activeEvent!!
    val scope = rememberCoroutineScope()

    var team1Name by remember { mutableStateOf(event.team1Name) }
    var team1Flag by remember { mutableStateOf(event.team1Flag) }
    var team2Name by remember { mutableStateOf(event.team2Name) }
    var team2Flag by remember { mutableStateOf(event.team2Flag) }
    var status by remember { mutableStateOf(event.status) }
    var date by remember { mutableStateOf(event.date) }
    var time by remember { mutableStateOf(event.time) }
    var eventCategory by remember { mutableStateOf(event.category) }
    var league by remember { mutableStateOf(event.league) }
    var round by remember { mutableStateOf(event.round) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Match Details", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("TEAMS CONFIG", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

            OutlinedTextField(
                value = team1Name,
                onValueChange = { team1Name = it },
                label = { Text("Team 1 Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = team1Flag,
                onValueChange = { team1Flag = it },
                label = { Text("Team 1 Logo URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = team2Name,
                onValueChange = { team2Name = it },
                label = { Text("Team 2 Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = team2Flag,
                onValueChange = { team2Flag = it },
                label = { Text("Team 2 Logo URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("MATCH DATA", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

            OutlinedTextField(
                value = league,
                onValueChange = { league = it },
                label = { Text("League / Competition") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = round,
                onValueChange = { round = it },
                label = { Text("Round / Stage") },
                modifier = Modifier.fillMaxWidth()
            )

            // Status select dropdown
            var isStatusExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isStatusExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Status: $status")
                }
                DropdownMenu(expanded = isStatusExpanded, onDismissRequest = { isStatusExpanded = false }) {
                    listOf("LIVE", "UPCOMING", "FINISHED").forEach { stat ->
                        DropdownMenuItem(
                            text = { Text(stat) },
                            onClick = {
                                status = stat
                                isStatusExpanded = false
                            }
                        )
                    }
                }
            }

            // Category choice dropdown
            var isCategoryExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isCategoryExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Category: $eventCategory")
                }
                DropdownMenu(expanded = isCategoryExpanded, onDismissRequest = { isCategoryExpanded = false }) {
                    listOf("Football", "Cricket", "Basketball", "Tennis", "Esports", "Others").forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                eventCategory = cat
                                isCategoryExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Time (HH:MM)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (team1Name.isNotEmpty() && team2Name.isNotEmpty()) {
                        val updated = event.copy(
                            team1Name = team1Name,
                            team1Flag = team1Flag,
                            team2Name = team2Name,
                            team2Flag = team2Flag,
                            status = status,
                            date = date,
                            time = time,
                            category = eventCategory,
                            league = league,
                            round = round
                        )
                        scope.launch {
                            viewModel.updateEvent(updated, viewModel.getStreamsForEventSync(event.id))
                        }
                        navController.navigateUp()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SAVE EVENT DETAILS", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("CANCEL")
            }
        }
    }
}

// =========================================================================
// SCREEN 8: STREAM MANAGER SCREEN
// =========================================================================
@Composable
fun KaziStreamManagerScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val activeEvent by viewModel.activeEventForStreams.collectAsStateWithLifecycle()

    if (activeEvent == null) {
        LaunchedEffect(Unit) {
            navController.navigate(KaziRoutes.HOME) { popUpTo(KaziRoutes.HOME) { inclusive = true } }
        }
        return
    }

    val event = activeEvent!!
    val streamsFlow = remember(event.id) { viewModel.getStreamsForEventFlow(event.id) }
    val streamsList by streamsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Streams", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(KaziRoutes.ADD_STREAM)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New stream link")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "${event.team1Name} vs ${event.team2Name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Stream Servers manager",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (streamsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Server channels saved. Click '+' to add.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(streamsList) { stream ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stream.serverName, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = stream.streamUrl,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(stream.quality, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    viewModel.activeStreamForEdit.value = stream
                                    navController.navigate(KaziRoutes.EDIT_STREAM)
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Stream")
                                }
                                IconButton(onClick = {
                                    viewModel.deleteStream(stream)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Stream", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 9: ADD STREAM CHANNEL SCREEN
// =========================================================================
@Composable
fun KaziAddStreamScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val activeEvent by viewModel.activeEventForStreams.collectAsStateWithLifecycle()

    if (activeEvent == null) {
        LaunchedEffect(Unit) {
            navController.navigate(KaziRoutes.HOME) { popUpTo(KaziRoutes.HOME) { inclusive = true } }
        }
        return
    }

    val event = activeEvent!!

    var quality by remember { mutableStateOf("FHD") }
    var serverName by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf("") }
    var linkType by remember { mutableStateOf("HLS") }
    var clearKeyId by remember { mutableStateOf("") }
    var clearKey by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Stream", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Configure streaming server specs for matches.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

            OutlinedTextField(
                value = serverName,
                onValueChange = { serverName = it },
                label = { Text("Server Name / Label (e.g., Server FHD 1)") },
                modifier = Modifier.fillMaxWidth().testTag("server_name_input")
            )

            OutlinedTextField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                label = { Text("Stream URL (M3U8, MPD, MP4, RTMP)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Quality Select
            var isQualityExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isQualityExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Quality Badge: $quality")
                }
                DropdownMenu(expanded = isQualityExpanded, onDismissRequest = { isQualityExpanded = false }) {
                    listOf("FHD", "HD", "SD").forEach { q ->
                        DropdownMenuItem(
                            text = { Text(q) },
                            onClick = {
                                quality = q
                                isQualityExpanded = false
                            }
                        )
                    }
                }
            }

            // Link Type Select
            var isTypeExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isTypeExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Link Type: $linkType")
                }
                DropdownMenu(expanded = isTypeExpanded, onDismissRequest = { isTypeExpanded = false }) {
                    listOf("HLS", "DRM").forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = {
                                linkType = t
                                isTypeExpanded = false
                            }
                        )
                    }
                }
            }

            if (linkType == "DRM") {
                OutlinedTextField(
                    value = clearKeyId,
                    onValueChange = { clearKeyId = it },
                    label = { Text("ক্লিয়ার কীড (Clear Key ID)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clearKey,
                    onValueChange = { clearKey = it },
                    label = { Text("ক্লিয়ার কি (Clear Key)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (serverName.isNotEmpty() && streamUrl.isNotEmpty()) {
                        val stream = StreamEntity(
                            eventId = event.id,
                            quality = quality,
                            serverName = serverName,
                            streamUrl = streamUrl,
                            linkType = linkType,
                            clearKeyId = clearKeyId,
                            clearKey = clearKey
                        )
                        viewModel.addStream(stream)
                        navController.navigateUp()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SAVE SERVER STREAM", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("CANCEL")
            }
        }
    }
}

// =========================================================================
// SCREEN 10: EDIT STREAM CHANNEL SCREEN
// =========================================================================
@Composable
fun KaziEditStreamScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val activeStream by viewModel.activeStreamForEdit.collectAsStateWithLifecycle()

    if (activeStream == null) {
        LaunchedEffect(Unit) {
            navController.navigate(KaziRoutes.HOME) { popUpTo(KaziRoutes.HOME) { inclusive = true } }
        }
        return
    }

    val stream = activeStream!!

    var quality by remember { mutableStateOf(stream.quality) }
    var serverName by remember { mutableStateOf(stream.serverName) }
    var streamUrl by remember { mutableStateOf(stream.streamUrl) }
    var linkType by remember { mutableStateOf(stream.linkType) }
    var clearKeyId by remember { mutableStateOf(stream.clearKeyId) }
    var clearKey by remember { mutableStateOf(stream.clearKey) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Stream Server", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = serverName,
                onValueChange = { serverName = it },
                label = { Text("Server Label") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = streamUrl,
                onValueChange = { streamUrl = it },
                label = { Text("Stream URL") },
                modifier = Modifier.fillMaxWidth()
            )

            // Quality Badge select drop down
            var isQualityExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isQualityExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Quality: $quality")
                }
                DropdownMenu(expanded = isQualityExpanded, onDismissRequest = { isQualityExpanded = false }) {
                    listOf("FHD", "HD", "SD").forEach { q ->
                        DropdownMenuItem(
                            text = { Text(q) },
                            onClick = {
                                quality = q
                                isQualityExpanded = false
                            }
                        )
                    }
                }
            }

            // Link Type Select
            var isTypeExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isTypeExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Link Type: $linkType")
                }
                DropdownMenu(expanded = isTypeExpanded, onDismissRequest = { isTypeExpanded = false }) {
                    listOf("HLS", "DRM").forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = {
                                linkType = t
                                isTypeExpanded = false
                            }
                        )
                    }
                }
            }

            if (linkType == "DRM") {
                OutlinedTextField(
                    value = clearKeyId,
                    onValueChange = { clearKeyId = it },
                    label = { Text("ক্লিয়ার কীড (Clear Key ID)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clearKey,
                    onValueChange = { clearKey = it },
                    label = { Text("ক্লিয়ার কি (Clear Key)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (serverName.isNotEmpty() && streamUrl.isNotEmpty()) {
                        val updated = stream.copy(
                            quality = quality,
                            serverName = serverName,
                            streamUrl = streamUrl,
                            linkType = linkType,
                            clearKeyId = clearKeyId,
                            clearKey = clearKey
                        )
                        viewModel.updateStream(updated)
                        navController.navigateUp()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("UPDATE SERVER LINK", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.deleteStreamById(stream.id)
                    navController.navigateUp()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("DELETE STREAM", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// =========================================================================
// SCREEN 11: GITHUB SETTINGS SCREEN
// =========================================================================
@Composable
fun KaziGitHubSettingsScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val currentConfig by viewModel.gitHubConfig.collectAsStateWithLifecycle()

    var username by remember { mutableStateOf("") }
    var repository by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var isSyncEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(currentConfig) {
        currentConfig?.let {
            username = it.username
            repository = it.repository
            token = it.token
            isSyncEnabled = it.isSyncEnabled
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("GitHub Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configure your private/public GitHub repository to upload and pull matches automatically. The app reads and updates 'LiveEvents.json'.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("GitHub Username") },
                modifier = Modifier.fillMaxWidth().testTag("username_input")
            )

            OutlinedTextField(
                value = repository,
                onValueChange = { repository = it },
                label = { Text("Repository Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Personal Access Token (PAT)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Upload Sync", fontWeight = FontWeight.Bold)
                    Text("Auto-upload changes to GitHub whenever events are saved locally.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(
                    checked = isSyncEnabled,
                    onCheckedChange = { isSyncEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Operation Actions
            Button(
                onClick = {
                    val config = GitHubConfigEntity(
                        username = username,
                        repository = repository,
                        token = token,
                        isSyncEnabled = isSyncEnabled
                    )
                    viewModel.saveGitHubConfig(config)
                    viewModel.syncFromGitHub()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.syncFromGitHub()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "Sync from GitHub")
                Spacer(modifier = Modifier.width(8.dp))
                Text("PULL LIVEEVENTS.JSON", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.uploadToGitHub()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload to GitHub")
                Spacer(modifier = Modifier.width(8.dp))
                Text("UPLOAD LOCAL DATABASE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// SCREEN 12: GENERAL SETTINGS SCREEN
// =========================================================================
@Composable
fun KaziSettingsScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val currentSettings by viewModel.localSettings.collectAsStateWithLifecycle()

    var isDarkMode by remember { mutableStateOf(true) }
    var isAutoRefresh by remember { mutableStateOf(true) }
    var isNotificationsEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(currentSettings) {
        currentSettings?.let {
            isDarkMode = it.isDarkMode
            isAutoRefresh = it.isAutoRefresh
            isNotificationsEnabled = it.isNotificationsEnabled
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("THEME OPTIONS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dark Mode", fontWeight = FontWeight.Bold)
                    Text("Turn on eye-safe dark slate interface layouts.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = {
                        isDarkMode = it
                        currentSettings?.let { settings ->
                            viewModel.saveLocalSettings(settings.copy(isDarkMode = it))
                        }
                    }
                )
            }

            Divider()

            Text("REFRESH & UPDATES", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Refresh Sync", fontWeight = FontWeight.Bold)
                    Text("Download database updates on startup automatically.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(
                    checked = isAutoRefresh,
                    onCheckedChange = {
                        isAutoRefresh = it
                        currentSettings?.let { settings ->
                            viewModel.saveLocalSettings(settings.copy(isAutoRefresh = it))
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Match Notifications Alerts", fontWeight = FontWeight.Bold)
                    Text("Receive push notices on start of subscribed matches.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(
                    checked = isNotificationsEnabled,
                    onCheckedChange = {
                        isNotificationsEnabled = it
                        currentSettings?.let { settings ->
                            viewModel.saveLocalSettings(settings.copy(isNotificationsEnabled = it))
                        }
                    }
                )
            }

            Divider()

            Text("DATA BACKUPS & ABOUT", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)

            Button(
                onClick = { viewModel.backupDatabase() },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Backup, contentDescription = "Backup DB")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup Database locally")
            }

            Button(
                onClick = { viewModel.restoreDatabase() },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = "Restore DB")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Backup file")
            }

            Button(
                onClick = { navController.navigate(KaziRoutes.ABOUT) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = "About App")
                Spacer(modifier = Modifier.width(8.dp))
                Text("About KaziTV App")
            }
        }
    }
}

// =========================================================================
// SCREEN 14: FAVORITES SCREEN
// =========================================================================
@Composable
fun KaziFavoritesScreen(navController: NavHostController, viewModel: KaziViewModel) {
    val favoritesList by viewModel.favoriteEvents.collectAsStateWithLifecycle()
    var selectedEventForBottomSheet by remember { mutableStateOf<EventEntity?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subscribed Favorites", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (favoritesList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Empty", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Subscribed Favorites Saved", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Click the heart icon on any sports card on the home screen to access them quickly here.", fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("favorites_list_card"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                favoritesList.forEachIndexed { index, event ->
                                    HtmlStyleMatchRow(
                                        event = event,
                                        onClick = { selectedEventForBottomSheet = event }
                                    )
                                    if (index < favoritesList.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            thickness = 1.dp,
                                            modifier = Modifier.padding(horizontal = 20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedEventForBottomSheet != null) {
            val event = selectedEventForBottomSheet!!
            KaziEventActionBottomSheet(
                event = event,
                onDismiss = { selectedEventForBottomSheet = null },
                onWatchStream = {
                    viewModel.activeEventForStreams.value = event
                    selectedEventForBottomSheet = null
                    navController.navigate(KaziRoutes.WATCH_STREAM_SELECT)
                },
                onFullEdit = {
                    viewModel.activeEventForEdit.value = event
                    selectedEventForBottomSheet = null
                    navController.navigate(KaziRoutes.EDIT_EVENT)
                },
                onEditStreamsOnly = {
                    viewModel.activeEventForStreams.value = event
                    selectedEventForBottomSheet = null
                    navController.navigate(KaziRoutes.STREAM_MANAGER)
                },
                onDelete = {
                    viewModel.deleteEvent(event)
                    selectedEventForBottomSheet = null
                }
            )
        }
    }
}

// =========================================================================
// SCREEN 17: ABOUT SCREEN
// =========================================================================
@Composable
fun KaziAboutScreen(navController: NavHostController, viewModel: KaziViewModel) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("About KaziTV", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KaziLogoBadge(
                size = 100.dp,
                fontSize = 48.sp
            )

            Text("KaziTV Premium Live TV", fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("Version 1.0.0 (Stable Release)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Developer Information", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("KaziTV is developed using modern Jetpack Compose and Kotlin DSL to provide lightning fast video playback with advanced Media3 gesture controllers.")
                    Text("Email: support@kazitv.com", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Website: www.kazitv.com", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Privacy Policy & Terms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Your streaming logs are processed strictly on your local device. GitHub Personal Access Tokens (PAT) and repository synchronization keys are encrypted locally inside SQLite database records.")
                }
            }
        }
    }
}

// =========================================================================
// SCREEN 20: EXIT DOUBLE CHECK DIALOG
// =========================================================================
@Composable
fun KaziExitDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exit KaziTV", fontWeight = FontWeight.Black) },
        text = { Text("Are you sure you want to exit KaziTV?", fontSize = 15.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.testTag("exit_confirm_button")
            ) {
                Text("Yes, Exit", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("exit_cancel_button")
            ) {
                Text("No, Stay")
            }
        }
    )
}

// =========================================================================
// GLOBAL HELPERS
// =========================================================================
@Composable
fun SyncStateNotification(
    syncState: SyncState,
    onDismiss: () -> Unit
) {
    LaunchedEffect(syncState) {
        if (syncState is SyncState.Success || syncState is SyncState.Error) {
            delay(3000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = syncState !is SyncState.Idle,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier.fillMaxWidth()
    ) {
        val containerColor = when (syncState) {
            is SyncState.Loading -> MaterialTheme.colorScheme.primary
            is SyncState.Success -> Color.Green
            is SyncState.Error -> Color.Red
            else -> Color.Gray
        }

        val contentColor = when (syncState) {
            is SyncState.Loading -> MaterialTheme.colorScheme.onPrimary
            else -> Color.White
        }

        val messageText = when (syncState) {
            is SyncState.Loading -> syncState.message
            is SyncState.Success -> syncState.message
            is SyncState.Error -> syncState.errorMessage
            else -> ""
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .statusBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (syncState is SyncState.Loading) {
                    CircularProgressIndicator(
                        color = contentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = messageText,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun KaziLogoBadge(
    size: androidx.compose.ui.unit.Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.233f))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3D5AFE), Color(0xFF00B0FF))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "K",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = fontSize
        )
    }
}

// =========================================================================
// HTML MATCH ROW STYLING COMPOSABLES & HELPERS
// =========================================================================
fun getSportColor(category: String): Color {
    return when (category.lowercase()) {
        "football", "soccer" -> Color(0xFF30B94D)
        "cricket" -> Color(0xFF0A84FF)
        "basketball" -> Color(0xFFFF9500)
        else -> Color(0xFF8E8E93)
    }
}

fun formatMatchDate(dateStr: String): String {
    if (dateStr.isEmpty()) return ""
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val day = parts[2].toIntOrNull() ?: return dateStr
            val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: return dateStr
            val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            if (monthIdx in 0..11) {
                "$day ${months[monthIdx]}"
            } else {
                dateStr
            }
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}

fun formatMatchTime(timeStr: String): String {
    if (timeStr.isEmpty()) return ""
    return try {
        val parts = timeStr.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].toIntOrNull() ?: return timeStr
            val minute = parts[1]
            val suffix = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            "$displayHour:$minute $suffix"
        } else {
            timeStr
        }
    } catch (e: Exception) {
        timeStr
    }
}

@Composable
fun HtmlStyleMatchRow(
    event: EventEntity,
    onClick: () -> Unit
) {
    val isLive = event.status == "LIVE"
    val sportColorVal = getSportColor(event.category)
    val isSystemDark = isSystemInDarkTheme()

    val backgroundColor = if (isLive) {
        if (isSystemDark) {
            Color(0xFF2B1618) // Soft dark red background
        } else {
            Color(0xFFFFF8F8) // Soft light red background
        }
    } else {
        Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team 1
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TeamFlag(url = event.team1Flag, char = event.team1Name.firstOrNull() ?: '1', size = 48.dp)
                Text(
                    text = event.team1Name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Match Info Center
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isLive) {
                    Row(
                        modifier = Modifier
                            .background(
                                if (isSystemDark) Color(0xFF381A1C) else Color(0xFFFFF0F0),
                                RoundedCornerShape(20.dp)
                            )
                            .border(
                                1.dp,
                                Color(0xFFFF3B30).copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "blink")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(alpha)
                                .background(Color(0xFFFF3B30), CircleShape)
                        )
                        Text(
                            text = "Live",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF3B30)
                        )
                    }

                    if (event.time.isNotEmpty()) {
                        Text(
                            text = formatMatchTime(event.time),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF8E8E93),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val datePart = if (event.date.isNotEmpty()) {
                        formatMatchDate(event.date)
                    } else {
                        event.status
                    }
                    Text(
                        text = datePart.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E8E93),
                        letterSpacing = 0.4.sp,
                        textAlign = TextAlign.Center
                    )

                    if (event.time.isNotEmpty()) {
                        Text(
                            text = formatMatchTime(event.time),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Team 2
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TeamFlag(url = event.team2Flag, char = event.team2Name.firstOrNull() ?: '2', size = 48.dp)
                Text(
                    text = event.team2Name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Meta (Category, separators, league, round)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 11.dp, top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.category,
                color = sportColorVal,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = "|",
                color = if (isSystemDark) Color(0x33FFFFFF) else Color(0xFFD1D1D6),
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 5.dp)
            )
            
            Text(
                text = event.league,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (event.round.isNotEmpty() && event.round != "N/A" && event.round != "null") {
                Text(
                    text = "|",
                    color = if (isSystemDark) Color(0x33FFFFFF) else Color(0xFFD1D1D6),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 5.dp)
                )

                Text(
                    text = event.round,
                    color = sportColorVal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
