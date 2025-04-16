package com.example.recipeat.ui.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

class PermissionsViewModel : ViewModel() {
    private val _storagePermissionGranted = mutableStateOf(false)
    val storagePermissionGranted: State<Boolean> = _storagePermissionGranted

    fun setStoragePermissionGranted(granted: Boolean) {
        _storagePermissionGranted.value = granted
    }

    fun checkStoragePermission(context: Context) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
               Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        _storagePermissionGranted.value = granted
    }

}
