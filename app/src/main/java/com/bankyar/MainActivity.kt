package com.bankyar

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bankyar.navigation.BankYarNavGraph
import com.bankyar.ui.theme.BankYarTheme
import com.bankyar.ui.viewmodels.ThemeViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BankYarTheme(darkTheme = isDarkMode) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        BankYarNavGraph()
                    }
                }
            }
        }
    }
}
