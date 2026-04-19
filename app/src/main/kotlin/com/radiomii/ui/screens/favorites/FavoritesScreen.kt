package com.radiomii.ui.screens.favorites

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomii.R
import com.radiomii.domain.model.Station
import com.radiomii.ui.theme.isEffectiveDarkTheme
import com.radiomii.ui.components.FavoriteRow
import com.radiomii.ui.components.FilterChipsRow
import com.radiomii.ui.AppViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val filterNames by viewModel.filterNames.collectAsStateWithLifecycle()
    val stationFilterMap by viewModel.stationFilterMap.collectAsStateWithLifecycle()

    val settings by appViewModel.settings.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val noFiltersHint = stringResource(R.string.favorites_no_filters_snackbar)

    var isEditMode by remember { mutableStateOf(false) }
    var isCompact by remember { mutableStateOf(settings.compactRow) }
    var showFilterBar by remember { mutableStateOf(settings.showFilterBar) }
    // Keep local UI state in sync with persisted settings
    LaunchedEffect(settings.showFilterBar) {
        showFilterBar = settings.showFilterBar
    }
    // Immediate toggle OR persisted setting shows the filter bar.
    val effectiveShowFilterBar = remember(showFilterBar, settings.showFilterBar) { showFilterBar || settings.showFilterBar }
    var activeFilterIndex by remember { mutableIntStateOf(-1) }
    // Use explicit MutableState for these dialog/show flags so writes are observable
    val showDeleteFilterDialog = remember { mutableStateOf<String?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    val showReorderFiltersDialog = remember { mutableStateOf(false) }
    val stationForAssignFilter = remember { mutableStateOf<Station?>(null) }

    val displayedFavorites = remember(favorites, filterNames, activeFilterIndex) {
        viewModel.filteredFavorites(favorites, filterNames, activeFilterIndex)
    }

    var reorderList by remember(displayedFavorites) { mutableStateOf(displayedFavorites) }

    val lazyListState = rememberLazyListState()
    val viewListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorderList = reorderList.toMutableList().apply { add(to.index, removeAt(from.index)) }
        if (activeFilterIndex >= 0) {
            // A filter group is active: reorderList is only a subset of all favorites.
            // Merge the reordered subset back into the full list so the other stations
            // are not lost when saving.
            viewModel.reorderFavoritesSubset(reorderList, favorites)
        } else {
            viewModel.reorderFavorites(reorderList)
        }
    }

    // Reset scroll on filter change to avoid stale positions causing visual glitches.
    LaunchedEffect(activeFilterIndex) {
        lazyListState.scrollToItem(0)
        viewListState.scrollToItem(0)
    }

    val showScrollToTop by remember {
        derivedStateOf {
            if (isEditMode) lazyListState.firstVisibleItemIndex > 0
            else viewListState.firstVisibleItemIndex > 0
        }
    }

    // Hide filter bar if no filters exist at all
    LaunchedEffect(filterNames) {
        if (filterNames.isEmpty()) showFilterBar = false
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (isEditMode) lazyListState.animateScrollToItem(0)
                            else viewListState.animateScrollToItem(0)
                        }
                    },
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                }
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Image(
                        painter = painterResource(
                            if (!isEffectiveDarkTheme(settings.themeMode)) R.drawable.logo_lm else R.drawable.logo_dm
                        ),
                        contentDescription = stringResource(R.string.nav_favorites),
                        modifier = Modifier.height(28.dp),
                    )
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { isEditMode = false }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.common_done),
                            )
                        }
                    } else {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isCompact) stringResource(R.string.favorites_expanded_view) else stringResource(R.string.favorites_compact_view)) },
                                    onClick = {
                                        val newCompact = !isCompact
                                        isCompact = newCompact
                                        appViewModel.setCompactRow(newCompact)
                                        menuExpanded = false
                                    },
                                    leadingIcon = { Icon(if (isCompact) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess, null) },
                                    enabled = favorites.isNotEmpty(),
                                )
                                // Show/Hide filter buttons
                                DropdownMenuItem(
                                    text = { Text(if (effectiveShowFilterBar) stringResource(R.string.favorites_hide_filter_bar) else stringResource(R.string.favorites_show_filter_bar)) },
                                    onClick = {
                                        menuExpanded = false
                                        if (filterNames.isEmpty()) {
                                            scope.launch { snackbarHostState.showSnackbar(noFiltersHint) }
                                        } else {
                                            val newShow = !effectiveShowFilterBar
                                            showFilterBar = newShow
                                            appViewModel.setShowFilterBar(newShow)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (effectiveShowFilterBar) Icons.Outlined.FilterAltOff
                                            else Icons.Outlined.FilterAlt,
                                            null,
                                        )
                                    },
                                    enabled = favorites.isNotEmpty(),
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.favorites_edit)) },
                                    onClick = { isEditMode = true; menuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.Reorder, null) },
                                    enabled = favorites.isNotEmpty(),
                                )
                            }
                        }
                    } // end else
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedVisibility(visible = effectiveShowFilterBar && filterNames.isNotEmpty()) {
                FilterChipsRow(
                    filterNames = filterNames,
                    activeFilterIndex = activeFilterIndex,
                    onFilterSelected = { activeFilterIndex = it },
                    onLongPress = { showReorderFiltersDialog.value = true },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            if (favorites.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.favorites_empty_title),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.favorites_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            } else {
                if (isEditMode) {
                    // Edit mode: plain LazyColumn with drag-to-reorder and swipe-to-delete.
                    // No crossfade – animations during dragging would be disruptive.
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(reorderList, key = { _, s -> s.stationuuid }) { _, station ->
                            // Avoid using a translucent color here because the SwipeToDismiss background
                            // (delete / assign) sits behind the row and will show through.
                            val isUnassigned = settings.highlightUnassigned
                                && filterNames.isNotEmpty()
                                && stationFilterMap[station.stationuuid].isNullOrEmpty()
                            val rowColor = if (isUnassigned) {
                                androidx.compose.ui.graphics.lerp(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    0.6f,
                                )
                            } else {
                                MaterialTheme.colorScheme.background
                            }
                            // ReorderableItem is only used in edit mode.
                            ReorderableItem(reorderState, key = station.stationuuid, enabled = true) { isDragging ->
                                val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp, label = "drag_elevation")
                                val dismissState = rememberSwipeToDismissBoxState()
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = true,
                                    enableDismissFromEndToStart = true,
                                    onDismiss = { value ->
                                        if (value == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.removeFavorite(station.stationuuid)
                                        } else if (value == SwipeToDismissBoxValue.StartToEnd) {
                                            stationForAssignFilter.value = station
                                            scope.launch { dismissState.reset() }
                                        }
                                    },
                                    backgroundContent = {
                                        val direction = dismissState.dismissDirection
                                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.tertiary)
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.CenterStart,
                                            ) {
                                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onTertiary)
                                            }
                                        } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.error)
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.CenterEnd,
                                            ) {
                                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onError)
                                            }
                                        }
                                    },
                                ) {
                                    Surface(shadowElevation = elevation, color = rowColor) {
                                        FavoriteRow(
                                            station = station,
                                            isCompact = isCompact,
                                            isEditMode = true,
                                            onClick = { appViewModel.play(station) },
                                            modifier = Modifier,
                                            dragHandleModifier = Modifier.draggableHandle(),
                                            isNewsStation = settings.scheduledNews.enabled && settings.scheduledNews.stationId == station.stationuuid,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // View mode: crossfade between filter views.
                    // Each AnimatedContent slot computes its own list snapshot from targetIndex so
                    // the outgoing slot keeps the old filtered list (fade-out) while the incoming
                    // slot immediately shows the new filtered list (fade-in).
                    AnimatedContent(
                        targetState = activeFilterIndex,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
                        },
                        label = "filter_crossfade",
                        modifier = Modifier.fillMaxSize(),
                    ) { targetIndex ->
                        val slotList = remember(favorites, filterNames, targetIndex) {
                            viewModel.filteredFavorites(favorites, filterNames, targetIndex)
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            state = if (targetIndex == activeFilterIndex) viewListState else rememberLazyListState(),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            itemsIndexed(slotList, key = { _, s -> s.stationuuid }) { _, station ->
                                Surface(color = MaterialTheme.colorScheme.background) {
                                    FavoriteRow(
                                        station = station,
                                        isCompact = isCompact,
                                        isEditMode = false,
                                        onClick = { appViewModel.play(station) },
                                        modifier = Modifier,
                                        dragHandleModifier = Modifier,
                                        isNewsStation = settings.scheduledNews.enabled && settings.scheduledNews.stationId == station.stationuuid,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete filter confirmation
    showDeleteFilterDialog.value?.let { name ->
        AlertDialog(
            onDismissRequest = { showDeleteFilterDialog.value = null },
            title = { Text(stringResource(R.string.favorites_delete_filter)) },
            text = { Text(stringResource(R.string.favorites_delete_filter_message, name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFilter(name)
                    if (activeFilterIndex >= filterNames.size - 1) activeFilterIndex = -1
                    showDeleteFilterDialog.value = null
                }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFilterDialog.value = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    // Reorder filters dialog (triggered by long-press on filter chips)
    if (showReorderFiltersDialog.value) {
        ReorderFiltersDialog(
            filterNames = filterNames,
            onReorder = { viewModel.reorderFilters(it) },
            onDelete = { viewModel.deleteFilter(it) },
            onDismiss = { showReorderFiltersDialog.value = false },
        )
    }

    // Assign filter dialog
    stationForAssignFilter.value?.let { station ->
        val assignedFilters = stationFilterMap[station.stationuuid] ?: emptyList()
        AssignFilterDialog(
            station = station,
            filterNames = filterNames,
            assignedFilters = assignedFilters,
            onToggle = { filterName -> viewModel.toggleStationFilter(station.stationuuid, filterName) },
            onDismiss = { stationForAssignFilter.value = null },
            onRenameStation = { newName -> viewModel.updateStationName(station.stationuuid, newName) },
        )
    }
}
