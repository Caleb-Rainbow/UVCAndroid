package com.herohan.uvcapp.utils

import android.hardware.usb.UsbDevice

/**
 * Composite key for USB device identity.
 *
 * Uses `vendorId_productId_deviceName` to distinguish between multiple identical
 * cameras (same vendorId/productId). Note: `deviceName` is a kernel-assigned path
 * (e.g. `/dev/bus/usb/001/002`) that changes on replug. This is acceptable because
 * USB detach events already clean up the associated slot, and a replugged camera
 * is treated as a new device.
 *
 * **Trade-off:** Two identical cameras can be distinguished, but the key is not
 * stable across physical replug events.
 */
fun UsbDevice.identityKey(): String =
    "${vendorId}_${productId}_${deviceName}"
