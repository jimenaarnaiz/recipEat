package com.example.recipeat.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.recipeat.data.model.User
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class UserRepository(
    private val sharedPreferences: SharedPreferences,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {


    private val sessionIniciadaKey = "sesionIniciada_"

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> get() = _uid


    init {
        // Al iniciar el Repository, verifica si hay un usuario autenticado
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _uid.value = currentUser.uid
            // Guardamos el estado de la sesión por UID único
            sharedPreferences.edit().putBoolean(sessionIniciadaKey + currentUser.uid, true).apply()
        } else {
            // Si no hay un usuario autenticado, marca que la sesión no está activa
            sharedPreferences.edit().clear().apply()
        }
    }



    fun getUidValue(): String? {
        return _uid.value
    }


    // Login utilizando corrutinas
    suspend fun login(email: String, password: String): String {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                val uid = user.uid
                val userRef = db.collection("users").document(uid)

                // Guardar el UID
                _uid.value = uid

                // Verificar si el usuario existe en Firestore
                val document = userRef.get().await()
                if (document.exists()) {
                    sharedPreferences.edit().putBoolean(sessionIniciadaKey + uid, true).apply()
                    Log.d("UsersRepository", "Usuario $uid logueado exitosamente")
                    "success"
                } else {
                    Log.e("UsersRepository", "No se encontró el usuario en Firestore.")
                    "Error: usuario no encontrado en Firestore."
                }
            } else {
                Log.e("UsersRepository", "Usuario no autenticado después del login.")
                "Error: usuario no autenticado después del login."
            }
        } catch (e: Exception) {
            Log.e("UsersRepository", "Error al iniciar sesión", e)
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> "Invalid credentials. Please, check your email or password."
                is FirebaseAuthInvalidUserException -> "User does not exist. Please, check your email."
                is FirebaseNetworkException -> "Network error. Please, check your internet connection."
                else -> "Login error: ${e.localizedMessage}"
            }
        }
    }


    fun logOut() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Cuando hacemos logout, eliminamos el estado de la sesión de ese UID
            sharedPreferences.edit().putBoolean(sessionIniciadaKey + currentUser.uid, false).apply()
        }
        auth.signOut()
        _uid.value = null
    }

    // Función para verificar si la sesión está activa
    suspend fun isSessionActive(): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        val sessionActive = currentUser != null && sharedPreferences.getBoolean(
            sessionIniciadaKey + currentUser.uid,
            false
        )
        Log.d("SessionStatus", "Sesión activa: $sessionActive")
        return@withContext sessionActive
    }

    //con rutinas
    suspend fun register(username: String, email: String, password: String): String {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                val uid = user.uid
                val userRef = db.collection("users").document(uid)
                userRef.set(mapOf("username" to username, "email" to email)).await()
                Log.d("UsersRepository", "Usuario registrado exitosamente")
                "success"
            } else {
                Log.e("UsersRepository", "Usuario no autenticado después del registro.")
                "Error: usuario no autenticado después del registro."
            }
        } catch (e: Exception) {
            Log.e("UsersRepository", "Error al registrar el usuario", e)
            when (e) {
                is FirebaseAuthUserCollisionException -> "This email is already in use by another account."
                is FirebaseNetworkException -> "Network error. Please check your internet connection."
                else -> "Registration error: ${e.localizedMessage}"
            }
        }
    }



    suspend fun obtenerUsername(): String? {
        try {
            val userRef = db.collection("users").document(_uid.value.toString())
            val document = userRef.get().await()
            return if (document.exists()) {
                document.getString("username")
            } else {
                Log.d("UsersViewModel", "ObtenerUsername: Usuario con uid ${_uid.value} no encontrado")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error al obtener el username", e)
            return null
        }
    }

    // Devuelve el objeto User dado el uid
    suspend fun obtenerUsuarioCompleto(uid: String): User? {
        try {
            val userRef = db.collection("users").document(uid)
            val document = userRef.get().await()

            return if (document.exists()) {
                val username = document.getString("username")
                val email = document.getString("email")

                User(
                    username = username ?: "",
                    email = email ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error al obtener el usuario completo", e)
            return null
        }
    }

    // Función suspendida para obtener solo los campos específicos del usuario (username, profileImageUrl, email)
    suspend fun obtenerUsuarioCompletoPorCampos(uid: String): Triple<String?, String?, String?> {
        val userRef = db.collection("users").document(uid)
        return try {
            val document = userRef.get().await()
            if (document.exists()) {
                val username = document.getString("username")
                val profileImageUrl = document.getString("image")
                val email = document.getString("email")
                Triple(username, profileImageUrl, email) // Retorna los valores
            } else {
                Triple(null, null, null) // Usuario no encontrado
            }
        } catch (e: Exception) {
            // Error al obtener datos
            Triple(null, null, null)
        }
    }


    // cambia el username y la foto
    suspend fun actualizarUserProfile(newUsername: String?, newProfileImage: String?): Boolean {
        try {
            val userRef = db.collection("users").document(_uid.value.toString())
            val updatedData = mutableMapOf<String, Any>()

            newUsername?.let { updatedData["username"] = it }
            newProfileImage?.let { updatedData["image"] = it }

            userRef.update(updatedData).await()
            Log.d("UserRepository", "User data updated successfully in Firestore")
            return true
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to update user data in Firestore", e)
            return false
        }
    }


    /**
     * Guarda la imagen de perfil en el almacenamiento local del dispositivo
     * (en el directorio de archivos internos de la aplicación) bajo el nombre profile_image.jpg
     * o el id de la receta a crear o editar.
     */
    fun saveImageLocally(context: Context, imageUri: Uri, recetaId: String?) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val fileName =
                if (recetaId.isNullOrBlank()) "profile_image${_uid.value}.jpg" else "$recetaId.jpg"
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use {
                // guarda el Bitmap en el archivo como un archivo JPEG con la máxima calidad (100).
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            inputStream?.close()
            Log.d("UserRepository", "Imagen guardada en: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error al guardar la imagen: ${e.message}")
        }
    }

    /**
     * Busca el archivo, lo lee y lo convierte nuevamente en un Bitmap.
     * Si el archivo no existe o hay un error, se devuelve null.
     */
    fun loadImageFromFile(context: Context, recetaId: String?): Bitmap? {
        try {
            val imagen =
                if (recetaId.isNullOrBlank()) "profile_image${_uid.value}.jpg" else "$recetaId.jpg"
            val file = File(context.filesDir, imagen)
            if (file.exists()) {
                Log.d("UserRepository", "Cargando Imagen de receta : ${file.absolutePath}")
                return BitmapFactory.decodeFile(file.absolutePath)
            }else{
                Log.d("UserRepository", "Archivo no encontrado: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error al cargar la imagen", e)
        }
        return null
    }


    // Eliminar la imagen de la receta si borra la receta
    fun deleteImage(context: Context, recetaId: String) {
        val file = File(context.filesDir, "$recetaId.jpg")
        if (file.exists()) {
            file.delete()
            Log.d("UserRepository", "Imagen de receta eliminada: ${file.absolutePath}")
        }
    }




}













