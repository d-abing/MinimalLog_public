package com.aube.minimallog.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aube.minimallog.R
import com.aube.minimallog.presentation.model.MemoryItem
import com.aube.minimallog.presentation.viewmodel.MemoryViewModel
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onEdit: (MemoryItem) -> Unit,
    vm: MemoryViewModel = hiltViewModel()
) {

    val item by vm.memory.collectAsState()

    item?.let { item ->

        var showDeleteConfirm by remember { mutableStateOf(false) }
        val dateText = remember(item.date) {
            DateTimeFormatter.ofPattern("LLLL d, yyyy", Locale.getDefault()).format(item.date)
        }
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Image
            if (item.imageUrl != null) {
                AsyncImage(
                    model = if (item.imageUrl.startsWith("/")) File(item.imageUrl) else item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {

                    AsyncImage(
                        model = R.drawable.app_logo,
                        contentDescription = stringResource(R.string.label_app_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                    )
                }
            }

            // Content
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondary
                )

                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "${item.description}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    FlowTagChips(tags = item.tags)
                }

                Spacer(Modifier.height(24.dp))

                // Bottom actions in content (Edit / Delete / Share)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(Modifier.weight(0.5f))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        border = BorderStroke(0.3.dp, MaterialTheme.colorScheme.onSecondary),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    Spacer(Modifier.weight(0.2f))
                    OutlinedButton(
                        onClick = { onEdit(item) },
                        border = BorderStroke(0.3.dp, MaterialTheme.colorScheme.onSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                    Spacer(Modifier.weight(0.5f))
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.text_delete_confirm)) },
                text = { Text(stringResource(R.string.text_delete_confirm_detail)) },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        vm.delete()
                        onDelete()
                    }) { Text(stringResource(R.string.action_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        }
    } ?: Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun FlowTagChips(tags: List<String>) {
    // 간단한 FlowRow 대체 (Compose 1.5+의 FlowRow 사용 가능하면 교체)
    val rows = tags.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.secondary)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) { Text(tag, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}