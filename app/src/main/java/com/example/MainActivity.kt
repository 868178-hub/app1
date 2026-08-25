package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RunViewModel
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {
    private val runViewModel: RunViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OpenStreetMap configuration
        try {
            Configuration.getInstance().load(
                applicationContext,
                getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
            )
            Configuration.getInstance().userAgentValue = packageName
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = runViewModel)
            }
        }
    }
}
