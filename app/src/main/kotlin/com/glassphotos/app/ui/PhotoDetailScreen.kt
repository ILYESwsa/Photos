package com.glassphotos.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.glassphotos.app.data.Photo
import com.glassphotos.app.ui.glasscomponents.GlassDetailActionBar
import kotlin.math.abs

@Composable
fun PhotoDetailScreen(
    photos: List<Photo>,
    initialIndex: Int,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Photo) -> Unit,
    onDelete: (Photo) -> Unit,
    onShare: (Photo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
    val backdrop = rememberLayerBackdrop()
    var controlsVisible by remember { mutableStateOf(true) }

    // Swipe-down-to-dismiss drag offset, with a fade tied to drag distance.
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThresholdPx = 300f
    val dragProgress = (abs(dragOffsetY) / dismissThresholdPx).coerceIn(0f, 1f)
    val scale by animateFloatAsState(targetValue = 1f - dragProgress * 0.15f, label = "detailScale")
    val backgroundAlpha = 1f - dragProgress * 0.7f

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .graphicsLayer {
                    translationY = dragOffsetY
                    scaleX = scale
                    scaleY = scale
                }
                .pointerInput(Unit) {
                    detectTapGestures { controlsVisible = !controlsVisible }
                }
        ) { page ->
            ZoomableImage(
                photo = photos[page],
                onDragOffsetChange = { dragOffsetY = it },
                onDismiss = onDismiss,
                dismissThresholdPx = dismissThresholdPx,
            )
        }

        val currentPhoto = photos.getOrNull(pagerState.currentPage)

        AnimatedVisibility(
            visible = controlsVisible && dragProgress < 0.1f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
            ) {
                Row(
                    Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .padding(8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { onDismiss() }
                            }
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    currentPhoto?.let {
                        Text(
                            it.bucketName,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible && dragProgress < 0.1f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            currentPhoto?.let { photo ->
                GlassDetailActionBar(
                    backdrop = backdrop,
                    isFavorite = favoriteIds.contains(photo.id),
                    onShare = { onShare(photo) },
                    onToggleFavorite = { onToggleFavorite(photo) },
                    onDelete = { onDelete(photo) },
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    photo: Photo,
    onDragOffsetChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    dismissThresholdPx: Float,
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        if (newScale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    var verticalDrag by remember { mutableFloatStateOf(0f) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(scale) {
                if (scale <= 1f) {
                    detectVerticalSwipeToDismiss(
                        onDrag = { delta ->
                            verticalDrag += delta
                            onDragOffsetChange(verticalDrag)
                        },
                        onEnd = {
                            if (abs(verticalDrag) > dismissThresholdPx) {
                                onDismiss()
                            } else {
                                verticalDrag = 0f
                                onDragOffsetChange(0f)
                            }
                        }
                    )
                }
            }
            .transformable(state = transformState),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(photo.uri).build(),
            contentDescription = photo.displayName,
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
        )
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectVerticalSwipeToDismiss(
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
) {
    androidx.compose.foundation.gestures.detectVerticalDragGestures(
        onVerticalDrag = { change, dragAmount ->
            change.consume()
            onDrag(dragAmount)
        },
        onDragEnd = onEnd,
        onDragCancel = onEnd,
    )
}
