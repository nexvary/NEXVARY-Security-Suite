package com.nexvary.securitysuite.wireless

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {
    fun shareSession(context: Context, session: SessionSnapshot) {
        val reports = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reports, session.id + ".csv")
        file.writeText(toCsv(session))
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".files",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/csv")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Share NEXVARY session report"))
    }

    fun toCsv(session: SessionSnapshot): String = buildString {
        appendLine("NEXVARY Security Suite Wireless Watch")
        appendLine("session_id," + csv(session.id))
        appendLine("started," + csv(format(session.startedAtMs)))
        appendLine("ended," + csv(format(session.endedAtMs)))
        appendLine("stable_id,source,name,address,category,label,severity,first_seen,last_seen,strongest_rssi,observations")
        session.devices.forEach { d ->
            appendLine(
                listOf(
                    d.stableId,
                    d.source.name,
                    d.name.orEmpty(),
                    d.address.orEmpty(),
                    d.category?.name.orEmpty(),
                    d.label.orEmpty(),
                    d.severity.name,
                    format(d.firstSeenMs),
                    format(d.lastSeenMs),
                    d.strongestRssi.toString(),
                    d.observations.toString(),
                ).joinToString(",") { csv(it) },
            )
        }
    }

    private fun format(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(ms))

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}
