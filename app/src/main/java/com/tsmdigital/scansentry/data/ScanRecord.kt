package com.tsmdigital.scansentry.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawValue: String,
    val format: String?,
    val createdAtEpochMs: Long,
    val isFavorite: Boolean = false,
)
