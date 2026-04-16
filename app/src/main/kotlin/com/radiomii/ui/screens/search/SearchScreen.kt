package com.radiomii.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.radiomii.R
import com.radiomii.domain.model.DEFAULT_TAG_LIST
import com.radiomii.domain.model.TagShortcutMode
import com.radiomii.domain.model.SearchMode
import com.radiomii.domain.model.Station
import com.radiomii.ui.components.SearchRow
import com.radiomii.ui.AppViewModel
import com.radiomii.ui.theme.isEffectiveDarkTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    appViewModel: AppViewModel,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val moreAvailable by viewModel.moreAvailable.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val searchVersion by viewModel.searchVersion.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    // Derived from persisted settings; changes go through appViewModel.setSearchOptions().
    val searchMode = settings.searchOptions.searchMode

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    // True when the genre-cards grid is visible:
    // TAG mode + tag buttons enabled + CARDS suggestion mode + no active query.
    val showingCardsGrid = searchMode == SearchMode.TAG
        && query.isEmpty()
        && settings.searchOptions.showTagButtons
        && settings.searchOptions.tagShortcutMode == TagShortcutMode.CARDS

    // animateFloatAsState has no initial animation, so the grid appears instantly on cold start.
    // A plain float avoids the ColumnScope.AnimatedVisibility ambiguity inside Box-in-Column.
    val cardsGridAlpha by animateFloatAsState(
        targetValue = if (showingCardsGrid) 1f else 0f,
        animationSpec = tween(600),
        label = "cards_grid_alpha",
    )

    // Dimming is only meaningful in a dark theme; suppress it for light mode
    val effectiveTrueBlack = settings.trueBlack && isEffectiveDarkTheme(settings.themeMode)

    // Suppress FAB when cards grid is active (list is not in composition).
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    // Scroll to the first item whenever a fresh search starts
    LaunchedEffect(searchVersion) {
        if (searchVersion > 0) listState.scrollToItem(0)
    }

    var searchInitialized by remember { mutableStateOf(false) }
    var lastSearchedOptions by remember { mutableStateOf(settings.searchOptions) }

    LaunchedEffect(settings.searchOptions, isResumed) {
        // Skip when backgrounded or cards grid is active (no network needed).
        if (!isResumed || showingCardsGrid) return@LaunchedEffect

        val effectiveTerm = if (searchMode == SearchMode.TAG) "" else query
        // Re-search only if something beyond a bare mode-toggle changed.
        val settingsChangedBeyondMode =
            lastSearchedOptions.copy(searchMode = searchMode) != settings.searchOptions

        if (!searchInitialized || effectiveTerm.isNotEmpty() || settingsChangedBeyondMode) {
            viewModel.search(effectiveTerm, settings.searchOptions)
        }
        lastSearchedOptions = settings.searchOptions
        searchInitialized = true
    }

    // Trigger load-more when approaching end
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            moreAvailable && lastVisible >= totalItems - 10
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar and NAME/TAG toggle button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = query,
                    onValueChange = {
                        val changed = query != it
                        query = it
                        if (changed && it.isEmpty() && !showingCardsGrid) {
                            viewModel.search("", settings.searchOptions.copy(searchMode = searchMode))
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                if (!showingCardsGrid) {
                                    viewModel.search("", settings.searchOptions.copy(searchMode = searchMode))
                                }
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            if (searchMode == SearchMode.NAME)
                                stringResource(R.string.search_by_name)
                            else
                                stringResource(R.string.search_by_tag),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide()
                        viewModel.search(query, settings.searchOptions.copy(searchMode = searchMode))
                    }),
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        val newMode = if (searchMode == SearchMode.NAME) SearchMode.TAG else SearchMode.NAME
                        val newOpts = settings.searchOptions.copy(searchMode = newMode)
                        appViewModel.setSearchOptions(newOpts)
                        if (query.isNotEmpty()) {
                            // Clear the term and fetch with empty query when switching modes
                            query = ""
                            viewModel.search("", newOpts)
                        }
                        // If query is already empty, persisting the new mode is sufficient – no fetch needed
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(
                        imageVector = if (searchMode == SearchMode.NAME) Icons.Outlined.LibraryMusic else Icons.AutoMirrored.Filled.ShortText,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Tag chip row — shown in TAG mode with BUTTONS suggestion mode when tag buttons are enabled
            AnimatedVisibility(
                visible = searchMode == SearchMode.TAG
                    && settings.searchOptions.showTagButtons
                    && settings.searchOptions.tagShortcutMode == TagShortcutMode.BUTTONS
            ) {
                TagChipsRow(
                    selectedTag = query,
                    tagList = settings.searchOptions.tagOrder.ifEmpty { DEFAULT_TAG_LIST },
                    onTagSelected = { tag ->
                        query = tag
                        viewModel.search(tag, settings.searchOptions.copy(searchMode = SearchMode.TAG))
                    },
                )
            }

            // Content area — a Box so the cards grid can animate as a top layer
            // above the base content without leaving gaps in the layout.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // ── Base layer: spinner / no-results / results list ──────────
                // Guard: don't show spinner while cards grid is fading out.
                if (isLoading && results.isEmpty() && !showingCardsGrid) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (!isLoading && error != null && results.isEmpty() && !showingCardsGrid) {
                    // Network/server error — show message with retry button
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.search_error_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(onClick = {
                                viewModel.search(query, settings.searchOptions.copy(searchMode = searchMode))
                            }) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                    }
                } else if (results.isEmpty() && query.isNotEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (!showingCardsGrid) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(results, key = { _, s -> s.stationuuid }) { _, station ->
                            val favMatch = favorites.find { it.stationuuid == station.stationuuid }
                            SearchRow(
                                station = station,
                                isFavorite = favMatch != null,
                                displayName = favMatch?.displayName ?: station.name,
                                onClick = {
                                    appViewModel.play(station)
                                    onStationClick(station)
                                },
                                onFavoriteToggle = { viewModel.toggleFavorite(station) },
                                activeSortOrder = settings.searchOptions.sortOrder,
                            )
                        }

                        if (isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                // Stay in composition while cardsGridAlpha > 0 so the fade-out completes.
                if (cardsGridAlpha > 0f) {
                    GenreCardsGrid(
                        tagList = settings.searchOptions.tagOrder.ifEmpty { DEFAULT_TAG_LIST },
                        trueBlack = effectiveTrueBlack,
                        onTagSelected = { tag ->
                            query = tag
                            keyboardController?.hide()
                            viewModel.search(tag, settings.searchOptions.copy(searchMode = SearchMode.TAG))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(cardsGridAlpha),
                    )
                }
            }
        }

        // FAB in BoxScope to avoid ColumnScope.AnimatedVisibility ambiguity; hidden when cards grid is active.
        AnimatedVisibility(
            visible = showScrollToTop && !showingCardsGrid,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            FloatingActionButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun TagChipsRow(
    selectedTag: String,
    tagList: List<String>,
    onTagSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val selectedIndex = if (selectedTag.isEmpty()) 0
        else tagList.indexOfFirst { it.equals(selectedTag, ignoreCase = true) }.let {
            if (it >= 0) it + 1 else -1
        }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            val avgSize = listState.layoutInfo.visibleItemsInfo.map { it.size }.average().takeIf { it.isFinite() }?.toInt() ?: 0
            val viewportWidth = listState.layoutInfo.viewportSize.width
            if (avgSize > 0 && viewportWidth > 0) {
                val scrollOffset = avgSize / 2 - viewportWidth / 2
                listState.animateScrollToItem(selectedIndex, scrollOffset)
            }
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.padding(vertical = 4.dp),
    ) {
        item {
            FilterChip(
                selected = selectedTag.isEmpty(),
                onClick = { onTagSelected("") },
                label = { Text(stringResource(R.string.favorites_all_stations)) },
            )
        }
        itemsIndexed(tagList) { _, tag ->
            FilterChip(
                selected = selectedTag.equals(tag, ignoreCase = true),
                onClick = { onTagSelected(tag) },
                label = { Text(tag) },
            )
        }
    }
}


private val GENRE_CARD_COLORS = listOf(
    Color(0xFFBBCFF8), // pastel blue
    Color(0xFFB5DDB5), // pastel green
    Color(0xFFF5E6A3), // pastel yellow
    Color(0xFFF5B8C2), // pastel rose
    Color(0xFFD0B8F5), // pastel violet
    Color(0xFFF5CFA3), // pastel peach
    Color(0xFFB3DED9), // pastel mint
    Color(0xFFD4C9F0), // pastel lavender
    Color(0xFFF5C6A0), // pastel coral
    Color(0xFFC8E6C9), // pastel sage
)

// Maps tag name to genre illustration drawable.
private val GENRE_DRAWABLES: Map<String, Int> = mapOf(
    "Alternative" to R.drawable.genre_alternative,
    "Rock"       to R.drawable.genre_rock,
    "Hard Rock"  to R.drawable.genre_hardrock,
    "Metal"      to R.drawable.genre_metal,
    "Punk"      to R.drawable.genre_punk,
    "Indie"      to R.drawable.genre_indie,
    "Soul"       to R.drawable.genre_soul,
    "Funk"       to R.drawable.genre_funk,
    "Reggae"     to R.drawable.genre_reggae,
    "Chillout"     to R.drawable.genre_chillout,
    "Pop"     to R.drawable.genre_pop,
    "Hiphop"     to R.drawable.genre_hiphop,
    "House"     to R.drawable.genre_house,
    "Electro"     to R.drawable.genre_electro,
    "Ambient"     to R.drawable.genre_ambient,
    "Progressive"     to R.drawable.genre_progressive,
    "Smooth Jazz"     to R.drawable.genre_smooth_jazz,
    "Jazz"       to R.drawable.genre_jazz,
    "Blues"       to R.drawable.genre_blues,
    "Salsa"       to R.drawable.genre_salsa,
    "Latino"       to R.drawable.genre_latino,
    "Disco"      to R.drawable.genre_disco,
    "Oldies"     to R.drawable.genre_oldies,
    "60s"        to R.drawable.genre_60s,
    "70s"        to R.drawable.genre_70s,
    "80s"        to R.drawable.genre_80s,
    "90s"        to R.drawable.genre_90s,
    "Country"        to R.drawable.genre_country,
    "African Music" to R.drawable.genre_african_music,
    "World Music" to R.drawable.genre_world_music,
    "Classical" to R.drawable.genre_classical,
    "News" to R.drawable.genre_news,
)

@Composable
private fun GenreCardsGrid(
    tagList: List<String>,
    onTagSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    trueBlack: Boolean = false,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        itemsIndexed(tagList) { index, tag ->
            val baseColor = GENRE_CARD_COLORS[index % GENRE_CARD_COLORS.size]
            val bgColor = if (trueBlack) Color(
                red   = baseColor.red   * 0.8f,
                green = baseColor.green * 0.8f,
                blue  = baseColor.blue  * 0.8f,
                alpha = baseColor.alpha,
            ) else baseColor
            val drawableRes = GENRE_DRAWABLES[tag]
            Card(
                onClick = { onTagSelected(tag) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                modifier = Modifier.aspectRatio(1.618f),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (drawableRes != null) {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            alpha = 0.82f,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f)
                                .align(Alignment.Center)
                                .padding(24.dp),
                        )
                    }
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xdf101010),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 8.dp, end = 12.dp),
                    )
                }
            }
        }
    }
}
