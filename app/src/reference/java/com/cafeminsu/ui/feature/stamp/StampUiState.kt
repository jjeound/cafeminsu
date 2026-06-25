package com.cafeminsu.ui.feature.stamp

import com.cafeminsu.domain.model.StampEvent

sealed interface StampUiState {
    data object Loading : StampUiState

    data class Content(
        val currentCount: Int,
        val goalCount: Int,
        val history: List<StampEvent>,
    ) : StampUiState {
        val progress: Float = stampProgress(currentCount, goalCount)
        val isGoalReached: Boolean = reachedGoal(currentCount, goalCount)
        val remainingCount: Int = remainingStamps(currentCount, goalCount)
    }

    data class Empty(
        val currentCount: Int,
        val goalCount: Int,
        val message: String,
    ) : StampUiState {
        val progress: Float = stampProgress(currentCount, goalCount)
        val isGoalReached: Boolean = reachedGoal(currentCount, goalCount)
        val remainingCount: Int = remainingStamps(currentCount, goalCount)
    }

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : StampUiState

    data class NeedsLogin(
        val message: String,
        val actionLabel: String,
    ) : StampUiState
}

private fun stampProgress(currentCount: Int, goalCount: Int): Float =
    if (goalCount <= EmptyGoalCount) {
        EmptyProgress
    } else {
        currentCount.coerceIn(0, goalCount).toFloat() / goalCount.toFloat()
    }

private fun reachedGoal(currentCount: Int, goalCount: Int): Boolean =
    goalCount > EmptyGoalCount && currentCount >= goalCount

private fun remainingStamps(currentCount: Int, goalCount: Int): Int =
    (goalCount - currentCount).coerceAtLeast(0)

private const val EmptyGoalCount = 0
private const val EmptyProgress = 0f
