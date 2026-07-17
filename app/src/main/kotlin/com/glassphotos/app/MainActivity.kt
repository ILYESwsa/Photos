package com.glassphotos.app

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.glassphotos.app.data.Photo
import com.glassphotos.app.ui.GlassPhotosRoot

class MainActivity : ComponentActivity() {

    private val imagePermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var hasPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, imagePermission) ==
                        PackageManager.PERMISSION_GRANTED
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> hasPermission = granted }

            // Handles the RecoverableSecurityException flow required on API 30+ to delete
            // a MediaStore item the app doesn't own the write permission for by default.
            var pendingDeleteCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
            val deleteLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    pendingDeleteCallback?.invoke()
                }
                pendingDeleteCallback = null
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GlassPhotosRoot(
                        hasPermission = hasPermission,
                        onRequestPermission = { permissionLauncher.launch(imagePermission) },
                        onDeletePhoto = { photo, onDeleted ->
                            deletePhoto(photo, onDeleted, deleteLauncher) { callback ->
                                pendingDeleteCallback = callback
                            }
                        },
                    )
                }
            }
        }
    }

    private fun deletePhoto(
        photo: Photo,
        onDeleted: () -> Unit,
        deleteLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>,
        setPendingCallback: (() -> Unit) -> Unit,
    ) {
        try {
            contentResolver.delete(photo.uri, null, null)
            onDeleted()
        } catch (e: SecurityException) {
            // On API 29+, deleting another app's (or MediaStore-owned) media requires
            // user confirmation via a system dialog surfaced through this recoverable exception.
            val intentSender = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    MediaStore.createDeleteRequest(
                        contentResolver,
                        listOf(photo.uri)
                    ).intentSender
                }
                e is RecoverableSecurityException -> e.userAction.actionIntent.intentSender
                else -> null
            }
            if (intentSender != null) {
                setPendingCallback(onDeleted)
                deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
        }
    }
}
