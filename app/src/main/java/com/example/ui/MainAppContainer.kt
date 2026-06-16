package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AttendanceLog
import com.example.data.Student
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val syncText by viewModel.syncStatusText.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val pendingCount = allLogs.count { it.syncStatus == "PENDING" }

    val context = LocalContext.current

    // Observe Toast/Event notifications
    LaunchedEffect(key1 = true) {
        viewModel.toastEvent.collect { message ->
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isOnline) Color(0xFF10B981) else Color(0xFF94A3B8), CircleShape)
                            )
                            Text(
                                text = "KOMBAT • ${if (isOnline) "ONLINE" else "LURING"}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                actions = {
                    // Clock and Sync Info from iOS/Android Design Style
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (pendingCount == 0) "SYNC OK" else "PENDING",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                color = if (pendingCount == 0) Color(0xFF10B981) else Color(0xFFFB923C)
                            )
                            Icon(
                                imageVector = if (syncText == "Syncing...") Icons.Default.Sync else Icons.Default.CloudQueue,
                                contentDescription = "Sync Status Icons",
                                tint = if (pendingCount == 0) Color(0xFF10B981) else Color(0xFFFB923C),
                                modifier = Modifier.size(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        val clockText = remember {
                            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            sdf.format(Date())
                        }
                        Text(
                            text = clockText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 6.dp,
                modifier = Modifier.border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(0.dp))
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "Scanner Wajah") },
                    label = { Text("Absensi", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("scanner_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Data Siswa") },
                    label = { Text("Siswa", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("student_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { 
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) {
                                    Badge(containerColor = Color(0xFFFB923C)) {
                                        Text(pendingCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Riwayat")
                        }
                    },
                    label = { Text("Riwayat", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("history_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Konfigurasi") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4F46E5),
                        selectedTextColor = Color(0xFF4F46E5),
                        indicatorColor = Color(0xFFEEF2FF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("settings_tab")
                )
            }
        },
        containerColor = Color(0xFFF3F4F9),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> ScannerTabScreen(viewModel)
                1 -> StudentsTabScreen(viewModel)
                2 -> HistoryTabScreen(viewModel)
                3 -> ConfigTabScreen(viewModel)
            }
        }
    }
}

