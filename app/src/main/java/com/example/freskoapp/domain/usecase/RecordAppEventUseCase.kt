package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.repository.EventRepository
import java.time.Clock

// App-level events that belong to no product: APP_OPEN and NOTIFICATION_TAPPED.
class RecordAppEventUseCase(private val events: EventRepository, private val clock: Clock = Clock.systemUTC()) {
    suspend operator fun invoke(type: EventType) {
        require(type == EventType.APP_OPEN || type == EventType.NOTIFICATION_TAPPED)
        events.record(ProductEvent(type = type, timestamp = clock.millis()))
    }
}
