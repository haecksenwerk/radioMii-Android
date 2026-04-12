package com.radiomii.data.player

import com.radiomii.domain.model.IcyMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ICY metadata is parsed in the service because MediaController doesn't reliably forward
// onMetadata() across IPC. The service writes here; PlayerController reads it.
@Singleton
class IcyMetadataHolder @Inject constructor() {
    private val _metadata = MutableStateFlow<IcyMetadata?>(null)
    val metadata: StateFlow<IcyMetadata?> = _metadata.asStateFlow()

    fun update(metadata: IcyMetadata?) {
        _metadata.value = metadata
    }

    fun clear() {
        _metadata.value = null
    }
}
