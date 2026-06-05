package com.herohan.uvcapp.utils

import android.hardware.usb.UsbDevice

/** Composite key for stable USB device identity across hot-plug events. */
fun UsbDevice.identityKey(): String =
    "${vendorId}_${productId}_${deviceName}"
