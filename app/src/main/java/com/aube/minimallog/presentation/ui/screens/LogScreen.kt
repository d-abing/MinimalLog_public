package com.aube.minimallog.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aube.minimallog.R
import com.aube.minimallog.presentation.model.MemoryItem
import com.aube.minimallog.presentation.ui.component.MLRainbowBorderChip
import com.aube.minimallog.presentation.ui.component.MLSearchField
import com.aube.minimallog.presentation.viewmodel.MemoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---- Public API --------------------------------------------------------------

@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    onMemoryClick: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val vm: MemoryViewModel = hiltViewModel()
    val memories by vm.memories.collectAsState()
    val allTags = memories.map { it.tags }.flatten().distinct()

    var selectedTags by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var isFavoriteOn by rememberSaveable { mutableStateOf(false) }

    val query by vm.query.collectAsState()
    val sort by vm.sort.collectAsState()


    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Search & Sort
        Spacer(Modifier.height(4.dp))

        SearchAndSortRow(
            query = query,
            onQueryChange = vm::onQueryChange,
            sort = sort,
            onSortChange = vm::onSortChange
        )

        Spacer(Modifier.height(16.dp))
        TagChipsRow(
            tags = allTags,
            selected = selectedTags,
            isFavoriteOn = isFavoriteOn,
            onFavoriteFilterClick = { isFavoriteOn = !isFavoriteOn },
            onToggle = { selectedTags = if (it in selectedTags) selectedTags - it else selectedTags + it }
        )

        Spacer(Modifier.height(12.dp))

        val filteredSorted = remember(memories, query, selectedTags, sort, isFavoriteOn) {
            memories
                .filter { m ->
                    val q = query.trim().lowercase()
                    val qOk = q.isBlank() ||
                            m.title.lowercase().contains(q) ||
                            m.description.lowercase().contains(q) ||
                            m.tags.any { it.lowercase().contains(q) }
                    val tagOk = selectedTags.isEmpty() || m.tags.any { it in selectedTags }
                    val favOk = !isFavoriteOn || m.isFavorite
                    qOk && tagOk && favOk
                }
                .let { list ->
                    when (sort) {
                        SortOption.NEW_FIRST -> list.sortedWith(
                            compareByDescending<MemoryItem> { it.date }.thenByDescending { it.id }
                        )
                        SortOption.OLD_FIRST -> list.sortedWith(
                            compareBy<MemoryItem> { it.date }.thenBy { it.id }
                        )
                    }
                }
        }

        if (filteredSorted.isEmpty()) {
            EmptyState()
            return@Column
        }

        // Group by month
        val grouped = remember(filteredSorted) { groupByYearMonth(filteredSorted) }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            grouped.forEach { (ym, items) ->
                item(key = "header-$ym") {
                    MonthHeader(ym = ym)
                }
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.id }
                ) { _, item ->
                    MemoryRow(
                        item = item,
                        onClick = { onMemoryClick(item.id) },
                        onToggleFavorite = { vm.onToggleFavorite(item) },
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ---- Models & helpers --------------------------------------------------------

enum class SortOption { NEW_FIRST, OLD_FIRST }

private val monthFormatter =
    DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()) // e.g., "September 2025"

private fun groupByYearMonth(list: List<MemoryItem>): LinkedHashMap<YearMonthKey, List<MemoryItem>> {
    val map = linkedMapOf<YearMonthKey, MutableList<MemoryItem>>()
    list.forEach { m ->
        val key = YearMonthKey(m.date.year, m.date.monthValue)
        map.getOrPut(key) { mutableListOf() }.add(m)
    }
    return map.mapValues { it.value.toList() } as LinkedHashMap<YearMonthKey, List<MemoryItem>>
}

private data class YearMonthKey(val year: Int, val month: Int) {
    fun pretty(): String = monthFormatter.format(LocalDate.of(year, month, 1))
}

// ---- Composables -------------------------------------------------------------

@Composable
private fun SearchAndSortRow(
    query: String,
    onQueryChange: (String) -> Unit,
    sort: SortOption,
    onSortChange: (SortOption) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        MLSearchField(
            searchQuery = query,
            onSearchQueryChanged = onQueryChange
        )

        val sortName = when (sort) {
            SortOption.NEW_FIRST -> stringResource(R.string.action_newest)
            SortOption.OLD_FIRST -> stringResource(R.string.action_oldest)
        }

        val targetSort = when (sort) {
            SortOption.NEW_FIRST -> SortOption.OLD_FIRST
            SortOption.OLD_FIRST -> SortOption.NEW_FIRST
        }

        Text(
            text = sortName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(60.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onSortChange(targetSort) }
        )
    }
}

