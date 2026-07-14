package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mode: String,      // "Range", "Dice", "Coin", "List Draw"
    val result: String,    // "42", "5, 2 (Total: 7)", "Heads", "Picked: Alice"
    val details: String,   // Custom detailed representation
    val timestamp: Long = System.currentTimeMillis()
)
