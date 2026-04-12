package com.radiomii.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private const val DISCOVERY_URL = "https://all.api.radio-browser.info/json/servers"
private val FALLBACKS = listOf("de1.api.radio-browser.info", "de2.api.radio-browser.info")
private const val CACHE_TTL_MS = 3_600_000L // 1 hour

@Singleton
class ServerDiscovery @Inject constructor(private val okHttpClient: OkHttpClient) {

    @Volatile private var cachedBaseUrl: String? = null
    @Volatile private var cacheTime: Long = 0L

    suspend fun getBaseUrl(): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cached = cachedBaseUrl
        if (cached != null && (now - cacheTime) < CACHE_TTL_MS) return@withContext cached

        val url = try {
            val request = Request.Builder().url(DISCOVERY_URL).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body.string()
            response.close()
            val servers = JSONArray(body)
            if (servers.length() > 0) {
                val idx = Random.nextInt(servers.length())
                val serverName = servers.getJSONObject(idx).optString("name")
                "https://$serverName/"
            } else {
                "https://${FALLBACKS[0]}/"
            }
        } catch (_: Exception) {
            "https://${FALLBACKS[0]}/"
        }

        cachedBaseUrl = url
        cacheTime = now
        url
    }
}
