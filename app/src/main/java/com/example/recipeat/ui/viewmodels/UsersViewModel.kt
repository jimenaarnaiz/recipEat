package com.example.recipeat.ui.viewmodels

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeat.data.model.User
import com.example.recipeat.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UsersViewModel(application: Application, private val userRepository: UserRepository): AndroidViewModel(application){

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> get() = _uid


    init {
        isSessionActive { isActive ->
            Log.d("UserViewModel", "Sesión activa: $isActive")
            if (isActive) {
                obtenerUid() // Solo si hay sesión activa
            }
        }
    }


    private fun obtenerUid() {
        val currentUid = userRepository.getUidValue()
        _uid.value = currentUid
    }


    // Función para obtener el UID del usuario
    fun getUidValue(): String? {
        return userRepository.getUidValue()
    }

    // Función para iniciar sesión
    fun login(email: String, password: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val resultMessage = userRepository.login(email, password)
                // Llama al callback con el resultado
                onResult(resultMessage)
                Log.d("UserViewModel", "Login result: $resultMessage")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error durante el login", e)
                // En caso de error, pasa 'false' al callback
                onResult("Unknown error during registration: ${e.localizedMessage}")
            }
        }
    }


    // Función para registrar un nuevo usuario
    fun register(username: String, email: String, password: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val resultMessage = userRepository.register(username, email, password)
                // Llama al callback con el resultado
                onResult(resultMessage)
                Log.d("UserViewModel", "Register result: $resultMessage")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error durante el registro", e)
                onResult("Unknown error during registration: ${e.localizedMessage}")
            }
        }
    }


    fun isSessionActive(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val sessionActive = userRepository.isSessionActive()
                onResult(sessionActive)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error al verificar la sesión", e)
                onResult(false)
            }
        }
    }


    // Función para cerrar sesión
    fun logOut() {
        viewModelScope.launch {
            try {
                userRepository.logOut()
                Log.d("UserViewModel", "Sesión cerrada")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error al cerrar sesión", e)
            }
        }
    }

    // Función para obtener el username del usuario
    // En el ViewModel
    fun obtenerUsername(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val username = userRepository.obtenerUsername() // Obtenemos el username desde el repositorio
                onResult(username) // Llamamos al callback con el resultado
                Log.d("UserViewModel", "Username obtenido: $username")
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error al obtener el username", e)
                onResult(null) // En caso de error, devolvemos null
            }
        }
    }


    // Función para manejar el envío del enlace de restablecimiento de contraseña
    fun sendPasswordResetEmail(email: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = userRepository.sendPasswordResetEmail(email)
            onResult(result)
        }
    }

    // Función para actualizar el perfil del usuario
    fun actualizarUserProfile(
        newUsername: String?,
        newProfileImage: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val success = userRepository.actualizarUserProfile(newUsername, newProfileImage)
            onResult(success)
        }
    }


    // Llama al repositorio para obtener el usuario completo (con los campos específicos)
    fun obtenerUsuarioCompletoPorCampos(uid: String, onResult: (String, String?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val (username, profileImageUrl, email) = userRepository.obtenerUsuarioCompletoPorCampos(uid)
                // Llama al callback con los valores obtenidos
                if (username != null) {
                    onResult(username, profileImageUrl, email)
                }
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error al obtener el usuario completo", e)
                // Llama al callback con valores nulos en caso de error
                onResult("", null, null)
            }
        }
    }

    // Llama al repositorio para obtener el objeto completo de User
    fun obtenerUsuarioCompleto(uid: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            try {
                val user = userRepository.obtenerUsuarioCompleto(uid)
                onResult(user)
            } catch (e: Exception) {
                Log.e("UserViewModel", "Error al obtener el usuario completo", e)
                onResult(null)
            }
        }
    }


    fun saveImageLocally(context: Context, imageUri: Uri, recetaId: String?) {
        userRepository.saveImageLocally(context, imageUri, recetaId)
    }

    fun loadImageFromFile(context: Context, recetaId: String?): Bitmap? {
        return userRepository.loadImageFromFile(context, recetaId)
    }

    fun deleteImage(context: Context, recetaId: String) {
        userRepository.deleteImage(context, recetaId)
    }






}