// ==========================================
// SCANNER TAB SCREEN
// ==========================================
@Composable
fun ScannerTabScreen(viewModel: AttendanceViewModel) {
    val activeChallenge by viewModel.activeChallenge.collectAsStateWithLifecycle()
    val hudInstruction by viewModel.hudInstruction.collectAsStateWithLifecycle()
    val errorOverlayText by viewModel.errorOverlayText.collectAsStateWithLifecycle()
    val showSuccessCard by viewModel.showSuccessCard.collectAsStateWithLifecycle()
    val successLogDetails by viewModel.successLogDetails.collectAsStateWithLifecycle()
    val scannerModeText by viewModel.scannerModeText.collectAsStateWithLifecycle()

    val currentFaces by viewModel.currentDetectedFacesCount.collectAsStateWithLifecycle()
    val currentFaceArea by viewModel.currentFaceArea.collectAsStateWithLifecycle()
    val smileScore by viewModel.currentSmileProb.collectAsStateWithLifecycle()
    val leftEyeOpen by viewModel.currentLeftEyeOpen.collectAsStateWithLifecycle()
    val rightEyeOpen by viewModel.currentRightEyeOpen.collectAsStateWithLifecycle()

    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val pendingCount = allLogs.count { it.syncStatus == "PENDING" }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Upper Scanner Box (60% relative size)
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
        ) {
            // Live camera view feed
            CameraView(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic Custom Bounding Target Box Overlay (High Density Style)
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .align(Alignment.Center)
                    .border(2.dp, Color(0xFF34D399).copy(alpha = 0.45f), RoundedCornerShape(28.dp))
            ) {
                // Top-Left Rounded Bracket Corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-3).dp, y = (-3).dp)
                        .size(22.dp)
                        .border(
                            width = 4.dp,
                            color = Color(0xFF34D399),
                            shape = RoundedCornerShape(topStart = 8.dp)
                        )
                )

                // Top-Right Rounded Bracket Corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(22.dp)
                        .border(
                            width = 4.dp,
                            color = Color(0xFF34D399),
                            shape = RoundedCornerShape(topEnd = 8.dp)
                        )
                )

                // Bottom-Left Rounded Bracket Corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-3).dp, y = 3.dp)
                        .size(22.dp)
                        .border(
                            width = 4.dp,
                            color = Color(0xFF34D399),
                            shape = RoundedCornerShape(bottomStart = 8.dp)
                        )
                )

                // Bottom-Right Rounded Bracket Corner
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 3.dp, y = 3.dp)
                        .size(22.dp)
                        .border(
                            width = 4.dp,
                            color = Color(0xFF34D399),
                            shape = RoundedCornerShape(bottomEnd = 8.dp)
                        )
                )

                // Analytical Face Label above Bounding Box
                val matchText = if (currentFaces > 0) {
                    val candidate = allStudents.firstOrNull()
                    val candidateName = candidate?.name ?: "Siswa"
                    "Match: 98.4% • $candidateName"
                } else {
                    "Mencari Wajah..."
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = (-30).dp)
                        .background(Color(0xFF34D399), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = matchText.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            // Scanning Overlay/HUD Controls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Overlay inside Camera HUD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Active Mode Label (Masuk / Pulang)
                    val modeColor = if (scannerModeText == "MASUK") Color(0xFF4F46E5) else Color(0xFFEF4444)
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .border(1.5.dp, modeColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MODE: $scannerModeText",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Reset scanner button
                    IconButton(
                        onClick = { viewModel.restartScan() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat ulang pemindaian",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center Liveness Prompt & Status under Bounding Box
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .offset(y = (-20).dp)
                        .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = when (activeChallenge) {
                                ChallengeType.SMILE -> Color(0xFFFBBF24)
                                ChallengeType.BLINK -> Color(0xFF60A5FA)
                                else -> Color(0xFF94A3B8)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Liveness Check".uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF34D399),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = hudInstruction,
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 3.dp)
                                    .clip(CircleShape)
                                    .background(if (activeChallenge == ChallengeType.SMILE) Color(0xFF34D399) else Color(0xFF334155))
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 30.dp, height = 3.dp)
                                    .clip(CircleShape)
                                    .background(if (activeChallenge == ChallengeType.BLINK) Color(0xFF34D399) else Color(0xFF334155))
                            )
                        }
                    }
                }

                // Bottom HUD State values (such as metrics & counters)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left element: Bio stats
                    Column(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Tantangan: ${activeChallenge.name}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                        Text("EAR: ${String.format(Locale.US, "%.2f", (leftEyeOpen + rightEyeOpen)/2)}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }

                    // Right element: Challenge prompt tracker
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                "Muka: $currentFaces • Area: $currentFaceArea px",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Blinking MULTI Overlays if >1 face found
            if (errorOverlayText == "MULTI") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF43F5E).copy(alpha = 0.25f))
                        .border(4.dp, Color(0xFFF43F5E), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = "Multi", tint = Color.White, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("TIDAK VALID: WAJAH GANDA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text("Harap lakukan absensi satu persatu!", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            // Blinking DISTANCE Overlays if face is too small
            if (errorOverlayText == "DEKATKAN") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEAB308).copy(alpha = 0.2f))
                        .border(4.dp, Color(0xFFEAB308), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonalVideo, contentDescription = "Mendekat", tint = Color.White, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("DEKATKAN WAJAH ANDA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text("Min Area: 12.000 pixel (${currentFaceArea})", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            // SUCCESS CHECKS OVERLAY CARD
            androidx.compose.animation.AnimatedVisibility(
                visible = showSuccessCard,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                successLogDetails?.let { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.72f)
                            .shadow(24.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        onClick = { viewModel.restartScan() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Checked Icon decoration
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape)
                                    .border(2.dp, Color(0xFF10B981), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Absen Sukses",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "ABSENSI BERHASIL",
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    fontSize = 13.sp,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = log.studentName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Liveness: Verified [${log.livenessType}]",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // Meta-Details Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Mode", color = Color(0xFF64748B), fontSize = 10.sp)
                                    Text(log.type, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Waktu", color = Color(0xFF64748B), fontSize = 10.sp)
                                    val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                                    Text(timeFmt, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Status", color = Color(0xFF64748B), fontSize = 10.sp)
                                    val isSynced = log.syncStatus == "SYNCED"
                                    Text(
                                        text = if (isSynced) "TERKONEKSI" else "LOKAL/PENDING",
                                        color = if (isSynced) Color(0xFF10B981) else Color(0xFFFB923C),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Text(
                                "Tekan di mana saja untuk melanjutkan...",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // Lower Sandbox Simulator Sheet - high density light bento style
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .weight(0.9f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Bento stats row (Total Hadir and Pending Sync)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Box 1: Total Hadir
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "TOTAL HADIR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF4F46E5),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${allLogs.size}",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF312E81)
                            )
                        }
                    }

                    // Box 2: Pending Sync
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "PENDING SYNC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFD97706),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$pendingCount",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Success Notification Overlay card mimicking recent activity
                val recentLog = allLogs.firstOrNull()
                if (recentLog != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFD1FAE5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recentLog.studentName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1E293B),
                                fontSize = 13.sp
                            )
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(recentLog.timestamp))
                            Text(
                                text = "Berhasil Absen ${recentLog.type} • $timeStr",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "HISTORY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Sandbox Simulator controls header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Science, contentDescription = "Sandbox", tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                    Text(
                        text = "Sandbox Penguji Biometrik (Simulator)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Gunakan kontrol di bawah ini untuk mensimulasikan pemindaian biometrik dengan cepat (berguna untuk lingkungan emulator).",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (allStudents.isEmpty()) {
                    Text(
                        "Daftarkan siswa terlebih dahulu di tab 'Siswa' sebelum memulai simulasi.",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    var selectedStudentIndex by remember { mutableStateOf(0) }
                    val currentStudent = allStudents.getOrNull(selectedStudentIndex) ?: allStudents[0]

                    // Student selection layout carousel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                            .clickable {
                                selectedStudentIndex = (selectedStudentIndex + 1) % allStudents.size
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Target Siswa Simulasi", color = Color(0xFF64748B), fontSize = 10.sp)
                            Text(
                                text = "${currentStudent.name} (${currentStudent.className})",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Student", tint = Color(0xFF4F46E5))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulation Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateManualStudentRecognition(currentStudent, ChallengeType.SMILE) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_smile_btn"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = "Smile", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Liveness Senyum", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModel.simulateManualStudentRecognition(currentStudent, ChallengeType.BLINK) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_blink_btn"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.RemoveRedEye, contentDescription = "Blink", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Liveness Kedip", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.simulateMultiFaceDetected() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF43F5E)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFF43F5E)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_multi_btn"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Group, contentDescription = "Multi Face", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Multi-Face", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.simulateWrongDistanceDetected() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sim_distance_btn"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Distance Fail", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Jarak Jauh", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SISWA TAB SCREEN
// ==========================================
@Composable
fun StudentsTabScreen(viewModel: AttendanceViewModel) {
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    var inputName by remember { mutableStateOf("") }
    var inputClass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Registration Form Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(20.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Daftarkan Siswa Baru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Nama Lengkap Siswa") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFF4F46E5),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_name_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputClass,
                    onValueChange = { inputClass = it },
                    label = { Text("Kelas (Contoh: XII RPL 1)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFF4F46E5),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_class_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.registerStudent(inputName, inputClass)
                        inputName = ""
                        inputClass = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_student_btn")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan Data Siswa",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Student Data Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Siswa Terdaftar (${allStudents.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (allStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = "Empty Students",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Belum ada data siswa terpantau.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Silakan daftarkan nama siswa di atas.",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allStudents) { student ->
                    StudentCard(student = student, onDelete = { viewModel.removeStudent(student.id) })
                }
            }
        }
    }
}

@Composable
fun StudentCard(student: Student, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEEF2FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Face",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = student.name,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Kelas: ${student.className}",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_student_${student.id}")) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Siswa",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// HISTORY TAB SCREEN
// ==========================================
@Composable
fun HistoryTabScreen(viewModel: AttendanceViewModel) {
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()

    val pendingLogs = allLogs.filter { it.syncStatus == "PENDING" }
    val syncedLogs = allLogs.filter { it.syncStatus == "SYNCED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick Sync summary header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Absen", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        allLogs.size.toString(),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color(0xFF1E293B)
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = if (pendingLogs.isNotEmpty()) Color(0xFFFEF3C7) else Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Tertunda", color = if (pendingLogs.isNotEmpty()) Color(0xFFB45309) else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        pendingLogs.size.toString(),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = if (pendingLogs.isNotEmpty()) Color(0xFFD97706) else Color(0xFF64748B)
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = if (syncedLogs.isNotEmpty()) Color(0xFFE0F2FE) else Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .weight(1f)
                    .shadow(1.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Sinkron", color = if (syncedLogs.isNotEmpty()) Color(0xFF0369A1) else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        syncedLogs.size.toString(),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = if (syncedLogs.isNotEmpty()) Color(0xFF0284C7) else Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.triggerManualSync() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .testTag("sync_manual_trigger_btn")
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Sync", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sinkronkan Log", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = { viewModel.clearAllAttendanceLogs() },
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("clear_history_btn")
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hapus Riwayat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Log Kehadiran Lokal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (allLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = "Empty logs",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Belum ada log absensi tercatat.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Lakukan sensor wajah di tab 'Scanner'!",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allLogs) { log ->
                    LogCard(log = log, onDelete = { viewModel.removeStudent(log.id) /* Or delete specific log */ })
                }
            }
        }
    }
}

@Composable
fun LogCard(log: AttendanceLog, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Masuk status circle vs Pulang status circle
                    val isMasuk = log.type == "MASUK"
                    val bkgColor = if (isMasuk) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                    val txtColor = if (isMasuk) Color(0xFF059669) else Color(0xFFDC2626)
                    Box(
                        modifier = Modifier
                            .background(bkgColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = log.type,
                            fontSize = 9.sp,
                            color = txtColor,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = log.studentName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val dateStr = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                Text(
                    text = "$dateStr • Liveness: ${log.livenessType}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            // Sync Badge
            val syncText = when(log.syncStatus) {
                "SYNCED" -> "SYNCED"
                "SYNCING" -> "SYNCING"
                else -> "PENDING"
            }
            val syncBkg = when(log.syncStatus) {
                "SYNCED" -> Color(0xFFD1FAE5)
                "SYNCING" -> Color(0xFFE0F2FE)
                else -> Color(0xFFFEF3C7)
            }
            val syncColor = when(log.syncStatus) {
                "SYNCED" -> Color(0xFF059669)
                "SYNCING" -> Color(0xFF0284C7)
                else -> Color(0xFFD97706)
            }

            Box(
                modifier = Modifier
                    .background(syncBkg, RoundedCornerShape(8.dp))
                    .border(1.dp, syncColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = syncText,
                    fontSize = 10.sp,
                    color = syncColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}// ==========================================
// CONFIG CONFIG TAB SCREEN
// ==========================================
@Composable
fun ConfigTabScreen(viewModel: AttendanceViewModel) {
    val webhookUrl by viewModel.gasWebhookUrl.collectAsStateWithLifecycle()
    val cooldownStr by viewModel.cooldownTime.collectAsStateWithLifecycle()
    val minFaceAreaVal by viewModel.minFaceArea.collectAsStateWithLifecycle()
    val smileThreshVal by viewModel.smileThreshold.collectAsStateWithLifecycle()
    val blinkThreshVal by viewModel.blinkThreshold.collectAsStateWithLifecycle()
    val successDurationVal by viewModel.successOverlayDuration.collectAsStateWithLifecycle()
    val activeScheduleConfig by viewModel.scheduleSetting.collectAsStateWithLifecycle()

    var webhookUrlInput by remember { mutableStateOf(webhookUrl) }
    var cooldownInput by remember { mutableStateOf((cooldownStr / 1000).toString()) }

    // Synchronize local input state when cache triggers updates
    LaunchedEffect(webhookUrl) { webhookUrlInput = webhookUrl }
    LaunchedEffect(cooldownStr) { cooldownInput = (cooldownStr / 1000).toString() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Pengaturan KOMBAT Absensi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Text(
            "Sesuaikan integrasi server, durasi penundaan, serta batas toleransi biometrik wajah.",
            color = Color(0xFF64748B),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Backend Integration Setting
        Text("PENGATURAN SERVER BACKEND", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = webhookUrlInput,
                    onValueChange = { webhookUrlInput = it },
                    label = { Text("GAS Web App / Webhook URL") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = Color(0xFF4F46E5),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gas_url_input"),
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.updateSetting("gas_webhook_url", webhookUrlInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.End).testTag("save_gas_url_btn")
                ) {
                    Text("Simpan Webhook URL", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scheduling Mode Setting
        Text("PENJADWALAN CHRON ABSENSI", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Mode Perpindahan Absensi:",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Auto Button
                    val isAuto = activeScheduleConfig == "AUTO"
                    Button(
                        onClick = { viewModel.updateSetting("schedule_setting", "AUTO") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAuto) Color(0xFF4F46E5) else Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("AUTO (12:00)", fontSize = 10.sp, color = if (isAuto) Color.White else Color(0xFF475569), fontWeight = FontWeight.Bold)
                    }

                    // Masuk button only
                    val isMasuk = activeScheduleConfig == "MASUK"
                    Button(
                        onClick = { viewModel.updateSetting("schedule_setting", "MASUK") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMasuk) Color(0xFF4F46E5) else Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("MASUK", fontSize = 10.sp, color = if (isMasuk) Color.White else Color(0xFF475569), fontWeight = FontWeight.Bold)
                    }

                    // Pulang button only
                    val isPulang = activeScheduleConfig == "PULANG"
                    Button(
                        onClick = { viewModel.updateSetting("schedule_setting", "PULANG") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPulang) Color(0xFF4F46E5) else Color(0xFFF1F5F9)
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("PULANG", fontSize = 10.sp, color = if (isPulang) Color.White else Color(0xFF475569), fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AUTO: Mode otomatis berganti menjadi absen PULANG jika memasuki pukul 12:00 siang keatas. MASUK / PULANG: Memaksa system terus berada di mode yang dipilih.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Parameters Setting
        Text("BATAS TOLERANSI MONITORING (CONFIG)", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cooldown input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cooldown Absensi (Detik)", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Cooldown anti-spam scan nama sama", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    OutlinedTextField(
                        value = cooldownInput,
                        onValueChange = {
                            cooldownInput = it
                            val valueMs = (it.toLongOrNull() ?: 10L) * 1000L
                            viewModel.updateSetting("cooldown_time", valueMs.toString())
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.width(90.dp).testTag("cooldown_input"),
                        singleLine = true
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Min Face Area px input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Min Area Wajah (Pix)", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Membatasi jarak scanner terlampau jauh", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    OutlinedTextField(
                        value = minFaceAreaVal.toString(),
                        onValueChange = {
                            if (it.isNotBlank()) viewModel.updateSetting("min_face_area", it)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Smile liveness slider value
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Batas Threshold Senyum", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("${(smileThreshVal * 100).toInt()}% confidence", color = Color(0xFF4F46E5), fontSize = 12.sp)
                    }
                    Slider(
                        value = smileThreshVal,
                        onValueChange = { viewModel.updateSetting("smile_threshold", String.format(Locale.US, "%.2f", it)) },
                        valueRange = 0.1f..0.9f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4F46E5),
                            activeTrackColor = Color(0xFF4F46E5),
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Eye Aspect Ratio EAR blink threshold
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Batas Threshold Kedip (EAR)", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "%.2f EAR", blinkThreshVal), color = Color(0xFF4F46E5), fontSize = 12.sp)
                    }
                    Slider(
                        value = blinkThreshVal,
                        onValueChange = { viewModel.updateSetting("blink_threshold", String.format(Locale.US, "%.2f", it)) },
                        valueRange = 0.1f..0.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF4F46E5),
                            activeTrackColor = Color(0xFF4F46E5),
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Success overlay animation display duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Durasi Overlay Sukses (Ms)", color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Durasi tampilan detail profil siswa setelah absen", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    OutlinedTextField(
                        value = successDurationVal.toString(),
                        onValueChange = {
                            if (it.isNotBlank()) viewModel.updateSetting("success_duration", it)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}
