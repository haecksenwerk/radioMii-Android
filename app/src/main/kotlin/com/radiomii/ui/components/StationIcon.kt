package com.radiomii.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.radiomii.R

private val iconShape = RoundedCornerShape(8.dp)

@Composable
fun StationIcon(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(iconShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        AsyncImage(
            model = url.takeIf { it.isNotBlank() },
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.ic_radiomii_logo),
            error = painterResource(R.drawable.ic_radiomii_logo),
            fallback = painterResource(R.drawable.ic_radiomii_logo),
            modifier = Modifier.size(size),
        )
    }
}

