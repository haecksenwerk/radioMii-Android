package com.radiomii.ui.screens.appinfo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.radiomii.BuildConfig
import com.radiomii.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    onBack: () -> Unit,
    onNavigateToLegal: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // ─── Logo ──────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))

            val logoScaleY = remember { Animatable(1f) }
            val logoScaleX = remember { Animatable(1f) }

            LaunchedEffect(Unit) {
                // squish on impact (parallel)
                kotlinx.coroutines.coroutineScope {
                    launch { logoScaleY.animateTo(0.8f, animationSpec = tween(durationMillis = 100)) }
                    launch { logoScaleX.animateTo(1.2f, animationSpec = tween(durationMillis = 100)) }
                }
                // Phase 3: spring back to natural shape
                kotlinx.coroutines.coroutineScope {
                    launch {
                        logoScaleY.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        )
                    }
                    launch {
                        logoScaleX.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        )
                    }
                }
            }

            Image(
                painter = painterResource(R.drawable.ic_radiomii_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleY = logoScaleY.value
                        scaleX = logoScaleX.value
                    },
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = (-20).dp),
            )
            Spacer(modifier = Modifier.height(32.dp))

            // ─── App Info section ──────────────────────────────────────────
            Text(
                text = stringResource(R.string.app_info_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                InfoRow(
                    icon = Icons.AutoMirrored.Filled.Label,
                    label = stringResource(R.string.app_info_version),
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.GIT_HASH})",
                )
                InfoRow(
                    painter = painterResource(R.drawable.ic_spinvdrs),
                    label = stringResource(R.string.app_info_author),
                    value = "haecksenwerk",
                )
                InfoRow(
                    icon = Icons.Default.Radio,
                    label = stringResource(R.string.app_info_radio_source),
                    value = "https://www.radio-browser.info/",
                )
                NavigationRow(
                    icon = Icons.Default.Gavel,
                    title = stringResource(R.string.app_info_licenses),
                    subtitle = stringResource(R.string.app_info_licenses_desc),
                    onClick = onNavigateToLicenses,
                )
                NavigationRow(
                    icon = Icons.Default.VerifiedUser,
                    title = stringResource(R.string.app_info_legal_info),
                    subtitle = stringResource(R.string.app_info_legal_info_desc),
                    onClick = onNavigateToLegal,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        when {
            painter != null -> {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NavigationRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
