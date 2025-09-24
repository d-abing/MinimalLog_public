package com.aube.minimallog

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.aube.minimallog.data.datastore.LocalePrefs
import com.aube.minimallog.presentation.ui.nav.MinimalLogApp
import com.aube.minimallog.presentation.util.AppLocaleManager
import com.aube.minimallog.ui.theme.MinimalLogTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val tag = kotlinx.coroutines.runBlocking {
            LocalePrefs.flow(applicationContext).first()  // import kotlinx.coroutines.flow.first
        }
        AppLocaleManager.apply(tag)

        super.onCreate(savedInstanceState)

        setContent {
            MinimalLogTheme {
                StatusBar()
                MinimalLogApp()
            }
        }
    }
}

@Composable
fun StatusBar(
    color: Color = Color.White,
    darkIcons: Boolean = true
) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = color,
            darkIcons = darkIcons
        )
    }
}