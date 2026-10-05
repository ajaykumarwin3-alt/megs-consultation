package com.megs.consultation.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY dateTime ASC")
    fun observeAll(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun get(id: Long): Appointment?

    @Query("SELECT * FROM appointments WHERE status = 'UPCOMING' AND dateTime > :now")
    suspend fun upcomingAfter(now: Long): List<Appointment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(a: Appointment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<Appointment>)

    @Delete
    suspend fun delete(a: Appointment)
}
