package com.herohan.uvcdemo.ui

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.herohan.uvcapp.IImageCapture
import com.herohan.uvcapp.VideoCapture
import com.serenegiant.opengl.renderer.MirrorMode
import com.serenegiant.usb.Format
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException
import java.util.concurrent.CancellationException

data class CameraUiState(
    val isCameraConnected: Boolean = false,
    val isRecording: Boolean = false,
    val previewWidth: Int = 640,
    val previewHeight: Int = 480,
    val previewRotation: Int = 0,
    val recordTimeMillis: Long = 0,
    val deviceList: List<UsbDevice> = emptyList(),
    val selectedDevice: UsbDevice? = null,
    val supportedFormats: List<Format> = emptyList(),
    val currentPreviewSize: Size? = null,
    val toastMessage: String? = null,
    val showDeviceListDialog: Boolean = false,
    val showCameraControlsDialog: Boolean = false,
    val showVideoFormatDialog: Boolean = false,
)

class BaseCameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    private var _cameraHelper: ICameraHelper? = null
    private var stateCallback: ICameraHelper.StateCallback? = null

    init {
        initCameraHelper()
    }

    private fun initCameraHelper() {
        if (_cameraHelper == null) {
            val helper = CameraHelper()
            val callback = createStateCallback()
            helper.setStateCallback(callback)
            helper.imageCaptureConfig = helper.imageCaptureConfig.setJpegCompressionQuality(90)
            helper.videoCaptureConfig = helper.videoCaptureConfig
                .setBitRate((1024 * 1024 * 25 * 0.25).toInt())
                .setVideoFrameRate(25)
                .setIFrameInterval(1)
            _cameraHelper = helper
            stateCallback = callback
        }
    }

    private fun createStateCallback() = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            if (_uiState.value.selectedDevice == null) {
                _uiState.update { it.copy(selectedDevice = device) }
                selectDevice(device)
            }
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
            _cameraHelper?.openCamera()
            _cameraHelper?.setButtonCallback { button, state ->
                _uiState.update { it.copy(toastMessage = "Button: $button, State: $state") }
            }
        }

        override fun onCameraOpen(device: UsbDevice) {
            _cameraHelper?.startPreview()
            val size = _cameraHelper?.previewSize
            if (size != null) {
                _uiState.update {
                    it.copy(previewWidth = size.width, previewHeight = size.height, currentPreviewSize = size)
                }
            }
            _uiState.update {
                it.copy(isCameraConnected = true, supportedFormats = _cameraHelper?.supportedFormatList ?: emptyList())
            }
        }

        override fun onCameraClose(device: UsbDevice) {
            if (_uiState.value.isRecording) stopRecordInternal()
            _uiState.update {
                it.copy(isCameraConnected = false, showCameraControlsDialog = false,
                    showDeviceListDialog = false, showVideoFormatDialog = false)
            }
        }

        override fun onDeviceClose(device: UsbDevice) {}
        override fun onDetach(device: UsbDevice) {
            if (device == _uiState.value.selectedDevice) _uiState.update { it.copy(selectedDevice = null) }
        }

        override fun onCancel(device: UsbDevice) {
            if (device == _uiState.value.selectedDevice) _uiState.update { it.copy(selectedDevice = null) }
        }
    }

    fun selectDevice(device: UsbDevice) {
        _uiState.update { it.copy(isCameraConnected = false, selectedDevice = device) }
        _cameraHelper?.selectDevice(device)
    }

    fun openCamera(savedSize: Size? = null) { _cameraHelper?.openCamera(savedSize) }
    fun startPreview() { _cameraHelper?.startPreview() }
    fun stopPreview() { _cameraHelper?.stopPreview() }
    fun safelyEject() { _cameraHelper?.closeCamera() }
    fun getUvcControl(): UVCControl? = _cameraHelper?.uvcControl
    fun getCameraHelper(): ICameraHelper? = _cameraHelper
    fun getPreviewSize(): Size? = _cameraHelper?.previewSize
    fun getSupportedFormatList(): List<Format> = _cameraHelper?.supportedFormatList ?: emptyList()

    fun addSurface(surface: Any, isRecordable: Boolean) { _cameraHelper?.addSurface(surface, isRecordable) }
    fun removeSurface(surface: Any) { _cameraHelper?.removeSurface(surface) }

    fun setPreviewSize(size: Size) {
        if (_uiState.value.isCameraConnected && _cameraHelper?.isRecording != true) {
            _cameraHelper?.stopPreview()
            _cameraHelper?.setPreviewSize(size)
            _cameraHelper?.startPreview()
            _uiState.update { it.copy(previewWidth = size.width, previewHeight = size.height, currentPreviewSize = size) }
        }
    }

    fun rotateBy(angle: Int) {
        var r = (_uiState.value.previewRotation + angle) % 360
        if (r < 0) r += 360
        _uiState.update { it.copy(previewRotation = r) }
        _cameraHelper?.previewConfig = _cameraHelper?.previewConfig?.setRotation(r) ?: return
    }

    fun flipHorizontally() {
        _cameraHelper?.previewConfig = _cameraHelper?.previewConfig?.setMirror(MirrorMode.MIRROR_HORIZONTAL) ?: return
    }

    fun flipVertically() {
        _cameraHelper?.previewConfig = _cameraHelper?.previewConfig?.setMirror(MirrorMode.MIRROR_VERTICAL) ?: return
    }

    fun takePicture() {
        if (_uiState.value.isRecording) return
        try {
            val file = File(com.herohan.uvcdemo.utils.SaveHelper.getSavePhotoPath())
            val options = IImageCapture.OutputFileOptions.Builder(file).build()
            _cameraHelper?.takePicture(options, object : IImageCapture.OnImageCaptureCallback {
                override fun onImageSaved(outputFileResults: IImageCapture.OutputFileResults) {
                    _uiState.update { it.copy(toastMessage = "Saved: ${outputFileResults.savedUri?.path ?: file.absolutePath}") }
                }
                override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
                    _uiState.update { it.copy(toastMessage = message) }
                }
            })
        } catch (e: CancellationException) { throw e }
        catch (e: IOException) { _uiState.update { it.copy(toastMessage = "Save failed: ${e.message}") } }
        catch (e: SecurityException) { _uiState.update { it.copy(toastMessage = "Permission denied") } }
    }

    fun toggleVideoRecord() {
        try {
            if (!_uiState.value.isRecording) {
                if (_uiState.value.isCameraConnected && _cameraHelper?.isRecording != true) startRecord()
            } else {
                stopRecordInternal()
            }
        } catch (e: CancellationException) { throw e }
        catch (e: IOException) { stopRecordInternal(); _uiState.update { it.copy(toastMessage = "Record failed: ${e.message}") } }
        catch (e: SecurityException) { stopRecordInternal(); _uiState.update { it.copy(toastMessage = "Permission denied") } }
    }

    private fun stopRecordInternal() {
        if (_cameraHelper?.isRecording == true) _cameraHelper?.stopRecording()
        _uiState.update { it.copy(isRecording = false, recordTimeMillis = 0) }
    }

    private fun startRecord() {
        val file = File(com.herohan.uvcdemo.utils.SaveHelper.getSaveVideoPath())
        val options = VideoCapture.OutputFileOptions.Builder(file).build()
        _cameraHelper?.startRecording(options, object : VideoCapture.OnVideoCaptureCallback {
            override fun onStart() { _uiState.update { it.copy(isRecording = true) } }
            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                _uiState.update { it.copy(isRecording = false, recordTimeMillis = 0, toastMessage = "Saved: ${outputFileResults.savedUri?.path ?: file.absolutePath}") }
            }
            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                _uiState.update { it.copy(isRecording = false, recordTimeMillis = 0, toastMessage = message) }
            }
        })
    }

    fun updateRecordTime(millis: Long) { _uiState.update { it.copy(recordTimeMillis = millis) } }

    fun showDeviceListDialog() {
        _uiState.update { it.copy(deviceList = _cameraHelper?.deviceList ?: emptyList(), showDeviceListDialog = true) }
    }
    fun dismissDeviceListDialog() { _uiState.update { it.copy(showDeviceListDialog = false) } }
    fun showCameraControlsDialog() { _uiState.update { it.copy(showCameraControlsDialog = true) } }
    fun dismissCameraControlsDialog() { _uiState.update { it.copy(showCameraControlsDialog = false) } }
    fun showVideoFormatDialog() { _uiState.update { it.copy(showVideoFormatDialog = true) } }
    fun dismissVideoFormatDialog() { _uiState.update { it.copy(showVideoFormatDialog = false) } }
    fun clearToast() { _uiState.update { it.copy(toastMessage = null) } }

    override fun onCleared() {
        super.onCleared()
        _cameraHelper?.release()
        _cameraHelper = null
        stateCallback = null
    }
}
