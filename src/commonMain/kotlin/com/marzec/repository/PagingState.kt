package com.marzec.repository

import kotlinx.serialization.Serializable

/**
 * Persisted progress for one paged query.
 *
 * This state is stored separately from the cached models. The model cache must be updated before
 * this state advances, so a failed state write can safely retry the same cursor.
 */
@Serializable
data class PagingState<ID, CURSOR>(
    val nextCursor: CURSOR? = null,
    val isComplete: Boolean = false,
    /** Keeps cycle detection correct even when paging resumes in a new repository instance. */
    val visitedCursors: List<CURSOR> = emptyList(),
    /** Tracks the current session's loaded prefix and detects missing cached models after restart. */
    val loadedItemIds: List<ID> = emptyList()
)
