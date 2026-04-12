package com.radiomii.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.radiomii.R
import com.radiomii.ui.navigation.Screen

@Composable
fun AppBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (Screen) -> Unit,
) {
    NavigationBar {
        // Favorites
        val favSelected = currentDestination?.hasRoute(Screen.Favorites::class) == true
        // Heartbeat animation: use an Animatable and run a single pulse when selected
        val favScale = remember { Animatable(if (favSelected) 1.2f else 1f) }
        LaunchedEffect(favSelected) {
            if (favSelected) {
                // quick up, slight down, then settle at selected scale
                favScale.animateTo(1.2f, animationSpec = tween(durationMillis = 160))
                favScale.animateTo(0.90f, animationSpec = tween(durationMillis = 150))
                favScale.animateTo(1f, animationSpec = tween(durationMillis = 170))
            }
        }
        NavigationBarItem(
            selected = favSelected,
            onClick = { onNavigate(Screen.Favorites) },
            icon = {
                Icon(
                    imageVector = if (favSelected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).scale(favScale.value),
                )
            },
            label = { Text(stringResource(R.string.nav_favorites)) },
        )

        // Search
        val searchSelected = currentDestination?.hasRoute(Screen.Search::class) == true
        val searchScale by animateFloatAsState(
            targetValue = if (searchSelected) 1.2f else 1f,
            animationSpec = tween(300),
            label = "search_scale",
        )

        // Circular movement for the magnifier: do exactly one full circle when selected
        val density = LocalDensity.current
        // reduced base radius so the movement is subtle and ends centered
        val radiusBaseDp = 4.dp
        val radiusBasePx = with(density) { radiusBaseDp.toPx() }

        val angleAnim = remember { Animatable(0f) }
        val radiusAnim = remember { Animatable(0f) }

        LaunchedEffect(searchSelected) {
            if (searchSelected) {
                // start at angle 0 and full radius, then animate angle to 360 and radius to 0 concurrently
                angleAnim.snapTo(0f)
                radiusAnim.snapTo(radiusBasePx)
                val duration = 700
                launch {
                    angleAnim.animateTo(360f, animationSpec = tween(durationMillis = duration, easing = LinearEasing))
                }
                launch {
                    radiusAnim.animateTo(0f, animationSpec = tween(durationMillis = duration, easing = LinearEasing))
                }
            } else {
                angleAnim.snapTo(0f)
                radiusAnim.snapTo(0f)
            }
        }

        val rad = Math.toRadians(angleAnim.value.toDouble())
        val offsetX = (cos(rad) * radiusAnim.value).roundToInt()
        val offsetY = (sin(rad) * radiusAnim.value).roundToInt()

        NavigationBarItem(
            selected = searchSelected,
            onClick = { onNavigate(Screen.Search) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            translationX = if (searchSelected) offsetX.toFloat() else 0f
                            translationY = if (searchSelected) offsetY.toFloat() else 0f
                        }
                        .scale(searchScale),
                )
            },
            label = { Text(stringResource(R.string.nav_search)) },
        )

        // Settings
        val settingsSelected = currentDestination?.hasRoute(Screen.Settings::class) == true ||
            currentDestination?.hasRoute(Screen.AppInfo::class) == true ||
            currentDestination?.hasRoute(Screen.LegalInfo::class) == true ||
            currentDestination?.hasRoute(Screen.Licenses::class) == true
        val settingsRotation by animateFloatAsState(
            targetValue = if (settingsSelected) 90f else 0f,
            animationSpec = tween(500),
            label = "settings_rotation",
        )
        NavigationBarItem(
            selected = settingsSelected,
            onClick = { onNavigate(Screen.Settings) },
            icon = {
                Icon(
                    imageVector = if (settingsSelected) Icons.Default.Settings else Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).rotate(settingsRotation),
                )
            },
            label = { Text(stringResource(R.string.nav_settings)) },
        )
    }
}
