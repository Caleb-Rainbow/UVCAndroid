package com.herohan.uvcapp.ui

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Looper
import com.herohan.uvcapp.CameraException
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.herohan.uvcapp.IImageCapture
import com.herohan.uvcapp.VideoCapture
import com.herohan.uvcapp.utils.SaveHelper
import com.serenegiant.opengl.renderer.MirrorMode
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCControl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.IOException

class CameraSlotController(
    val slotId: String,
    val slotIndex: Int,
    private val appContext: Context,
) {

    companion object {
        /** ~6.25 Mbps */
        private const val VIDEO_BITRATE_BPS = (1024 * 1024 * 25 / 4)
        /** Timeout to detect silent preview failure */
        private const val PREVIEW_CHECK_TIMEOUT_MS = 3000L
    }

    private val _state = MutableStateFlow(CameraSlotState(slotId = slotId, slotIndex = slotIndex))
    val state = _state.asStateFlow()

    private var _cameraHelper: ICameraHelper? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var _previewStarted = false
    private var _previewCheckRunnable: Runnable? = null
    @Volatile private var isReleased = false

    /** Called on the main thread whenever this slot's state changes. */
    var onStateChanged: (() -> Unit)? = null

    /** Called when this slot should be removed (e.g. preview failed). */
    var onRemoveRequested: (() -> Unit)? = null

    init {
        initCameraHelper()
    }

    private fun notifyStateChanged() {
        if (!isReleased) {
            onStateChanged?.invoke()
        }
    }

    private fun initCameraHelper() {
        val helper = CameraHelper()
        val callback = createStateCallback()
        helper.setStateCallback(callback)
        helper.imageCaptureConfig =
            helper.imageCaptureConfig.setJpegCompressionQuality(90)
        helper.videoCaptureConfig =
            helper.videoCaptureConfig
                .setBitRate(VIDEO_BITRATE_BPS)
                .setVideoFrameRate(25)
                .setIFrameInterval(1)
        _cameraHelper = helper
    }

    private fun createStateCallback(): ICameraHelper.StateCallback {
        return object : ICameraHelper.StateCallback {
            override fun onAttach(device: UsbDevice) {
                // No-op: scanner helper handles device detection
            }

            override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
                if (isReleased) return
                _cameraHelper?.openCamera()
                _cameraHelper?.setButtonCallback { button, btnState ->
                    if (isReleased) return@setButtonCallback
                    _state.update { it.copy(toastMessage = "按钮: $button, 状态: $btnState") }
                    notifyStateChanged()
                }
            }

            override fun onCameraOpen(device: UsbDevice) {
                if (isReleased) return
                _cameraHelper?.startPreview()
                // Start timeout to detect silent preview failure (USB bandwidth exhaustion)
                _previewStarted = false
                _previewCheckRunnable = Runnable {
                    if (isReleased) return@Runnable
                    if (!_previewStarted && _state.value.isCameraConnected) {
                        _cameraHelper?.closeCamera()
                        _state.update {
                            it.copy(
                                isCameraConnected = false,
                                boundDevice = null,
                            )
                        }
                        notifyStateChanged()
                        // Delay removal so toast can be shown
                        mainHandler.postDelayed({
                            if (!isReleased) {
                                onRemoveRequested?.invoke()
                            }
                        }, 100)
                    }
                }
                mainHandler.postDelayed(_previewCheckRunnable!!, PREVIEW_CHECK_TIMEOUT_MS)

                val size = _cameraHelper?.previewSize
                if (size != null) {
                    _state.update {
                        it.copy(
                            previewWidth = size.width,
                            previewHeight = size.height,
                            currentPreviewSize = size,
                        )
                    }
                }
                _state.update {
                    it.copy(
                        isCameraConnected = true,
                        supportedFormats = _cameraHelper?.supportedFormatList ?: emptyList(),
                        boundDevice = device,
                    )
                }
                notifyStateChanged()
            }

            override fun onCameraClose(device: UsbDevice) {
                if (isReleased) return
                if (_state.value.isRecording) {
                    stopRecordInternal()
                }
                _state.update {
                    it.copy(
                        isCameraConnected = false,
                        showCameraControlsDialog = false,
                        showDeviceListDialog = false,
                        showVideoFormatDialog = false,
                        boundDevice = null,
                    )
                }
                notifyStateChanged()
            }

            override fun onDeviceClose(device: UsbDevice) {}

            override fun onDetach(device: UsbDevice) {
                if (isReleased) return
                if (device == _state.value.boundDevice) {
                    _state.update { it.copy(boundDevice = null) }
                    notifyStateChanged()
                }
            }

            override fun onCancel(device: UsbDevice) {
                if (isReleased) return
                _state.update { it.copy(boundDevice = null) }
                notifyStateChanged()
            }

            override fun onError(device: UsbDevice, e: CameraException) {
                if (isReleased) return
                _state.update {
                    it.copy(
                        isCameraConnected = false,
                        toastMessage = "摄像头打开失败: USB 带宽不足",
                        boundDevice = null,
                    )
                }
                notifyStateChanged()
            }
        }
    }

    fun selectDevice(device: UsbDevice) {
        _state.update { it.copy(isCameraConnected = false, boundDevice = device) }
        _cameraHelper?.selectDevice(device)
    }

    fun addSurface(surface: Any, isRecordable: Boolean) {
        _cameraHelper?.addSurface(surface, isRecordable)
    }

    fun removeSurface(surface: Any) {
        _cameraHelper?.removeSurface(surface)
    }

    fun safelyEject() {
        _cameraHelper?.closeCamera()
    }

    fun takePicture() {
        if (_state.value.isRecording) return
        try {
            val file = File(SaveHelper.getSavePhotoPath(appContext))
            val options = IImageCapture.OutputFileOptions.Builder(file).build()
            _cameraHelper?.takePicture(options, object : IImageCapture.OnImageCaptureCallback {
                override fun onImageSaved(outputFileResults: IImageCapture.OutputFileResults) {
                    if (isReleased) return
                    _state.update {
                        it.copy(
                            toastMessage = "已保存: ${outputFileResults.savedUri?.path ?: file.absolutePath}"
                        )
                    }
                    notifyStateChanged()
                }

                override fun onError(imageCaptureError: Int, message: String, cause: Throwable?) {
                    if (isReleased) return
                    _state.update { it.copy(toastMessage = "拍照失败") }
                    notifyStateChanged()
                }
            })
        } catch (e: IOException) {
            _state.update { it.copy(toastMessage = "保存失败，请检查存储空间") }
            notifyStateChanged()
        } catch (e: SecurityException) {
            _state.update { it.copy(toastMessage = "权限被拒绝") }
            notifyStateChanged()
        }
    }

    fun toggleVideoRecord() {
        val isCurrentlyRecording = _state.value.isRecording
        try {
            if (!isCurrentlyRecording) {
                if (_state.value.isCameraConnected && _cameraHelper?.isRecording != true) {
                    startRecord()
                }
            } else {
                stopRecordInternal()
            }
        } catch (e: IOException) {
            stopRecordInternal()
            _state.update { it.copy(toastMessage = "录像失败，请检查存储空间") }
            notifyStateChanged()
        } catch (e: SecurityException) {
            stopRecordInternal()
            _state.update { it.copy(toastMessage = "权限被拒绝") }
            notifyStateChanged()
        }
    }

    private fun stopRecordInternal() {
        if (_cameraHelper?.isRecording == true) {
            _cameraHelper?.stopRecording()
        }
        _state.update { it.copy(isRecording = false, recordTimeMillis = 0) }
        notifyStateChanged()
    }

    private fun startRecord() {
        val file = File(SaveHelper.getSaveVideoPath(appContext))
        val options = VideoCapture.OutputFileOptions.Builder(file).build()
        _cameraHelper?.startRecording(options, object : VideoCapture.OnVideoCaptureCallback {
            override fun onStart() {
                if (isReleased) return
                _state.update { it.copy(isRecording = true) }
                notifyStateChanged()
            }

            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                if (isReleased) return
                _state.update { it.copy(isRecording = false, recordTimeMillis = 0) }
                _state.update {
                    it.copy(
                        toastMessage = "已保存: ${outputFileResults.savedUri?.path ?: file.absolutePath}"
                    )
                }
                notifyStateChanged()
            }

            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                if (isReleased) return
                _state.update { it.copy(isRecording = false, recordTimeMillis = 0) }
                _state.update { it.copy(toastMessage = "录像失败") }
                notifyStateChanged()
            }
        })
    }

    fun updateRecordTime(millis: Long) {
        _state.update { it.copy(recordTimeMillis = millis) }
        notifyStateChanged()
    }

    fun setPreviewSize(size: Size) {
        if (_state.value.isCameraConnected && _cameraHelper?.isRecording != true) {
            _cameraHelper?.stopPreview()
            _cameraHelper?.setPreviewSize(size)
            _cameraHelper?.startPreview()
            _state.update {
                it.copy(
                    previewWidth = size.width,
                    previewHeight = size.height,
                    currentPreviewSize = size,
                )
            }
            notifyStateChanged()
        }
    }

    fun rotateBy(angle: Int) {
        var rotation = _state.value.previewRotation + angle
        rotation %= 360
        if (rotation < 0) rotation += 360
        _state.update { it.copy(previewRotation = rotation) }
        val config = _cameraHelper?.previewConfig?.setRotation(rotation) ?: return
        _cameraHelper?.previewConfig = config
        notifyStateChanged()
    }

    fun flipHorizontally() {
        val config = _cameraHelper?.previewConfig?.setMirror(MirrorMode.MIRROR_HORIZONTAL)
        if (config != null) {
            _cameraHelper?.previewConfig = config
        } else if (!isReleased) {
            _state.update { it.copy(toastMessage = "无法翻转: 摄像头未就绪") }
            notifyStateChanged()
        }
    }

    fun flipVertically() {
        val config = _cameraHelper?.previewConfig?.setMirror(MirrorMode.MIRROR_VERTICAL)
        if (config != null) {
            _cameraHelper?.previewConfig = config
        } else if (!isReleased) {
            _state.update { it.copy(toastMessage = "无法翻转: 摄像头未就绪") }
            notifyStateChanged()
        }
    }

    fun getUvcControl(): UVCControl? = _cameraHelper?.uvcControl

    // Dialog visibility
    fun showDeviceListDialog() {
        _state.update { it.copy(showDeviceListDialog = true) }
        notifyStateChanged()
    }

    fun dismissDeviceListDialog() {
        _state.update { it.copy(showDeviceListDialog = false) }
        notifyStateChanged()
    }

    fun showCameraControlsDialog() {
        _state.update { it.copy(showCameraControlsDialog = true) }
        notifyStateChanged()
    }

    fun dismissCameraControlsDialog() {
        _state.update { it.copy(showCameraControlsDialog = false) }
        notifyStateChanged()
    }

    fun showVideoFormatDialog() {
        _state.update { it.copy(showVideoFormatDialog = true) }
        notifyStateChanged()
    }

    fun dismissVideoFormatDialog() {
        _state.update { it.copy(showVideoFormatDialog = false) }
        notifyStateChanged()
    }

    fun clearToast() {
        _state.update { it.copy(toastMessage = null) }
        notifyStateChanged()
    }

    /** Called when the first preview frame is rendered — confirms preview is working. */
    fun confirmPreviewStarted() {
        _previewStarted = true
        _previewCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        _previewCheckRunnable = null
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        onStateChanged = null
        onRemoveRequested = null
        _previewCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        _previewCheckRunnable = null
        _cameraHelper?.release()
        _cameraHelper = null
    }
}
