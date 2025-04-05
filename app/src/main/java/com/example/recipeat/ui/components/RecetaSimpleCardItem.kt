package com.example.recipeat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.ui.viewmodels.UsersViewModel

// Para Favs e Historial
@Composable
fun RecetaSimpleCardItem(
    recetaId: String,
    title: String,
    image: String,
    navController: NavHostController,
    esDeUser: Boolean,
    usersViewModel: UsersViewModel
) {

    val context = LocalContext.current
    val bitmap = usersViewModel.loadImageFromFile(context, recetaId = recetaId)

    // Determinamos el painter según esDeUser
    val painter = if (esDeUser) {
        if (image.isBlank() || bitmap == null) {
            painterResource(id = R.drawable.food_placeholder)
        } else {
            BitmapPainter(bitmap.asImageBitmap())
        }
    } else {
        rememberAsyncImagePainter(image, error = painterResource(id = R.drawable.food_placeholder))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .shadow(4.dp)
            .clickable {
                navController.navigate("detalles/$recetaId/$esDeUser")
            },
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painter,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop // Asegurarse de que la imagen se recorte y llene el espacio
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(8.dp),
                maxLines = 1, // Limitar el texto a una sola línea
                overflow = TextOverflow.Ellipsis // Truncar el texto si es muy largo
            )
        }
    }
}