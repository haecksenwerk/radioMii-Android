package com.radiomii.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.radiomii.R
import com.radiomii.domain.model.MusicProvider
import com.radiomii.domain.model.SortOrder
import com.radiomii.domain.model.DEFAULT_TAG_LIST
import com.radiomii.domain.model.TagShortcutMode
import com.radiomii.domain.model.ThemeColor
import com.radiomii.domain.model.ThemeMode
// Country model not used directly in this file (picker dialog moved out)
import com.radiomii.ui.screens.favorites.CreateFilterDialog
import com.radiomii.ui.screens.favorites.ReorderFiltersDialog
import com.radiomii.ui.theme.CUSTOM_SWATCH_COLORS
import com.radiomii.ui.theme.isEffectiveDarkTheme

private data class LanguageOption(val code: String, val nativeName: String, val displayName: String)

private val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("system", "System", "System language"),
    LanguageOption("en", "English", "English"),
    LanguageOption("bg", "Български", "Bulgarian"),
    LanguageOption("de", "Deutsch", "German"),
    LanguageOption("fr", "Français", "French"),
    LanguageOption("it", "Italiano", "Italian"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAppInfo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // True Black is only applicable in a dark theme
    val isDarkThemeActive = isEffectiveDarkTheme(settings.themeMode)
    val importExportMessage by viewModel.importExportMessage.collectAsStateWithLifecycle()
    val filterNames by viewModel.filterNames.collectAsStateWithLifecycle()
    val lastExportUri by viewModel.lastExportUri.collectAsStateWithLifecycle()

    // Use explicit MutableState<Boolean> so assignments use `.value` and are visible to the compiler
    val showLanguageDialog = remember { mutableStateOf(false) }
    val showClearFavoritesDialog = remember { mutableStateOf(false) }
    val showCountryDialog = remember { mutableStateOf(false) }
    val showReorderTagsDialog = remember { mutableStateOf(false) }
    val showCreateFilterDialog = remember { mutableStateOf(false) }
    val showReorderFiltersDialog = remember { mutableStateOf(false) }
    val showCustomStreamDialog = remember { mutableStateOf(false) }

    val countries by viewModel.countries.collectAsStateWithLifecycle()
    val countriesLoading by viewModel.countriesLoading.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Resolve strings in composable scope so they can be used inside LaunchedEffect
    val strFavoritesSaved    = stringResource(R.string.favorites_saved)
    val strFavoritesLoaded   = stringResource(R.string.favorites_loaded)
    val strFavoritesSaveError = stringResource(R.string.favorites_save_error)
    val strFavoritesLoadError = stringResource(R.string.favorites_load_error)

    // SAF launchers
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let { viewModel.saveFavoritesToUri(it) } }
    val loadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.loadFavoritesFromUri(it) } }

    // Handle import/export feedback
    LaunchedEffect(importExportMessage) {
        importExportMessage?.let { event ->
            val text = when (event) {
                is ImportExportEvent.Saved     -> strFavoritesSaved
                is ImportExportEvent.Loaded    -> strFavoritesLoaded
                is ImportExportEvent.SaveError -> strFavoritesSaveError
                is ImportExportEvent.LoadError -> strFavoritesLoadError
            }
            snackbarHostState.showSnackbar(text)
            viewModel.clearImportExportMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                windowInsets = WindowInsets(0),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ─── Search ──────────────────────────────────────────────────────
            item {
                val bitrateStops = remember { listOf(0, 96, 128, 192, 256) }
                // stringResource must be read in the composable scope before remember {}
                val anyCountryLabel = stringResource(R.string.search_country_any)
                val countryDisplay = remember(settings.searchOptions.country, countries) {
                    if (settings.searchOptions.country.isBlank()) {
                        anyCountryLabel
                    } else {
                        countries.find {
                            it.iso31661.equals(settings.searchOptions.country, ignoreCase = true)
                        }?.name ?: settings.searchOptions.country
                    }
                }

                SectionHeader(title = stringResource(R.string.nav_search))

                // Country + bitrate
                SettingsCard {
                    ClickableRow(
                        title = stringResource(R.string.search_country),
                        subtitle = countryDisplay,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = {
                            viewModel.loadCountries()
                            showCountryDialog.value = true
                        },
                        trailingContent = {
                            if (countriesLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                    SegmentedButtonRow(
                        title = stringResource(R.string.settings_search_min_bitrate),
                        options = bitrateStops,
                        selected = settings.searchOptions.bitrateMin,
                        onSelect = { viewModel.setSearchBitrateMin(it) },
                        labelOf = { "$it" },
                    )
                }

                // Sort criteria + direction
                SettingsCard {
                    SegmentedButtonRowIconsOnly(
                        title = stringResource(R.string.settings_sort_criteria),
                        options = SortOrder.entries,
                        selected = settings.searchOptions.sortOrder,
                        onSelect = { viewModel.setSearchSortOrder(it) },
                        labelOf = { order ->
                            when (order) {
                                SortOrder.VOTES -> stringResource(R.string.settings_sort_votes)
                                SortOrder.CLICK_COUNT -> stringResource(R.string.settings_sort_clicks)
                                SortOrder.COUNTRY -> stringResource(R.string.settings_sort_country)
                                SortOrder.BITRATE -> stringResource(R.string.settings_sort_bitrate)
                            }
                        },
                        iconOf = { order ->
                            when (order) {
                                SortOrder.VOTES -> Icon(Icons.Outlined.ThumbUp, null, Modifier.size(SegmentedButtonDefaults.IconSize))
                                SortOrder.CLICK_COUNT -> Icon(Icons.Outlined.AdsClick, null, Modifier.size(SegmentedButtonDefaults.IconSize))
                                SortOrder.COUNTRY -> Icon(Icons.Default.Public, null, Modifier.size(SegmentedButtonDefaults.IconSize))
                                SortOrder.BITRATE -> Text("KB/s", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        },
                    )
                    SegmentedButtonRow(
                        title = stringResource(R.string.settings_sort_order),
                        options = listOf(false, true),
                        selected = settings.searchOptions.reverse,
                        onSelect = { viewModel.setSearchReverse(it) },
                        labelOf = { if (!it) stringResource(R.string.settings_sort_ascending) else stringResource(R.string.settings_sort_descending) },
                        iconOf = { reverse ->
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(SegmentedButtonDefaults.IconSize)
                                    .graphicsLayer { scaleY = if (!reverse) -1f else 1f },
                            )
                        },
                    )
                }

                // Display / filter switches
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_search_ignore_offline),
                        subtitle = stringResource(R.string.settings_search_ignore_offline_desc),
                        checked = settings.searchOptions.hidebroken,
                        onCheckedChange = { viewModel.setSearchHideBroken(it) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_search_secure_only),
                        subtitle = stringResource(R.string.settings_search_secure_only_desc),
                        checked = settings.searchOptions.isHttpsOnly,
                        onCheckedChange = { viewModel.setSearchHttpsOnly(it) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_search_tag_buttons),
                        subtitle = stringResource(R.string.settings_search_tag_buttons_desc),
                        checked = settings.searchOptions.showTagButtons,
                        onCheckedChange = { viewModel.setSearchShowTagButtons(it) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LibraryMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    SegmentedButtonRow(
                        title = stringResource(R.string.settings_search_genre_shortcuts),
                        options = TagShortcutMode.entries,
                        selected = settings.searchOptions.tagShortcutMode,
                        onSelect = { viewModel.setSearchGenreSuggestionMode(it) },
                        enabled = settings.searchOptions.showTagButtons,
                        labelOf = { mode ->
                            when (mode) {
                                TagShortcutMode.CARDS -> stringResource(R.string.settings_search_genre_cards)
                                TagShortcutMode.BUTTONS -> stringResource(R.string.settings_search_genre_buttons)
                            }
                        },
                    )
                    ClickableRow(
                        title = stringResource(R.string.settings_reorder_tags),
                        subtitle = stringResource(R.string.settings_search_tag_order_desc),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showReorderTagsDialog.value = true },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }


            // ─── Favorites ───────────────────────────────────────────────────────
            item {
                SectionHeader(title = stringResource(R.string.settings_favorites_title))

                // Filter management card (moved from FavoritesScreen menu)
                SettingsCard {
                    ClickableRow(
                        title = stringResource(R.string.settings_favorites_create_filter),
                        subtitle = stringResource(R.string.settings_favorites_create_filter_desc),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showCreateFilterDialog.value = true },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    ClickableRow(
                        title = stringResource(R.string.settings_favorites_reorder_filters),
                        subtitle = stringResource(R.string.settings_favorites_reorder_filters_desc),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showReorderFiltersDialog.value = true },
                        enabled = filterNames.size > 1,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )

                    SwitchRow(
                        title = stringResource(R.string.settings_favorites_highlight_unassigned),
                        subtitle = stringResource(R.string.settings_favorites_highlight_unassigned_desc),
                        checked = settings.highlightUnassigned,
                        onCheckedChange = { viewModel.setHighlightUnassigned(it) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.FilterAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }

                SettingsCard {
                    ClickableRow(
                        title = stringResource(R.string.settings_favorites_add_custom),
                        subtitle = stringResource(R.string.settings_favorites_add_custom_desc),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Radio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showCustomStreamDialog.value = true },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                SettingsCard {
                    ClickableRow(
                        title = stringResource(R.string.settings_favorites_save),
                        subtitle = stringResource(R.string.settings_favorites_save_desc),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { saveLauncher.launch("radiomii_favorites.json") },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    // Only shown after a successful first save – lets the user overwrite without picker
                    if (lastExportUri != null) {
                        ClickableRow(
                            title = stringResource(R.string.settings_favorites_overwrite),
                            subtitle = stringResource(R.string.settings_favorites_overwrite_desc),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Save,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            onClick = { viewModel.overwriteLastBackup() },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                    ClickableRow(
                        title = stringResource(R.string.settings_favorites_import),
                        subtitle = stringResource(R.string.settings_favorites_import_desc),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { loadLauncher.launch(arrayOf("application/json", "text/plain")) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    ClickableRow(
                        title = stringResource(R.string.settings_favorites_delete_all),
                        subtitle = stringResource(R.string.settings_favorites_delete_all_desc),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showClearFavoritesDialog.value = true },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }


            // ─── Appearance ────────────────────────────────────────────────────

            item {
                SectionHeader(title = stringResource(R.string.settings_appearance_title))
                SettingsCard {
                    SegmentedButtonRow(
                        title = stringResource(R.string.settings_appearance_theme_mode),
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        onSelect = { viewModel.setThemeMode(it) },
                        labelOf = { mode ->
                            when (mode) {
                                ThemeMode.LIGHT -> stringResource(R.string.settings_appearance_light)
                                ThemeMode.DARK -> stringResource(R.string.settings_appearance_dark)
                                ThemeMode.SYSTEM -> stringResource(R.string.settings_appearance_system)
                            }
                        },
                        iconOf = { mode ->
                            val icon = when (mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.SettingsSuggest
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_appearance_true_black),
                        subtitle = stringResource(R.string.settings_appearance_true_black_desc),
                        checked = settings.trueBlack,
                        onCheckedChange = { viewModel.setTrueBlack(it) },
                        enabled = isDarkThemeActive,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Contrast,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    val colorOptions = remember {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            ThemeColor.entries
                        } else {
                            ThemeColor.entries.filter { it != ThemeColor.DYNAMIC }
                        }
                    }
                    SegmentedButtonRow(
                        title = stringResource(R.string.settings_appearance_theme_color),
                        options = colorOptions,
                        selected = settings.themeColor,
                        onSelect = { viewModel.setThemeColor(it) },
                        labelOf = { color ->
                            when (color) {
                                ThemeColor.MII -> "Mii"
                                ThemeColor.DYNAMIC -> stringResource(R.string.settings_appearance_dynamic)
                                ThemeColor.CUSTOM -> stringResource(R.string.settings_appearance_colors)
                            }
                        },
                        iconOf = { color ->
                            when (color) {
                                ThemeColor.MII -> Image(
                                    painter = painterResource(R.drawable.ic_radiomii_logo),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.tint(
                                        LocalContentColor.current,
                                        BlendMode.SrcIn,
                                    ),
                                    modifier = Modifier.size(24.dp),
                                )
                                ThemeColor.DYNAMIC -> Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                                ThemeColor.CUSTOM -> Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        }
                    )
                    AnimatedVisibility(
                        visible = settings.themeColor == ThemeColor.CUSTOM,
                        enter = expandVertically(expandFrom = Alignment.Top, animationSpec = tween(durationMillis = 300)) +
                                slideInVertically(initialOffsetY = { fullHeight -> -fullHeight / 2 }, animationSpec = tween(durationMillis = 300)),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(durationMillis = 220)) +
                               slideOutVertically(targetOffsetY = { fullHeight -> -fullHeight / 2 }, animationSpec = tween(durationMillis = 220)),
                    ) {
                        ColorSwatchRow(
                            selectedIndex = settings.customSourceColorIndex,
                            onSelect = { viewModel.setCustomColorIndex(it) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    val langOption = SUPPORTED_LANGUAGES.firstOrNull { it.code == settings.language }
                    val langDisplay = langOption?.nativeName ?: stringResource(R.string.settings_appearance_language_system)
                    ClickableRow(
                        title = stringResource(R.string.settings_appearance_language),
                        subtitle = langDisplay,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { showLanguageDialog.value = true },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }



            // ─── Playback ────────────────────────────────────────────────────
            item {
                SectionHeader(title = stringResource(R.string.settings_playback_title))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_playback_background),
                        subtitle = stringResource(R.string.settings_playback_background_desc),
                        checked = settings.backgroundPlayback,
                        onCheckedChange = { viewModel.setBackgroundPlayback(it) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_playback_resume_call),
                        subtitle = stringResource(R.string.settings_playback_resume_call_desc),
                        checked = settings.resumeAfterCall,
                        onCheckedChange = { viewModel.setResumeAfterCall(it) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }

            // ─── Open In… ────────────────────────────────────────────────────
            item {
                SectionHeader(title = stringResource(R.string.settings_find_in_title))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.settings_find_in_show_button),
                        subtitle = stringResource(R.string.settings_find_in_show_button_desc),
                        checked = settings.showFindOnButton,
                        onCheckedChange = { viewModel.setShowFindOnButton(it) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                    if (settings.showFindOnButton) {
                        SegmentedButtonRow(
                            title = stringResource(R.string.settings_find_in_provider),
                            options = MusicProvider.entries,
                            selected = settings.findOnProvider,
                            onSelect = { viewModel.setFindOnProvider(it) },
                            labelOf = { provider ->
                                when (provider) {
                                    MusicProvider.SPOTIFY -> "Spotify"
                                    MusicProvider.YOUTUBE -> "YouTube"
                                }
                            },
                            iconOf = { provider ->
                                when (provider) {
                                    MusicProvider.SPOTIFY -> Icon(
                                        painter = painterResource(id = R.drawable.ic_spotify),
                                        contentDescription = null,
                                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                    )
                                    MusicProvider.YOUTUBE -> Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                    )
                                }
                            },
                        )
                    }
                }
            }



            // ─── About ───────────────────────────────────────────────────────
            item {
                SectionHeader(title = stringResource(R.string.settings_about_title))
                SettingsCard {
                    ClickableRow(
                        title = stringResource(R.string.settings_about_app_info),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = onNavigateToAppInfo,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }
        }
    }

    // ─── Country Picker Dialog ──────────────────────────────────────────────
    if (showCountryDialog.value) {
        CountryPickerDialog(
            countries = countries,
            selectedCountry = settings.searchOptions.country,
            onSelect = { country ->
                viewModel.setSearchCountry(country)
                showCountryDialog.value = false
            },
            onDismiss = { showCountryDialog.value = false },
        )
    }

    // ─── Language Selection Dialog ──────────────────────────────────────────
    if (showLanguageDialog.value) {
        LanguagePickerDialog(
            selected = settings.language,
            onSelect = { code ->
                viewModel.setLanguage(code)
                showLanguageDialog.value = false
            },
            onDismiss = { showLanguageDialog.value = false },
        )
    }

    // ─── Reorder Tags Dialog ────────────────────────────────────────────────
    if (showReorderTagsDialog.value) {
        ReorderTagsDialog(
            tagOrder = settings.searchOptions.tagOrder.ifEmpty { DEFAULT_TAG_LIST },
            onReorder = { newOrder -> viewModel.setSearchTagOrder(newOrder) },
            onDismiss = { showReorderTagsDialog.value = false },
        )
    }

    // ─── Create Filter Dialog ───────────────────────────────────────────────
    if (showCreateFilterDialog.value) {
        CreateFilterDialog(
            existingFilters = filterNames,
            onCreate = { viewModel.createFilter(it) },
            onDismiss = { showCreateFilterDialog.value = false },
        )
    }

    // ─── Reorder Filters Dialog ─────────────────────────────────────────────
    if (showReorderFiltersDialog.value) {
        ReorderFiltersDialog(
            filterNames = filterNames,
            onReorder = { viewModel.reorderFilters(it) },
            onDelete = { viewModel.deleteFilter(it) },
            onDismiss = { showReorderFiltersDialog.value = false },
        )
    }

    // ─── Add Custom Stream Dialog ───────────────────────────────────────────
    if (showCustomStreamDialog.value) {
        CustomStreamDialog(
            onAdd = { viewModel.addFavorite(it) },
            onDismiss = { showCustomStreamDialog.value = false },
        )
    }

    // ─── Clear Favorites Confirmation Dialog ────────────────────────────────
    if (showClearFavoritesDialog.value) {        AlertDialog(
            onDismissRequest = { showClearFavoritesDialog.value = false },
            title = { Text(stringResource(R.string.dialog_delete_all_favorites_title)) },
            text = { Text(stringResource(R.string.dialog_delete_all_favorites_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllFavorites()
                        showClearFavoritesDialog.value = false
                    },
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFavoritesDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

// ─── Color Swatch Row ────────────────────────────────────────────────────────

@Composable
private fun ColorSwatchRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    // Measured viewport width via onGloballyPositioned on the container Box
    var viewportWidthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedIndex, viewportWidthPx) {
        if (selectedIndex >= 0 && viewportWidthPx > 0f) {
            // item width = 44dp circle + 12dp spacing
            val itemWidthPx = with(density) { (44.dp + 12.dp).toPx() }
            val itemCenterPx = selectedIndex * itemWidthPx + with(density) { 34.dp.toPx() }
            val targetScrollPx = (itemCenterPx - viewportWidthPx / 2).toInt().coerceAtLeast(0)
            scrollState.animateScrollTo(targetScrollPx)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { viewportWidthPx = it.size.width.toFloat() },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            CUSTOM_SWATCH_COLORS.forEachIndexed { index, color ->
                val isSelected = index == selectedIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .then(
                            if (isSelected)
                                Modifier.border(3.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            else
                                Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 32.dp else 40.dp)
                            .clip(if (isSelected) RoundedCornerShape(8.dp) else CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

// ─── Language Picker Dialog ──────────────────────────────────────────────────

@Composable
private fun LanguagePickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_select)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        text = {
            LazyColumn {
                items(SUPPORTED_LANGUAGES) { lang ->
                    ListItem(
                        headlineContent = { Text(lang.nativeName) },
                        supportingContent = if (lang.nativeName != lang.displayName) {
                            { Text(lang.displayName) }
                        } else null,
                        leadingContent = {
                            RadioButton(selected = lang.code == selected, onClick = null)
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lang.code) }
                    )
                }
            }
        }
    )
}

// Country picker dialog and flag emoji helper moved to `CountryPickerDialog.kt`.
