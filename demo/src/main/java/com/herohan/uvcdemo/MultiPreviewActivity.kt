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

class MultiPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UVCAndroidTheme {
                MultiPreviewScreen()
            }
        }
    }
}

@Composable
private fun MultiPreviewScreen(
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

    Scaffold(
        topBar = {
            MultiPreviewTopAppBar()
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues),
        ) {
            if (uiState.isCameraConnected) {
                // 2x2 grid of camera previews
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        CameraPreview(
                            onAddSurface = { surface -> viewModel.addSurface(surface, false) },
                            onRemoveSurface = { surface -> viewModel.removeSurface(surface) },
                            previewWidth = uiState.previewWidth,
                            previewHeight = uiState.previewHeight,
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                        )
                        CameraPreview(
                            onAddSurface = { surface -> viewModel.addSurface(surface, false) },
                            onRemoveSurface = { surface -> viewModel.removeSurface(surface) },
                            previewWidth = uiState.previewWidth,
                            previewHeight = uiState.previewHeight,
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        CameraPreview(
                            onAddSurface = { surface -> viewModel.addSurface(surface, false) },
                            onRemoveSurface = { surface -> viewModel.removeSurface(surface) },
                            previewWidth = uiState.previewWidth,
                            previewHeight = uiState.previewHeight,
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                        )
                        CameraPreview(
                            onAddSurface = { surface -> viewModel.addSurface(surface, false) },
                            onRemoveSurface = { surface -> viewModel.removeSurface(surface) },
                            previewWidth = uiState.previewWidth,
                            previewHeight = uiState.previewHeight,
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
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
private fun MultiPreviewTopAppBar() {
    TopAppBar(
        title = { Text(stringResource(R.string.entry_multi_preview)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