@Composable
private fun TagChipsRow(
    tags: List<String>,
    selected: Set<String>,
    isFavoriteOn: Boolean,
    onFavoriteFilterClick: () -> Unit,
    onToggle: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            MLRainbowBorderChip(
                label = "❤",
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isFavoriteOn) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.tertiary,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onFavoriteFilterClick()
                    }
            )
        }

        tags.forEach { tag ->
            item(key = tag) {
                FilterChip(
                    selected = tag in selected,
                    onClick = { onToggle(tag) },
                    label = { Text(tag) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        selectedLabelColor = Color.Black,
                        labelColor = Color.Black,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.height(36.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(ym: YearMonthKey) {
    Text(
        text = ym.pretty(),
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondary
        ),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun MemoryRow(
    item: MemoryItem,
    onClick: () -> Unit,
    onToggleFavorite: (MemoryItem) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onClick() }
            .padding(top = 8.dp, bottom = 12.dp, start = 12.dp, end = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 4.dp)
        ) {
            Text(
                text = "Day ${item.date.dayOfMonth}", // 필요 시 포맷 변경
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )

            val iv = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
            val ic = if (item.isFavorite) Color(0xFFF22959) else MaterialTheme.colorScheme.onSecondary
            Icon(imageVector = iv,
                contentDescription = stringResource(R.string.action_toggle_favorite),
                tint = ic,
                modifier = Modifier.size(16.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onToggleFavorite(item) }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 썸네일
            if (item.imageUrl == null) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = R.drawable.app_logo,
                        contentDescription = stringResource(R.string.label_app_logo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                    )
                }
            } else {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            // 텍스트 영역
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 2.dp).fillMaxWidth()
                )
                Spacer(Modifier.height(2.dp))
                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7D7A6F),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 2.dp).fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (item.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        }
                        if (item.tags.size > 3) {
                            Text("+${item.tags.size - 3}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.text_no_memory), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.text_no_memory_detail),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF7D7A6F)
        )
    }
}

@Composable
@Preview(showBackground = true)
fun PreviewLogScreen() {
    val dummyMemories = listOf(
        MemoryItem(
            id = 1,
            title = "Old Sneakers",
            description = "These sneakers carried me through countless journeys in college.",
            imageUrl = "https://picsum.photos/200/200?1",
            date = LocalDate.of(2025, 9, 20),
            tags = listOf("Shoes", "College"),
            isFavorite = true
        ),
        MemoryItem(
            id = 2,
            title = "First Guitar",
            description = "My very first acoustic guitar. A bit scratched but full of stories.",
            imageUrl = "https://picsum.photos/200/200?2",
            date = LocalDate.of(2025, 9, 18),
            tags = listOf("Music", "Hobby"),
            isFavorite = false
        ),
        MemoryItem(
            id = 3,
            title = "Family Mug",
            description = "Gifted by my sister on her trip to London. Still love the design.",
            imageUrl = "https://picsum.photos/200/200?3",
            date = LocalDate.of(2025, 8, 5),
            tags = listOf("Gift", "Family"),
            isFavorite = false
        )
    )

    LogScreen(
        onMemoryClick = { },
    )
}
