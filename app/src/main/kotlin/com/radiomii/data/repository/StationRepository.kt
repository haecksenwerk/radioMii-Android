package com.radiomii.data.repository

import com.radiomii.data.remote.RadioBrowserService
import com.radiomii.data.remote.ServerDiscovery
import com.radiomii.data.remote.dto.toDomain
import com.radiomii.domain.model.Country
import com.radiomii.domain.model.SearchOptions
import com.radiomii.domain.model.SearchMode
import com.radiomii.domain.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 50

@Singleton
class StationRepository @Inject constructor(
    private val serverDiscovery: ServerDiscovery,
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {

    private var service: RadioBrowserService? = null
    private var serviceBaseUrl: String = ""

    private var initialCache: Pair<SearchRequestKey, List<Station>>? = null

    private data class SearchRequestKey(
        val searchTerm: String,
        val options: SearchOptions
    )

    private suspend fun getService(): RadioBrowserService {
        val baseUrl = serverDiscovery.getBaseUrl()
        if (service == null || baseUrl != serviceBaseUrl) {
            serviceBaseUrl = baseUrl
            service = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(
                    okHttpClient.newBuilder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                )
                .addConverterFactory(
                    json.asConverterFactory("application/json; charset=UTF8".toMediaType())
                )
                .build()
                .create(RadioBrowserService::class.java)
        }
        return checkNotNull(service)
    }

    suspend fun searchStations(
        offset: Int,
        searchTerm: String,
        options: SearchOptions,
    ): List<Station> = withContext(Dispatchers.IO) {
        if (offset == 0) {
            val key = SearchRequestKey(searchTerm, options)
            val cached = initialCache
            if (cached != null && cached.first == key) {
                initialCache = null // Consume once
                return@withContext cached.second
            }
        }

        val svc = getService()
        val term = searchTerm.trim().lowercase()
        val name = if (options.searchMode == SearchMode.NAME && term.isNotEmpty()) term else null
        val tag = if (options.searchMode == SearchMode.TAG && term.isNotEmpty()) term else null
        val countryCode = options.country.ifBlank { null }
        val isHttps = when {
            options.isHttpsOnly -> "true"
            else -> null
        }
        val result = svc.searchStations(
            name = name,
            tag = tag,
            countrycode = countryCode,
            order = options.sortOrder.apiValue,
            reverse = options.reverse,
            hidebroken = options.hidebroken,
            isHttps = isHttps,
            bitrateMin = options.bitrateMin,
            limit = PAGE_SIZE,
            offset = offset,
        ).map { it.toDomain() }

        if (offset == 0) {
            initialCache = SearchRequestKey(searchTerm, options) to result
        }

        result
    }


    suspend fun getCountries(): List<Country> = withContext(Dispatchers.IO) {
        getService().getCountries()
            .filter { it.name.isNotBlank() }
            .map { it.toDomain() }
            .sortedBy { it.name }
    }


    suspend fun vote(uuid: String): Result<com.radiomii.data.remote.dto.VoteResultDto> =
        withContext(Dispatchers.IO) {
            runCatching { getService().voteForStation(uuid) }
        }
}
