package com.herohan.uvcdemo.ui

import android.graphics.SurfaceTexture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.serenegiant.widget.AspectRatioTextureView

@Composable
fun CameraPreview(
    onAddSurface: (SurfaceTexture) -> Unit,
    onRemoveSurface: (SurfaceTexture) -> Unit,
    previewWidth: Int,
    previewHeight: Int,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = if (previewHeight > 0) previewWidth.toFloat() / previewHeight.toFloat() else 1f
    val callbacks = remember {
        object { var onAdd: ((SurfaceTexture) -> Unit)? = null; var onRemove: ((SurfaceTexture) -> Unit)? = null }
    }
    callbacks.onAdd = onAddSurface; callbacks.onRemove = onRemoveSurface

    AndroidView(
        factory = { ctx ->
            AspectRatioTextureView(ctx).apply {
                setAspectRatio(previewWidth, previewHeight)
                surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(s: SurfaceTexture, w: Int, h: Int) { callbacks.onAdd?.invoke(s) }
                    override fun onSurfaceTextureSizeChanged(s: SurfaceTexture, w: Int, h: Int) {}
                    override fun onSurfaceTextureDestroyed(s: SurfaceTexture): Boolean { callbacks.onRemove?.invoke(s); return false }
                    override fun onSurfaceTextureUpdated(s: SurfaceTexture) {}
                }
            }
        },
        update = { it.setAspectRatio(previewWidth, previewHeight) },
        modifier = modifier.fillMaxWidth().aspectRatio(aspectRatio).background(Color.Black),
    )
}
