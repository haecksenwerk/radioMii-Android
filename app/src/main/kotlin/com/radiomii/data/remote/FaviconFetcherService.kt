package com.radiomii.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton



private const val MIN_ICON_SIZE = 32
private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

private object Priority {
    const val OG_IMAGE = 50
    const val APPLE_TOUCH_ICON = 40
    const val APPLE_TOUCH_PROBE = 38
    const val MS_TILE = 35
    const val MANIFEST_ICON = 30
    const val JSON_LD_LOGO = 25
    const val JSON_LD_IMAGE = 24
    const val MICRODATA_LOGO = 25
    const val SVG_ICON = 15
    const val LINK_ICON = 10
    const val THUMBNAIL_META = 8
    const val GOOGLE_FAVICON = 7
    const val FAVICON_ICO = 5
    const val FAVICON_SVG_PROBE = 5
    const val LOGO_HEURISTIC = 2
}

private data class IconCandidate(val url: String, val priority: Int, val size: Int)

// Fetches ranked favicon/icon candidates from a station's homepage.
// Returns up to maxResults validated image URLs, ordered by priority and size.
@Singleton
class FaviconFetcherService @Inject constructor(
    baseOkHttpClient: OkHttpClient,
) {
    // Use a child client with tighter timeouts for favicon probing, sharing the
    // connection pool and cache of the app-wide DI-provided OkHttpClient.
    private val client: OkHttpClient = baseOkHttpClient.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchTopCandidates(
        homepageUrl: String,
        maxResults: Int = 3,
    ): List<String> = withContext(Dispatchers.IO) {
        if (homepageUrl.isBlank()) return@withContext emptyList()
        var origin = runCatching { getOrigin(homepageUrl) }.getOrNull()
            ?: return@withContext emptyList()

        val candidates = mutableListOf<IconCandidate>()
        var html = ""

        // Fetch homepage HTML
        runCatching {
            val request = Request.Builder()
                .url(homepageUrl)
                .header("Accept", "text/html")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            val response = client.newCall(request).execute()
            html = response.body.string()
            // Update origin from effective URL after potential redirects
            val effectiveUrl = response.request.url
            origin = "${effectiveUrl.scheme}://${effectiveUrl.host}" +
                if (effectiveUrl.port != -1 && !isDefaultPort(effectiveUrl.scheme, effectiveUrl.port))
                    ":${effectiveUrl.port}" else ""
            response.close()

            candidates.addAll(extractIconCandidates(html, origin))

            val manifestUrl = extractManifestUrl(html, origin)
            if (manifestUrl != null) {
                candidates.addAll(fetchManifestIcons(manifestUrl))
            }
        }

        // Probe well-known icon paths in parallel
        candidates.addAll(
            probeHeadUrls(
                listOf(
                    IconCandidate("$origin/favicon.ico", Priority.FAVICON_ICO, 0),
                    IconCandidate("$origin/apple-touch-icon.png", Priority.APPLE_TOUCH_PROBE, 180),
                    IconCandidate("$origin/favicon.svg", Priority.FAVICON_SVG_PROBE, 0),
                )
            )
        )

        // Heuristic logo scan (last resort)
        if (html.isNotBlank()) {
            candidates.addAll(extractLogoImgHeuristic(html, origin))
        }

        // External APIs fallback
        if (candidates.isEmpty()) {
            candidates.addAll(fetchFromExternalApis(origin))
        }

        if (candidates.isEmpty()) return@withContext emptyList()

        val deduplicated = deduplicateCandidates(candidates)
            .sortedWith(compareByDescending<IconCandidate> { it.priority }.thenByDescending { it.size })

        // Validate top candidates and collect up to maxResults valid ones
        val validated = mutableListOf<String>()
        for (candidate in deduplicated.take(10)) {
            if (validated.size >= maxResults) break
            if (validateImageUrl(candidate.url)) {
                validated.add(candidate.url)
            }
        }
        // Fallback: return top unvalidated pick if nothing validated
        if (validated.isEmpty() && deduplicated.isNotEmpty()) {
            return@withContext listOf(deduplicated.first().url)
        }
        validated
    }


    private fun getOrigin(url: String): String {
        val parsed = URL(url)
        val port = parsed.port
        return if (port == -1 || isDefaultPort(parsed.protocol, port)) {
            "${parsed.protocol}://${parsed.host}"
        } else {
            "${parsed.protocol}://${parsed.host}:$port"
        }
    }

    private fun isDefaultPort(scheme: String, port: Int): Boolean =
        (scheme == "http" && port == 80) || (scheme == "https" && port == 443)

    private fun resolveUrl(href: String?, origin: String): String? {
        if (href.isNullOrBlank()) return null
        return when {
            href.startsWith("http://") || href.startsWith("https://") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> "$origin$href"
            else -> "$origin/$href"
        }
    }

    private fun decodeHtmlEntities(str: String): String =
        str.replace("&amp;", "&", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'")
            .replace("&#x2F;", "/")


    private fun extractAttr(tag: String, attr: String, origin: String): String? {
        val m = Regex("""$attr\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(tag)
        return m?.groupValues?.get(1)?.trim()
            ?.let { resolveUrl(decodeHtmlEntities(it), origin) }
    }

    private fun extractSize(tag: String): Int {
        val m = Regex("""sizes\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(tag)
            ?: return 0
        var max = 0
        Regex("""(\d+)x\d+""", RegexOption.IGNORE_CASE).findAll(m.groupValues[1]).forEach { match ->
            val n = match.groupValues[1].toIntOrNull() ?: 0
            if (n > max) max = n
        }
        return max
    }


    private fun extractMetaCandidates(
        html: String,
        origin: String,
        attrName: String,
        attrValue: String,
        priority: Int,
        size: Int = 0,
    ): List<IconCandidate> {
        val candidates = mutableListOf<IconCandidate>()
        val regex = Regex(
            """<meta\s+[^>]*$attrName\s*=\s*["']$attrValue["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        for (match in regex.findAll(html)) {
            val url = extractAttr(match.value, "content", origin)
            if (url != null) candidates.add(IconCandidate(url, priority, size))
        }
        return candidates
    }

    private fun extractAppleTouchIconCandidates(
        html: String,
        origin: String,
    ): List<IconCandidate> {
        val candidates = mutableListOf<IconCandidate>()
        val regex = Regex(
            """<link\s+[^>]*rel\s*=\s*["']apple-touch-icon(?:[- ]precomposed)?["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        for (match in regex.findAll(html)) {
            val url = extractAttr(match.value, "href", origin) ?: continue
            val size = extractSize(match.value)
            if (size > 0 && size < MIN_ICON_SIZE) continue
            candidates.add(IconCandidate(url, Priority.APPLE_TOUCH_ICON, size))
        }
        return candidates
    }


    private fun extractIconCandidates(html: String, origin: String): List<IconCandidate> {
        val candidates = mutableListOf<IconCandidate>()

        // <meta> tags: og:image, thumbnail, msapplication-TileImage
        candidates.addAll(extractMetaCandidates(html, origin, "property", "og:image", Priority.OG_IMAGE))
        candidates.addAll(extractMetaCandidates(html, origin, "name", "thumbnail", Priority.THUMBNAIL_META))
        candidates.addAll(extractMetaCandidates(html, origin, "name", "msapplication-TileImage", Priority.MS_TILE, 150))

        // <link rel="apple-touch-icon"> with size filtering
        candidates.addAll(extractAppleTouchIconCandidates(html, origin))

        // <link rel="icon"> / shortcut icon — SVG gets higher priority
        val linkIconRegex = Regex(
            """<link\s+[^>]*rel\s*=\s*["'](?:shortcut\s+)?icon["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        for (match in linkIconRegex.findAll(html)) {
            val url = extractAttr(match.value, "href", origin) ?: continue
            val size = extractSize(match.value)
            if (size > 0 && size < MIN_ICON_SIZE) continue
            val isSvg = Regex(
                """type\s*=\s*["']image/svg\+xml["']""", RegexOption.IGNORE_CASE
            ).containsMatchIn(match.value) || url.lowercase().endsWith(".svg")
            candidates.add(
                IconCandidate(url, if (isSvg) Priority.SVG_ICON else Priority.LINK_ICON, size)
            )
        }

        // Schema.org microdata itemprop="logo" on <link> and <img>
        for (match in Regex(
            """<link\s+[^>]*itemprop\s*=\s*["']logo["'][^>]*>""", RegexOption.IGNORE_CASE
        ).findAll(html)) {
            val url = extractAttr(match.value, "href", origin)
            if (url != null) candidates.add(IconCandidate(url, Priority.MICRODATA_LOGO, 0))
        }
        for (match in Regex(
            """<img\s+[^>]*itemprop\s*=\s*["']logo["'][^>]*>""", RegexOption.IGNORE_CASE
        ).findAll(html)) {
            val url = extractAttr(match.value, "src", origin)
            if (url != null) candidates.add(IconCandidate(url, Priority.MICRODATA_LOGO, 0))
        }

        // JSON-LD logos
        candidates.addAll(extractJsonLdLogos(html, origin))

        return candidates
    }


    private fun extractManifestUrl(html: String, origin: String): String? {
        val match = Regex(
            """<link\s+[^>]*rel\s*=\s*["']manifest["'][^>]*>""", RegexOption.IGNORE_CASE
        ).find(html) ?: return null
        return extractAttr(match.value, "href", origin)
    }

    private fun fetchManifestIcons(manifestUrl: String): List<IconCandidate> {
        val candidates = mutableListOf<IconCandidate>()
        return runCatching {
            val request = Request.Builder()
                .url(manifestUrl)
                .header("User-Agent", USER_AGENT)
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body.string()
            response.close()
            val manifestOrigin = runCatching { getOrigin(manifestUrl) }.getOrNull()
                ?: return candidates
            val manifest = JSONObject(body)
            val icons = manifest.optJSONArray("icons") ?: return candidates

            for (i in 0 until icons.length()) {
                val icon = icons.optJSONObject(i) ?: continue
                val src = icon.optString("src").takeIf { it.isNotBlank() } ?: continue
                val url = resolveUrl(src, manifestOrigin) ?: continue
                if (icon.optString("purpose").trim().lowercase() == "maskable") continue
                val sizesStr = icon.optString("sizes")
                var size = 0
                Regex("""(\d+)x\d+""", RegexOption.IGNORE_CASE).findAll(sizesStr).forEach { m ->
                    val n = m.groupValues[1].toIntOrNull() ?: 0
                    if (n > size) size = n
                }
                if (size > 0 && size < MIN_ICON_SIZE) continue
                candidates.add(IconCandidate(url, Priority.MANIFEST_ICON, size))
            }
            candidates
        }.getOrDefault(candidates)
    }


    private fun extractJsonLdLogos(html: String, origin: String): List<IconCandidate> {
        val candidates = mutableListOf<IconCandidate>()
        val scriptRegex = Regex(
            """<script\s+[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>([\s\S]*?)</script>""",
            RegexOption.IGNORE_CASE
        )
        for (match in scriptRegex.findAll(html)) {
            runCatching {
                val content = match.groupValues[1]
                val items = mutableListOf<JSONObject>()
                runCatching {
                    val arr = JSONArray(content)
                    for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { items.add(it) }
                }.onFailure {
                    val obj = JSONObject(content)
                    val graph = obj.optJSONArray("@graph")
                    if (graph != null) {
                        for (i in 0 until graph.length()) graph.optJSONObject(i)?.let { items.add(it) }
                    } else {
                        items.add(obj)
                    }
                }

                val relevantTypes = setOf(
                    "organization", "radiostation", "radiobroadcastservice",
                    "broadcastservice", "website", "localbusiness",
                    "newsmediaorganization", "webpage"
                )
                for (item in items) {
                    val types: List<String> = when (val typeVal = item.opt("@type")) {
                        is JSONArray -> (0 until typeVal.length()).mapNotNull { typeVal.optString(it) }
                        is String -> listOf(typeVal)
                        else -> emptyList()
                    }
                    if (types.none { it.lowercase() in relevantTypes }) continue
                    val logoUrl = extractJsonLdField(item, "logo", origin)
                    if (logoUrl != null) {
                        candidates.add(IconCandidate(logoUrl, Priority.JSON_LD_LOGO, 0))
                    } else {
                        val imageUrl = extractJsonLdField(item, "image", origin)
                        if (imageUrl != null) {
                            candidates.add(IconCandidate(imageUrl, Priority.JSON_LD_IMAGE, 0))
                        }
                    }
                }
            }
        }
        return candidates
    }

    private fun extractJsonLdField(item: JSONObject, field: String, origin: String): String? {
        val value = item.opt(field) ?: return null
        return when {
            value is String -> resolveUrl(value, origin)
            value is JSONArray && value.length() > 0 -> {
                val first = value.opt(0)
                when {
                    first is String -> resolveUrl(first, origin)
                    first is JSONObject -> resolveUrl(first.optString("url"), origin)
                    else -> null
                }
            }
            value is JSONObject -> resolveUrl(value.optString("url"), origin)
            else -> null
        }
    }


    private fun extractLogoImgHeuristic(html: String, origin: String): List<IconCandidate> {
        val candidates = mutableListOf<IconCandidate>()
        val imgRegex = Regex("""<img\s[^>]+>""", RegexOption.IGNORE_CASE)
        for (match in imgRegex.findAll(html)) {
            val t = match.value
            val isLogo = listOf("class", "id", "alt").any { attr ->
                Regex("""$attr\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                    .find(t)?.groupValues?.get(1)?.contains("logo", ignoreCase = true) == true
            }
            if (!isLogo) continue
            val w = Regex("""width\s*=\s*["']?(\d+)""", RegexOption.IGNORE_CASE)
                .find(t)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val h = Regex("""height\s*=\s*["']?(\d+)""", RegexOption.IGNORE_CASE)
                .find(t)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            if ((w > 0 && w < MIN_ICON_SIZE) || (h > 0 && h < MIN_ICON_SIZE)) continue
            val url = extractAttr(t, "src", origin) ?: continue
            candidates.add(IconCandidate(url, Priority.LOGO_HEURISTIC, maxOf(w, h)))
        }

        // Also check <img> inside a home-link <a href="/">
        val escapedOrigin = Regex.escape(origin)
        val homeLinkRegex = Regex(
            """<a\s[^>]*href\s*=\s*["'](?:/|$escapedOrigin)["'][^>]*>[\s\S]*?<img\s[^>]+>""",
            RegexOption.IGNORE_CASE
        )
        for (match in homeLinkRegex.findAll(html)) {
            val imgTag = Regex("""<img\s[^>]+>""", RegexOption.IGNORE_CASE)
                .find(match.value) ?: continue
            val url = extractAttr(imgTag.value, "src", origin) ?: continue
            if (candidates.none { it.url == url }) {
                candidates.add(IconCandidate(url, Priority.LOGO_HEURISTIC, 0))
            }
        }
        return candidates
    }


    private suspend fun probeHeadUrls(
        probes: List<IconCandidate>,
        minContentLength: Int = 0,
    ): List<IconCandidate> = supervisorScope {
        probes.map { probe ->
            async {
                runCatching {
                    val request = Request.Builder()
                        .url(probe.url)
                        .header("User-Agent", USER_AGENT)
                        .head()
                        .build()
                    val response = client.newCall(request).execute()
                    val ct = response.header("Content-Type") ?: ""
                    val cl = response.header("Content-Length")?.toIntOrNull() ?: 0
                    val code = response.code
                    response.close()
                    if (code != 200) return@runCatching null
                    if (!ct.contains("image") && !ct.contains("svg")) return@runCatching null
                    if (minContentLength > 0 && cl <= minContentLength) return@runCatching null
                    probe
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun fetchFromExternalApis(origin: String): List<IconCandidate> {
        val domain = runCatching { URL(origin).host }.getOrNull() ?: return emptyList()
        return probeHeadUrls(
            listOf(
                IconCandidate(
                    "https://www.google.com/s2/favicons?domain=$domain&sz=128",
                    Priority.GOOGLE_FAVICON,
                    128
                ),
                IconCandidate(
                    "https://icons.duckduckgo.com/ip3/$domain.ico",
                    Priority.GOOGLE_FAVICON,
                    0
                ),
            ),
            minContentLength = 200
        )
    }


    private fun validateImageUrl(url: String): Boolean {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .head()
                .build()
            val response = client.newCall(request).execute()
            val ct = response.header("Content-Type") ?: ""
            val code = response.code
            response.close()
            code in 200..399 && (ct.contains("image") || ct.contains("svg") || url.endsWith(".ico"))
        }.getOrDefault(false)
    }


    private fun deduplicateCandidates(candidates: List<IconCandidate>): List<IconCandidate> {
        val map = mutableMapOf<String, IconCandidate>()
        for (c in candidates) {
            val existing = map[c.url]
            if (existing == null
                || c.priority > existing.priority
                || (c.priority == existing.priority && c.size > existing.size)
            ) {
                map[c.url] = c
            }
        }
        return map.values.toList()
    }
}

