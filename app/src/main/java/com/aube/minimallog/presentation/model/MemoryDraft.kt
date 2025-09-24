package com.aube.minimallog.presentation.model

import android.net.Uri
import java.time.LocalDate

data class MemoryDraft(
    val id: Long? = null,
    val title: String,
    val description: String,
    val imageUri: Uri?,
    val date: LocalDate,
    val tags: List<String>,
    val isFavorite: Boolean
)