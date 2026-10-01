package com.nexvary.securitysuite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nexvary.securitysuite.wireless.AlertSeverity
import com.nexvary.securitysuite.wireless.CustomSignature
import com.nexvary.securitysuite.wireless.DeviceCategory
import com.nexvary.securitysuite.wireless.DeviceFilter
import com.nexvary.securitysuite.wireless.DeviceFilterState
import com.nexvary.securitysuite.wireless.DisplayDevice
import com.nexvary.securitysuite.wireless.LiveStats
import com.nexvary.securitysuite.wireless.ProtocolDecoders
import com.nexvary.securitysuite.wireless.RadioSource
import com.nexvary.securitysuite.wireless.ReportExporter
import com.nexvary.securitysuite.wireless.SessionDiff
import com.nexvary.securitysuite.wireless.SessionSnapshot
import com.nexvary.securitysuite.wireless.WirelessViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecuritySuiteApp(vm: WirelessViewModel) {
    val state by vm.state.collectAsState()
    var arabic by rememberSaveable { mutableStateOf(Locale.getDefault().language == "ar") }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    NexvaryTheme {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                NexvaryPalette.Midnight,
                                NexvaryPalette.DeepNavy,
                                Color(0xFF07131D),
                            ),
                        ),
                    ),
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Surface(
                            modifier = Modifier.navigationBarsPadding(),
                            color = NexvaryPalette.Panel.copy(alpha = 0.98f),
                            shadowElevation = 8.dp,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                BottomItem(tab == 0, { tab = 0 }, Icons.Default.Radar, if (arabic) "مباشر" else "Live")
                                BottomItem(tab == 1, { tab = 1 }, Icons.Default.Wifi, if (arabic) "الجلسات" else "Sessions")
                                BottomItem(tab == 2, { tab = 2 }, Icons.Default.Bluetooth, if (arabic) "المراقبة" else "Watch")
                                BottomItem(tab == 3, { tab = 3 }, Icons.Default.Security, if (arabic) "حول" else "About")
                            }
                        }
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Header(arabic = arabic, onLanguage = { arabic = !arabic })
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.weight(1f)) {
                            when (tab) {
                                0 -> LiveScreen(
                                    devices = state.devices,
                                    filter = state.filter,
                                    stats = state.stats,
                                    scanning = state.scanning,
                                    recording = state.recording,
                                    status = state.status,
                                    arabic = arabic,
                                    toggleScan = vm::toggleScan,
                                    toggleRecording = vm::toggleRecording,
                                    clearLive = vm::clearLive,
                                    setQuery = vm::setQuery,
                                    setMinRssi = vm::setMinRssi,
                                    setSource = vm::setSourceFilter,
                                    setCategory = vm::setCategoryFilter,
                                    toggleMatched = vm::toggleMatchedOnly,
                                )
                                1 -> SessionsScreen(
                                    sessions = state.sessions,
                                    diff = state.lastDiff,
                                    arabic = arabic,
                                    compare = vm::compareLastTwo,
                                    deleteSession = vm::deleteSession,
                                    clearSessions = vm::clearSessions,
                                )
                                2 -> WatchScreen(
                                    watchTerms = state.watchTerms,
                                    signatures = state.customSignatures,
                                    arabic = arabic,
                                    addWatch = vm::addWatchTerm,
                                    removeWatch = vm::removeWatchTerm,
                                    addSignature = vm::addCustomSignature,
                                    removeSignature = vm::removeCustomSignature,
                                )
                                else -> AboutScreen(arabic)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    val accent = if (selected) NexvaryPalette.ElectricBlue else NexvaryPalette.Gunmetal
    Card(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                NexvaryPalette.ElectricBlue.copy(alpha = 0.16f)
            } else {
                NexvaryPalette.PanelRaised.copy(alpha = 0.7f)
            },
        ),
        border = BorderStroke(1.dp, accent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) NexvaryPalette.ElectricBlue else NexvaryPalette.Silver,
            )
            Text(
                label,
                color = if (selected) NexvaryPalette.ElectricBlue else NexvaryPalette.Silver,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun Header(arabic: Boolean, onLanguage: () -> Unit) {
    NexvaryCard(borderColor = NexvaryPalette.Silver) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = NexvaryPalette.ElectricBlue,
                    contentColor = NexvaryPalette.Midnight,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Column(Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        "NEXVARY",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = NexvaryPalette.Platinum,
                    )
                    Text(
                        "Security Suite LAB",
                        color = NexvaryPalette.Silver,
                    )
                }
            }
            OutlinedButton(
                onClick = onLanguage,
                border = BorderStroke(1.dp, NexvaryPalette.Gold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexvaryPalette.Gold),
            ) {
                Text(if (arabic) "EN" else "AR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LiveScreen(
    devices: List<DisplayDevice>,
    filter: DeviceFilterState,
    stats: LiveStats,
    scanning: Boolean,
    recording: Boolean,
    status: String,
    arabic: Boolean,
    toggleScan: () -> Unit,
    toggleRecording: () -> Unit,
    clearLive: () -> Unit,
    setQuery: (String) -> Unit,
    setMinRssi: (Int) -> Unit,
    setSource: (RadioSource?) -> Unit,
    setCategory: (DeviceCategory?) -> Unit,
    toggleMatched: () -> Unit,
) {
    val visible = DeviceFilter.apply(devices, filter)

    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AccentButton(
                text = if (arabic) {
                    if (scanning) "إيقاف المسح" else "بدء المسح"
                } else {
                    if (scanning) "Stop scan" else "Start scan"
                },
                color = if (scanning) NexvaryPalette.NeonGreen else NexvaryPalette.ElectricBlue,
                onClick = toggleScan,
            )
            AccentButton(
                text = if (arabic) {
                    if (recording) "حفظ الجلسة" else "تسجيل جلسة"
                } else {
                    if (recording) "Save session" else "Record session"
                },
                color = if (recording) NexvaryPalette.Amber else NexvaryPalette.Gold,
                onClick = toggleRecording,
            )
            OutlinedButton(
                onClick = clearLive,
                border = BorderStroke(1.dp, NexvaryPalette.Silver),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexvaryPalette.Silver),
            ) {
                Text(if (arabic) "مسح" else "Clear")
            }
        }

        Text(status, color = if (scanning) NexvaryPalette.NeonGreen else NexvaryPalette.Silver)
        Spacer(Modifier.height(6.dp))
        StatsCard(stats, arabic)
        Spacer(Modifier.height(3.dp))

        OutlinedTextField(
            value = filter.query,
            onValueChange = setQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(if (arabic) "بحث بالاسم أو العنوان أو التصنيف" else "Search name, address or classification") },
        )

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            NexvaryFilter(filter.source == null, { setSource(null) }, if (arabic) "كل الراديو" else "All radio")
            NexvaryFilter(filter.source == RadioSource.BLE, { setSource(RadioSource.BLE) }, "BLE")
            NexvaryFilter(filter.source == RadioSource.WIFI, { setSource(RadioSource.WIFI) }, "Wi-Fi")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            NexvaryFilter(filter.category == null, { setCategory(null) }, if (arabic) "الكل" else "All")
            NexvaryFilter(filter.category == DeviceCategory.TRACKER, { setCategory(DeviceCategory.TRACKER) }, if (arabic) "متعقبات" else "Trackers")
            NexvaryFilter(filter.category == DeviceCategory.DRONE, { setCategory(DeviceCategory.DRONE) }, if (arabic) "درون" else "Drones")
            NexvaryFilter(filter.matchedOnly, toggleMatched, if (arabic) "مصنف" else "Matched")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            NexvaryFilter(filter.minRssi == -100, { setMinRssi(-100) }, if (arabic) "أي إشارة" else "Any signal")
            NexvaryFilter(filter.minRssi == -80, { setMinRssi(-80) }, "≥ -80")
            NexvaryFilter(filter.minRssi == -65, { setMinRssi(-65) }, "≥ -65")
        }

        Text(
            (if (arabic) "الظاهر: " else "Visible: ") + visible.size + " / " + devices.size,
            style = MaterialTheme.typography.labelMedium,
            color = NexvaryPalette.Silver,
        )
        Spacer(Modifier.height(6.dp))

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (arabic) "لا توجد نتائج مطابقة" else "No matching observations", color = NexvaryPalette.Silver)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(visible, key = { it.observation.stableId }) { item ->
                    DeviceCard(item, arabic)
                }
            }
        }
    }
}

