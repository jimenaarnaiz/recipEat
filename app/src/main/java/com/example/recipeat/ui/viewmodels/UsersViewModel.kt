package com.example.recipeat.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.recipeat.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream

class UsersViewModel: ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> get() = _uid


    // Función para obtener el valor del uid, ya que este solo cambia en login y logout
    fun getUidValue(): String? {
        Log.d("UsersViewModel", "getUidValue: ${uid.value}")
        return _uid.value
    }

    fun logOut() {
        FirebaseAuth.getInstance().signOut()
        _uid.value = null // Limpiar el UID y cualquier otro dato dependiente de la sesión
    }


    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        user.reload().addOnCompleteListener { reloadTask ->
                            if (reloadTask.isSuccessful) {
                                val newEmail = user.email // Nuevo email después de verificación
                                val uid = user.uid
                                val db = FirebaseFirestore.getInstance()
                                val userRef = db.collection("users").document(uid)

                                // Guardar el UID en el ViewModel
                                _uid.value = uid // Aquí guardamos el UID

                                userRef.get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val firestoreEmail = document.getString("email")

                                            if (newEmail != null && firestoreEmail != null && newEmail != firestoreEmail) {
                                                // Actualizar Firestore con el nuevo email
                                                userRef.update("email", newEmail)
                                                    .addOnSuccessListener {
                                                        Log.d("Login", "Email actualizado en Firestore.")
                                                        onResult(true)
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("Login", "Error al actualizar email en Firestore", e)
                                                        onResult(false)
                                                    }
                                            } else {
                                                onResult(true)
                                            }
                                        } else {
                                            Log.e("Login", "No se encontró el usuario en Firestore.")
                                            onResult(false)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Login", "Error al obtener datos del usuario en Firestore", e)
                                        onResult(false)
                                    }
                            } else {
                                Log.e("Login", "Error al recargar usuario")
                                onResult(false)
                            }
                        }
                    } else {
                        Log.e("Login", "Usuario no autenticado después del login.")
                        onResult(false)
                    }
                } else {
                    Log.e("Login", "Error al iniciar sesión", task.exception)
                    onResult(false)
                }
            }
    }



    fun register(username: String, email: String, password: String, onResult: (Boolean) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    // Crear un objeto Usuario con datos del usuario
                    val userData = User(
                        //id = user?.uid ?: "", // El UID de Firebase
                        username = username,
                        image = "", // URL de la imagen (si la tienes)
                        email = email
                    )

                    // Almacenar la información del usuario en Firestore
                    user?.let {
                        db.collection("users")
                            .document(it.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                // Mostrar mensaje de éxito en el registro
                                //Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                onResult(true) // Registro exitoso
                            }
                            .addOnFailureListener { e ->
                                // Si falla la escritura en Firestore, manejar el error
                                //Toast.makeText(context, "Error al guardar los datos en Firestore", Toast.LENGTH_SHORT).show()
                                Log.w("Register", "Error writing document", e)
                                onResult(false) // Error al almacenar en la base de datos
                            }
                    }
                } else {
                    // Si el registro en Firebase Authentication falla
                    //Toast.makeText(context, "Error en el registro. Intenta nuevamente.", Toast.LENGTH_SHORT).show()
                    Log.w("Register", "Error en el registro", task.exception)
                    onResult(false)  // Retorna false si el registro falla
                }
            }
    }


    // Función para obtener el username de un usuario
    fun obtenerUsername(onResult: (String?) -> Unit) {
        val userRef = db.collection("users").document(_uid.value.toString())

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    onResult(username) // Devuelve el username si existe
                } else {
                    Log.d("UsersViewModel", "Usuario con uid $uid no encontrado")
                    onResult(null) // Usuario no encontrado
                }
            }
            .addOnFailureListener {
                onResult(null) // Error al obtener el usuario
            }
    }

    fun obtenerUsuarioCompleto(uid: String, onResult: (String?, String?, String?) -> Unit) {
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtener los valores de Firestore
                    val username = document.getString("username")
                    val profileImageUrl = document.getString("image")
                    val email = document.getString("email")

                    onResult(username, profileImageUrl, email) // Devuelve username y profileImageUrl
                } else {
                    onResult(null, null, null) // Usuario no encontrado
                }
            }
            .addOnFailureListener {
                onResult(null, null, null) // Error al obtener datos
            }
    }


    // Devuelve el objeto User dado el uid
    fun obtenerUsuarioCompleto(uid: String, onResult: (User?) -> Unit) {
        val userRef = db.collection("users").document(uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtener los valores de Firestore
                    val username = document.getString("username")
                    val profileImageUrl = document.getString("image")
                    val email = document.getString("email")

                    // Crear un objeto User con la información obtenida
                    val user = User(
                        username = username ?: "",
                        image = profileImageUrl,
                        email = email ?: ""
                    )

                    // Devolver el usuario a través del callback
                    onResult(user)
                } else {
                    // Si el documento no existe, devolver null
                    onResult(null)
                }
            }
            .addOnFailureListener {
                // En caso de error, devolver null
                onResult(null)
            }
    }

    /**
     * TODO ELIMINAR LA OPCION DE CAMBIAR CORREO
     */
    fun actualizarUserProfile(
        newUsername: String?,
        newEmail: String?,
        newProfileImage: String?,
        onResult: (Boolean) -> Unit, // Retornamos mensaje de error opcional
        uid: String
    ) {
        val user = auth.currentUser
        val db = FirebaseFirestore.getInstance()

        // Verificar que el usuario está autenticado antes de continuar
        if (user == null) {
            Log.e("UpdateUser", "No authenticated user found.")
            onResult(false, /*"Usuario no autenticado."*/)
            return
        }

        Log.d("UpdateUser", "uid: ${uid}  y user.uid: ${user.uid}")

        // Verificar si el nuevo email ya está registrado antes de actualizarlo
        if (newEmail != null && newEmail != user.email) {
            user.verifyBeforeUpdateEmail(newEmail)
                .addOnCompleteListener { verifyTask ->
                    if (verifyTask.isSuccessful) {
                        Log.d(
                            "UpdateUser",
                            "Verification email sent. User must verify before update."
                        )
                        onResult(true /*"Se ha enviado un correo de verificación. Verifícalo antes de actualizar."*/)
                    } else {
                        Log.e(
                            "UpdateUser",
                            "Failed to send verification email",
                            verifyTask.exception
                        )
                        onResult(false /*"Error al enviar el correo de verificación."*/)
                    }
                }
            return
        }

        // Actualizar username y profile image en Firestore
        if (newUsername != null || newProfileImage != null) {
            val userRef = db.collection("users").document(uid)
            val updatedData = mutableMapOf<String, Any>()

            newUsername?.let { updatedData["username"] = it }
            newProfileImage?.let { updatedData["image"] = it }

            userRef.update(updatedData)
                .addOnSuccessListener {
                    Log.d("UpdateUser", "User data updated successfully in Firestore")
                    onResult(true)
                }
                .addOnFailureListener { e ->
                    Log.e("UpdateUser", "Failed to update user data in Firestore", e)
                    onResult(false, /*"Error al actualizar los datos en Firestore."*/)
                }
        } else {
            onResult(true, )
        }
    }


