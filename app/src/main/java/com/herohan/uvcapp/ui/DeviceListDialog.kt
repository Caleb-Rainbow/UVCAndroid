package com.herohan.uvcapp.ui

import android.hardware.usb.UsbDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.herohan.uvcapp.R
import com.herohan.uvcapp.utils.identityKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListDialog(
    deviceList: List<UsbDevice>,
    currentDevice: UsbDevice?,
    onDeviceSelected: (UsbDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedDeviceKey by remember { mutableStateOf(currentDevice?.identityKey()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = stringResource(R.string.device_list_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (deviceList.isEmpty()) {
                Text(
                    text = stringResource(R.string.device_list_empty_tip),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            } else {
                LazyColumn {
                    items(deviceList, key = { it.deviceId }) { device ->
                        DeviceItem(
                            device = device,
                            isSelected = device.identityKey() == selectedDeviceKey,
                            onClick = { selectedDeviceKey = device.identityKey() },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.device_list_cancel_button))
                }
                Spacer(modifier = Modifier.width(8.dp))
                val selectedDevice = deviceList.find { it.identityKey() == selectedDeviceKey }
                TextButton(
                    onClick = {
                        selectedDevice?.let { onDeviceSelected(it) }
                        onDismiss()
                    },
                    enabled = selectedDevice != null,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun DeviceItem(
    device: UsbDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = device.productName ?: device.manufacturerName ?: device.deviceName,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = device.deviceName,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
