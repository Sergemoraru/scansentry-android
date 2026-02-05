package com.tsmdigital.scansentry.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_records ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ScanRecord>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: ScanRecord)

    @Query("DELETE FROM scan_records")
    suspend fun clearAll()

    @Update
    suspend fun update(record: ScanRecord)

    @Delete
    suspend fun delete(record: ScanRecord)
}
