package com.herohan.uvcapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.herohan.uvcapp.R
import com.serenegiant.usb.UVCControl

@Composable
fun CameraControlsDialog(
    control: UVCControl?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (control == null) {
                Text(
                    text = stringResource(R.string.camera_controls_text_not_connected),
                    modifier = Modifier.padding(32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                CameraControlsContent(control = control)
            }

            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.camera_controls_button_close))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    control?.let { resetAllControls(it) }
                }) {
                    Text(stringResource(R.string.camera_controls_button_reset))
                }
            }
        }
    }
}

@Composable
private fun CameraControlsContent(control: UVCControl) {
    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_brightness),
        isEnabled = control.isBrightnessEnable,
        value = control.brightness.toFloat(),
        limits = control.updateBrightnessLimit(),
        onValueChange = { control.brightness = it.toInt() },
    )

    var contrastAuto by remember { mutableStateOf(control.contrastAuto) }
    UvcSeekBarWithAuto(
        label = stringResource(R.string.camera_controls_text_contrast),
        isEnabled = control.isContrastEnable,
        value = control.contrast.toFloat(),
        limits = control.updateContrastLimit(),
        autoEnabled = control.isContrastAutoEnable,
        autoChecked = contrastAuto,
        onValueChange = { control.contrast = it.toInt() },
        onAutoChange = { isChecked ->
            contrastAuto = isChecked
            if (isChecked) control.resetContrast()
            control.contrastAuto = isChecked
        },
    )

    var hueAuto by remember { mutableStateOf(control.hueAuto) }
    UvcSeekBarWithAuto(
        label = stringResource(R.string.camera_controls_text_hue),
        isEnabled = control.isHueEnable,
        value = control.hue.toFloat(),
        limits = control.updateHueLimit(),
        autoEnabled = control.isHueAutoEnable,
        autoChecked = hueAuto,
        onValueChange = { control.hue = it.toInt() },
        onAutoChange = { isChecked ->
            hueAuto = isChecked
            if (isChecked) control.resetHue()
            control.hueAuto = isChecked
        },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_saturation),
        isEnabled = control.isSaturationEnable,
        value = control.saturation.toFloat(),
        limits = control.updateSaturationLimit(),
        onValueChange = { control.saturation = it.toInt() },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_sharpness),
        isEnabled = control.isSharpnessEnable,
        value = control.sharpness.toFloat(),
        limits = control.updateSharpnessLimit(),
        onValueChange = { control.sharpness = it.toInt() },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_gamma),
        isEnabled = control.isGammaEnable,
        value = control.gamma.toFloat(),
        limits = control.updateGammaLimit(),
        onValueChange = { control.gamma = it.toInt() },
    )

    var whiteBalanceAuto by remember { mutableStateOf(control.whiteBalanceAuto) }
    UvcSeekBarWithAuto(
        label = stringResource(R.string.camera_controls_text_white_balance),
        isEnabled = control.isWhiteBalanceEnable,
        value = control.whiteBalance.toFloat(),
        limits = control.updateWhiteBalanceLimit(),
        autoEnabled = control.isWhiteBalanceAutoEnable,
        autoChecked = whiteBalanceAuto,
        onValueChange = { control.whiteBalance = it.toInt() },
        onAutoChange = { isChecked ->
            whiteBalanceAuto = isChecked
            if (isChecked) control.resetWhiteBalance()
            control.whiteBalanceAuto = isChecked
        },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_backlight_comp),
        isEnabled = control.isBacklightCompEnable,
        value = control.backlightComp.toFloat(),
        limits = control.updateBacklightCompLimit(),
        onValueChange = { control.backlightComp = it.toInt() },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_gain),
        isEnabled = control.isGainEnable,
        value = control.gain.toFloat(),
        limits = control.updateGainLimit(),
        onValueChange = { control.gain = it.toInt() },
    )

    var exposureAuto by remember { mutableStateOf(control.isExposureTimeAuto) }
    UvcSeekBarWithAuto(
        label = stringResource(R.string.camera_controls_text_exposure_time),
        isEnabled = control.isExposureTimeAbsoluteEnable,
        value = control.exposureTimeAbsolute.toFloat(),
        limits = control.updateExposureTimeAbsoluteLimit(),
        autoEnabled = control.isAutoExposureModeEnable,
        autoChecked = exposureAuto,
        onValueChange = { control.exposureTimeAbsolute = it.toInt() },
        onAutoChange = { isChecked ->
            exposureAuto = isChecked
            if (isChecked) control.resetExposureTimeAbsolute()
            control.isExposureTimeAuto = isChecked
        },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_iris),
        isEnabled = control.isIrisAbsoluteEnable,
        value = control.irisAbsolute.toFloat(),
        limits = control.updateIrisAbsoluteLimit(),
        onValueChange = { control.irisAbsolute = it.toInt() },
    )

    var focusAuto by remember { mutableStateOf(control.focusAuto) }
    UvcSeekBarWithAuto(
        label = stringResource(R.string.camera_controls_text_focus),
        isEnabled = control.isFocusAbsoluteEnable,
        value = control.focusAbsolute.toFloat(),
        limits = control.updateFocusAbsoluteLimit(),
        autoEnabled = control.isFocusAutoEnable,
        autoChecked = focusAuto,
        onValueChange = { control.focusAbsolute = it.toInt() },
        onAutoChange = { isChecked ->
            focusAuto = isChecked
            if (isChecked) control.resetFocusAbsolute()
            control.focusAuto = isChecked
        },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_zoom),
        isEnabled = control.isZoomAbsoluteEnable,
        value = control.zoomAbsolute.toFloat(),
        limits = control.updateZoomAbsoluteLimit(),
        onValueChange = { control.zoomAbsolute = it.toInt() },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_pan),
        isEnabled = control.isPanAbsoluteEnable,
        value = control.panAbsolute.toFloat(),
        limits = control.updatePanAbsoluteLimit(),
        onValueChange = { control.panAbsolute = it.toInt() },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_tilt),
        isEnabled = control.isTiltAbsoluteEnable,
        value = control.tiltAbsolute.toFloat(),
        limits = control.updateTiltAbsoluteLimit(),
        onValueChange = { control.tiltAbsolute = it.toInt() },
    )

    UvcSeekBar(
        label = stringResource(R.string.camera_controls_text_roll),
        isEnabled = control.isRollAbsoluteEnable,
        value = control.rollAbsolute.toFloat(),
        limits = control.updateRollAbsoluteLimit(),
        onValueChange = { control.rollAbsolute = it.toInt() },
    )

    UvcRadioGroup(
        label = stringResource(R.string.camera_controls_text_power_line_freq),
        isEnabled = control.isPowerlineFrequencyEnable,
        options = listOf(
            stringResource(R.string.camera_controls_text_disable),
            stringResource(R.string.camera_controls_text_50hz),
            stringResource(R.string.camera_controls_text_60hz),
            stringResource(R.string.camera_controls_text_auto),
        ),
        selectedIndex = control.powerlineFrequency,
        onSelected = { control.powerlineFrequency = it },
    )
}

