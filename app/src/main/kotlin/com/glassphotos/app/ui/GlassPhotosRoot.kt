package com.glassphotos.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.glassphotos.app.data.Photo
import com.glassphotos.app.data.PhotoRepository
import com.glassphotos.app.ui.glasscomponents.BottomTab
import com.glassphotos.app.ui.glasscomponents.GlassBottomBar
import com.glassphotos.app.ui.glasscomponents.GlassTopBar

private val tabs = listOf(
    BottomTab("Library", "🖼"),
    BottomTab("Search", "🔍"),
)

@Composable
fun GlassPhotosRoot(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onDeletePhoto: (Photo, onDeleted: () -> Unit) -> Unit,
) {
    if (!hasPermission) {
        PermissionScreen(onGrantClick = onRequestPermission)
        return
    }

    val context = LocalContext.current
    var allPhotos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var favoriteIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(Unit) {
        allPhotos = PhotoRepository.loadAllPhotos(context)
        isLoading = false
    }

    val sections = remember(allPhotos) { PhotoRepository.groupByDay(allPhotos) }
    val albums = remember(allPhotos) { PhotoRepository.groupByAlbum(allPhotos) }
    val backdrop = rememberLayerBackdrop()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            Modifier
                .layerBackdrop(backdrop)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> LibraryScreen(
                    sections = sections,
                    isLoading = isLoading,
                    onPhotoClick = { photo ->
                        selectedPhotoIndex = allPhotos.indexOf(photo)
                    },
                    backdrop = backdrop,
                )
                1 -> SearchScreen(
                    albums = albums,
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    backdrop = backdrop,
                    onAlbumClick = { /* future: open album-filtered grid */ },
                )
            }
        }

        GlassTopBar(
            title = if (selectedTab == 0) "Library" else "Search",
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        GlassBottomBar(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        selectedPhotoIndex?.let { index ->
            PhotoDetailScreen(
                photos = allPhotos,
                initialIndex = index,
                favoriteIds = favoriteIds,
                onToggleFavorite = { photo ->
                    favoriteIds = if (favoriteIds.contains(photo.id)) {
                        favoriteIds - photo.id
                    } else {
                        favoriteIds + photo.id
                    }
                },
                onShare = { photo ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, photo.uri as Uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share photo"))
                },
                onDelete = { photo ->
                    onDeletePhoto(photo) {
                        allPhotos = allPhotos.filterNot { it.id == photo.id }
                        selectedPhotoIndex = null
                    }
                },
                onDismiss = { selectedPhotoIndex = null },
            )
        }
    }
}
