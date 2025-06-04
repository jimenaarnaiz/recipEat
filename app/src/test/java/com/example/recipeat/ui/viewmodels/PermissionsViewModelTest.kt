package com.example.recipeat.ui.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PermissionsViewModelTest {

    private lateinit var viewModel: PermissionsViewModel
    private lateinit var context: Context

    @Before
    fun setUp() {
        viewModel = PermissionsViewModel()
        context = mockk(relaxed = true)  // Context necesita estar relajado para evitar errores al llamar a métodos sin definir
    }

    @Test
    fun `setStoragePermissionGranted actualiza correctamente el estado`() {
        viewModel.setStoragePermissionGranted(true)
        assertTrue(viewModel.storagePermissionGranted.value)

        viewModel.setStoragePermissionGranted(false)
        assertFalse(viewModel.storagePermissionGranted.value)
    }


}
