package com.example.recipeat.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.recipeat.data.model.RecetaSimple
import com.example.recipeat.ui.theme.Cherry
import com.example.recipeat.ui.theme.LightYellow
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarView(
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    recetasHistorial: List<RecetaSimple>
) {
    // Definir los últimos 30 días
    val last30Days = LocalDate.now().minusDays(30)..LocalDate.now()

    // Calcular el primer y último día del mes seleccionado
    val firstDayOfMonth = selectedDate.withDayOfMonth(1)
    val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)

    // Generar lista de días entre el primer y último día del mes
    val displayedDays = generateSequence(firstDayOfMonth) { it.plusDays(1) }
        .takeWhile { it <= lastDayOfMonth }
        .toList()

    // Ajustar selectedDate si no está dentro de los últimos 30 días
    val validSelectedDate = if (selectedDate !in last30Days) {
        last30Days.start // Ajusta al primer día de los últimos 30 días si la fecha no está dentro
    } else {
        selectedDate
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7), // 7 días por fila
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        itemsIndexed(displayedDays) { index, day ->
            val isSelected = day == validSelectedDate
            val isInLast30Days = day in last30Days // Comprobamos si el día está dentro de los últimos 30 días

            // Verificar si hay recetas para el día en el historial
            val isCookedDay = recetasHistorial.any {
                val recetaDate = it.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                recetaDate == day
            }

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        when {
                            isSelected -> Cherry
                            isCookedDay -> LightYellow
                            !isInLast30Days -> Color.Transparent
                            else -> Color.White // Gris para días fuera de los últimos 30 días
                        }
                    )
                    .clickable { if (isInLast30Days) onDaySelected(day) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        !isInLast30Days -> Color.Gray
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
