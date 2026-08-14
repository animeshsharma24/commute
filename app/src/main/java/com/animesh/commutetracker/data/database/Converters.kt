package com.animesh.commutetracker.data.database

import androidx.room.TypeConverter
import com.animesh.commutetracker.data.model.CommuteDirection
import com.animesh.commutetracker.data.model.CommuteStatus
import com.animesh.commutetracker.data.model.DetectionMethod
import com.animesh.commutetracker.data.model.TransportMode

class Converters {
    @TypeConverter
    fun fromDirection(direction: CommuteDirection): String = direction.name

    @TypeConverter
    fun toDirection(name: String): CommuteDirection = CommuteDirection.valueOf(name)

    @TypeConverter
    fun fromStatus(status: CommuteStatus): String = status.name

    @TypeConverter
    fun toStatus(name: String): CommuteStatus = CommuteStatus.valueOf(name)

    @TypeConverter
    fun fromDetectionMethod(method: DetectionMethod): String = method.name

    @TypeConverter
    fun toDetectionMethod(name: String): DetectionMethod = DetectionMethod.valueOf(name)

    @TypeConverter
    fun fromTransport(mode: TransportMode?): String? = mode?.name

    @TypeConverter
    fun toTransport(name: String?): TransportMode? = name?.let { TransportMode.valueOf(it) }
}
