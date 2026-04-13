package com.topit.ecotrace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.topit.ecotrace.presentation.EcoTraceAppRoot
import com.topit.ecotrace.ui.theme.EcoTraceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoTraceTheme {
                EcoTraceAppRoot()
            }
        }
    }
}