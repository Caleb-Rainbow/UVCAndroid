package com.herohan.uvcapp.ui

import android.app.Application
import android.hardware.usb.UsbDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.R
import com.herohan.uvcapp.ICameraHelper
import com.herohan.uvcapp.utils.identityKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.LinkedHashMap

data class MultiCameraUiState(
    val hasCameraPermission: Boolean = false,
    /** Slot states keyed by slot ID. Insertion order preserved (LinkedHashMap via syncSlotStates). */
    val slots: Map<String, CameraSlotState> = emptyMap(),
    val allDevices: List<UsbDevice> = emptyList(),
    val globalToastMessage: String? = null,
    val showOpenDeviceDialog: Boolean = false,
)

class MultiCameraViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val MAX_SLOTS = 4
    }

    private val _uiState = MutableStateFlow(MultiCameraUiState())
    val uiState: StateFlow<MultiCameraUiState> = _uiState.asStateFlow()

    /**
     * Active camera slots. LinkedHashMap preserves insertion order for grid display.
     *
     * Threading: all access occurs on the main thread. USB callbacks are marshalled to
     * the main thread by [CameraHelper.StateCallbackWrapper]. Coroutines use
     * [viewModelScope] which defaults to [kotlinx.coroutines.Dispatchers.Main].
     * The [_deviceMutex] provides logical serialization for device operations,
     * not thread safety.
     */
    private val _slots = LinkedHashMap<String, CameraSlotController>()
    private val _deviceMutex = Mutex()
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
                refreshAllDevices()
            }

            override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {}

            override fun onCameraOpen(device: UsbDevice) {}

            override fun onCameraClose(device: UsbDevice) {}

            override fun onDeviceClose(device: UsbDevice) {}

            override fun onDetach(device: UsbDevice) {
                refreshAllDevices()
                // Remove the slot that was bound to this device
                val slotToRemove = _slots.entries.find {
                    it.value.state.value.boundDeviceKey == device.identityKey()
                }
                slotToRemove?.let { (key, controller) ->
                    controller.release()
                    _slots.remove(key)
                    syncSlotStates()
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

        val controller = CameraSlotController(slotId, slotIndex, getApplication<Application>(), viewModelScope)
        controller.onStateChanged = {
            syncSlotStates()
        }
        controller.onRemoveRequested = {
            _uiState.update {
                it.copy(globalToastMessage = getApplication<Application>().getString(R.string.slot_camera_open_failed))
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
        val devices = _scannerHelper?.deviceList?.toList() ?: emptyList()
        _uiState.update { it.copy(allDevices = devices) }
    }

    /** User explicitly opens a device — creates a new slot for it. */
    fun openDevice(device: UsbDevice) {
        if (!_uiState.value.hasCameraPermission) {
            _uiState.update { it.copy(globalToastMessage = getApplication<Application>().getString(R.string.global_camera_permission_required)) }
            return
        }

        if (_slots.size >= MAX_SLOTS) {
            _uiState.update { it.copy(globalToastMessage = getApplication<Application>().getString(R.string.global_max_cameras_reached)) }
            return
        }
        val existingSlot = _slots.values.find {
            it.state.value.boundDeviceKey == device.identityKey()
        }
        if (existingSlot != null) {
            _uiState.update { it.copy(globalToastMessage = getApplication<Application>().getString(R.string.global_device_in_use)) }
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
                it.copy(globalToastMessage = getApplication<Application>().getString(R.string.global_camera_permission_required))
            }
            return
        }

        viewModelScope.launch {
            try {
                // Phase A: Validate and eject under lock
                val controller = _deviceMutex.withLock {
                    val ctrl = _slots[slotId] ?: return@launch

                    // Check device is not bound to another slot
                    val conflictSlot = _slots.entries.find { entry ->
                        entry.key != slotId &&
                            entry.value.state.value.boundDeviceKey == device.identityKey()
                    }
                    if (conflictSlot != null) {
                        _uiState.update {
                            it.copy(globalToastMessage = getApplication<Application>().getString(R.string.global_device_in_use))
                        }
                        return@launch
                    }

                    // Eject current camera if connected
                    if (ctrl.state.value.isCameraConnected) {
                        ctrl.safelyEject()
                    }
                    ctrl
                }
                // Lock released — other slot operations can proceed during the wait

                // Phase B: Wait for disconnect (unlocked)
                if (controller.state.value.isCameraConnected) {
                    withTimeoutOrNull(5000L) {
                        controller.state
                            .map { it.isCameraConnected }
                            .first { !it }
                    }
                }

                // Phase C: Revalidate and select under lock
                _deviceMutex.withLock {
                    // Re-validate after wait: slot may have been removed by onDetach
                    if (!_slots.containsKey(slotId)) return@launch

                    controller.selectDevice(device)
                }

                // Wait for the camera to open or fail (reactive, not fixed delay)
                withTimeoutOrNull(5000L) {
                    controller.state.map { it.isCameraConnected }.first { it }
                }

                // Final check: slot may have been removed during the wait
                if (_slots.containsKey(slotId)) {
                    refreshAllDevices()
                }
            } catch (e: CancellationException) {
                throw e
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
        val newSlots = _slots.mapValues { it.value.state.value }
        _uiState.update { current ->
            if (current.slots == newSlots) current else current.copy(slots = newSlots)
        }
    }

    override fun onCleared() {
        super.onCleared()
        _slots.values.forEach { it.release() }
        _slots.clear()
        _scannerHelper?.release()
        _scannerHelper = null
    }
}
