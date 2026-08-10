package com.aube.minimallog.presentation.ui.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aube.minimallog.R
import com.aube.minimallog.presentation.ui.component.AdaptiveBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalLogApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    var memoryTopBarActions by remember { mutableStateOf<MemoryTopBarActions?>(null) }
    var logTopBarActions by remember { mutableStateOf<LogTopBarActions?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_title),
                            contentDescription = stringResource(R.string.label_app_logo),
                            modifier = Modifier
                                .height(40.dp)
                        )
                    },
                    actions = {
                        if (currentRoute == Screen.Log.route) logTopBarActions?.let { actions ->
                            IconButton(onClick = actions.onToggleView) {
                                Icon(
                                    painter = painterResource(
                                        if (actions.photoOnly) R.drawable.ic_view_cards
                                        else R.drawable.ic_view_photo_grid,
                                    ),
                                    contentDescription = stringResource(
                                        if (actions.photoOnly) R.string.action_list_view
                                        else R.string.action_photo_view,
                                    ),
                                    tint = MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                        if (currentRoute == Screen.Memory.route) memoryTopBarActions?.let { actions ->
                            Row(
                                modifier = Modifier.padding(end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                IconButton(
                                    onClick = actions.onDelete,
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete_soft),
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = Color(0xFFC95D6B),
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                                IconButton(
                                    onClick = actions.onEdit,
                                    modifier = Modifier.size(38.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_edit_soft),
                                        contentDescription = stringResource(R.string.action_edit),
                                        tint = Color(0xFF477487),
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    )
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        },
        bottomBar = {
            Column {
                AdaptiveBanner(
                    modifier = Modifier
                        .fillMaxWidth(),
                    adUnitId = stringResource(R.string.ad_unit_id_banner)
                )
                MinimalLogBottomBar(
                    navController = navController,
                    items = listOf(
                        Screen.Home,
                        Screen.Log,
                        Screen.Add,
                        Screen.Settings,
                    )
                )
            }
        }
    ) { paddingValues ->
        MainNavHost(
            navController = navController,
            onMemoryTopBarActionsChange = { memoryTopBarActions = it },
            onLogTopBarActionsChange = { logTopBarActions = it },
            modifier = Modifier
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        )
    }
}

data class MemoryTopBarActions(
    val onDelete: () -> Unit,
    val onEdit: () -> Unit,
)

data class LogTopBarActions(
    val photoOnly: Boolean,
    val onToggleView: () -> Unit,
)
