package com.herohan.uvcdemo

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herohan.uvcdemo.ui.BaseCameraViewModel
import com.herohan.uvcdemo.ui.CameraPreview
import com.herohan.uvcdemo.ui.DeviceListDialog
import com.herohan.uvcdemo.ui.theme.UVCAndroidTheme
import kotlinx.coroutines.delay

class MultiCameraNewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UVCAndroidTheme {
                MultiCameraNewScreen()
            }
        }
    }
}

@Composable
private fun MultiCameraNewScreen() {
    val viewModelLeft: BaseCameraViewModel = viewModel(key = "left_new")
    val viewModelRight: BaseCameraViewModel = viewModel(key = "right_new")
    val uiStateLeft by viewModelLeft.uiState.collectAsStateWithLifecycle()
    val uiStateRight by viewModelRight.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Serial queue state for device selection
    var pendingDevice by remember { mutableStateOf<UsbDevice?>(null) }
    var pendingTarget by remember { mutableStateOf<String?>(null) } // "left" or "right"

    // Toast handling for left camera
    LaunchedEffect(uiStateLeft.toastMessage) {
        uiStateLeft.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModelLeft.clearToast()
        }
    }

    // Toast handling for right camera
    LaunchedEffect(uiStateRight.toastMessage) {
        uiStateRight.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModelRight.clearToast()
        }
    }

    // Process pending device selection serially
    LaunchedEffect(pendingDevice, pendingTarget) {
        val device = pendingDevice ?: return@LaunchedEffect
        val target = pendingTarget ?: return@LaunchedEffect

        // Wait a moment to avoid permission dialog conflicts
        delay(500)

        when (target) {
            "left" -> {
                if (uiStateLeft.isCameraConnected) viewModelLeft.safelyEject()
                viewModelLeft.selectDevice(device)
            }
            "right" -> {
                if (uiStateRight.isCameraConnected) viewModelRight.safelyEject()
                viewModelRight.selectDevice(device)
            }
        }

        pendingDevice = null
        pendingTarget = null
    }

    Scaffold(
        topBar = {
            MultiCameraNewTopAppBar()
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues),
        ) {
            // Two cameras side by side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Left camera
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    if (uiStateLeft.isCameraConnected) {
                        CameraPreview(
                            onAddSurface = { surface -> viewModelLeft.addSurface(surface, false) },
                            onRemoveSurface = { surface -> viewModelLeft.removeSurface(surface) },
                            previewWidth = uiStateLeft.previewWidth,
                            previewHeight = uiStateLeft.previewHeight,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.multi_camera_left_empty),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Right camera
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    if (uiStateRight.isCameraConnected) {
                        CameraPreview(
                            onAddSurface = { surface -> viewModelRight.addSurface(surface, false) },
                            onRemoveSurface = { surface -> viewModelRight.removeSurface(surface) },
                            previewWidth = uiStateRight.previewWidth,
                            previewHeight = uiStateRight.previewHeight,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.multi_camera_right_empty),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Buttons for each camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = {
                    pendingTarget = "left"
                    viewModelLeft.showDeviceListDialog()
                }) {
                    Text(stringResource(R.string.multi_camera_open_left))
                }
                Button(onClick = { viewModelLeft.safelyEject() }) {
                    Text(stringResource(R.string.multi_camera_close_left))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = {
                    pendingTarget = "right"
                    viewModelRight.showDeviceListDialog()
                }) {
                    Text(stringResource(R.string.multi_camera_open_right))
                }
                Button(onClick = { viewModelRight.safelyEject() }) {
                    Text(stringResource(R.string.multi_camera_close_right))
                }
            }
        }
    }

    // Device list dialog for left camera
    if (uiStateLeft.showDeviceListDialog) {
        DeviceListDialog(
            deviceList = uiStateLeft.deviceList,
            currentDevice = uiStateLeft.selectedDevice,
            onDeviceSelected = { device ->
                pendingDevice = device
                // Don't select immediately - let the serial queue handle it
            },
            onDismiss = { viewModelLeft.dismissDeviceListDialog() },
        )
    }

    // Device list dialog for right camera
    if (uiStateRight.showDeviceListDialog) {
        DeviceListDialog(
            deviceList = uiStateRight.deviceList,
            currentDevice = uiStateRight.selectedDevice,
            onDeviceSelected = { device ->
                pendingDevice = device
                // Don't select immediately - let the serial queue handle it
            },
            onDismiss = { viewModelRight.dismissDeviceListDialog() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiCameraNewTopAppBar() {
    TopAppBar(
        title = { Text(stringResource(R.string.entry_multi_camera_new)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
