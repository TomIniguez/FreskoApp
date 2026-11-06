package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.ProductEvent
import com.example.freskoapp.domain.repository.EventRepository
import java.time.Clock
import kotlin.coroutines.cancellation.CancellationException

// App-level events that belong to no product: APP_OPEN and NOTIFICATION_TAPPED.
class RecordAppEventUseCase(private val events: EventRepository, private val clock: Clock = Clock.systemUTC()) {
    suspend operator fun invoke(type: EventType) {
        require(type == EventType.APP_OPEN || type == EventType.NOTIFICATION_TAPPED)
        try {
            events.record(ProductEvent(type = type, timestamp = clock.millis()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best effort: it runs at startup outside any screen, so a database that can't be written must not
            // crash the app. Losing one APP_OPEN is better than the inventory never showing its error state.
        }
    }
}
