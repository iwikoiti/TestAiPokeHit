package com.example.testaipokehit.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * В БД хранится ТОЛЬКО id избранных покемонов — остальные данные всегда свежие с API.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int
)