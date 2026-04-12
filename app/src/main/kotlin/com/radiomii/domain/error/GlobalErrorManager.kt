package com.radiomii.domain.error
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
enum class GlobalError {
    NO_INTERNET,
    SERVER_UNREACHABLE,
    PLAYBACK_ERROR,
}
@Singleton
class GlobalErrorManager @Inject constructor() {
    private val _errorEvent = MutableSharedFlow<GlobalError>(extraBufferCapacity = 4)
    val errorEvent = _errorEvent.asSharedFlow()
    fun emitError(error: GlobalError) {
        _errorEvent.tryEmit(error)
    }
}
