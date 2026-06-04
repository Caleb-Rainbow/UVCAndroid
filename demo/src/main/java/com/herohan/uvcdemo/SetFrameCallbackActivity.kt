package com.herohan.uvcdemo

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.serenegiant.usb.UVCCamera
import kotlinx.coroutines.delay
import java.text.DecimalFormat

class SetFrameCallbackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UVCAndroidTheme {
                SetFrameCallbackScreen()
            }
        }
    }
}

@Composable
private fun SetFrameCallbackScreen(
    viewModel: BaseCameraViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Toast handling
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // FPS tracking
    var frameCount by remember { mutableIntStateOf(0) }
    var fpsDisplay by remember { mutableStateOf("0.0 fps") }
    val decimalFormat = remember { DecimalFormat(" #.0' fps'") }

    LaunchedEffect(uiState.isCameraConnected) {
        if (uiState.isCameraConnected) {
            val cameraHelper = viewModel.getCameraHelper()
            val size = viewModel.getPreviewSize()
            if (cameraHelper != null && size != null) {
                cameraHelper.setFrameCallback({ frame ->
                    frameCount++
                }, UVCCamera.PIXEL_FORMAT_NV21)
            }

            // FPS counter loop
            var lastCount = 0
            while (true) {
                delay(1000)
                val currentCount = frameCount
                val fps = (currentCount - lastCount).toFloat()
                fpsDisplay = decimalFormat.format(fps)
                lastCount = currentCount
            }
        } else {
            frameCount = 0
            fpsDisplay = "0.0 fps"
        }
    }

    Scaffold(
        topBar = {
            SetFrameCallbackTopAppBar(fpsDisplay = fpsDisplay)
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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

            // Frame info area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.DarkGray)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isCameraConnected) {
                    val size = uiState.currentPreviewSize
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Frame Callback Info",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Format: NV21 | Size: ${size?.width ?: 0}x${size?.height ?: 0}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                        Text(
                            text = "Frames received: $frameCount | $fpsDisplay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                } else {
                    Text(
                        text = "No frame data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                    )
                }
            }

            // Buttons
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetFrameCallbackTopAppBar(fpsDisplay: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.entry_set_frame_callback))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fpsDisplay,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
