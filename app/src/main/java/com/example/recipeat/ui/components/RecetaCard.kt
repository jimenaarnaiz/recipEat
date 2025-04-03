package com.example.recipeat.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.viewmodels.UsersViewModel

@Composable
fun RecetaCard(receta: Receta, navController: NavController, usersViewModel: UsersViewModel) {

    val esDeUser = receta.userId.isNotBlank()

    Log.d("RecetaCard", "idReceta: ${receta.id} esDeUser: $esDeUser userID: ${receta.userId} ")

    Card(
        modifier = Modifier
            .fillMaxWidth() // Hace que la Card ocupe tdo el ancho disponible
            .padding(vertical = 8.dp) // Separación vertical entre las Cards
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .clickable { navController.navigate("detalles/${receta.id}/$esDeUser") },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally // Alinear el contenido en el centro
        ) {

            val context = LocalContext.current
            val bitmap = usersViewModel.loadImageFromFile(context, recetaId = receta.id)

            // Cargar la imagen de la receta con esquinas redondeadas y sin padding
            // Para el caso de carga remota
            var imagen by remember { mutableStateOf("") }
            imagen = if (receta.image?.isNotBlank() == true) {
                receta.image
            } else {
                "android.resource://com.example.recipeat/${R.drawable.food_placeholder}"
            }

            // Determinamos el painter según esDeUser
            val painter = if (esDeUser) {
                if (receta.image.toString().isBlank() || bitmap == null) {
                    painterResource(id = R.drawable.food_placeholder)
                } else {
                    BitmapPainter(bitmap.asImageBitmap())
                }
            } else {
                rememberAsyncImagePainter(imagen)
            }

            Image(
                painter = painter,
                contentDescription = receta.title,
                modifier = Modifier
                    .fillMaxWidth() // La imagen ocupa todo el ancho de la Card
                    .height(220.dp) // Mantener la altura fija
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    ),
                contentScale = ContentScale.Crop // La imagen se recorta para llenar el espacio
            )

            // Título de la receta
            Text(
                text = receta.title,
                modifier = Modifier
                    .padding(top = 8.dp) // Padding entre la imagen y el texto
                    .padding(start = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, // Limitar el texto a una sola línea
                overflow = TextOverflow.Ellipsis // Truncar el texto si es muy largo

            )

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically, // Alineación vertical de los íconos y el texto
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // Espaciado entre los íconos y los textos
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Ícono y texto para el tiempo
                    Icon(
                        imageVector = Icons.Default.AccessTimeFilled,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 1.dp) // Espaciado entre el ícono y el texto
                    )


                    Text(
                        text = receta.time.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Ícono y texto para el número de ingredientes usados
                    Icon(
                        imageVector = Icons.Default.ShoppingBasket,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 1.dp) // Espaciado entre el ícono y el texto
                    )

                    Text(
                        text = receta.usedIngredientCount.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Log.d("RecetaCard", "home steps: ${receta.steps.size}")
                }
            }
        }
    }
}