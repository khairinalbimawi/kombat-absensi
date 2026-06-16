package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_logs")
data class AttendanceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentName: String,
    val timestamp: Long,
    val type: String, // "MASUK" or "PULANG"
    val syncStatus: String, // "PENDING", "SYNCING", "SYNCED"
    val confidence: Double,
    val livenessType: String // "SMILE", "BLINK", "MANUAL"
)

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val className: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val faceDescriptor: String = "" // Simulates high-precision biometric landmarks or 128D embedding
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
