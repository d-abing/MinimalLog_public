package com.aube.minimallog.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aube.minimallog.R
import com.aube.minimallog.presentation.viewmodel.MemoryViewModel
import java.io.File

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    isSeeMoreClick: (Long) -> Unit = {},
) {
    val vm: MemoryViewModel = hiltViewModel()
    val item by vm.memories.collectAsState()
    val count = item.size
    val todayMemory by vm.todayMemory.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Message
        Message(count)

        // Today's Memory
        Text(
            text = stringResource(R.string.text_today_s_memory),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        todayMemory?.let { todayMemory ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = todayMemory.imageUrl?.let { File(it) } ?: R.drawable.app_logo,
                            modifier = Modifier.then(
                                if (todayMemory.imageUrl == null) Modifier.size(100.dp)
                                else Modifier.fillMaxSize()
                            ),
                            contentDescription = todayMemory.title,
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${todayMemory.title}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        )
                        Text(
                            text = "${todayMemory.description}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.action_see_more),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSecondary),
                            modifier = Modifier.fillMaxWidth()
                                .clickable(
                                    indication =  null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    isSeeMoreClick(todayMemory.id)
                                },
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        } ?: Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.text_no_memory),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun Message(count: Int) {
    val plant = plantEmojiForCount(count)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "$plant " + stringResource(R.string.text_slogan),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "$count " + stringResource(R.string.text_decision),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.text_slogan_detail),
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF7D7A6F))
        )
    }
}

fun plantEmojiForCount(count: Int): String = when {
    count >= 500 -> "🏕"
    count >= 300 -> "🌳"
    count >= 200 -> "🌲"
    count >= 100 -> "🌿"
    count >= 50  -> "🍃"
    count >= 30  -> "🍀"
    count >= 10  -> "☘"
    else         -> "🌱"
}