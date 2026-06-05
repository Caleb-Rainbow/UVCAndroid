package com.herohan.uvcapp.ui

import android.Manifest
import android.content.pm.ActivityInfo
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.herohan.uvcapp.R
import com.herohan.uvcapp.ui.theme.UVCAndroidTheme
import com.herohan.uvcapp.utils.identityKey

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()

        setContent {
            UVCAndroidTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen(
    viewModel: MultiCameraViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isLandscape by rememberSaveable { mutableStateOf(false) }

    val safeToast = rememberSafeToastTrigger()

    val toggleLandscape: () -> Unit = {
        (context as? ComponentActivity)?.let { activity ->
            isLandscape = !isLandscape
            activity.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    // Request CAMERA permission
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

    // Global toast handling
    LaunchedEffect(uiState.globalToastMessage) {
        uiState.globalToastMessage?.let { msg ->
            safeToast(msg)
            viewModel.clearGlobalToast()
        }
    }

    // Per-slot toast handling — keyed by slotId for stable identity
    uiState.slots.forEach { (slotId, slotState) ->
        key(slotId) {
            LaunchedEffect(slotId, slotState.toastMessage) {
                slotState.toastMessage?.let { msg ->
                    safeToast(msg)
                    viewModel.clearSlotToast(slotId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                isLandscape = isLandscape,
                onLandscapeClick = toggleLandscape,
                onOpenDeviceClick = { viewModel.showOpenDeviceDialog() },
                canAddSlot = uiState.slots.size < MultiCameraViewModel.MAX_SLOTS,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues),
        ) {
            when {
                uiState.slots.isEmpty() -> {
                    // No cameras — show tip and open button
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.main_connect_usb_tip),
                            modifier = Modifier.padding(32.dp),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.showOpenDeviceDialog() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(stringResource(R.string.action_open_camera))
                        }
                    }
                }

                uiState.slots.size == 1 -> {
                    // Single camera — full screen
                    val (slotId, slotState) = uiState.slots.entries.first()
                    val singleController = viewModel.getSlotController(slotId) ?: return@Box
                    CameraSlotComposable(
                        state = slotState,
                        controller = singleController,
                        allDevices = uiState.allDevices,
                        isMultiSlot = false,
                        onEnqueueDevice = { id, device ->
                            viewModel.enqueueDeviceSelection(id, device)
                        },
                        onRemoveSlot = { viewModel.removeSlot(slotId) },
                        onRequestRecord = rememberSafeRecordRequest(singleController, context),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    // Multiple cameras — grid layout
                    MultiCameraGrid(
                        slots = uiState.slots,
                        allDevices = uiState.allDevices,
                        viewModel = viewModel,
                        isLandscape = isLandscape,
                        context = context,
                    )
                }
            }
        }
    }

    // Global device selection dialog (for opening a new camera)
    if (uiState.showOpenDeviceDialog) {
        val boundDeviceKeys = uiState.slots.values
            .mapNotNull { it.boundDeviceKey }
            .toSet()
        val availableDevices = uiState.allDevices.filter { it.identityKey() !in boundDeviceKeys }

        DeviceListDialog(
            deviceList = availableDevices,
            currentDevice = null,
            onDeviceSelected = { device ->
                viewModel.dismissOpenDeviceDialog()
                viewModel.openDevice(device)
            },
            onDismiss = { viewModel.dismissOpenDeviceDialog() },
        )
    }
}

@Composable
private fun MultiCameraGrid(
    slots: Map<String, CameraSlotState>,
    allDevices: List<UsbDevice>,
    viewModel: MultiCameraViewModel,
    isLandscape: Boolean,
    context: android.content.Context,
) {
    val columns = if (isLandscape) {
        if (slots.size <= 2) 2 else 3
    } else {
        1
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = slots.entries.toList(),
            key = { it.key },
        ) { (slotId, slotState) ->
            val gridController = viewModel.getSlotController(slotId) ?: return@items
            CameraSlotComposable(
                state = slotState,
                controller = gridController,
                allDevices = allDevices,
                isMultiSlot = true,
                onEnqueueDevice = { id, device ->
                    viewModel.enqueueDeviceSelection(id, device)
                },
                onRemoveSlot = { viewModel.removeSlot(slotId) },
                onRequestRecord = rememberSafeRecordRequest(gridController, context),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isLandscape) Modifier.aspectRatio(16f / 9f)
                        else Modifier
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopAppBar(
    isLandscape: Boolean,
    onOpenDeviceClick: () -> Unit,
    onLandscapeClick: () -> Unit,
    canAddSlot: Boolean,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        actions = {
            if (canAddSlot) {
                IconButton(onClick = onOpenDeviceClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.action_open_camera),
                    )
                }
            }
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
        },
    )
}

@Composable
private fun rememberSafeToastTrigger(): (String) -> Unit {
    val context = LocalContext.current
    val lifecycle = (context as? ComponentActivity)?.lifecycle
    return remember {
        { msg: String ->
            if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun rememberSafeRecordRequest(
    controller: CameraSlotController,
    context: android.content.Context,
): () -> Unit = remember(controller, context) {
    {
        val activity = context as? ComponentActivity ?: return@remember
        XXPermissions.with(activity)
            .permission(PermissionLists.getRecordAudioPermission())
            .request { _, allGranted ->
                if (allGranted) {
                    controller.toggleVideoRecord()
                } else {
                    Toast.makeText(activity, activity.getString(R.string.slot_audio_permission_required), Toast.LENGTH_SHORT).show()
                }
            }
    }
}