@Composable
private fun UvcSeekBar(
    label: String,
    isEnabled: Boolean,
    value: Float,
    limits: IntArray?,
    onValueChange: (Float) -> Unit,
) {
    val min = limits?.getOrNull(0)?.toFloat() ?: 0f
    val max = limits?.getOrNull(1)?.toFloat() ?: 100f
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isEnabled) Color.Unspecified else Color.Gray,
        )
        Slider(
            value = if (max > min) (sliderValue - min) / (max - min) else 0f,
            onValueChange = { fraction ->
                sliderValue = min + fraction * (max - min)
                onValueChange(sliderValue)
            },
            enabled = isEnabled && max > min,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = sliderValue.toInt().toString(),
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isEnabled) Color.Unspecified else Color.Gray,
        )
    }
}

@Composable
private fun UvcSeekBarWithAuto(
    label: String,
    isEnabled: Boolean,
    value: Float,
    limits: IntArray?,
    autoEnabled: Boolean,
    autoChecked: Boolean,
    onValueChange: (Float) -> Unit,
    onAutoChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(110.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (isEnabled) Color.Unspecified else Color.Gray,
        )
        if (autoEnabled) {
            Checkbox(
                checked = autoChecked,
                onCheckedChange = onAutoChange,
                enabled = autoEnabled,
            )
            Text(
                text = stringResource(R.string.camera_controls_text_auto),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }
    }
    if (!autoChecked) {
        UvcSeekBar(
            label = "",
            isEnabled = isEnabled,
            value = value,
            limits = limits,
            onValueChange = onValueChange,
        )
    }
}

@Composable
private fun UvcRadioGroup(
    label: String,
    isEnabled: Boolean,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isEnabled) Color.Unspecified else Color.Gray,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            options.forEachIndexed { index, option ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = index == selectedIndex,
                        onClick = { if (isEnabled) onSelected(index) },
                        enabled = isEnabled,
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) Color.Unspecified else Color.Gray,
                    )
                }
            }
        }
    }
}

private fun resetAllControls(control: UVCControl) {
    control.resetBrightness()
    control.resetContrast()
    control.resetContrastAuto()
    control.resetHue()
    control.resetHueAuto()
    control.resetSaturation()
    control.resetSharpness()
    control.resetGamma()
    control.resetWhiteBalance()
    control.resetWhiteBalanceAuto()
    control.resetBacklightComp()
    control.resetGain()
    control.resetExposureTimeAbsolute()
    control.resetAutoExposureMode()
    control.resetIrisAbsolute()
    control.resetFocusAbsolute()
    control.resetFocusAuto()
    control.resetZoomAbsolute()
    control.resetPanAbsolute()
    control.resetTiltAbsolute()
    control.resetRollAbsolute()
    control.resetPowerlineFrequency()
}
