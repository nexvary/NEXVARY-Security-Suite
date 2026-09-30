package com.nexvary.securitysuite.wireless

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object ObservationBus {
    private val _events = MutableSharedFlow<RadioObservation>(extraBufferCapacity = 512)
    val events = _events.asSharedFlow()

    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    fun emit(observation: RadioObservation) {
        _events.tryEmit(observation)
    }

    fun setActive(value: Boolean) {
        _active.value = value
    }
}
