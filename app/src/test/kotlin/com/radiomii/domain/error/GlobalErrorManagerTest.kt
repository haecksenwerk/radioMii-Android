package com.radiomii.domain.error

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// Unit tests for GlobalErrorManager event emission via SharedFlow.
class GlobalErrorManagerTest {

    @Test
    fun `emitError delivers event to collector`() = runTest {
        val manager = GlobalErrorManager()
        manager.errorEvent.test {
            manager.emitError(GlobalError.NO_INTERNET)
            assertEquals(GlobalError.NO_INTERNET, awaitItem())
        }
    }

    @Test
    fun `emitError delivers SERVER_UNREACHABLE`() = runTest {
        val manager = GlobalErrorManager()
        manager.errorEvent.test {
            manager.emitError(GlobalError.SERVER_UNREACHABLE)
            assertEquals(GlobalError.SERVER_UNREACHABLE, awaitItem())
        }
    }

    @Test
    fun `emitError delivers PLAYBACK_ERROR`() = runTest {
        val manager = GlobalErrorManager()
        manager.errorEvent.test {
            manager.emitError(GlobalError.PLAYBACK_ERROR)
            assertEquals(GlobalError.PLAYBACK_ERROR, awaitItem())
        }
    }

    @Test
    fun `multiple errors are all delivered in order`() = runTest {
        val manager = GlobalErrorManager()
        manager.errorEvent.test {
            manager.emitError(GlobalError.NO_INTERNET)
            manager.emitError(GlobalError.SERVER_UNREACHABLE)
            manager.emitError(GlobalError.PLAYBACK_ERROR)

            assertEquals(GlobalError.NO_INTERNET, awaitItem())
            assertEquals(GlobalError.SERVER_UNREACHABLE, awaitItem())
            assertEquals(GlobalError.PLAYBACK_ERROR, awaitItem())
        }
    }

    @Test
    fun `rapid emissions within buffer capacity are not dropped`() = runTest {
        val manager = GlobalErrorManager()
        manager.errorEvent.test {
            // Emit 4 errors rapidly (== extraBufferCapacity) with an active collector
            manager.emitError(GlobalError.NO_INTERNET)
            manager.emitError(GlobalError.SERVER_UNREACHABLE)
            manager.emitError(GlobalError.PLAYBACK_ERROR)
            manager.emitError(GlobalError.NO_INTERNET)

            assertEquals(GlobalError.NO_INTERNET, awaitItem())
            assertEquals(GlobalError.SERVER_UNREACHABLE, awaitItem())
            assertEquals(GlobalError.PLAYBACK_ERROR, awaitItem())
            assertEquals(GlobalError.NO_INTERNET, awaitItem())
        }
    }

    @Test
    fun `all GlobalError values can be emitted and received`() = runTest {
        val manager = GlobalErrorManager()
        manager.errorEvent.test {
            GlobalError.entries.forEach { error ->
                manager.emitError(error)
                assertEquals(error, awaitItem())
            }
        }
    }
}


