package com.aube.minimallog.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aube.minimallog.domain.usecase.AddMemoryUseCase
import com.aube.minimallog.domain.usecase.DeleteMemoryUseCase
import com.aube.minimallog.domain.usecase.ObserveMemoriesUseCase
import com.aube.minimallog.domain.usecase.ObserveMemoryByIdUseCase
import com.aube.minimallog.domain.usecase.ObserveSearchUseCase
import com.aube.minimallog.domain.usecase.ObserveTodayMemoryUseCase
import com.aube.minimallog.domain.usecase.ToggleFavoriteUseCase
import com.aube.minimallog.presentation.model.MemoryDraft
import com.aube.minimallog.presentation.model.MemoryItem
import com.aube.minimallog.presentation.model.MemoryUiMapper
import com.aube.minimallog.presentation.ui.screens.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val observeToday: ObserveTodayMemoryUseCase,
    private val observeMemories: ObserveMemoriesUseCase,
    private val observeById: ObserveMemoryByIdUseCase,
    private val addMemory: AddMemoryUseCase,
    private val observeSearch: ObserveSearchUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val deleteMemory: DeleteMemoryUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val memoryId: Long? = savedStateHandle["memoryId"]
    private val editId: Long? = savedStateHandle["editId"]

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _sort = MutableStateFlow(SortOption.NEW_FIRST)
    val sort: StateFlow<SortOption> = _sort

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val todayMemory: StateFlow<MemoryItem?> =
        observeToday(LocalDate.now())
            .map { it?.let(MemoryUiMapper::fromDomain) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)


    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val memories: StateFlow<List<MemoryItem>> =
        combine(_query, _sort) { q, s -> q.trim() to s }
            .debounce(200)
            .distinctUntilChanged()
            .flatMapLatest { (q, _) ->
                observeSearch(q)
            }
            .map { list -> list.map(MemoryUiMapper::fromDomain) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memory: StateFlow<MemoryItem?> =
        observeById(memoryId)
            .map { it?.let(MemoryUiMapper::fromDomain) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val editTarget: StateFlow<MemoryItem?> =
        (editId?.let { id ->
            observeById(id).map { it?.let(MemoryUiMapper::fromDomain) }
        } ?: flowOf(null))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)


    fun save(memoryDraft: MemoryDraft) = viewModelScope.launch {
        try {
            val id = addMemory(
                id = memoryDraft.id,
                title = memoryDraft.title,
                description = memoryDraft.description,
                imageUri = memoryDraft.imageUri,
                date = memoryDraft.date,
                tags = memoryDraft.tags,
                isFavorite = memoryDraft.isFavorite
            )
            _events.send(Event.Saved(id))
        } catch (t: Throwable) {
            _events.send(Event.Error(t.message ?: "Save failed"))
        }
    }



    fun onQueryChange(q: String) { _query.value = q }

    fun onSortChange(s: SortOption) { _sort.value = s }

    fun onToggleFavorite(item: MemoryItem) {
        viewModelScope.launch {
            try {
                toggleFavorite(item.id)
            } catch (_: Throwable) {
                // TODO: 에러 처리(스낵바 등)
            }
        }
    }

    fun delete() = viewModelScope.launch {
        val id = memoryId ?: return@launch
        runCatching { deleteMemory(id) }
            .onSuccess { _events.send(Event.Deleted) }
            .onFailure { _events.send(Event.Error(it.message ?: "Delete failed")) }
    }

    sealed interface Event {
        data class Saved(val id: Long) : Event
        data class Error(val message: String) : Event
        object Deleted : Event
    }
}