package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ChallengeType {
    SMILE, BLINK, NONE
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(application, database.attendanceDao())
    private val soundHelper = SoundHelper(application)

    // Data streams
    val allLogs: StateFlow<List<AttendanceLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudents: StateFlow<List<Student>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSettings: StateFlow<List<AppSetting>> = repository.allSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Network & Sync status
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncStatusText = MutableStateFlow("Sync Ready")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    // Config cache (StateFlows for UI binders)
    val gasWebhookUrl = MutableStateFlow("")
    val cooldownTime = MutableStateFlow(10000L)
    val minFaceArea = MutableStateFlow(12000)
    val scoreThreshold = MutableStateFlow(0.5f)
    val matchThreshold = MutableStateFlow(0.45f)
    val smileThreshold = MutableStateFlow(0.4f)
    val blinkThreshold = MutableStateFlow(0.24f)
    val successOverlayDuration = MutableStateFlow(3000L)

    // "AUTO", "MASUK", "PULANG"
    val scheduleSetting = MutableStateFlow("AUTO")

    // UI Scanning & HUD states
    private val _activeChallenge = MutableStateFlow(ChallengeType.NONE)
    val activeChallenge: StateFlow<ChallengeType> = _activeChallenge.asStateFlow()

    private val _hudInstruction = MutableStateFlow("Tempelkan Wajah di Kamera")
    val hudInstruction: StateFlow<String> = _hudInstruction.asStateFlow()

    private val _errorOverlayText = MutableStateFlow<String?>(null)
    val errorOverlayText: StateFlow<String?> = _errorOverlayText.asStateFlow()

    private val _currentDetectedFacesCount = MutableStateFlow(0)
    val currentDetectedFacesCount: StateFlow<Int> = _currentDetectedFacesCount.asStateFlow()

    private val _currentFaceArea = MutableStateFlow(0)
    val currentFaceArea: StateFlow<Int> = _currentFaceArea.asStateFlow()

    private val _currentSmileProb = MutableStateFlow(0.0f)
    val currentSmileProb: StateFlow<Float> = _currentSmileProb.asStateFlow()

    private val _currentLeftEyeOpen = MutableStateFlow(1.0f)
    val currentLeftEyeOpen: StateFlow<Float> = _currentLeftEyeOpen.asStateFlow()

    private val _currentRightEyeOpen = MutableStateFlow(1.0f)
    val currentRightEyeOpen: StateFlow<Float> = _currentRightEyeOpen.asStateFlow()

    // Success scan details overlay
    private val _showSuccessCard = MutableStateFlow(false)
    val showSuccessCard: StateFlow<Boolean> = _showSuccessCard.asStateFlow()

    private val _successLogDetails = MutableStateFlow<AttendanceLog?>(null)
    val successLogDetails: StateFlow<AttendanceLog?> = _successLogDetails.asStateFlow()

    private val _scannerModeText = MutableStateFlow("MASUK") // Live mode: MASUK / PULANG
    val scannerModeText: StateFlow<String> = _scannerModeText.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    private var activeLivenessTrackerJob: Job? = null
    private var dismissOverlayJob: Job? = null

    init {
        viewModelScope.launch {
            repository.checkSeedData()
            loadAllConfigSettings()
            monitorNetwork()
            updateScannerModeAutomatically()
        }
    }

    private fun monitorNetwork() {
        viewModelScope.launch {
            while (true) {
                val available = repository.isNetworkAvailable()
                if (available != _isOnline.value) {
                    _isOnline.value = available
                    if (available) {
                        _toastEvent.emit("Internet Terhubung kembali. Memulai otomatis sinkronisasi...")
                        triggerManualSync()
                    }
                }
                delay(3000)
            }
        }
    }

    private fun updateScannerModeAutomatically() {
        viewModelScope.launch {
            while (true) {
                val configMode = scheduleSetting.value
                if (configMode == "AUTO") {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val targetMode = if (hour >= 12) "PULANG" else "MASUK"
                    _scannerModeText.value = targetMode
                } else {
                    _scannerModeText.value = configMode
                }
                delay(10000) // check every 10s
            }
        }
    }

    // Load from Room back to memory
    suspend fun loadAllConfigSettings() {
        gasWebhookUrl.value = repository.getSettingValue("gas_webhook_url", "")
        cooldownTime.value = repository.getSettingValue("cooldown_time", "10000").toLongOrNull() ?: 10000L
        minFaceArea.value = repository.getSettingValue("min_face_area", "12000").toIntOrNull() ?: 12000
        scoreThreshold.value = repository.getSettingValue("score_threshold", "0.5").toFloatOrNull() ?: 0.5f
        matchThreshold.value = repository.getSettingValue("match_threshold", "0.45").toFloatOrNull() ?: 0.45f
        smileThreshold.value = repository.getSettingValue("smile_threshold", "0.4").toFloatOrNull() ?: 0.4f
        blinkThreshold.value = repository.getSettingValue("blink_threshold", "0.24").toFloatOrNull() ?: 0.24f
        successOverlayDuration.value = repository.getSettingValue("success_duration", "3000").toLongOrNull() ?: 3000L
        scheduleSetting.value = repository.getSettingValue("schedule_setting", "AUTO")

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        _scannerModeText.value = if (scheduleSetting.value == "AUTO") {
            if (hour >= 12) "PULANG" else "MASUK"
        } else {
            scheduleSetting.value
        }
    }

    fun updateSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
            loadAllConfigSettings()
            _toastEvent.emit("Pengaturan '$key' berhasil diperbarui.")
        }
    }

    // Delete Student
    fun removeStudent(id: Int) {
        viewModelScope.launch {
            repository.deleteStudentById(id)
            _toastEvent.emit("Siswa berhasil dihapus.")
        }
    }

    // Add Student
    fun registerStudent(name: String, className: String) {
        viewModelScope.launch {
            if (name.isBlank() || className.isBlank()) {
                _toastEvent.emit("Error: Nama dan Kelas tidak boleh kosong!")
                return@launch
            }
            repository.insertStudent(name, className)
            _toastEvent.emit("Siswa $name berhasil terdaftar!")
        }
    }

    // Clear logs
    fun clearAllAttendanceLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _toastEvent.emit("Semua riwayat absensi lokal berhasil dibersihkan.")
        }
    }

    // Manual Sync trigger
    fun triggerManualSync() {
        viewModelScope.launch {
            _syncStatusText.value = "Syncing..."
            val result = repository.syncLogs()
            when (result) {
                is SyncResult.Success -> {
                    _syncStatusText.value = "Synced"
                    _toastEvent.emit(result.message)
                }
                is SyncResult.Error -> {
                    _syncStatusText.value = "Pending"
                    _toastEvent.emit(result.message)
                }
            }
        }
    }

    /**
     * Resets the active scanner/liveness targets
     */
    fun restartScan() {
        activeLivenessTrackerJob?.cancel()
        _activeChallenge.value = ChallengeType.NONE
        _errorOverlayText.value = null

        // Choose random liveness challenge
        val randomChallenge = if (Random().nextBoolean()) ChallengeType.SMILE else ChallengeType.BLINK
        _activeChallenge.value = randomChallenge
        _hudInstruction.value = when (randomChallenge) {
            ChallengeType.SMILE -> "SILAKAN SENYUM LEBAR 😊"
            ChallengeType.BLINK -> "SILAKAN BERKEDIP MATA 😉"
            else -> "Mendekat ke Kamera"
        }
    }

    /**
     * Safe entry point from Camera Analyzer. Converts ML Kit parameters into logs.
     */
    fun processCameraFrame(
        facesFound: Int,
        boundingBoxSize: Int,
        smileProb: Float,
        leftEyeOpen: Float,
        rightEyeOpen: Float,
        matchedStudentName: String? = null // if ML Kit matching is simulated
    ) {
        // Cache for HUD
        _currentDetectedFacesCount.value = facesFound
        _currentFaceArea.value = boundingBoxSize
        _currentSmileProb.value = smileProb
        _currentLeftEyeOpen.value = leftEyeOpen
        _currentRightEyeOpen.value = rightEyeOpen

        // Only process challenges if scanning is active (no active success card showing)
        if (_showSuccessCard.value) return

        // Challenge check: Multi-face check
        if (facesFound > 1) {
            _errorOverlayText.value = "MULTI"
            _hudInstruction.value = "Hanya boleh 1 wajah di dalam frame!"
            return
        }

        if (facesFound == 0) {
            _errorOverlayText.value = null
            _hudInstruction.value = "Silakan tempelkan wajah..."
            return
        }

        // Challenge check: Distance check (Area min 12,000 pixels)
        val limitArea = minFaceArea.value
        if (boundingBoxSize < limitArea) {
            _errorOverlayText.value = "DEKATKAN"
            _hudInstruction.value = "DEKATKAN WAJAH ANDA KE KAMERA"
            return
        }

        // We have a single face up-close! Clear structural warnings
        _errorOverlayText.value = null

        // Initialize active challenge if none
        if (_activeChallenge.value == ChallengeType.NONE) {
            restartScan()
            return
        }

        // Check if the current challenge succeeded
        var challengeSucceeded = false
        var livenessType = "MANUAL"

        when (_activeChallenge.value) {
            ChallengeType.SMILE -> {
                livenessType = "SMILE"
                if (smileProb >= smileThreshold.value) {
                    challengeSucceeded = true
                }
            }
            ChallengeType.BLINK -> {
                livenessType = "BLINK"
                val avgEyeOpen = (leftEyeOpen + rightEyeOpen) / 2
                if (avgEyeOpen <= blinkThreshold.value) {
                    challengeSucceeded = true
                }
            }
            ChallengeType.NONE -> {}
        }

        if (challengeSucceeded) {
            // Succeeded liveness! Now proceed to record attendance.
            _activeChallenge.value = ChallengeType.NONE
            _hudInstruction.value = "Liveness Valid! Memproses..."
            
            // Assign matched student (default: pick first student if matchedStudentName is null,
            // or we pick student from the active user's selection)
            viewModelScope.launch {
                val studentToRecord = if (!matchedStudentName.isNullOrBlank()) {
                    allStudents.value.firstOrNull { it.name == matchedStudentName }
                } else {
                    // fall back: pick first student in DB if none provided, or simulated
                    allStudents.value.firstOrNull()
                }

                if (studentToRecord == null) {
                    soundHelper.playOscillatorSound(isSuccess = false)
                    _hudInstruction.value = "Wajah tidak dikenal!"
                    delay(2000)
                    restartScan()
                    return@launch
                }

                executeLogAttendance(studentToRecord, livenessType)
            }
        }
    }

    private suspend fun executeLogAttendance(student: Student, livenessType: String) {
        val mode = _scannerModeText.value
        val result = repository.recordAttendance(
            studentName = student.name,
            type = mode,
            livenessType = livenessType,
            confidence = 0.92 + (Random().nextDouble() * 0.07) // simulated matching high level
        )

        when (result) {
            is AttendanceLogResult.Success -> {
                soundHelper.playOscillatorSound(isSuccess = true)
                soundHelper.speak( IndonesianGreetingMessage(student.name, mode) )

                val log = AttendanceLog(
                    studentName = student.name,
                    timestamp = System.currentTimeMillis(),
                    type = mode,
                    syncStatus = if (result.isSynced) "SYNCED" else "PENDING",
                    confidence = 0.95,
                    livenessType = livenessType
                )
                _successLogDetails.value = log
                _showSuccessCard.value = true

                dismissOverlayJob?.cancel()
                dismissOverlayJob = viewModelScope.launch {
                    delay(successOverlayDuration.value)
                    _showSuccessCard.value = false
                    restartScan()
                }
            }
            is AttendanceLogResult.Cooldown -> {
                soundHelper.playOscillatorSound(isSuccess = false)
                _hudInstruction.value = "COOLDOWN ACTIVE"
                _toastEvent.emit(result.message)
                delay(2000)
                restartScan()
            }
            is AttendanceLogResult.AlreadyScanned -> {
                soundHelper.playOscillatorSound(isSuccess = false)
                _hudInstruction.value = "SUDAH ABSEN HARI INI"
                _toastEvent.emit(result.message)
                delay(2000)
                restartScan()
            }
        }
    }

    /**
     * Indonesian-friendly Speak out format
     */
    private fun IndonesianGreetingMessage(name: String, mode: String): String {
        return if (mode == "MASUK") {
            "$name, berhasil masuk. Semangat belajar!"
        } else {
            "$name, berhasil pulang. Hati-hati di jalan!"
        }
    }

    /**
     * Admin/Simulation sandbox trigger: Allows manual override/demo to trigger a successful
     * biometric match, which is perfect for emulation environments without real cameras.
     */
    fun simulateManualStudentRecognition(student: Student, simulatedChallenge: ChallengeType) {
        viewModelScope.launch {
            if (_showSuccessCard.value) return@launch

            _errorOverlayText.value = null
            _activeChallenge.value = simulatedChallenge
            _currentDetectedFacesCount.value = 1
            _currentFaceArea.value = 16500 // simulated valid area

            when (simulatedChallenge) {
                ChallengeType.SMILE -> {
                    _currentSmileProb.value = 0.92f
                    _hudInstruction.value = "Menyelesaikan tantangan senyum..."
                }
                ChallengeType.BLINK -> {
                    _currentLeftEyeOpen.value = 0.05f
                    _currentRightEyeOpen.value = 0.05f
                    _hudInstruction.value = "Menyelesaikan tantangan kedip..."
                }
                ChallengeType.NONE -> {
                    _hudInstruction.value = "Menyelesaikan tantangan baypass..."
                }
            }

            delay(600)
            executeLogAttendance(student, simulatedChallenge.name)
        }
    }

    fun simulateMultiFaceDetected() {
        viewModelScope.launch {
            _currentDetectedFacesCount.value = 3
            _errorOverlayText.value = "MULTI"
            _hudInstruction.value = "Hanya boleh 1 wajah di dalam frame!"
            soundHelper.playOscillatorSound(isSuccess = false)
            delay(3000)
            restartScan()
        }
    }

    fun simulateWrongDistanceDetected() {
        viewModelScope.launch {
            _currentDetectedFacesCount.value = 1
            _currentFaceArea.value = 4500 // too far
            _errorOverlayText.value = "DEKATKAN"
            _hudInstruction.value = "DEKATKAN WAJAH ANDA KE KAMERA"
            soundHelper.playOscillatorSound(isSuccess = false)
            delay(3000)
            restartScan()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundHelper.shutdown()
    }
}
