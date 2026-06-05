package com.herohan.uvcapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.herohan.uvcapp.R
import com.serenegiant.usb.Format
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFormatDialog(
    formatList: List<Format>,
    currentSize: Size?,
    onFormatSelected: (Size) -> Unit,
    onDismiss: () -> Unit,
) {
    val initData = remember(formatList, currentSize) {
        buildFormatData(formatList, currentSize)
    }

    var selectedType by remember { mutableIntStateOf(initData.initialType) }
    var selectedResolution by remember { mutableStateOf(initData.initialResolution) }
    var selectedFps by remember { mutableIntStateOf(initData.initialFps) }

    val typeNames = remember(initData) { initData.typeNames }
    val typeKeys = remember(initData) { initData.typeKeys }
    val resolutions = remember(selectedType, initData) {
        initData.getResolutions(selectedType)
    }
    val fpsList = remember(selectedType, selectedResolution, initData) {
        initData.getFpsList(selectedType, selectedResolution)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.video_format_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Format dropdown
            FormatDropdown(
                label = stringResource(R.string.video_format_format),
                options = typeNames,
                selectedIndex = typeKeys.indexOf(selectedType).coerceAtLeast(0),
                onSelected = { index ->
                    val newType = typeKeys[index]
                    if (newType != selectedType) {
                        selectedType = newType
                        val newResolutions = initData.getResolutions(newType)
                        selectedResolution = newResolutions.firstOrNull() ?: ""
                        val newFpsList = initData.getFpsList(newType, selectedResolution)
                        selectedFps = newFpsList.firstOrNull() ?: 0
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Resolution dropdown
            FormatDropdown(
                label = stringResource(R.string.video_format_resolution),
                options = resolutions,
                selectedIndex = resolutions.indexOf(selectedResolution).coerceAtLeast(0),
                onSelected = { index ->
                    val newResolution = resolutions[index]
                    if (newResolution != selectedResolution) {
                        selectedResolution = newResolution
                        val newFpsList = initData.getFpsList(selectedType, newResolution)
                        selectedFps = newFpsList.firstOrNull() ?: 0
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Frame rate dropdown
            FormatDropdown(
                label = stringResource(R.string.video_format_frame_rate),
                options = fpsList.map { it.toString() },
                selectedIndex = fpsList.indexOf(selectedFps).coerceAtLeast(0),
                onSelected = { index ->
                    selectedFps = fpsList[index]
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.video_format_cancel_button))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val parts = selectedResolution.split("x")
                        if (parts.size == 2) {
                            val size = Size(
                                selectedType,
                                parts[0].toIntOrNull() ?: 640,
                                parts[1].toIntOrNull() ?: 480,
                                selectedFps,
                                ArrayList(fpsList),
                            )
                            onFormatSelected(size)
                        }
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.video_format_ok_button))
                }
            }
        }
    }
}

@Composable
private fun FormatDropdown(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val safeIndex = selectedIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = options.getOrElse(safeIndex) { "" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

private data class FormatInitData(
    val typeKeys: List<Int>,
    val typeNames: List<String>,
    private val typeToResolutions: Map<Int, LinkedHashMap<String, List<Int>>>,
    val initialType: Int,
    val initialResolution: String,
    val initialFps: Int,
) {
    fun getResolutions(type: Int): List<String> =
        typeToResolutions[type]?.keys?.toList() ?: emptyList()

    fun getFpsList(type: Int, resolution: String): List<Int> =
        typeToResolutions[type]?.get(resolution) ?: emptyList()
}

private fun buildFormatData(formatList: List<Format>, currentSize: Size?): FormatInitData {
    val typeToName = linkedMapOf<Int, String>()
    val typeToResolutions = linkedMapOf<Int, LinkedHashMap<String, List<Int>>>()

    for (format in formatList) {
        val type = when (format.type) {
            UVCCamera.UVC_VS_FORMAT_UNCOMPRESSED -> UVCCamera.UVC_VS_FRAME_UNCOMPRESSED
            UVCCamera.UVC_VS_FORMAT_MJPEG -> UVCCamera.UVC_VS_FRAME_MJPEG
            else -> continue
        }
        if (!typeToName.containsKey(type)) {
            typeToName[type] = when (type) {
                UVCCamera.UVC_VS_FRAME_UNCOMPRESSED -> "YUV"
                UVCCamera.UVC_VS_FRAME_MJPEG -> "MJPEG"
                else -> "Unknown"
            }
            typeToResolutions[type] = linkedMapOf()
        }
        for (descriptor in format.frameDescriptors) {
            val fpsList = descriptor.intervals.map { it.fps }
            typeToResolutions[type]?.put("${descriptor.width}x${descriptor.height}", fpsList)
        }
    }

    val typeKeys = typeToName.keys.toList()
    val typeNames = typeToName.values.toList()

    val initialType = currentSize?.type ?: typeKeys.firstOrNull() ?: 0
    val initialResolutions = typeToResolutions[initialType]?.keys?.toList() ?: emptyList()
    val initialResolution = if (currentSize != null) {
        "${currentSize.width}x${currentSize.height}"
    } else {
        initialResolutions.firstOrNull() ?: "640x480"
    }.let { res ->
        if (res in initialResolutions) res else initialResolutions.firstOrNull() ?: "640x480"
    }

    val initialFpsList = typeToResolutions[initialType]?.get(initialResolution) ?: emptyList()
    val initialFps = currentSize?.fps?.takeIf { it in initialFpsList }
        ?: initialFpsList.firstOrNull() ?: 30

    return FormatInitData(
        typeKeys = typeKeys,
        typeNames = typeNames,
        typeToResolutions = typeToResolutions,
        initialType = initialType,
        initialResolution = initialResolution,
        initialFps = initialFps,
    )
}
