package com.herohan.uvcdemo.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.herohan.uvcdemo.BasicPreviewActivity
import com.herohan.uvcdemo.CustomPreviewActivity
import com.herohan.uvcdemo.MultiCameraActivity
import com.herohan.uvcdemo.MultiCameraNewActivity
import com.herohan.uvcdemo.MultiPreviewActivity
import com.herohan.uvcdemo.R
import com.herohan.uvcdemo.RecordVideoActivity
import com.herohan.uvcdemo.SetFrameCallbackActivity
import com.herohan.uvcdemo.TakePictureActivity
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen() {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DemoButton(stringResource(R.string.entry_basic_preview)) {
                startActivityWithPermission(context, BasicPreviewActivity::class.java)
            }
            DemoButton(stringResource(R.string.entry_custom_preview)) {
                startActivityWithPermission(context, CustomPreviewActivity::class.java)
            }
            DemoButton(stringResource(R.string.entry_multi_preview)) {
                startActivityWithPermission(context, MultiPreviewActivity::class.java)
            }
            DemoButton(stringResource(R.string.entry_multi_camera)) {
                startActivityWithPermission(context, MultiCameraActivity::class.java)
            }
            DemoButton(stringResource(R.string.entry_multi_camera_new)) {
                startActivityWithPermission(context, MultiCameraNewActivity::class.java)
            }
            DemoButton(stringResource(R.string.entry_take_picture)) {
                XXPermissions.with(context)
                    .permission(PermissionLists.getCameraPermission())
                    .permission(PermissionLists.getManageExternalStoragePermission())
                    .request { _, allGranted ->
                        if (allGranted) context.startActivity(Intent(context, TakePictureActivity::class.java))
                    }
            }
            DemoButton(stringResource(R.string.entry_record_video)) {
                XXPermissions.with(context)
                    .permission(PermissionLists.getCameraPermission())
                    .permission(PermissionLists.getManageExternalStoragePermission())
                    .permission(PermissionLists.getRecordAudioPermission())
                    .request { _, allGranted ->
                        if (allGranted) context.startActivity(Intent(context, RecordVideoActivity::class.java))
                    }
            }
            DemoButton(stringResource(R.string.entry_set_frame_callback)) {
                startActivityWithPermission(context, SetFrameCallbackActivity::class.java)
            }
        }
    }
}

@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text)
    }
}

private fun startActivityWithPermission(context: Context, activityClass: Class<*>) {
    XXPermissions.with(context)
        .permission(PermissionLists.getCameraPermission())
        .request { _, allGranted ->
            if (allGranted) context.startActivity(Intent(context, activityClass))
        }
}
