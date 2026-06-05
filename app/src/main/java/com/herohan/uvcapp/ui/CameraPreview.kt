package com.herohan.uvcapp.ui

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.herohan.uvcapp.ICameraHelper
import com.serenegiant.widget.AspectRatioTextureView

@Composable
fun CameraPreview(
    onAddSurface: (SurfaceTexture) -> Unit,
    onRemoveSurface: (SurfaceTexture) -> Unit,
    previewWidth: Int,
    previewHeight: Int,
    onFirstFrame: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = if (previewHeight > 0) previewWidth.toFloat() / previewHeight.toFloat() else 1f

    val currentOnAdd by rememberUpdatedState(onAddSurface)
    val currentOnRemove by rememberUpdatedState(onRemoveSurface)
    val currentOnFrame by rememberUpdatedState(onFirstFrame)
    var firstFrameReceived by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            AspectRatioTextureView(ctx).apply {
                setAspectRatio(previewWidth, previewHeight)
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture, width: Int, height: Int
                    ) {
                        currentOnAdd(surface)
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture, width: Int, height: Int
                    ) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        currentOnRemove(surface)
                        return false
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        if (!firstFrameReceived) {
                            firstFrameReceived = true
                            currentOnFrame?.invoke()
                        }
                    }
                }
            }
        },
        update = { view ->
            view.setAspectRatio(previewWidth, previewHeight)
        },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(Color.Black),
    )
}
