package com.aube.minimallog.domain.model

import java.time.LocalDate

data class Memory(
    val id: Long = 0L,
    val title: String,
    val description: String,
    val imagePath: String?,
    val date: LocalDate,
    val tags: List<String>,
    val isFavorite: Boolean
)