@Composable
private fun StatsCard(stats: LiveStats, arabic: Boolean) {
    NexvaryCard(borderColor = if (stats.highAttention > 0) NexvaryPalette.Amber else NexvaryPalette.ElectricBlue) {
        Text(
            if (arabic) "ملخص الإشارات الحية" else "Live signal summary",
            fontWeight = FontWeight.Bold,
            color = NexvaryPalette.ElectricBlue,
        )
        Spacer(Modifier.height(5.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Metric(if (arabic) "الإجمالي" else "TOTAL", stats.total.toString(), NexvaryPalette.Platinum)
            Metric("BLE", stats.ble.toString(), NexvaryPalette.Purple)
            Metric("WI-FI", stats.wifi.toString(), NexvaryPalette.ElectricBlue)
            Metric(if (arabic) "مصنف" else "MATCH", stats.matched.toString(), NexvaryPalette.NeonGreen)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            (if (arabic) "متعقبات " else "Trackers ") + stats.trackers +
                " · " + (if (arabic) "درون " else "Drones ") + stats.drones +
                " · " + (if (arabic) "تنبيه مرتفع " else "High attention ") + stats.highAttention,
            color = if (stats.highAttention > 0) NexvaryPalette.Amber else NexvaryPalette.Silver,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Metric(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, color.copy(alpha = 0.75f)),
    ) {
        Column(
            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = color, fontWeight = FontWeight.Black)
            Text(label, color = NexvaryPalette.Silver, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DeviceCard(device: DisplayDevice, arabic: Boolean) {
    val observation = device.observation
    val match = device.match
    val severityColor = when (match?.severity) {
        AlertSeverity.CRITICAL -> NexvaryPalette.Danger
        AlertSeverity.HIGH -> NexvaryPalette.Amber
        AlertSeverity.MEDIUM -> NexvaryPalette.Gold
        AlertSeverity.LOW -> NexvaryPalette.ElectricBlue
        else -> NexvaryPalette.Silver
    }

    NexvaryCard(borderColor = severityColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (observation.source == RadioSource.BLE) Icons.Default.Bluetooth else Icons.Default.Wifi,
                contentDescription = null,
                tint = if (observation.source == RadioSource.BLE) NexvaryPalette.Purple else NexvaryPalette.ElectricBlue,
            )
            Text(
                "  " + (
                    match?.label
                        ?: observation.name
                        ?: if (arabic) "جهاز غير مصنف" else "Unclassified device"
                    ),
                fontWeight = FontWeight.Bold,
                color = NexvaryPalette.Platinum,
            )
        }
        Text(observation.name ?: "—", color = NexvaryPalette.Silver)
        Text(
            (observation.address ?: "—") + " · RSSI " + observation.rssi + " dBm",
            color = NexvaryPalette.Silver,
            style = MaterialTheme.typography.bodySmall,
        )
        match?.let {
            Text(
                severityLabel(it.severity, arabic) + " · " + it.category.name + " · " +
                    it.confidence + " · " + it.reason,
                color = severityColor,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        observation.serviceData["FCB2"]?.let { raw ->
            ProtocolDecoders.decodeDult(raw)?.let {
                Text(
                    if (it.separated) {
                        if (arabic) "DULT: منفصل عن المالك" else "DULT: separated from owner"
                    } else {
                        if (arabic) "DULT: قريب من المالك" else "DULT: near owner"
                    },
                    color = if (it.separated) NexvaryPalette.Amber else NexvaryPalette.NeonGreen,
                )
            }
        }

        observation.serviceData["FFFA"]?.let { raw ->
            ProtocolDecoders.decodeRemoteId(raw)?.let { rid ->
                Text(
                    "Remote ID: " + (rid.messageType ?: "unknown") + " · " + (rid.status ?: ""),
                    color = NexvaryPalette.Gold,
                )
                if (rid.latitude != null && rid.longitude != null) {
                    Text("Aircraft: " + rid.latitude + ", " + rid.longitude, color = NexvaryPalette.Silver)
                }
                if (rid.headingDegrees != null || rid.horizontalSpeedMps != null) {
                    Text(
                        "Heading " + (rid.headingDegrees?.toString() ?: "—") +
                            "° · " + (rid.horizontalSpeedMps?.toString() ?: "—") + " m/s",
                        color = NexvaryPalette.Silver,
                    )
                }
                if (!rid.uasId.isNullOrBlank()) Text("UAS: " + rid.uasId, color = NexvaryPalette.Platinum)
                if (!rid.operatorId.isNullOrBlank()) Text("Operator ID: " + rid.operatorId, color = NexvaryPalette.Platinum)
            }
        }

        Text(
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(observation.lastSeenMs)),
            color = NexvaryPalette.Silver,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun SessionsScreen(
    sessions: List<SessionSnapshot>,
    diff: SessionDiff?,
    arabic: Boolean,
    compare: () -> Unit,
    deleteSession: (String) -> Unit,
    clearSessions: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AccentButton(if (arabic) "قارن آخر جلستين" else "Compare last two", NexvaryPalette.ElectricBlue, compare)
                OutlinedButton(
                    onClick = clearSessions,
                    border = BorderStroke(1.dp, NexvaryPalette.Danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexvaryPalette.Danger),
                ) {
                    Text(if (arabic) "حذف الكل" else "Clear all")
                }
            }
        }

        diff?.let { comparison ->
            item {
                NexvaryCard(NexvaryPalette.Gold) {
                    Text(if (arabic) "نتيجة المقارنة" else "Session comparison", fontWeight = FontWeight.Bold, color = NexvaryPalette.Gold)
                    Text(
                        (if (arabic) "جديد " else "Added ") + comparison.added.size +
                            " · " + (if (arabic) "اختفى " else "Removed ") + comparison.removed.size +
                            " · " + (if (arabic) "مستمر " else "Persisted ") + comparison.persisted.size,
                        color = NexvaryPalette.Platinum,
                    )
                    comparison.added.take(5).forEach {
                        Text("+ " + (it.label ?: it.name ?: it.address ?: it.stableId), color = NexvaryPalette.NeonGreen)
                    }
                    comparison.removed.take(5).forEach {
                        Text("- " + (it.label ?: it.name ?: it.address ?: it.stableId), color = NexvaryPalette.Amber)
                    }
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Text(if (arabic) "لا توجد جلسات محفوظة بعد." else "No saved sessions yet.", color = NexvaryPalette.Silver)
            }
        }

        items(sessions, key = { it.id }) { session ->
            NexvaryCard(NexvaryPalette.Gunmetal) {
                Text(formatSessionTime(session.startedAtMs), fontWeight = FontWeight.Bold, color = NexvaryPalette.Platinum)
                Text(
                    (if (arabic) "أجهزة " else "Devices ") + session.devices.size +
                        " · " + (if (arabic) "مصنف " else "Matched ") + session.matchedCount +
                        " · " + (if (arabic) "متعقبات " else "Trackers ") + session.trackerCount +
                        " · " + (if (arabic) "درون " else "Drones ") + session.droneCount,
                    color = NexvaryPalette.Silver,
                )
                Text(
                    (if (arabic) "تنبيه مرتفع " else "High attention ") + session.highAttentionCount,
                    color = if (session.highAttentionCount > 0) NexvaryPalette.Amber else NexvaryPalette.Silver,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccentButton(if (arabic) "تصدير CSV" else "Export CSV", NexvaryPalette.Gold) {
                        ReportExporter.shareSession(context, session)
                    }
                    OutlinedButton(
                        onClick = { deleteSession(session.id) },
                        border = BorderStroke(1.dp, NexvaryPalette.Danger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexvaryPalette.Danger),
                    ) {
                        Text(if (arabic) "حذف" else "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchScreen(
    watchTerms: List<String>,
    signatures: List<CustomSignature>,
    arabic: Boolean,
    addWatch: (String) -> Unit,
    removeWatch: (String) -> Unit,
    addSignature: (String, String) -> Unit,
    removeSignature: (CustomSignature) -> Unit,
) {
    var term by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var keyword by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            NexvaryCard(NexvaryPalette.Amber) {
                Text(
                    if (arabic) "التنبيهات والتوقيعات" else "Alerts & signatures",
                    fontWeight = FontWeight.Bold,
                    color = NexvaryPalette.Amber,
                )
                Text(
                    if (arabic) {
                        "تطابق الاسم أو العنوان أو التصنيف يولد تنبيهًا. أحداث البروتوكول عالية الخطورة يمكنها التنبيه تلقائيًا."
                    } else {
                        "Name, address or classification matches can alert. High-attention protocol events can also alert automatically."
                    },
                    color = NexvaryPalette.Silver,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            OutlinedTextField(
                value = term,
                onValueChange = { term = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (arabic) "اسم، عنوان أو كلمة" else "Name, address or keyword") },
            )
        }
        item {
            AccentButton(if (arabic) "إضافة تنبيه" else "Add alert", NexvaryPalette.Amber) {
                addWatch(term)
                term = ""
            }
        }
        items(watchTerms, key = { "watch-" + it }) { value ->
            NexvaryCard(NexvaryPalette.Gunmetal) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(value, modifier = Modifier.weight(1f), color = NexvaryPalette.Platinum)
                    OutlinedButton(
                        onClick = { removeWatch(value) },
                        border = BorderStroke(1.dp, NexvaryPalette.Danger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexvaryPalette.Danger),
                    ) {
                        Text(if (arabic) "حذف" else "Remove")
                    }
                }
            }
        }
        item { HorizontalDivider(color = NexvaryPalette.Gunmetal) }
        item {
            Text(
                if (arabic) "توقيعات أجهزة مخصصة" else "Custom Device Signatures",
                fontWeight = FontWeight.Bold,
                color = NexvaryPalette.ElectricBlue,
            )
        }
        item {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (arabic) "اسم التوقيع" else "Signature label") },
            )
        }
        item {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (arabic) "كلمة في الاسم أو SSID" else "Name / SSID keyword") },
            )
        }
        item {
            AccentButton(if (arabic) "إضافة توقيع" else "Add signature", NexvaryPalette.ElectricBlue) {
                addSignature(label, keyword)
                label = ""
                keyword = ""
            }
        }
        items(signatures, key = { it.label + "|" + it.keyword }) { signature ->
            NexvaryCard(NexvaryPalette.ElectricBlueDark) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(signature.label + ": " + signature.keyword, modifier = Modifier.weight(1f), color = NexvaryPalette.Platinum)
                    OutlinedButton(
                        onClick = { removeSignature(signature) },
                        border = BorderStroke(1.dp, NexvaryPalette.Danger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexvaryPalette.Danger),
                    ) {
                        Text(if (arabic) "حذف" else "Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(arabic: Boolean) {
    val uri = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            NexvaryCard(NexvaryPalette.ElectricBlue) {
                Text(
                    if (arabic) "عن النظام" else "About the system",
                    fontWeight = FontWeight.Bold,
                    color = NexvaryPalette.ElectricBlue,
                )
                Text(
                    if (arabic) {
                        "منصة NEXVARY للمراقبة اللاسلكية المحلية عبر Wi-Fi وBluetooth LE. لا تتصل بالأجهزة المكتشفة، وتعرض مستوى الثقة وسبب التصنيف لتقليل الاستنتاجات الخاطئة."
                    } else {
                        "NEXVARY local wireless-awareness platform for Wi-Fi and Bluetooth LE. It does not connect to discovered devices and exposes classification confidence and reasons to reduce false conclusions."
                    },
                    color = NexvaryPalette.Platinum,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Text(
                if (arabic) "روابط NEXVARY الرسمية" else "Official NEXVARY links",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NexvaryPalette.Gold,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
        item {
            LinkButton(Icons.Default.Language, if (arabic) "الموقع الرسمي" else "Website", "nexvary.com", NexvaryPalette.ElectricBlue) {
                uri.openUri("https://nexvary.com/")
            }
        }
        item {
            LinkButton(Icons.Default.Language, "Facebook", "NEXVARY", Color(0xFF5CA8FF)) {
                uri.openUri("https://www.facebook.com/share/14p9krEn5ij/")
            }
        }
        item {
            LinkButton(Icons.Default.PlayCircle, "YouTube", "@NexvaryInc", NexvaryPalette.Danger) {
                uri.openUri("https://www.youtube.com/@NexvaryInc")
            }
        }
        item {
            LinkButton(Icons.Default.AlternateEmail, "X / Twitter", "@Nexvary", NexvaryPalette.Platinum) {
                uri.openUri("https://x.com/Nexvary")
            }
        }
        item {
            LinkButton(Icons.Default.Email, if (arabic) "البريد الإلكتروني" else "Email", "info@nexvary.com", NexvaryPalette.NeonGreen) {
                uri.openUri("mailto:info@nexvary.com")
            }
        }
        item {
            LinkButton(Icons.Default.Code, "GitHub", "github.com/nexvary", NexvaryPalette.Gold) {
                uri.openUri("https://github.com/nexvary")
            }
        }
    }
}

@Composable
private fun LinkButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NexvaryPalette.PanelRaised.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.75f)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Column(Modifier.padding(horizontal = 9.dp)) {
                Text(title, color = color, fontWeight = FontWeight.Bold)
                Text(subtitle, color = NexvaryPalette.Silver, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NexvaryCard(
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NexvaryPalette.Panel.copy(alpha = 0.94f),
        ),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

@Composable
private fun AccentButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = NexvaryPalette.Midnight,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NexvaryFilter(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = NexvaryPalette.Panel,
            labelColor = NexvaryPalette.Silver,
            selectedContainerColor = NexvaryPalette.ElectricBlue.copy(alpha = 0.25f),
            selectedLabelColor = NexvaryPalette.ElectricBlue,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = NexvaryPalette.Gunmetal,
            selectedBorderColor = NexvaryPalette.ElectricBlue,
        ),
    )
}

private fun severityLabel(severity: AlertSeverity, arabic: Boolean): String =
    if (!arabic) severity.name else when (severity) {
        AlertSeverity.INFO -> "معلومة"
        AlertSeverity.LOW -> "منخفض"
        AlertSeverity.MEDIUM -> "متوسط"
        AlertSeverity.HIGH -> "مرتفع"
        AlertSeverity.CRITICAL -> "حرج"
    }

private fun formatSessionTime(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ms))
