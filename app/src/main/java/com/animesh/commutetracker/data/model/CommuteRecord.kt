package com.animesh.commutetracker.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

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
    var status: CommuteStatus = CommuteStatus.COMPLETED,
    var detectionMethod: DetectionMethod = DetectionMethod.WIFI
)

@Entity(
    tableName = "commute_modes",
    foreignKeys = [
        ForeignKey(
            entity = CommuteRecord::class,
            parentColumns = ["id"],
            childColumns = ["commuteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("commuteId")]
)
data class CommuteMode(
    @PrimaryKey(autoGenerate = true)
    val modeId: Long = 0,
    val commuteId: Long,
    val transportMode: TransportMode,
    val durationMinutes: Int,
    val cost: Int
)

data class CommuteWithModes(
    @Embedded val record: CommuteRecord,
    @Relation(
        parentColumn = "id",
        entityColumn = "commuteId"
    )
    val modes: List<CommuteMode>
)
