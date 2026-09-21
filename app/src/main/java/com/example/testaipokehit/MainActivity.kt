package com.example.testaipokehit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.testaipokehit.screens.PokeHitApp
import com.example.testaipokehit.ui.theme.TestAiPokeHitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestAiPokeHitTheme {
                PokeHitApp()
            }
        }
    }
}