package com.dones.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dones.app.ui.DonesApp
import com.dones.app.ui.DonesTheme
import com.dones.app.ui.IntroScreen

class MainActivity : ComponentActivity() {

    private val viewModel: DonesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DonesTheme {
                // Intro de la iglesia (luz de Ergos + logo + "Llamados") cada vez que se abre la app.
                var showIntro by rememberSaveable { mutableStateOf(true) }
                Crossfade(targetState = showIntro, animationSpec = tween(500), label = "intro") { intro ->
                    if (intro) IntroScreen(onFinished = { showIntro = false }) else DonesApp(viewModel)
                }
            }
        }
    }
}
