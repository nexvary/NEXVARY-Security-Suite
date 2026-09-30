package com.nexvary.securitysuite.wireless

import android.content.Context

class WatchStore(context: Context) {
    private val prefs = context.getSharedPreferences("wireless_watch", Context.MODE_PRIVATE)

    fun watchTerms(): List<String> =
        prefs.getStringSet("watch_terms", emptySet()).orEmpty().sorted()

    fun saveWatchTerms(values: Collection<String>) {
        prefs.edit()
            .putStringSet("watch_terms", values.map { it.trim() }.filter { it.isNotBlank() }.toSet())
            .apply()
    }

    fun customSignatures(): List<CustomSignature> =
        prefs.getStringSet("custom_signatures", emptySet()).orEmpty().mapNotNull { raw ->
            val parts = raw.split("|||", limit = 3)
            if (parts.size < 2) return@mapNotNull null
            val source = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
                ?.let { runCatching { RadioSource.valueOf(it) }.getOrNull() }
            CustomSignature(parts[0], parts[1], source)
        }.sortedBy { it.label.lowercase() }

    fun saveCustomSignatures(values: Collection<CustomSignature>) {
        val encoded = values.map {
            it.label + "|||" + it.keyword + "|||" + (it.source?.name ?: "")
        }.toSet()
        prefs.edit().putStringSet("custom_signatures", encoded).apply()
    }
}
