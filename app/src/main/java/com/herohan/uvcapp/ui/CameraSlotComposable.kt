package com.herohan.uvcapp.ui

import android.hardware.usb.UsbDevice
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.herohan.uvcapp.R
import com.herohan.uvcapp.utils.identityKey
import com.herohan.uvcapp.utils.TimeFormatter
import android.widget.Toast
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState

@Composable
fun CameraSlotComposable(
    state: CameraSlotState,
    controller: CameraSlotController,
    allDevices: List<UsbDevice>,
    isMultiSlot: Boolean,
    onEnqueueDevice: (String, UsbDevice) -> Unit,
    onRemoveSlot: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Read recording time from dedicated flow — bypasses syncSlotStates for performance
    val recordTime by controller.recordTimeMillis.collectAsState()

    // Stable lambda references to avoid unnecessary recompositions
    val onAddSurface = remember(controller) {
        { surface: Any -> controller.addSurface(surface, false) }
    }
    val onRemoveSurface = remember(controller) {
        { surface: Any -> controller.removeSurface(surface) }
    }
    val onEnqueue = remember(state.slotId, onEnqueueDevice) {
        { device: UsbDevice -> onEnqueueDevice(state.slotId, device) }
    }

    // Recording timer — keyed by slotId to prevent recycling issues
    var recordStartTime by remember(state.slotId) { mutableStateOf(0L) }
    LaunchedEffect(state.slotId, state.isRecording) {
        if (state.isRecording) {
            recordStartTime = SystemClock.elapsedRealtime()
            while (true) {
                delay(250)
                controller.updateRecordTime(SystemClock.elapsedRealtime() - recordStartTime)
            }
        } else if (recordStartTime != 0L) {
            controller.updateRecordTime(0)
            recordStartTime = 0L
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        if (state.isCameraConnected) {
            CameraPreview(
                onAddSurface = onAddSurface,
                onRemoveSurface = onRemoveSurface,
                previewWidth = state.previewWidth,
                previewHeight = state.previewHeight,
                onFirstFrame = remember(controller) { { controller.confirmPreviewStarted() } },
                modifier = Modifier.align(Alignment.Center),
            )

            // Recording timer
            if (state.isRecording) {
                Text(
                    text = TimeFormatter.formatRecordTimeMMSS(recordTime.toInt()),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    style = if (isMultiSlot) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.headlineMedium,
                    color = Color.Red,
                )
            }

            // Top bar overlay for multi-slot mode
            if (isMultiSlot) {
                SlotTopBar(
                    state = state,
                    onControlsClick = remember(controller) {
                        { controller.showCameraControlsDialog() }
                    },
                    onDeviceClick = remember(controller) {
                        { controller.showDeviceListDialog() }
                    },
                    onSafelyEjectClick = remember(controller) {
                        { controller.safelyEject() }
                    },
                    onVideoFormatClick = remember(controller) {
                        { controller.showVideoFormatDialog() }
                    },
                    onRotateCWClick = remember(controller) {
                        { controller.rotateBy(90) }
                    },
                    onRotateCCWClick = remember(controller) {
                        { controller.rotateBy(-90) }
                    },
                    onFlipHorizontalClick = remember(controller) {
                        { controller.flipHorizontally() }
                    },
                    onFlipVerticalClick = remember(controller) {
                        { controller.flipVertically() }
                    },
                    onCloseClick = onRemoveSlot,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }

            // FAB column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(if (isMultiSlot) 4.dp else 16.dp),
            ) {
                FloatingActionButton(
                    onClick = {
                        val activity = context as? android.app.Activity ?: return@FloatingActionButton
                        XXPermissions.with(activity)
                            .permission(PermissionLists.getRecordAudioPermission())
                            .request { _, allGranted ->
                                if (allGranted) {
                                    controller.toggleVideoRecord()
                                } else {
                                    Toast.makeText(activity, activity.getString(R.string.slot_audio_permission_required), Toast.LENGTH_SHORT).show()
                                }
                            }
                    },
                    containerColor = if (state.isRecording) Color.Red
                    else MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = stringResource(R.string.action_record),
                        tint = Color.White,
                        modifier = if (isMultiSlot) Modifier.size(20.dp) else Modifier,
                    )
                }

                Spacer(modifier = Modifier.height(if (isMultiSlot) 4.dp else 12.dp))

                FloatingActionButton(
                    onClick = {
                        controller.takePicture()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(R.string.action_take_picture),
                        tint = Color.White,
                        modifier = if (isMultiSlot) Modifier.size(20.dp) else Modifier,
                    )
                }
            }
        } else {
            Text(
                text = if (isMultiSlot) {
                    stringResource(R.string.multi_camera_slot_empty, state.slotIndex)
                } else {
                    stringResource(R.string.main_connect_usb_tip)
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(if (isMultiSlot) 16.dp else 32.dp),
                style = if (isMultiSlot) MaterialTheme.typography.bodyMedium
                else MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }

    // Per-slot dialogs
    if (state.showDeviceListDialog) {
        DeviceListDialog(
            deviceList = allDevices,
            currentDevice = allDevices.find { it.identityKey() == state.boundDeviceKey },
            onDeviceSelected = onEnqueue,
            onDismiss = remember(controller) { { controller.dismissDeviceListDialog() } },
        )
    }

    if (state.showCameraControlsDialog) {
        CameraControlsDialog(
            control = controller.getUvcControl(),
            onDismiss = remember(controller) { { controller.dismissCameraControlsDialog() } },
        )
    }

    if (state.showVideoFormatDialog) {
        VideoFormatDialog(
            formatList = state.supportedFormats,
            currentSize = state.currentPreviewSize,
            onFormatSelected = remember(controller) { { size -> controller.setPreviewSize(size) } },
            onDismiss = remember(controller) { { controller.dismissVideoFormatDialog() } },
        )
    }
}

@Composable
private fun SlotTopBar(
    state: CameraSlotState,
    onControlsClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onSafelyEjectClick: () -> Unit,
    onVideoFormatClick: () -> Unit,
    onRotateCWClick: () -> Unit,
    onRotateCCWClick: () -> Unit,
    onFlipHorizontalClick: () -> Unit,
    onFlipVerticalClick: () -> Unit,
    onCloseClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.multi_camera_slot_label, state.slotIndex),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IconButton(onClick = onControlsClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_control),
                    contentDescription = stringResource(R.string.action_control),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onDeviceClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = stringResource(R.string.action_usb),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.action_more),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_safely_eject)) },
                onClick = { showMenu = false; onSafelyEjectClick() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_close_camera)) },
                onClick = { showMenu = false; onCloseClick() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_video_format)) },
                onClick = { showMenu = false; onVideoFormatClick() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rotate_90_CW)) },
                onClick = { showMenu = false; onRotateCWClick() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_rotate_90_CCW)) },
                onClick = { showMenu = false; onRotateCCWClick() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_flip_horizontally)) },
                onClick = { showMenu = false; onFlipHorizontalClick() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_flip_vertically)) },
                onClick = { showMenu = false; onFlipVerticalClick() },
            )
        }
    }
}
