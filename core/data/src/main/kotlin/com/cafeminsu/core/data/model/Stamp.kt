package com.cafeminsu.core.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.cafeminsu.core.database.model.entity.stamp.StampEntity
import com.cafeminsu.core.database.model.entity.stamp.StampHistoryEntity
import com.cafeminsu.core.model.reward.Stamp
import com.cafeminsu.core.model.reward.StampHistory
import com.cafeminsu.core.network.model.response.reward.StampDetailResponse
import com.cafeminsu.core.network.model.response.reward.StampHistoryResponse
import com.cafeminsu.core.network.model.response.reward.StampSummaryResponse
import java.time.Instant

fun StampSummaryResponse.asEntity(): StampEntity = StampEntity(storeId = storeId, storeName = storeName, count = count)

fun StampDetailResponse.asEntity(): StampEntity = StampEntity(storeId = storeId, storeName = storeName, count = count)

fun StampHistoryResponse.asEntity(storeId: Long): StampHistoryEntity =
    StampHistoryEntity(storeId = storeId, earnedCount = earnedCount, createdAtMillis = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(0L))

fun StampEntity.asExternalModel(histories: List<StampHistoryEntity>): Stamp =
    Stamp(storeId = storeId, storeName = storeName, count = count, histories = histories.map(StampHistoryEntity::asExternalModel))

fun StampHistoryEntity.asExternalModel(): StampHistory =
    StampHistory(earnedCount = earnedCount, createdAt = Instant.ofEpochMilli(createdAtMillis).toString())
