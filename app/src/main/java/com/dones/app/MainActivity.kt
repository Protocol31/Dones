package com.dones.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dones.app.ui.DonesApp
import com.dones.app.ui.DonesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DonesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DonesTheme {
                DonesApp(viewModel)
            }
        }
    }
}
