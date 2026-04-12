package com.radiomii.ui.screens.nowplaying

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.ClipEntry
import androidx.core.net.toUri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomii.R
import com.radiomii.domain.model.MusicProvider
import com.radiomii.domain.model.isMusicContent
import com.radiomii.ui.components.SleepTimerButton
import com.radiomii.ui.components.StationIcon
import com.radiomii.ui.AppViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    appViewModel: AppViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val station by viewModel.activeStation.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimer.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val density = LocalDensity.current

    val isFavorite = favorites.any { it.stationuuid == station?.stationuuid }
    val isNewsStation = settings.scheduledNews.enabled &&
        settings.scheduledNews.stationId == station?.stationuuid
    val isPlayingNews by viewModel.isPlayingNews.collectAsStateWithLifecycle()
    val showSkipButton = isPlayingNews && settings.scheduledNews.skipWhenPaused

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val voteSuccessMsg = stringResource(R.string.overlay_vote_success)
    val voteAlreadyMsg = stringResource(R.string.overlay_vote_already)
    val voteErrorMsg = stringResource(R.string.overlay_vote_error)

    val showSleepTimerDialog = remember { mutableStateOf(false) }
    val showNewsDialog = remember { mutableStateOf(false) }
    val showIconSearchDialog = remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    // Intercept back gesture to dismiss the sheet (Support for Predictive Back)
    BackHandler(onBack = onDismiss)

    // Swipe-to-dismiss state
    val offsetAnim = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 100.dp.toPx() }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (offsetAnim.value > dismissThresholdPx) {
                    onDismiss()
                } else {
                    scope.launch {
                        offsetAnim.animateTo(0f, animationSpec = spring())
                    }
                }
            },
            onDragCancel = {
                scope.launch {
                    offsetAnim.animateTo(0f, animationSpec = spring())
                }
            },
            onVerticalDrag = { change, dragAmount ->
                if (dragAmount > 0 || offsetAnim.value > 0) {
                    change.consume()
                    scope.launch {
                        offsetAnim.snapTo(
                            (offsetAnim.value + dragAmount).coerceAtLeast(0f)
                        )
                    }
                }
            },
        )
    }

    LaunchedEffect(Unit) {
        // showSnackbar is already a suspend function — no nested launch needed
        viewModel.voteEvent.collect { outcome ->
            val msg = when (outcome) {
                is VoteOutcome.Success -> voteSuccessMsg
                is VoteOutcome.AlreadyVoted -> voteAlreadyMsg
                is VoteOutcome.Error -> voteErrorMsg
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetAnim.value.roundToInt().coerceAtLeast(0)) }
            .then(dragModifier),
    ) {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(Color.Transparent)
                ) {
                    // ── Top app bar ───────────────────────────────────────
                    TopAppBar(
                        windowInsets = WindowInsets(0),
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        },
                        title = {},
                        actions = {
                            // Music provider open button
                            val showFindOn = settings.showFindOnButton &&
                                metadata != null &&
                                metadata!!.hasContent &&
                                isPlaying &&
                                (!settings.useMusicDetection || metadata!!.isMusicContent())
                            if (showFindOn) {
                                IconButton(onClick = {
                                    val artist = metadata!!.artist
                                    val title = metadata!!.title
                                    val query = URLEncoder.encode(
                                        "$artist $title".trim(),
                                        "UTF-8",
                                    )
                                    val uri = when (settings.findOnProvider) {
                                        MusicProvider.SPOTIFY ->
                                            "https://open.spotify.com/search/$query".toUri()
                                        MusicProvider.YOUTUBE ->
                                            "https://music.youtube.com/search?q=$query".toUri()
                                    }
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }) {
                                    Icon(
                                        imageVector = when (settings.findOnProvider) {
                                            MusicProvider.SPOTIFY -> Icons.Default.MusicNote
                                            MusicProvider.YOUTUBE -> Icons.Default.PlayCircle
                                        },
                                        contentDescription = when (settings.findOnProvider) {
                                            MusicProvider.SPOTIFY -> "Open in Spotify"
                                            MusicProvider.YOUTUBE -> "Open in YouTube Music"
                                        },
                                    )
                                }
                            }

                            // News button — only shown when station is in favorites
                            if (isFavorite) {
                                IconButton(onClick = { showNewsDialog.value = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Newspaper,
                                        contentDescription = stringResource(R.string.overlay_scheduled_news),
                                        tint = if (isNewsStation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }

                            // Favorite button
                            IconButton(onClick = { viewModel.toggleFavorite() }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            // 3-dot overflow menu
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.overlay_vote)) },
                                        onClick = { viewModel.vote(); menuExpanded = false },
                                        leadingIcon = { Icon(Icons.Outlined.ThumbUp, null) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.overlay_share_station)) },
                                        onClick = {
                                            station?.homepage?.takeIf { it.isNotBlank() }?.let { url ->
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, url)
                                                }
                                                context.startActivity(Intent.createChooser(intent, null))
                                            }
                                            menuExpanded = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Share, null) },
                                    )
                                    val metaDisplay = metadata?.display
                                    if (!metaDisplay.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.overlay_copy_song_info)) },
                                            onClick = {
                                                scope.launch {
                                                    clipboard.setClipEntry(
                                                        ClipEntry(ClipData.newPlainText("", metaDisplay))
                                                    )
                                                }
                                                menuExpanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                        )
                                    }
                                    if (!station?.homepage.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.overlay_open_homepage)) },
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, station?.homepage.orEmpty().toUri())
                                                context.startActivity(intent)
                                                menuExpanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.OpenInBrowser, null) },
                                        )
                                    }
                                    if (isFavorite && !station?.homepage.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.overlay_find_station_icon)) },
                                            onClick = {
                                                showIconSearchDialog.value = true
                                                menuExpanded = false
                                            },
                                            leadingIcon = { Icon(Icons.Default.ImageSearch, null) },
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Top part
                    Column(
                        modifier = Modifier
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                    Spacer(Modifier.height(8.dp))

                    // Station image
                    var containerWidthPx by remember { mutableIntStateOf(0) }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { containerWidthPx = it.width },
                    ) {
                        val imageSize = if (containerWidthPx > 0)
                            with(density) { containerWidthPx.toDp() - 48.dp }.coerceAtLeast(120.dp)
                        else 200.dp
                        StationIcon(
                            url = station?.favicon ?: "",
                            contentDescription = station?.name,
                            size = imageSize,
                        )
                    }

                    Spacer(Modifier.height(40.dp))

                    Text(
                        text = station?.name ?: stringResource(R.string.common_live_radio),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.TopStart) {
                        val metaDisplay = metadata?.display
                        if (!metaDisplay.isNullOrBlank()) {
                            Text(
                                text = metaDisplay,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth().basicMarquee(),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Station tag chips
                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterStart) {
                        station?.let { s ->
                            val chipLabels = buildList {
                                if (s.country.isNotBlank()) add(s.country)
                                if (s.state.isNotBlank()) add(s.state)
                                if (s.url.startsWith("https")) add("HTTPS")
                                val codecBitrate = when {
                                    s.codec.isNotBlank() && s.bitrate > 0 -> "${s.codec} ${s.bitrate}k"
                                    s.bitrate > 0 -> "${s.bitrate}k"
                                    s.codec.isNotBlank() -> s.codec
                                    else -> null
                                }
                                if (codecBitrate != null) add(codecBitrate)
                                s.tagList.forEach { add(it) }
                            }
                            if (chipLabels.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    chipLabels.forEach { label ->
                                        SuggestionChip(onClick = {}, label = { Text(label) })
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    } // End of top part

                    // Play/Pause/Skip button area
                    // Fixed height footer to prevent jumping when metadata changes and to center
                    // between chips and the sleep button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (showSkipButton) viewModel.skipCurrentNews()
                                else appViewModel.togglePlayPause()
                            },
                            modifier = Modifier.size(72.dp),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else if (showSkipButton) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Skip news",
                                    modifier = Modifier.size(48.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop" else "Play",
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        }
                    }
                }

                // Sleep timer button
                SleepTimerButton(
                    state = sleepTimer,
                    onShortPress = { showSleepTimerDialog.value = true },
                    onLongPress = { viewModel.resetSleepTimer() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    size = 56.dp,
                )
            }
        }
    }

    if (showSleepTimerDialog.value) {
        SleepTimerDialog(
            timerState = sleepTimer,
            onStart = { minutes, cancelOnStop -> appViewModel.startSleepTimer(minutes, cancelOnStop) },
            onReset = { appViewModel.resetSleepTimer() },
            onDismiss = {
                showSleepTimerDialog.value = false
            },
        )
    }

    if (showNewsDialog.value && station != null) {
        val s = station
        if (s != null) {
            val newsForStation = settings.scheduledNews.copy(
                enabled = settings.scheduledNews.enabled &&
                    settings.scheduledNews.stationId == s.stationuuid,
                stationId = s.stationuuid,
                stationName = s.name,
            )
            NewsScheduleDialog(
                scheduledNews = newsForStation,
                onSave = { news ->
                    appViewModel.setScheduledNews(news)
                    showNewsDialog.value = false
                },
                onDismiss = {
                    showNewsDialog.value = false
                },
            )
        }
    }

    if (showIconSearchDialog.value && station != null) {
        val s = station
        if (s != null) {
        StationIconSearchDialog(
            homepageUrl = s.homepage,
            onConfirm = { chosenUrl ->
                viewModel.applyFaviconChoice(chosenUrl)
                showIconSearchDialog.value = false
            },
            onDismiss = {
                showIconSearchDialog.value = false
            },
            viewModel = viewModel,
        )
        }
    }
}
