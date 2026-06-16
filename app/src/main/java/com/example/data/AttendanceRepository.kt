package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AttendanceRepository(
    private val context: Context,
    private val attendanceDao: AttendanceDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val allLogs: Flow<List<AttendanceLog>> = attendanceDao.getAllLogs()
    val allStudents: Flow<List<Student>> = attendanceDao.getAllStudents()
    val allSettings: Flow<List<AppSetting>> = attendanceDao.getAllSettings()

    suspend fun getSettingValue(key: String, defaultValue: String): String {
        return attendanceDao.getSetting(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        attendanceDao.insertSetting(AppSetting(key, value))
    }

    suspend fun insertStudent(name: String, className: String, faceDescriptor: String = "") {
        attendanceDao.insertStudent(Student(name = name, className = className, faceDescriptor = faceDescriptor))
    }

    suspend fun deleteStudentById(id: Int) {
        attendanceDao.deleteStudentById(id)
    }

    suspend fun clearLogs() {
        attendanceDao.clearAllLogs()
    }

    suspend fun deleteLog(id: Int) {
        attendanceDao.deleteLogById(id)
    }

    // Seed default students if empty
    suspend fun checkSeedData() {
        val count = attendanceDao.getStudentCount()
        if (count == 0) {
            insertStudent("Ahmad Fauzi", "XII RPL 1", "0.1,0.5,0.7")
            insertStudent("Siti Sholikah", "XII RPL 2", "0.2,0.6,0.8")
            insertStudent("Budi Pratama", "XI TKJ 1", "0.3,0.3,0.9")
            insertStudent("Rini Lestari", "XI TKJ 2", "0.4,0.4,0.1")
        }
    }

    // Perform manual sync with GAS or simulation
    suspend fun syncLogs(): SyncResult = withContext(Dispatchers.IO) {
        val pending = attendanceDao.getPendingLogs()
        if (pending.isEmpty()) {
            return@withContext SyncResult.Success(0, "Semua data sudah tersinkronisasi.")
        }

        if (!isNetworkAvailable()) {
            return@withContext SyncResult.Error("Tidak ada koneksi internet. Tersimpan offline.")
        }

        val webhookUrl = getSettingValue("gas_webhook_url", "")
        if (webhookUrl.isBlank()) {
            // Simulator Mode - Simulate sending with a slight delay
            for (log in pending) {
                attendanceDao.insertLog(log.copy(syncStatus = "SYNCED"))
            }
            return@withContext SyncResult.Success(pending.size, "Simulasi sinkronisasi offline berhasil.")
        }

        try {
            // Update items to syncing
            for (log in pending) {
                attendanceDao.insertLog(log.copy(syncStatus = "SYNCING"))
            }

            // Create JSON Array
            val jsonArray = JSONArray()
            for (log in pending) {
                val obj = JSONObject()
                obj.put("studentName", log.studentName)
                obj.put("timestamp", log.timestamp)
                obj.put("type", log.type)
                obj.put("liveness", log.livenessType)
                obj.put("confidence", log.confidence)
                jsonArray.put(obj)
            }

            val payload = JSONObject()
            payload.put("action", "recordAttendance")
            payload.put("logs", jsonArray)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(webhookUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    for (log in pending) {
                        attendanceDao.insertLog(log.copy(syncStatus = "SYNCED"))
                    }
                    SyncResult.Success(pending.size, "Berhasil disinkronkan ke Google Sheets.")
                } else {
                    for (log in pending) {
                        attendanceDao.insertLog(log.copy(syncStatus = "PENDING"))
                    }
                    SyncResult.Error("Server GAS merespons error Code ${response.code}.")
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error syncing logs: ", e)
            for (log in pending) {
                attendanceDao.insertLog(log.copy(syncStatus = "PENDING"))
            }
            SyncResult.Error("Gagal berinteraksi dengan server GAS: ${e.message}")
        }
    }

    // Insert new attendance log and try syncing immediately
    suspend fun recordAttendance(
        studentName: String,
        type: String,
        livenessType: String,
        confidence: Double
    ): AttendanceLogResult {
        // Prevent double scans: cooldown 10s or same mode in same day
        val now = System.currentTimeMillis()
        val allCurrentLogs = attendanceDao.getAllLogs().first()

        // 1. Cooldown check: if same name was scanned within Cooldown Time (default 10s)
        val cooldownStr = getSettingValue("cooldown_time", "10000")
        val cooldownMs = cooldownStr.toLongOrNull() ?: 10000L
        val recentLogForUser = allCurrentLogs.firstOrNull {
            it.studentName == studentName && (now - it.timestamp) < cooldownMs
        }
        if (recentLogForUser != null) {
            val remainSeconds = ((cooldownMs - (now - recentLogForUser.timestamp)) / 1000).coerceAtLeast(1)
            return AttendanceLogResult.Cooldown("Tunggu $remainSeconds detik sebelum menscan kembali nama ${studentName}!")
        }

        // 2. Prevent double attendance for same mode (e.g. MASUK or PULANG) in same day
        val todayStart = getTodayStartMillis()
        val hasDoubleScan = allCurrentLogs.any {
            it.studentName == studentName && it.type == type && it.timestamp >= todayStart
        }
        if (hasDoubleScan) {
            return AttendanceLogResult.AlreadyScanned("Siswa ${studentName} sudah melakukan absensi ${type} hari ini!")
        }

        // Save locally as PENDING
        val log = AttendanceLog(
            studentName = studentName,
            timestamp = now,
            type = type,
            syncStatus = "PENDING",
            confidence = confidence,
            livenessType = livenessType
        )
        attendanceDao.insertLog(log)

        // Try to trigger auto sync in background
        if (isNetworkAvailable()) {
            val syncResult = syncLogs()
            if (syncResult is SyncResult.Success) {
                return AttendanceLogResult.Success(studentName, type, isSynced = true)
            }
        }
        return AttendanceLogResult.Success(studentName, type, isSynced = false)
    }

    private fun getTodayStartMillis(): Long {
        val current = java.util.Calendar.getInstance()
        current.set(java.util.Calendar.HOUR_OF_DAY, 0)
        current.set(java.util.Calendar.MINUTE, 0)
        current.set(java.util.Calendar.SECOND, 0)
        current.set(java.util.Calendar.MILLISECOND, 0)
        return current.timeInMillis
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val act = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            act.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            act.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            act.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}

sealed class SyncResult {
    data class Success(val count: Int, val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

sealed class AttendanceLogResult {
    data class Success(val studentName: String, val type: String, val isSynced: Boolean) : AttendanceLogResult()
    data class Cooldown(val message: String) : AttendanceLogResult()
    data class AlreadyScanned(val message: String) : AttendanceLogResult()
}
