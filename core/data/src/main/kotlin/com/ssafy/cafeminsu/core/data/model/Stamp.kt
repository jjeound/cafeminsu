package com.ssafy.cafeminsu.core.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.ssafy.cafeminsu.core.database.model.entity.stamp.StampEntity
import com.ssafy.cafeminsu.core.database.model.entity.stamp.StampHistoryEntity
import com.ssafy.cafeminsu.core.model.reward.Stamp
import com.ssafy.cafeminsu.core.model.reward.StampHistory
import com.ssafy.cafeminsu.core.network.model.response.reward.StampDetailResponse
import com.ssafy.cafeminsu.core.network.model.response.reward.StampHistoryResponse
import com.ssafy.cafeminsu.core.network.model.response.reward.StampSummaryResponse
import java.time.Instant

fun StampSummaryResponse.asEntity(): StampEntity = StampEntity(storeId = storeId, storeName = storeName, count = count)

fun StampDetailResponse.asEntity(): StampEntity = StampEntity(storeId = storeId, storeName = storeName, count = count)

fun StampHistoryResponse.asEntity(storeId: Long): StampHistoryEntity =
    StampHistoryEntity(storeId = storeId, earnedCount = earnedCount, createdAtMillis = runCatching { Instant.parse(createdAt).toEpochMilli() }.getOrDefault(0L))

fun StampEntity.asExternalModel(histories: List<StampHistoryEntity>): Stamp =
    Stamp(storeId = storeId, storeName = storeName, count = count, histories = histories.map(StampHistoryEntity::asExternalModel))

fun StampHistoryEntity.asExternalModel(): StampHistory =
    StampHistory(earnedCount = earnedCount, createdAt = Instant.ofEpochMilli(createdAtMillis).toString())
