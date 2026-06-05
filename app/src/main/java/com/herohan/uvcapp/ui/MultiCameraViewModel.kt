package com.herohan.uvcapp.ui

import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedHashMap

data class MultiCameraUiState(
    val hasCameraPermission: Boolean = false,
    val slots: Map<String, CameraSlotState> = emptyMap(),
    val allDevices: List<UsbDevice> = emptyList(),
    val globalToastMessage: String? = null,
    val showOpenDeviceDialog: Boolean = false,
)

class MultiCameraViewModel : ViewModel() {

    companion object {
        const val MAX_SLOTS = 4
    }

    private val _uiState = MutableStateFlow(MultiCameraUiState())
    val uiState: StateFlow<MultiCameraUiState> = _uiState.asStateFlow()

    private val _slots = LinkedHashMap<String, CameraSlotController>()
    private val _deviceMutex = Mutex()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var _nextSlotIndex = 0

    /** Scanner helper that is always registered for USB device detection. */
    private var _scannerHelper: ICameraHelper? = null

    init {
        initScannerHelper()
    }

    private fun initScannerHelper() {
        val helper = CameraHelper()
        val callback = object : ICameraHelper.StateCallback {
            override fun onAttach(device: UsbDevice) {
                mainHandler.post {
                    refreshAllDevices()
                }
            }

            override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {}

            override fun onCameraOpen(device: UsbDevice) {}

            override fun onCameraClose(device: UsbDevice) {}

            override fun onDeviceClose(device: UsbDevice) {}

            override fun onDetach(device: UsbDevice) {
                mainHandler.post {
                    refreshAllDevices()
                    // Remove the slot that was bound to this device
                    val slotToRemove = _slots.entries.find {
                        it.value.state.value.boundDevice?.deviceId == device.deviceId
                    }
                    slotToRemove?.let { (key, controller) ->
                        controller.release()
                        _slots.remove(key)
                        syncSlotStates()
                    }
                }
            }

            override fun onCancel(device: UsbDevice) {}
        }
        helper.setStateCallback(callback)
        _scannerHelper = helper
    }

    private fun createSlotForDevice(device: UsbDevice) {
        val slotId = "slot_${_nextSlotIndex}"
        val slotIndex = _nextSlotIndex + 1
        _nextSlotIndex++

        val controller = CameraSlotController(slotId, slotIndex)
        controller.onStateChanged = {
            syncSlotStates()
        }
        controller.onRemoveRequested = {
            _uiState.update {
                it.copy(globalToastMessage = "摄像头打开失败: USB 带宽不足")
            }
            controller.release()
            _slots.remove(slotId)
            syncSlotStates()
        }
        _slots[slotId] = controller
        syncSlotStates()

        // Enqueue permission request for this device
        enqueueDeviceSelection(slotId, device)
    }

    fun onCameraPermissionGranted() {
        _uiState.update { it.copy(hasCameraPermission = true) }
    }

    fun refreshAllDevices() {
        val devices = _scannerHelper?.deviceList ?: emptyList()
        _uiState.update { it.copy(allDevices = devices) }
    }

    /** User explicitly opens a device — creates a new slot for it. */
    fun openDevice(device: UsbDevice) {
        if (_slots.size >= MAX_SLOTS) {
            _uiState.update { it.copy(globalToastMessage = "已达最大摄像头数量") }
            return
        }
        val existingSlot = _slots.values.find {
            it.state.value.boundDevice?.deviceId == device.deviceId
        }
        if (existingSlot != null) {
            _uiState.update { it.copy(globalToastMessage = "该设备已被其他摄像头使用") }
            return
        }
        createSlotForDevice(device)
    }

    /** User removes a slot — releases camera and removes from map. */
    fun removeSlot(slotId: String) {
        _slots[slotId]?.let { controller ->
            controller.release()
            _slots.remove(slotId)
            syncSlotStates()
        }
    }

    fun showOpenDeviceDialog() {
        refreshAllDevices()
        _uiState.update { it.copy(showOpenDeviceDialog = true) }
    }

    fun dismissOpenDeviceDialog() {
        _uiState.update { it.copy(showOpenDeviceDialog = false) }
    }

    fun enqueueDeviceSelection(slotId: String, device: UsbDevice) {
        if (!_uiState.value.hasCameraPermission) {
            _uiState.update {
                it.copy(globalToastMessage = "使用 USB 摄像头需要相机权限")
            }
            return
        }

        viewModelScope.launch {
            _deviceMutex.withLock {
                // Re-validate: check slot still exists
                val controller = _slots[slotId] ?: return@launch

                // Re-validate: check device is not bound to another slot
                val conflictSlot = _slots.entries.find { entry ->
                    entry.key != slotId &&
                        entry.value.state.value.boundDevice?.deviceId == device.deviceId
                }
                if (conflictSlot != null) {
                    _uiState.update {
                        it.copy(globalToastMessage = "该设备已被其他摄像头使用")
                    }
                    return@launch
                }

                // Eject current camera if connected
                if (controller.state.value.isCameraConnected) {
                    controller.safelyEject()
                    // Wait for disconnect (max 5 seconds)
                    var waitCount = 0
                    while (controller.state.value.isCameraConnected && waitCount < 50) {
                        delay(100)
                        waitCount++
                    }
                    delay(300)
                }

                // Re-validate after wait: slot may have been removed by onDetach
                if (!_slots.containsKey(slotId)) return@launch

                controller.selectDevice(device)

                // Wait for the camera to open or fail
                delay(500)

                refreshAllDevices()
            }
        }
    }

    fun showDeviceListDialog(slotId: String) {
        refreshAllDevices()
        _slots[slotId]?.showDeviceListDialog()
    }

    fun dismissDeviceListDialog(slotId: String) {
        _slots[slotId]?.dismissDeviceListDialog()
    }

    fun dismissCameraControlsDialog(slotId: String) {
        _slots[slotId]?.dismissCameraControlsDialog()
    }

    fun dismissVideoFormatDialog(slotId: String) {
        _slots[slotId]?.dismissVideoFormatDialog()
    }

    fun clearSlotToast(slotId: String) {
        _slots[slotId]?.clearToast()
    }

    fun clearGlobalToast() {
        _uiState.update { it.copy(globalToastMessage = null) }
    }

    fun getSlotController(slotId: String): CameraSlotController? = _slots[slotId]

    private fun syncSlotStates() {
        val slotStates = _slots.mapValues { it.value.state.value }
        _uiState.update { it.copy(slots = slotStates) }
    }

    override fun onCleared() {
        super.onCleared()
        _slots.values.forEach { it.release() }
        _slots.clear()
        _scannerHelper?.release()
        _scannerHelper = null
    }
}
