package com.herohan.uvcapp.ui

import com.serenegiant.usb.Format
import com.serenegiant.usb.Size

data class CameraSlotState(
    val slotId: String,
    val slotIndex: Int = 0,
    val isCameraConnected: Boolean = false,
    val isRecording: Boolean = false,
    val previewWidth: Int = 640,
    val previewHeight: Int = 480,
    val previewRotation: Int = 0,
    val showDeviceListDialog: Boolean = false,
    val showCameraControlsDialog: Boolean = false,
    val showVideoFormatDialog: Boolean = false,
    val supportedFormats: List<Format> = emptyList(),
    val currentPreviewSize: Size? = null,
    val toastMessage: String? = null,
    val boundDeviceKey: String? = null,
)
