package io.github.xhuohai.sudo.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    ImageViewerDialog(
        images = listOf(imageUrl),
        initialIndex = 0,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageViewerDialog(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    
    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    
    // Shared state for vertical swipe-to-dismiss
    var verticalOffset by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = with(density) { 150.dp.toPx() }
    val backgroundAlpha by animateFloatAsState(
        targetValue = (1f - abs(verticalOffset) / (dismissThreshold * 2)).coerceIn(0.3f, 1f),
        label = "bgAlpha"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha))
        ) {
            // Image gallery with tilt effect during swipe
            val tiltAngle = (verticalOffset / dismissThreshold * 8f).coerceIn(-15f, 15f)
            val swipeScale = (1f - abs(verticalOffset) / (dismissThreshold * 3)).coerceIn(0.85f, 1f)
            
            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = verticalOffset
                        rotationZ = tiltAngle
                        scaleX = swipeScale
                        scaleY = swipeScale
                    }
            ) {
                items(images.size) { index ->
                    ZoomableImage(
                        imageUrl = images[index],
                        modifier = Modifier.fillParentMaxSize(),
                        onTap = onDismiss,
                        onVerticalDrag = { delta -> verticalOffset += delta },
                        onVerticalDragEnd = {
                            if (abs(verticalOffset) > dismissThreshold) {
                                onDismiss()
                            } else {
                                verticalOffset = 0f
                            }
                        }
                    )
                }
            }
            
            // Page indicator
            if (images.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${currentPage + 1} / ${images.size}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.Close, "关闭", tint = Color.White, modifier = Modifier.size(28.dp))
            }
            
            // Bottom buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                IconButton(
                    onClick = {
                        val url = images.getOrNull(currentPage) ?: return@IconButton
                        scope.launch { downloadImage(context, url) }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Download, "下载", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                IconButton(
                    onClick = {
                        val url = images.getOrNull(currentPage) ?: return@IconButton
                        scope.launch { shareImage(context, url) }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Share, "分享", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomableImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onVerticalDragEnd: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Track if we're in a vertical drag
    var isDraggingVertically by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (scale <= 1f) {
                        onTap()
                    }
                },
                onDoubleClick = {
                    if (scale > 1f) {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        scale = 2.5f
                    }
                }
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    
                    var totalZoom = 1f
                    var cumulativeVerticalDrag = 0f
                    isDraggingVertically = false
                    
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        
                        // Handle zoom (2+ fingers)
                        if (event.changes.size >= 2) {
                            totalZoom *= zoomChange
                            scale = (scale * zoomChange).coerceIn(1f, 5f)
                            
                            if (scale > 1f) {
                                offsetX += panChange.x
                                offsetY += panChange.y
                            }
                            
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } 
                        // Handle single finger drag
                        else if (event.changes.size == 1) {
                            val change = event.changes.first()
                            
                            if (scale > 1f) {
                                // When zoomed in, allow panning the image
                                offsetX += panChange.x
                                offsetY += panChange.y
                                if (change.positionChanged()) change.consume()
                            } else {
                                // When not zoomed, check for vertical drag (swipe to close)
                                val dragY = panChange.y
                                val dragX = panChange.x
                                
                                // Only start vertical drag if vertical movement > horizontal
                                if (!isDraggingVertically && abs(dragY) > abs(dragX) * 1.5f && abs(cumulativeVerticalDrag + dragY) > 10f) {
                                    isDraggingVertically = true
                                }
                                
                                if (isDraggingVertically) {
                                    cumulativeVerticalDrag += dragY
                                    onVerticalDrag(dragY)
                                    if (change.positionChanged()) change.consume()
                                }
                                // Let horizontal drag pass through to LazyRow
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    
                    // End of gesture
                    if (isDraggingVertically) {
                        onVerticalDragEnd()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.White
            )
        }
        
        AsyncImage(
            model = imageUrl,
            contentDescription = "图片",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit,
            onState = { isLoading = it is AsyncImagePainter.State.Loading }
        )
    }
}

private suspend fun downloadImage(context: Context, imageUrl: String) {
    withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(imageUrl).build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: throw Exception("Failed")
                val filename = "sudo_${System.currentTimeMillis()}.jpg"
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val cv = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/sudo")
                    }
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "sudo")
                    dir.mkdirs()
                    val file = File(dir, filename)
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                }
                withContext(Dispatchers.Main) { Toast.makeText(context, "已保存到相册", Toast.LENGTH_SHORT).show() }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show() }
        }
    }
}

private suspend fun shareImage(context: Context, imageUrl: String) {
    withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context).data(imageUrl).build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: throw Exception("Failed")
                val cacheDir = File(context.cacheDir, "shared_images").also { it.mkdirs() }
                val file = File(cacheDir, "share_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) { context.startActivity(Intent.createChooser(intent, "分享图片")) }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show() }
        }
    }
}
