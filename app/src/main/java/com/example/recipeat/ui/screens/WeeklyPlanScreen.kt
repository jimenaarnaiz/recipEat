package com.example.recipeat.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.recipeat.R
import com.example.recipeat.data.model.DayMeal
import com.example.recipeat.data.model.DishTypes
import com.example.recipeat.data.model.Receta
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import com.example.recipeat.ui.viewmodels.PlanViewModel
import com.example.recipeat.ui.viewmodels.UsersViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeeklyPlanScreen(navController: NavHostController, planViewModel: PlanViewModel, usersViewModel: UsersViewModel) {
    // Definir un estado para el día seleccionado
    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek) }

    val uid = usersViewModel.getUidValue()

    // Llamar al iniciar
    LaunchedEffect(Unit) {
        planViewModel.iniciarGeneracionPlanSemanal(uid.toString())
        if (!planViewModel.esPrimeraVez()) planViewModel.obtenerPlanSemanal(uid.toString())
    }

    val planSemanal by planViewModel.planSemanal.observeAsState()
    // Verifica que el plan semanal no sea nulo antes de usarlo
    val weeklyPlan = planSemanal?.weekMeals ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título
        Text(
            text = "Weekly Meal Plan",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Días de la semana
        Row(
            //horizontalArrangement = Arrangement.SpaceEvenly, // Para distribuir los días uniformemente
            modifier = Modifier.fillMaxWidth()
        ) {
            // Llamamos a DayButton para cada día de la semana
            DayOfWeek.entries.forEach { day ->
                DayButton(day = day, selectedDay = selectedDay, onClick = { selectedDay = day })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar recetas del día seleccionado
        DayDetailsContent(weeklyPlan = weeklyPlan, selectedDay = selectedDay, navController)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayButton(day: DayOfWeek, selectedDay: DayOfWeek, onClick: (DayOfWeek) -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd")
    val date = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(day.ordinal.toLong())
    val formattedDate = date.format(dateFormatter)
    val isToday = day == DayOfWeek.from(LocalDate.now())

    // Usamos un Surface en lugar de un Button para que no haya scroll y se ajusten todos los días en la pantalla
    Surface(
        modifier = Modifier
            .padding(1.5.dp)
            .width(48.dp)
            .then(
                if (isToday) Modifier.border(2.dp, Color.DarkGray, RoundedCornerShape(8.dp)) else Modifier
            ),
        color = if (day == selectedDay) Cherry else LightYellow,
        shape = RoundedCornerShape(8.dp),
        onClick = { onClick(day) } // El onClick aún es funcional
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = day.name.take(3), // Primeras 3 letras del nombre del día
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold, // Aplica la negrita
                color = if (day == selectedDay) Color.White else Color.Black
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = if (day == selectedDay) Color.White else Color.Black
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayDetailsContent(weeklyPlan: Map<DayOfWeek, DayMeal>, selectedDay: DayOfWeek, navController: NavHostController) {
    // Aquí usamos el día seleccionado para obtener las recetas del día correspondiente
    val selectedMeals = weeklyPlan[DayOfWeek.entries.find { it.name == selectedDay.name }]

    if (selectedMeals != null) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MealDetailCard(dishType = DishTypes.breakfast.name, receta = selectedMeals.breakfast, navController)
            MealDetailCard(dishType = DishTypes.lunch.name, receta = selectedMeals.lunch, navController)
            MealDetailCard(dishType = DishTypes.dinner.name, receta = selectedMeals.dinner, navController)
        }
    }
}


@Composable
fun MealDetailCard(dishType: String, receta: Receta, navController: NavHostController) {
    val esDeUser = receta.userId.isNotBlank()

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = dishType.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyLarge,
            //fontWeight = FontWeight.Bold, // Aplica la negrita
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, shape = RoundedCornerShape(16.dp))
                .clickable { navController.navigate("detalles/${receta.id}/$esDeUser") },
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = receta.image,
                        placeholder = painterResource(id = R.drawable.food_placeholder),
                        error = painterResource(id = R.drawable.food_placeholder)
                    ),
                    contentDescription = receta.title,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = receta.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Tiempo de preparación",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${receta.time}'",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            imageVector = Icons.Default.ShoppingBasket,
                            contentDescription = "Ingredientes",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${receta.usedIngredientCount}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
