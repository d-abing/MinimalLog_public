package com.aube.minimallog.presentation.ui.nav

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import com.aube.minimallog.R

sealed class Screen(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: Any // ImageVector 또는 @DrawableRes Int
) {
    object Home    : Screen("home",               R.string.nav_home,    Icons.Default.Home)
    object Log     : Screen("log",                R.string.nav_log,     R.drawable.box)
    object Memory  : Screen("memory/{memoryId}",  R.string.nav_memory,  R.drawable.box)
    object Add     : Screen("add?editId={editId}",R.string.nav_add,     Icons.Default.Add)
    object Settings: Screen("settings",           R.string.nav_settings,Icons.Default.Settings)
}
