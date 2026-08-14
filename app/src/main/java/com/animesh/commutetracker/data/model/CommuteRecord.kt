package com.animesh.commutetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CommuteDirection {
    HOME_TO_OFFICE,
    OFFICE_TO_HOME
}

enum class TransportMode {
    METRO, BUS, SHUTTLE, BIKE, CAR, AUTO, WALK, OTHER
}

enum class CommuteStatus {
    PENDING_DETAILS,
    COMPLETED
}

enum class DetectionMethod {
    WIFI, LOCATION, MANUAL
}

@Entity(tableName = "commute_records")
data class CommuteRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // format YYYY-MM-DD or DD/MM/YYYY
    val direction: CommuteDirection,
    val startTimestamp: Long,
    val arrivalTimestamp: Long,
    val durationMinutes: Int,
    var transportMode: TransportMode? = null,
    var cost: Int? = null,
    var status: CommuteStatus = CommuteStatus.COMPLETED,
    var detectionMethod: DetectionMethod = DetectionMethod.WIFI
)
