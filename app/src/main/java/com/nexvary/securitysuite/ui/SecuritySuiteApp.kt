package com.nexvary.securitysuite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nexvary.securitysuite.BuildConfig
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
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.Radar, null) },
                            label = { Text(if (arabic) "مباشر" else "Live") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.Wifi, null) },
                            label = { Text(if (arabic) "الجلسات" else "Sessions") },
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Icon(Icons.Default.Bluetooth, null) },
                            label = { Text(if (arabic) "المراقبة" else "Watch") },
                        )
                        NavigationBarItem(
                            selected = tab == 3,
                            onClick = { tab = 3 },
                            icon = { Icon(Icons.Default.Language, null) },
                            label = { Text(if (arabic) "حول" else "About") },
                        )
                    }
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Header(arabic = arabic, onLanguage = { arabic = !arabic })
                    Spacer(Modifier.height(10.dp))
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

@Composable
private fun Header(arabic: Boolean, onLanguage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("NEXVARY", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(if (arabic) "Security Suite · Wireless Watch" else "Security Suite · Wireless Watch")
        }
        Button(onClick = onLanguage) { Text(if (arabic) "EN" else "AR") }
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
            Button(onClick = toggleScan) {
                Text(
                    if (arabic) {
                        if (scanning) "إيقاف المسح" else "بدء المسح"
                    } else {
                        if (scanning) "Stop scan" else "Start scan"
                    },
                )
            }
            Button(onClick = toggleRecording) {
                Text(
                    if (arabic) {
                        if (recording) "حفظ الجلسة" else "تسجيل جلسة"
                    } else {
                        if (recording) "Save session" else "Record session"
                    },
                )
            }
            Button(onClick = clearLive) {
                Text(if (arabic) "مسح القائمة" else "Clear")
            }
        }

        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        StatsCard(stats, arabic)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = filter.query,
            onValueChange = setQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(if (arabic) "بحث بالاسم أو العنوان أو التصنيف" else "Search name, address or classification") },
        )

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FilterChip(
                selected = filter.source == null,
                onClick = { setSource(null) },
                label = { Text(if (arabic) "كل الراديو" else "All radio") },
            )
            FilterChip(
                selected = filter.source == RadioSource.BLE,
                onClick = { setSource(RadioSource.BLE) },
                label = { Text("BLE") },
            )
            FilterChip(
                selected = filter.source == RadioSource.WIFI,
                onClick = { setSource(RadioSource.WIFI) },
                label = { Text("Wi-Fi") },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FilterChip(
                selected = filter.category == null,
                onClick = { setCategory(null) },
                label = { Text(if (arabic) "الكل" else "All") },
            )
            FilterChip(
                selected = filter.category == DeviceCategory.TRACKER,
                onClick = { setCategory(DeviceCategory.TRACKER) },
                label = { Text(if (arabic) "متعقبات" else "Trackers") },
            )
            FilterChip(
                selected = filter.category == DeviceCategory.DRONE,
                onClick = { setCategory(DeviceCategory.DRONE) },
                label = { Text(if (arabic) "درون" else "Drones") },
            )
            FilterChip(
                selected = filter.matchedOnly,
                onClick = toggleMatched,
                label = { Text(if (arabic) "مصنف فقط" else "Matched") },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FilterChip(
                selected = filter.minRssi == -100,
                onClick = { setMinRssi(-100) },
                label = { Text(if (arabic) "أي إشارة" else "Any signal") },
            )
            FilterChip(
                selected = filter.minRssi == -80,
                onClick = { setMinRssi(-80) },
                label = { Text("≥ -80") },
            )
            FilterChip(
                selected = filter.minRssi == -65,
                onClick = { setMinRssi(-65) },
                label = { Text("≥ -65") },
            )
        }

        Text(
            (if (arabic) "الظاهر: " else "Visible: ") + visible.size + " / " + devices.size,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(6.dp))

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (arabic) "لا توجد نتائج مطابقة" else "No matching observations")
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text(
                if (arabic) "ملخص الإشارات الحية" else "Live signal summary",
                fontWeight = FontWeight.Bold,
            )
            Text(
                (if (arabic) "الإجمالي " else "Total ") + stats.total +
                    " · BLE " + stats.ble +
                    " · Wi-Fi " + stats.wifi +
                    " · " + (if (arabic) "مصنف " else "matched ") + stats.matched,
            )
            Text(
                (if (arabic) "متعقبات " else "Trackers ") + stats.trackers +
                    " · " + (if (arabic) "درون " else "drones ") + stats.drones +
                    " · " + (if (arabic) "تنبيه مرتفع " else "high attention ") + stats.highAttention,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DeviceCard(device: DisplayDevice, arabic: Boolean) {
    val observation = device.observation
    val match = device.match
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (observation.source == RadioSource.BLE) Icons.Default.Bluetooth else Icons.Default.Wifi,
                    contentDescription = null,
                )
                Text(
                    "  " + (
                        match?.label
                            ?: observation.name
                            ?: if (arabic) "جهاز غير مصنف" else "Unclassified device"
                        ),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(observation.name ?: "—")
            Text(
                (observation.address ?: "—") + " · RSSI " + observation.rssi + " dBm",
                style = MaterialTheme.typography.bodySmall,
            )
            match?.let {
                Text(
                    severityLabel(it.severity, arabic) + " · " + it.category.name + " · " +
                        it.confidence + " · " + it.reason,
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
                    )
                }
            }

            observation.serviceData["FFFA"]?.let { raw ->
                ProtocolDecoders.decodeRemoteId(raw)?.let { rid ->
                    Text("Remote ID: " + (rid.messageType ?: "unknown") + " · " + (rid.status ?: ""))
                    if (rid.latitude != null && rid.longitude != null) {
                        Text("Aircraft: " + rid.latitude + ", " + rid.longitude)
                    }
                    if (rid.headingDegrees != null || rid.horizontalSpeedMps != null) {
                        Text(
                            "Heading " + (rid.headingDegrees?.toString() ?: "—") +
                                "° · " + (rid.horizontalSpeedMps?.toString() ?: "—") + " m/s",
                        )
                    }
                    if (!rid.uasId.isNullOrBlank()) Text("UAS: " + rid.uasId)
                    if (!rid.operatorId.isNullOrBlank()) Text("Operator ID: " + rid.operatorId)
                }
            }

            Text(
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(observation.lastSeenMs)),
                style = MaterialTheme.typography.labelSmall,
            )
        }
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
                Button(onClick = compare) {
                    Text(if (arabic) "قارن آخر جلستين" else "Compare last two")
                }
                Button(onClick = clearSessions) {
                    Text(if (arabic) "حذف الكل" else "Clear all")
                }
            }
        }

        diff?.let { comparison ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(if (arabic) "نتيجة المقارنة" else "Session comparison", fontWeight = FontWeight.Bold)
                        Text(
                            (if (arabic) "جديد " else "Added ") + comparison.added.size +
                                " · " + (if (arabic) "اختفى " else "removed ") + comparison.removed.size +
                                " · " + (if (arabic) "مستمر " else "persisted ") + comparison.persisted.size,
                        )
                        comparison.added.take(5).forEach {
                            Text("+ " + (it.label ?: it.name ?: it.address ?: it.stableId))
                        }
                        comparison.removed.take(5).forEach {
                            Text("- " + (it.label ?: it.name ?: it.address ?: it.stableId))
                        }
                    }
                }
            }
        }

        if (sessions.isEmpty()) {
            item {
                Text(if (arabic) "لا توجد جلسات محفوظة بعد." else "No saved sessions yet.")
            }
        }

        items(sessions, key = { it.id }) { session ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp)) {
                    Text(formatSessionTime(session.startedAtMs), fontWeight = FontWeight.Bold)
                    Text(
                        (if (arabic) "أجهزة " else "Devices ") + session.devices.size +
                            " · " + (if (arabic) "مصنف " else "matched ") + session.matchedCount +
                            " · " + (if (arabic) "متعقبات " else "trackers ") + session.trackerCount +
                            " · " + (if (arabic) "درون " else "drones ") + session.droneCount,
                    )
                    Text(
                        (if (arabic) "تنبيه مرتفع " else "High attention ") + session.highAttentionCount,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { ReportExporter.shareSession(context, session) }) {
                            Text(if (arabic) "تصدير CSV" else "Export CSV")
                        }
                        Button(onClick = { deleteSession(session.id) }) {
                            Text(if (arabic) "حذف" else "Delete")
                        }
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
            Text(
                if (arabic) "تنبيهات الأجهزة والعناوين" else "Device / address alerts",
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                if (arabic) {
                    "تظهر التنبيهات عند مطابقة الاسم أو العنوان أو التصنيف. التنبيهات عالية الخطورة الناتجة من البروتوكول تعمل تلقائيًا."
                } else {
                    "Alerts match name, address or classification. High-attention protocol events can also alert automatically."
                },
                style = MaterialTheme.typography.bodySmall,
            )
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
            Button(onClick = { addWatch(term); term = "" }) {
                Text(if (arabic) "إضافة تنبيه" else "Add alert")
            }
        }
        items(watchTerms, key = { "watch-" + it }) { value ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(value, modifier = Modifier.weight(1f))
                Button(onClick = { removeWatch(value) }) {
                    Text(if (arabic) "حذف" else "Remove")
                }
            }
        }
        item { HorizontalDivider() }
        item {
            Text(
                if (arabic) "توقيعات أجهزة مخصصة" else "Custom Device Signatures",
                fontWeight = FontWeight.Bold,
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
            Button(onClick = {
                addSignature(label, keyword)
                label = ""
                keyword = ""
            }) {
                Text(if (arabic) "إضافة توقيع" else "Add signature")
            }
        }
        items(signatures, key = { it.label + "|" + it.keyword }) { signature ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(signature.label + ": " + signature.keyword, modifier = Modifier.weight(1f))
                Button(onClick = { removeSignature(signature) }) {
                    Text(if (arabic) "حذف" else "Remove")
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("NEXVARY Security Suite", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Text("Version " + BuildConfig.VERSION_NAME)
        }
        item {
            Text(
                if (arabic) {
                    "يراقب التطبيق إعلانات Wi-Fi وBluetooth LE المتاحة عبر واجهات Android. لا يحاول الاتصال بالأجهزة المكتشفة. نتائج التوقيعات مؤشرات تقنية وليست إثباتًا لهوية الجهاز أو مالكه."
                } else {
                    "The app observes Wi-Fi and Bluetooth LE broadcasts exposed by Android. It does not attempt to connect to discovered devices. Signature matches are technical indicators, not proof of device identity or ownership."
                },
            )
        }
        item {
            Text(
                if (arabic) {
                    "يشمل الإصدار الحالي DULT وGoogle Find Hub وBluetooth Remote ID والجلسات والمقارنة والتقارير والتوقيعات المخصصة."
                } else {
                    "This release includes DULT, Google Find Hub, Bluetooth Remote ID, sessions, comparison, reports and custom signatures."
                },
            )
        }
        item {
            Button(onClick = { uri.openUri("https://github.com/nexvary/NEXVARY-Security-Suite") }) {
                Text(if (arabic) "مستودع NEXVARY" else "NEXVARY repository")
            }
        }
        item {
            Button(onClick = { uri.openUri("https://github.com/OffGridPete/Fieldwatch") }) {
                Text(if (arabic) "مشروع Fieldwatch الأصلي" else "Original Fieldwatch project")
            }
        }
        item {
            Text(
                if (arabic) {
                    "يتضمن المشروع أجزاءً مقتبسة أو معاد تصميمها من Fieldwatch وفق ترخيص MIT."
                } else {
                    "Includes portions adapted or redesigned from Fieldwatch under the MIT License."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
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
