package com.herohan.uvcapp.ui

import android.content.Intent
import android.Manifest
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.herohan.uvcapp.R
import com.herohan.uvcapp.ui.theme.UVCAndroidTheme
import com.herohan.uvcapp.utils.TimeFormatter
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UVCAndroidTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            @Suppress("DEPRECATION")
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            device?.let {
                // The ViewModel's StateCallback will handle onAttach
            }
        }
    }
}

@Composable
private fun MainScreen(
    viewModel: MainViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Request CAMERA permission on Android 16+ (required for USB video class devices)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onCameraPermissionGranted()
    }

    LaunchedEffect(Unit) {
        if (!uiState.hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Toast handling
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Recording timer
    var recordStartTime by remember { mutableStateOf(0L) }
    LaunchedEffect(uiState.isRecording) {
        if (uiState.isRecording) {
            recordStartTime = SystemClock.elapsedRealtime()
            while (true) {
                delay(250)
                viewModel.updateRecordTime(SystemClock.elapsedRealtime() - recordStartTime)
            }
        } else {
            viewModel.updateRecordTime(0)
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                isCameraConnected = uiState.isCameraConnected,
                onControlsClick = { viewModel.showCameraControlsDialog() },
                onDeviceClick = { viewModel.showDeviceListDialog() },
                onSafelyEjectClick = { viewModel.safelyEject() },
                onVideoFormatClick = { viewModel.showVideoFormatDialog() },
                onRotateCWClick = { viewModel.rotateBy(90) },
                onRotateCCWClick = { viewModel.rotateBy(-90) },
                onFlipHorizontalClick = { viewModel.flipHorizontally() },
                onFlipVerticalClick = { viewModel.flipVertically() },
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

                if (uiState.isRecording) {
                    Text(
                        text = TimeFormatter.formatRecordTimeMMSS(uiState.recordTimeMillis.toInt()),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Red,
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    FloatingActionButton(
                        onClick = {
                            XXPermissions.with(context as MainActivity)
                                .permission(PermissionLists.getManageExternalStoragePermission())
                                .permission(PermissionLists.getRecordAudioPermission())
                                .request { _, allGranted ->
                                    if (allGranted) viewModel.toggleVideoRecord()
                                }
                        },
                        containerColor = if (uiState.isRecording) Color.Red
                        else MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = stringResource(R.string.action_record),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FloatingActionButton(
                        onClick = {
                            XXPermissions.with(context as MainActivity)
                                .permission(PermissionLists.getManageExternalStoragePermission())
                                .request { _, allGranted ->
                                    if (allGranted) viewModel.takePicture()
                                }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.action_take_picture),
                        )
                    }
                }
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

@Composable
private fun MainTopAppBar(
    isCameraConnected: Boolean,
    onControlsClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onSafelyEjectClick: () -> Unit,
    onVideoFormatClick: () -> Unit,
    onRotateCWClick: () -> Unit,
    onRotateCCWClick: () -> Unit,
    onFlipHorizontalClick: () -> Unit,
    onFlipVerticalClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        actions = {
            if (isCameraConnected) {
                IconButton(onClick = onControlsClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_action_control),
                        contentDescription = stringResource(R.string.action_control),
                    )
                }
            }

            IconButton(onClick = onDeviceClick) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = stringResource(R.string.action_usb),
                )
            }

            if (isCameraConnected) {
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.action_more),
                    )
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
        },
    )
}
