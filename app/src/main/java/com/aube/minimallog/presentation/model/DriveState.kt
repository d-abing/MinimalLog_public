package com.aube.minimallog.presentation.model

data class DriveState(
    val connected: Boolean = false,
    val accountName: String? = null,
    val lastBackup: Long? = null, // epochMillis
    val busy: Boolean = false,
    val error: String? = null
)