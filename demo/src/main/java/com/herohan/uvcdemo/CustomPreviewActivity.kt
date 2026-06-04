package com.herohan.uvcdemo

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herohan.uvcdemo.ui.BaseCameraViewModel
import com.herohan.uvcdemo.ui.CameraControlsDialog
import com.herohan.uvcdemo.ui.CameraPreview
import com.herohan.uvcdemo.ui.DeviceListDialog
import com.herohan.uvcdemo.ui.VideoFormatDialog
import com.herohan.uvcdemo.ui.theme.UVCAndroidTheme

class CustomPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UVCAndroidTheme {
                CustomPreviewScreen()
            }
        }
    }
}

@Composable
private fun CustomPreviewScreen(
    viewModel: BaseCameraViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isLandscape by rememberSaveable { mutableStateOf(false) }

    val toggleLandscape: () -> Unit = {
        val activity = context as ComponentActivity
        isLandscape = !isLandscape
        activity.requestedOrientation = if (isLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Toast handling
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            CustomPreviewTopAppBar(
                isCameraConnected = uiState.isCameraConnected,
                isLandscape = isLandscape,
                onControlsClick = { viewModel.showCameraControlsDialog() },
                onDeviceClick = { viewModel.showDeviceListDialog() },
                onVideoFormatClick = { viewModel.showVideoFormatDialog() },
                onRotateCWClick = { viewModel.rotateBy(90) },
                onRotateCCWClick = { viewModel.rotateBy(-90) },
                onFlipHorizontalClick = { viewModel.flipHorizontally() },
                onFlipVerticalClick = { viewModel.flipVertically() },
                onLandscapeClick = toggleLandscape,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues),
        ) {
            if (uiState.isCameraConnected) {
                CameraPreview(
                    onAddSurface = { surface -> viewModel.addSurface(surface, false) },
                    onRemoveSurface = { surface -> viewModel.removeSurface(surface) },
                    previewWidth = uiState.previewWidth,
                    previewHeight = uiState.previewHeight,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Text(
                    text = stringResource(R.string.main_connect_usb_tip),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // Bottom buttons
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Button(onClick = { viewModel.showDeviceListDialog() }) {
            Text(stringResource(R.string.btn_open_camera))
        }
        Button(onClick = { viewModel.safelyEject() }) {
            Text(stringResource(R.string.btn_close_camera))
        }
    }

    // Dialogs
    if (uiState.showDeviceListDialog) {
        DeviceListDialog(
            deviceList = uiState.deviceList,
            currentDevice = uiState.selectedDevice,
            onDeviceSelected = { device ->
                if (uiState.isCameraConnected) viewModel.safelyEject()
                viewModel.selectDevice(device)
            },
            onDismiss = { viewModel.dismissDeviceListDialog() },
        )
    }

    if (uiState.showCameraControlsDialog) {
        CameraControlsDialog(
            control = viewModel.getUvcControl(),
            onDismiss = { viewModel.dismissCameraControlsDialog() },
        )
    }

    if (uiState.showVideoFormatDialog) {
        VideoFormatDialog(
            formatList = uiState.supportedFormats,
            currentSize = uiState.currentPreviewSize,
            onFormatSelected = { size -> viewModel.setPreviewSize(size) },
            onDismiss = { viewModel.dismissVideoFormatDialog() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomPreviewTopAppBar(
    isCameraConnected: Boolean,
    isLandscape: Boolean,
    onControlsClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onVideoFormatClick: () -> Unit,
    onRotateCWClick: () -> Unit,
    onRotateCCWClick: () -> Unit,
    onFlipHorizontalClick: () -> Unit,
    onFlipVerticalClick: () -> Unit,
    onLandscapeClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(stringResource(R.string.entry_custom_preview)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        actions = {
            IconButton(onClick = onDeviceClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_control),
                    contentDescription = stringResource(R.string.action_control),
                )
            }

            if (isCameraConnected) {
                IconButton(onClick = onLandscapeClick) {
                    Icon(
                        imageVector = if (isLandscape) Icons.Default.StayCurrentPortrait
                        else Icons.Default.StayCurrentLandscape,
                        contentDescription = stringResource(
                            if (isLandscape) R.string.action_portrait
                            else R.string.action_landscape
                        ),
                    )
                }
            }

            if (isCameraConnected) {
                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_control),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_control)) },
                            onClick = { showMenu = false; onControlsClick() },
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
        },
    )
}
