package com.radiomii.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.radiomii.R
import com.radiomii.domain.model.Station
import java.util.UUID

private val CODEC_OPTIONS = listOf("MP3", "AAC", "OGG", "Other")
private val BITRATE_OPTIONS = listOf(0, 128, 192, 256, 320)

@Composable
fun CustomStreamDialog(
    onAdd: (Station) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var homepage by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var selectedCodec by remember { mutableStateOf("MP3") }
    var selectedBitrate by remember { mutableIntStateOf(128) }

    val nameError = name.trim().isEmpty()
    val urlError = url.trim().isEmpty() || !url.trim().startsWith("http")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_stream_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.custom_stream_name)) },
                    singleLine = true,
                    isError = nameError && name.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.custom_stream_url)) },
                    singleLine = true,
                    isError = urlError && url.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    supportingText = {
                        if (urlError && url.isNotEmpty()) {
                            Text(stringResource(R.string.custom_stream_url_invalid))
                        }
                    },
                )

                OutlinedTextField(
                    value = homepage,
                    onValueChange = { homepage = it },
                    label = { Text(stringResource(R.string.custom_stream_homepage)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Codec selector
                Text(stringResource(R.string.custom_stream_codec), style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    CODEC_OPTIONS.forEachIndexed { index, codec ->
                        SegmentedButton(
                            selected = selectedCodec == codec,
                            onClick = { selectedCodec = codec },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = CODEC_OPTIONS.size),
                            icon = {},
                        ) {
                            Text(codec, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bitrate selector
                Text(stringResource(R.string.custom_stream_bitrate), style = MaterialTheme.typography.bodyMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    BITRATE_OPTIONS.forEachIndexed { index, bitrate ->
                        SegmentedButton(
                            selected = selectedBitrate == bitrate,
                            onClick = { selectedBitrate = bitrate },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = BITRATE_OPTIONS.size),
                            icon = {},
                        ) {
                            Text(if (bitrate == 0) "–" else "$bitrate", maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text(stringResource(R.string.custom_stream_tags)) },
                    placeholder = { Text(stringResource(R.string.custom_stream_tags_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = countryCode,
                    onValueChange = { countryCode = it.uppercase().take(2) },
                    label = { Text(stringResource(R.string.custom_stream_country_code)) },
                    placeholder = { Text(stringResource(R.string.custom_stream_country_code_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val station = Station(
                        stationuuid = UUID.randomUUID().toString(),
                        name = name.trim(),
                        url = url.trim(),
                        urlResolved = url.trim(),
                        homepage = homepage.trim(),
                        favicon = "",
                        tags = tagsInput.trim(),
                        country = "",
                        countrycode = countryCode.trim(),
                        codec = selectedCodec,
                        bitrate = selectedBitrate,
                        votes = 0,
                        isCustom = true,
                    )
                    onAdd(station)
                    onDismiss()
                },
                enabled = !nameError && !urlError,
            ) {
                Text(stringResource(R.string.custom_stream_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
