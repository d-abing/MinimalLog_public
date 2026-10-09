package com.aube.minimallog.presentation.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aube.minimallog.R
import com.aube.minimallog.presentation.model.MemoryItem
import com.aube.minimallog.presentation.model.LogViewMode
import com.aube.minimallog.presentation.ui.nav.LogTopBarActions
import com.aube.minimallog.presentation.ui.component.MLRainbowBorderChip
import com.aube.minimallog.presentation.ui.component.MLSearchField
import com.aube.minimallog.presentation.viewmodel.MemoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ---- Public API --------------------------------------------------------------

@Composable
fun LogScreen(
    modifier: Modifier = Modifier,
    onTopBarActionsChange: (LogTopBarActions?) -> Unit = {},
    onMemoryClick: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val vm: MemoryViewModel = hiltViewModel()
    val memories by vm.memories.collectAsState()
    val allTags = memories.map { it.tags }.flatten().distinct()

    var selectedTags by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var isFavoriteOn by rememberSaveable { mutableStateOf(false) }
    val viewMode by vm.logViewMode.collectAsState()

    SideEffect {
        onTopBarActionsChange(
            LogTopBarActions(
                viewMode = viewMode,
                onToggleView = vm::toggleLogViewMode,
            ),
        )
    }

    DisposableEffect(Unit) {
        onDispose { onTopBarActionsChange(null) }
    }

    val query by vm.query.collectAsState()
    val sort by vm.sort.collectAsState()


    Column(
        modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCF9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp,
                    ),
                )
                .background(Color(0xFFF4F8FB)),
        ) {
            SearchAndSortRow(
                query = query,
                onQueryChange = vm::onQueryChange,
                sort = sort,
                onSortChange = vm::onSortChange
            )

            TagChipsRow(
                tags = allTags,
                selected = selectedTags,
                isFavoriteOn = isFavoriteOn,
                onFavoriteFilterClick = { isFavoriteOn = !isFavoriteOn },
                onToggle = { selectedTags = if (it in selectedTags) selectedTags - it else selectedTags + it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFE3E9ED),
        )

        if (viewMode != LogViewMode.IMMERSIVE_GRID) {
            Spacer(Modifier.height(14.dp))
        }

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

        if (viewMode == LogViewMode.GROUPED_GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                grouped.forEach { (ym, monthItems) ->
                    item(
                        key = "photo-header-$ym",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        MonthHeader(ym = ym)
                    }
                    gridItems(
                        items = monthItems,
                        key = { item -> item.id },
                    ) { item ->
                        PhotoTile(
                            item = item,
                            onClick = { onMemoryClick(item.id) },
                            onToggleFavorite = { vm.onToggleFavorite(item) },
                            immersive = false,
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(12.dp)) }
            }
        } else if (viewMode == LogViewMode.IMMERSIVE_GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                gridItems(
                    items = filteredSorted,
                    key = { item -> item.id },
                ) { item ->
                    PhotoTile(
                        item = item,
                        onClick = { onMemoryClick(item.id) },
                        onToggleFavorite = { vm.onToggleFavorite(item) },
                        immersive = true,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                grouped.forEach { (ym, monthItems) ->
                    item(key = "header-$ym") { MonthHeader(ym = ym) }
                    items(items = monthItems, key = { item -> item.id }) { item ->
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
}

// ---- Models & helpers --------------------------------------------------------

enum class SortOption { NEW_FIRST, OLD_FIRST }

private fun groupByYearMonth(list: List<MemoryItem>): LinkedHashMap<YearMonthKey, List<MemoryItem>> {
    val map = linkedMapOf<YearMonthKey, MutableList<MemoryItem>>()
    list.forEach { m ->
        val key = YearMonthKey(m.date.year, m.date.monthValue)
        map.getOrPut(key) { mutableListOf() }.add(m)
    }
    return map.mapValues { it.value.toList() } as LinkedHashMap<YearMonthKey, List<MemoryItem>>
}

private data class YearMonthKey(val year: Int, val month: Int) {
    fun asDate(): LocalDate = LocalDate.of(year, month, 1)
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
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Color.Transparent)
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

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        )

        Row(
            modifier = Modifier
                .width(92.dp)
                .fillMaxHeight()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onSortChange(targetSort) }
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sortName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TagChipsRow(
    tags: List<String>,
    selected: Set<String>,
    isFavoriteOn: Boolean,
    onFavoriteFilterClick: () -> Unit,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
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
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val pattern = stringResource(R.string.date_month_year_pattern)
    val formattedMonth = remember(ym, locale, pattern) {
        DateTimeFormatter.ofPattern(pattern, locale).format(ym.asDate())
    }

    Text(
        text = formattedMonth,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF465057),
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun MemoryRow(
    item: MemoryItem,
    onClick: () -> Unit,
    onToggleFavorite: (MemoryItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = Color(0xFFE9EEF1),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            if (item.imageUrl == null) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
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
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 24.dp),
                )
                if (item.description.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF727B80),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 24.dp),
                    )
                }
                if (item.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item.tags.take(4).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 88.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (item.tags.size > 4) {
                            Text(
                                text = "+${item.tags.size - 4}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(start = 4.dp),
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }

        val favoriteIcon = if (item.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
        val favoriteColor = if (item.isFavorite) Color(0xFFF22959) else MaterialTheme.colorScheme.onSecondary
        Icon(
            imageVector = favoriteIcon,
            contentDescription = stringResource(R.string.action_toggle_favorite),
            tint = favoriteColor,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(13.dp)
                .size(20.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onToggleFavorite(item) },
        )
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
private fun PhotoTile(
    item: MemoryItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    immersive: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(if (immersive) RoundedCornerShape(0.dp) else RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = item.imageUrl ?: R.drawable.app_logo,
            contentDescription = item.title,
            modifier = if (item.imageUrl == null) Modifier.size(52.dp) else Modifier.fillMaxSize(),
            contentScale = if (item.imageUrl == null) ContentScale.Fit else ContentScale.Crop,
        )

        if (!immersive) {
            val favoriteIcon = if (item.isFavorite) {
                Icons.Filled.Favorite
            } else {
                Icons.Outlined.FavoriteBorder
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(30.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onToggleFavorite,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = if (item.isFavorite) {
                        Color(0xFFF22959)
                    } else {
                        Color.Black.copy(alpha = 0.45f)
                    },
                    modifier = Modifier.size(22.dp),
                )
                Icon(
                    imageVector = favoriteIcon,
                    contentDescription = stringResource(R.string.action_toggle_favorite),
                    tint = if (item.isFavorite) Color(0xFFF22959) else Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
