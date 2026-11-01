package com.example.freskoapp.domain.usecase

import com.example.freskoapp.domain.FakeEventRepository
import com.example.freskoapp.domain.model.EventType
import com.example.freskoapp.domain.model.ProductEvent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RecordAppEventUseCaseTest {
    private val events = FakeEventRepository()
    private val record = RecordAppEventUseCase(events, Clock.fixed(Instant.ofEpochMilli(42), ZoneOffset.UTC))

    @Test
    fun recordsAppOpenAndNotificationTapWithoutProduct() = runTest {
        record(EventType.APP_OPEN)
        record(EventType.NOTIFICATION_TAPPED)
        assertEquals(
            listOf(
                ProductEvent(id = 1, type = EventType.APP_OPEN, timestamp = 42),
                ProductEvent(id = 2, type = EventType.NOTIFICATION_TAPPED, timestamp = 42),
            ),
            events.events.value,
        )
    }

    @Test
    fun productEventsMustGoThroughTheirUseCase() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { record(EventType.UNITS_CONSUMED) } }
    }
}
