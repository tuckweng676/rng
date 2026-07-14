package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.HistoryItem
import com.example.data.repository.HistoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class RngTab {
    RANGE, DICE, COIN, LIST_DRAW
}

class RngViewModel(private val repository: HistoryRepository) : ViewModel() {

    // Global History
    val historyList: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Active Tab
    var activeTab by mutableStateOf(RngTab.RANGE)

    // --- RANGE MODE STATE ---
    var rangeMinText by mutableStateOf("1")
    var rangeMaxText by mutableStateOf("100")
    var rangeCount by mutableStateOf(1)
    var rangeAllowDuplicates by mutableStateOf(false)
    var rangeResults by mutableStateOf<List<Int>>(emptyList())
    var isRangeGenerating by mutableStateOf(false)

    // --- DICE MODE STATE ---
    var diceCount by mutableStateOf(1) // 1 to 6
    var diceResults by mutableStateOf<List<Int>>(emptyList())
    var isDiceRolling by mutableStateOf(false)

    // --- COIN MODE STATE ---
    var coinResult by mutableStateOf<String?>(null) // "Heads" or "Tails"
    var isCoinFlipping by mutableStateOf(false)

    // --- LIST DRAW MODE STATE ---
    var listInputText by mutableStateOf("Alice, Bob, Charlie, David, Emma")
    var listPickCount by mutableStateOf(1)
    var listDrawResults by mutableStateOf<List<String>>(emptyList())
    var isListDrawing by mutableStateOf(false)

    // --- UTILITIES ---
    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // --- ACTIONS ---

    fun generateNumbers() {
        if (isRangeGenerating) return
        val min = rangeMinText.toIntOrNull() ?: 1
        val max = rangeMaxText.toIntOrNull() ?: 100
        val actualMin = minOf(min, max)
        val actualMax = maxOf(min, max)
        val count = rangeCount.coerceIn(1, 100)

        isRangeGenerating = true
        viewModelScope.launch {
            // Animate / shuffle effect for 600ms
            val rangeSize = actualMax - actualMin + 1
            repeat(10) { step ->
                val tempResults = mutableListOf<Int>()
                repeat(count) {
                    tempResults.add(Random.nextInt(actualMin, actualMax + 1))
                }
                rangeResults = tempResults
                delay(40 + (step * 10L)) // slow down gradually
            }

            // Calculate final results
            val finalResults = mutableListOf<Int>()
            if (!rangeAllowDuplicates && rangeSize >= count) {
                // Generate unique numbers
                val pool = (actualMin..actualMax).toList().shuffled()
                finalResults.addAll(pool.take(count))
            } else {
                repeat(count) {
                    finalResults.add(Random.nextInt(actualMin, actualMax + 1))
                }
            }

            rangeResults = finalResults.sorted()
            isRangeGenerating = false

            // Insert to database
            val resultStr = rangeResults.joinToString(", ")
            val detailStr = "Min: $actualMin, Max: $actualMax | Count: $count | Duplicates: ${if (rangeAllowDuplicates) "Allowed" else "No"}"
            repository.insert(
                HistoryItem(
                    mode = "Range",
                    result = resultStr,
                    details = detailStr
                )
            )
        }
    }

    fun rollDice() {
        if (isDiceRolling) return
        isDiceRolling = true
        val count = diceCount.coerceIn(1, 6)

        viewModelScope.launch {
            // Shake effect
            repeat(12) { step ->
                val temp = mutableListOf<Int>()
                repeat(count) {
                    temp.add(Random.nextInt(1, 7))
                }
                diceResults = temp
                delay(30 + (step * 8L))
            }

            val finalResults = mutableListOf<Int>()
            repeat(count) {
                finalResults.add(Random.nextInt(1, 7))
            }
            diceResults = finalResults
            isDiceRolling = false

            val total = finalResults.sum()
            val resultStr = finalResults.joinToString(", ") + " (Total: $total)"
            val detailStr = "Dice rolled: $count"
            repository.insert(
                HistoryItem(
                    mode = "Dice",
                    result = resultStr,
                    details = detailStr
                )
            )
        }
    }

    fun flipCoin() {
        if (isCoinFlipping) return
        isCoinFlipping = true

        viewModelScope.launch {
            // Flipping effect
            repeat(15) { step ->
                coinResult = if (Random.nextBoolean()) "Heads" else "Tails"
                delay(20 + (step * 12L))
            }

            val finalResult = if (Random.nextBoolean()) "Heads" else "Tails"
            coinResult = finalResult
            isCoinFlipping = false

            repository.insert(
                HistoryItem(
                    mode = "Coin",
                    result = finalResult,
                    details = "Single Flip"
                )
            )
        }
    }

    fun drawFromList() {
        if (isListDrawing) return
        // Parse items
        val items = listInputText.split(Regex("[,\\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (items.isEmpty()) return

        isListDrawing = true
        val count = listPickCount.coerceIn(1, items.size)

        viewModelScope.launch {
            // Shuffle effect
            repeat(10) { step ->
                listDrawResults = items.shuffled().take(count)
                delay(50 + (step * 12L))
            }

            val finalResults = items.shuffled().take(count)
            listDrawResults = finalResults
            isListDrawing = false

            val resultStr = finalResults.joinToString(", ")
            val detailStr = "From ${items.size} items | Picked: $count"
            repository.insert(
                HistoryItem(
                    mode = "List Draw",
                    result = resultStr,
                    details = detailStr
                )
            )
        }
    }
}

class RngViewModelFactory(private val repository: HistoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RngViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RngViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
