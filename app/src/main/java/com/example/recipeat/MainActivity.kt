package com.example.recipeat

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.recipeat.data.database.AppDatabase
import com.example.recipeat.utils.PlanSemanalWorker
import com.example.recipeat.data.repository.RecetaRoomRepository
import com.example.recipeat.ui.components.BottomNavBar
import com.example.recipeat.ui.theme.RecipEatTheme
import com.example.recipeat.ui.viewmodels.PermissionsViewModel
import com.example.recipeat.ui.viewmodels.RoomViewModel
import com.example.recipeat.ui.viewmodels.factories.RoomViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private lateinit var permissionsViewModel: PermissionsViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Room Database
        val database = AppDatabase.getDatabase(this)
        val recetaRoomRepository = RecetaRoomRepository(database.recetaDao(), database.favoritoDao(), database.recienteDao())

        // Crear ViewModel usando un Factory para poder pasar por param el repository en el viewModel
        val roomViewModel = ViewModelProvider(this, RoomViewModelFactory(recetaRoomRepository))
            .get(RoomViewModel::class.java)

        // Verificar si el usuario está autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            // Si el usuario está autenticado, llamamos al worker para generar el plan semanal
            PlanSemanalWorker.configurarWorker(applicationContext)
            Log.d("Main", "Se ejecutó el Worker")
        }

        // Inicializar el ViewModel de permisos
        permissionsViewModel = ViewModelProvider(this).get(PermissionsViewModel::class.java)

        setContent {
            RecipEatTheme {
                val navController = rememberNavController() // Un solo NavController
                // Creamos un estado mutable para controlar la visibilidad de la BottomNavBar
                val showBottomNav = remember { mutableStateOf(true) }

                Scaffold(
                    modifier = Modifier
                        .statusBarsPadding(),
                    bottomBar = {
                        // Solo se muestra la barra de navegación si 'showBottomNav' es true
                        if (showBottomNav.value) {
                            BottomNavBar(navController = navController, visible = true)
                        }
                    }
                ) { innerPadding ->
                    // Llamamos a NavigationGraph y pasamos la función para manejar la visibilidad de la barra
                    NavigationGraph(navController, roomViewModel, permissionsViewModel) { visible ->
                        showBottomNav.value = visible // Actualiza el estado de visibilidad
                    }
                }
            }
        }
        // Solicitar permisos necesarios
        requestNecessaryPermissions()
    }


    /**
     * Solicitar permisos necesarios para la aplicación.
     */
    private fun requestNecessaryPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Para versiones anteriores a API 33
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        // Solicitar permisos si es necesario
        if (permissionsToRequest.isNotEmpty()) {
            askMultiplePermissions.launch(permissionsToRequest.toTypedArray())
        }

        // Log de permisos solicitados y estado actual
        logRequestedPermissions()
    }

    /**
     * Registro de permisos declarados en el manifiesto y su estado actual.
     */
    private fun logRequestedPermissions() {
        val requestedPermissions = packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_PERMISSIONS
        ).requestedPermissions
        requestedPermissions?.forEach { permission ->
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            Log.d("Permissions", "Permiso: $permission, Otorgado: $isGranted")
        } ?: run {
            Log.d("Permissions", "No se encontraron permisos solicitados en el manifiesto.")
        }
    }

    /**
     * Manejo del resultado de solicitudes de permisos múltiples.
     */
    private val askMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.forEach { (permission, isGranted) ->
            if (isGranted) {
                Log.d("Permissions", "Permiso otorgado: $permission")
            } else {
                Log.e("Permissions", "Permiso denegado: $permission")
            }
        }

        // Actualiza el estado en el ViewModel según si los permisos fueron otorgados
        permissionsViewModel.setStoragePermissionGranted(
            permissions.all { it.value } // Si todos los permisos fueron otorgados
        )
    }
}

