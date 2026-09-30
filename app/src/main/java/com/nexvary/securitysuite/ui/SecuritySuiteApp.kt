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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nexvary.securitysuite.wireless.CustomSignature
import com.nexvary.securitysuite.wireless.DeviceCategory
import com.nexvary.securitysuite.wireless.DisplayDevice
import com.nexvary.securitysuite.wireless.ProtocolDecoders
import com.nexvary.securitysuite.wireless.RadioSource
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
                            label = { Text(if (arabic) "الأجهزة" else "Devices") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.Bluetooth, null) },
                            label = { Text(if (arabic) "المراقبة" else "Watch") },
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Header(arabic = arabic, onLanguage = { arabic = !arabic })
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            0 -> DevicesScreen(
                                devices = state.devices,
                                scanning = state.scanning,
                                status = state.status,
                                arabic = arabic,
                                toggle = vm::toggleScan,
                            )
                            1 -> WatchScreen(
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
            Text(if (arabic) "Security Suite · مراقبة لاسلكية" else "Security Suite · Wireless Watch")
        }
        Button(onClick = onLanguage) { Text(if (arabic) "EN" else "AR") }
    }
}

@Composable
private fun DevicesScreen(
    devices: List<DisplayDevice>,
    scanning: Boolean,
    status: String,
    arabic: Boolean,
    toggle: () -> Unit,
) {
    var filter by remember { mutableStateOf<DeviceCategory?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = toggle) {
                Text(
                    if (arabic) {
                        if (scanning) "إيقاف المسح" else "بدء المسح"
                    } else {
                        if (scanning) "Stop scan" else "Start scan"
                    },
                )
            }
            Text("  " + status, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = filter == null,
                onClick = { filter = null },
                label = { Text(if (arabic) "الكل" else "All") },
            )
            FilterChip(
                selected = filter == DeviceCategory.TRACKER,
                onClick = { filter = DeviceCategory.TRACKER },
                label = { Text(if (arabic) "متعقبات" else "Trackers") },
            )
            FilterChip(
                selected = filter == DeviceCategory.DRONE,
                onClick = { filter = DeviceCategory.DRONE },
                label = { Text(if (arabic) "درون" else "Drones") },
            )
        }

        Spacer(Modifier.height(8.dp))
        val filtered = devices.filter { filter == null || it.match?.category == filter }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (arabic) "لا توجد إشارات بعد" else "No observations yet")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.observation.stableId }) { item ->
                    DeviceCard(item, arabic)
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DisplayDevice, arabic: Boolean) {
    val observation = device.observation
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (observation.source == RadioSource.BLE) Icons.Default.Bluetooth else Icons.Default.Wifi,
                    contentDescription = null,
                )
                Text(
                    "  " + (
                        device.match?.label
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

            device.match?.let { match ->
                Text(
                    match.category.name + " · " + match.confidence + " · " + match.reason,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            observation.serviceData["FFFA"]?.let { raw ->
                ProtocolDecoders.decodeRemoteId(raw)?.let { rid ->
                    Text("Remote ID: " + (rid.messageType ?: "unknown") + " · " + (rid.status ?: ""))
                    if (rid.latitude != null && rid.longitude != null) {
                        Text(rid.latitude.toString() + ", " + rid.longitude.toString())
                    }
                    if (!rid.uasId.isNullOrBlank()) Text("UAS: " + rid.uasId)
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
    Column(Modifier.fillMaxSize()) {
        Text("NEXVARY Security Suite", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (arabic) {
                "يراقب التطبيق الإعلانات اللاسلكية المتاحة للهاتف ولا يتصل بالأجهزة المكتشفة. تطابق التوقيع مؤشر وليس إثباتًا لهوية الجهاز أو مالكه."
            } else {
                "The app observes radio advertisements available to Android and does not connect to discovered devices. A signature match is an indicator, not proof of device identity or ownership."
            },
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { uri.openUri("https://github.com/nexvary/NEXVARY-Security-Suite") }) {
            Text(if (arabic) "مستودع NEXVARY" else "NEXVARY repository")
        }
        Spacer(Modifier.height(6.dp))
        Button(onClick = { uri.openUri("https://github.com/OffGridPete/Fieldwatch") }) {
            Text(if (arabic) "مشروع Fieldwatch الأصلي" else "Original Fieldwatch project")
        }
        Spacer(Modifier.height(12.dp))
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
