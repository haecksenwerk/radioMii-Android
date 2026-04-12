package com.radiomii.ui.screens.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.radiomii.R

/**
 * Dialog that searches for icon/favicon candidates from a station's homepage and
 * lets the user pick one of up to 3 results.
 *
 * @param homepageUrl  The station homepage URL to search.
 * @param onConfirm    Called with the chosen icon URL when the user taps a tile.
 * @param onDismiss    Called when the dialog is dismissed without a selection.
 * @param viewModel    The [NowPlayingViewModel] that drives the search state.
 */
@Composable
fun StationIconSearchDialog(
    homepageUrl: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: NowPlayingViewModel,
) {
    val state by viewModel.iconSearchState.collectAsState()

    // Kick off the search as soon as the dialog is shown
    LaunchedEffect(homepageUrl) {
        viewModel.searchFavicons(homepageUrl)
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.resetIconSearch()
            onDismiss()
        },
        title = { Text(stringResource(R.string.overlay_find_station_icon)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (val s = state) {
                    is IconSearchState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = stringResource(R.string.overlay_find_icon_searching),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is IconSearchState.Results -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.overlay_find_icon_pick),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            ) {
                                s.urls.forEach { url ->
                                    IconTile(
                                        url = url,
                                        onClick = { onConfirm(url) },
                                    )
                                }
                            }
                        }
                    }

                    is IconSearchState.NoResults -> {
                        Text(
                            text = stringResource(R.string.overlay_find_icon_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    is IconSearchState.Error -> {
                        Text(
                            text = stringResource(R.string.overlay_find_icon_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    else -> {} // Idle — nothing to show
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                viewModel.resetIconSearch()
                onDismiss()
            }) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun IconTile(
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(88.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            placeholder = painterResource(R.drawable.ic_radiomii_logo),
            error = painterResource(R.drawable.ic_radiomii_logo),
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

