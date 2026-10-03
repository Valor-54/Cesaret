package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GamepadProfileDao {
    @Query("SELECT * FROM gamepad_profiles ORDER BY isPreset DESC, id ASC")
    fun getAllProfiles(): Flow<List<GamepadProfileEntity>>

    @Query("SELECT * FROM gamepad_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): GamepadProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: GamepadProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: GamepadProfileEntity)

    @Query("DELETE FROM gamepad_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("SELECT COUNT(*) FROM gamepad_profiles")
    suspend fun getCount(): Int
}
