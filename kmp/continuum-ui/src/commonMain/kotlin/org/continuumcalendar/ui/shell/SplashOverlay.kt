package org.continuumcalendar.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.continuumcalendar.ui.theme.LocalContinuumPalette

@Composable
fun SplashOverlay(
    visibleMs: Long = 900,
    onFinished: () -> Unit = {},
) {
    var show by remember { mutableStateOf(true) }
    val palette = LocalContinuumPalette.current
    LaunchedEffect(Unit) {
        delay(visibleMs)
        show = false
        onFinished()
    }
    if (!show) return
    Box(
        Modifier
            .fillMaxSize()
            .background(palette.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Continuum",
            color = palette.accent,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            modifier = Modifier.padding(16.dp),
        )
    }
}
