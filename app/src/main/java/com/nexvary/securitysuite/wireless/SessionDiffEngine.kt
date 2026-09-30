package com.nexvary.securitysuite.wireless

object SessionDiffEngine {
    fun compare(first: SessionSnapshot, second: SessionSnapshot): SessionDiff {
        val a = first.devices.associateBy { it.stableId }
        val b = second.devices.associateBy { it.stableId }
        return SessionDiff(
            firstId = first.id,
            secondId = second.id,
            added = b.keys.minus(a.keys).mapNotNull(b::get).sortedByDescending { it.strongestRssi },
            removed = a.keys.minus(b.keys).mapNotNull(a::get).sortedByDescending { it.strongestRssi },
            persisted = a.keys.intersect(b.keys).mapNotNull(b::get).sortedByDescending { it.strongestRssi },
        )
    }
}
