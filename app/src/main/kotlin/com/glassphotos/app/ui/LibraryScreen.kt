package com.glassphotos.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.kashif_e.backdrop.Backdrop
import com.glassphotos.app.data.Photo
import com.glassphotos.app.data.PhotoSection
import androidx.compose.ui.platform.LocalContext

@Composable
fun LibraryScreen(
    sections: List<PhotoSection>,
    isLoading: Boolean,
    onPhotoClick: (Photo) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    topInsetHeight: Dp = 96.dp,
    bottomInsetHeight: Dp = 78.dp,
) {
    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (sections.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No photos found", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = topInsetHeight,
            bottom = bottomInsetHeight,
            start = 2.dp,
            end = 2.dp,
        ),
    ) {
        sections.forEach { section ->
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Text(
                    section.label,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 14.dp, top = 18.dp, bottom = 8.dp)
                )
            }
            items(section.photos, key = { it.id }) { photo ->
                PhotoThumbnail(photo = photo, onClick = { onPhotoClick(photo) })
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: Photo,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Box(
        Modifier
            .padding(1.5.dp)
            .aspectRatio(1f)
            .background(Color(0xFF1A1A1A))
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onClick,
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.uri)
                .crossfade(true)
                .build(),
            contentDescription = photo.displayName,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