//    fun confirmEmailUpdate(newEmail: String, uid: String, onResult: (Boolean, String?) -> Unit) {
//        val user = auth.currentUser
//        val db = FirebaseFirestore.getInstance()
//
//        if (user == null) {
//            onResult(false, "Usuario no autenticado.")
//            return
//        }
//
//        // Verificar que el usuario haya confirmado el email en Firebase
//        user.reload().addOnCompleteListener {
//            if (user.email == newEmail) {
//                db.collection("users").document(uid)
//                    .update("email", newEmail)
//                    .addOnSuccessListener {
//                        onResult(true, "Correo actualizado correctamente en Firestore.")
//                    }
//                    .addOnFailureListener { e ->
//                        Log.e("UpdateUser", "Failed to update email in Firestore", e)
//                        onResult(false, "Error al actualizar el correo en Firestore.")
//                    }
//            } else {
//                onResult(false, "El correo aún no ha sido verificado.")
//            }
//        }
//    }


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
            val fileName = if (recetaId.isNullOrBlank()) "profile_image.jpg" else "$recetaId.jpg"
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use {
                // guarda el Bitmap en el archivo como un archivo JPEG con la máxima calidad (100).
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            inputStream?.close()
            Log.d("ImageSave", "Imagen guardada en: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("ImageSaveError", "Error al guardar la imagen: ${e.message}")
        }
    }

    /**
     * Busca el archivo, lo lee y lo convierte nuevamente en un Bitmap.
     * Si el archivo no existe o hay un error, se devuelve null.
     */
    fun loadImageFromFile(context: Context, recetaId: String?): Bitmap? {
        try {
            val imagen = if (recetaId.isNullOrBlank()) "profile_image.jpg" else "$recetaId.jpg"
            val file = File(context.filesDir, imagen)
            if (file.exists()) {
                Log.d("ImageLoad", "Cargando Imagen de receta : ${file.absolutePath}")
                return BitmapFactory.decodeFile(file.absolutePath)
            }
        } catch (e: Exception) {
            Log.e("ImageLoad", "Error al cargar la imagen", e)
        }
        return null
    }


    // Eliminar la imagen de la receta si borra la receta
    fun deleteImage(context: Context, recetaId: String) {
        val file = File(context.filesDir, "$recetaId.jpg")
        if (file.exists()) {
            file.delete()
            Log.d("ImageDelete", "Imagen de receta eliminada: ${file.absolutePath}")
        }
    }



    //TODO eliminar cuando tenga todas las recetas bien
//    fun eliminarRecetasDeBebidaFirestore() {
//        val db = FirebaseFirestore.getInstance()
//        var count = 0
//
//        db.collection("recetas")
//            .get()
//            .addOnSuccessListener { result ->
//                val batch = db.batch() // Usamos batch para mejorar el rendimiento
//                for (document in result) {
//                    val dishTypes = document.get("dishTypes") as? List<String> ?: emptyList()
//
//                    if (dishTypes.contains("drink") || dishTypes.contains("beverage")) {
//                        batch.delete(db.collection("recetas").document(document.id))
//                        count++
//                    }
//                }
//
//                // Ejecutamos la operación en batch
//                batch.commit()
//                    .addOnSuccessListener {
//                        println("Total de recetas eliminadas: $count")
//                    }
//                    .addOnFailureListener { exception ->
//                        println("Error al eliminar recetas: $exception")
//                    }
//            }
//            .addOnFailureListener { exception ->
//                println("Error al obtener las recetas: $exception")
//            }
//    }








}