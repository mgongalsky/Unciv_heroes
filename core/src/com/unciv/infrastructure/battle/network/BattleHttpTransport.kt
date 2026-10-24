package com.unciv.infrastructure.battle.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

interface BattleHttpTransport {
    suspend fun request(method: String, path: String, body: String? = null, token: String? = null): String
}

/** An HTTP failure is distinct from an uncertain delivery caused by an I/O exception. */
class BattleHttpException(val status: Int, val responseBody: String) : IOException("Battle server returned HTTP $status")

/** No implicit retry or redirect: a lost command response must be reconciled through its receipt. */
class UrlBattleHttpTransport(
    baseUrl: String,
    private val connectTimeoutMillis: Int = 5000,
    private val readTimeoutMillis: Int = 10000,
    private val maximumResponseBytes: Int = 2 * 1024 * 1024
) : BattleHttpTransport {
    private val base = URI(baseUrl.trim().trimEnd('/'))

    init {
        require(base.scheme in setOf("http", "https") && base.host != null) { "Use an HTTP or HTTPS server address" }
        require(base.rawUserInfo == null && base.rawQuery == null && base.rawFragment == null) { "Server address must not contain credentials, a query or fragment" }
        require(base.rawPath.isNullOrEmpty() || base.rawPath == "/") { "Server address must contain only scheme, host and port" }
        require(connectTimeoutMillis > 0 && readTimeoutMillis > 0 && maximumResponseBytes > 0)
    }

    override suspend fun request(method: String, path: String, body: String?, token: String?): String =
        withContext(Dispatchers.IO) {
            require(method == "GET" || method == "POST")
            require(path.matches(Regex("/v1/[A-Za-z0-9_/-]+")) && !path.contains("//"))
            require(token == null || token.matches(Regex("[A-Za-z0-9_-]{43}")))
            val connection = base.resolve(path).toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.instanceFollowRedirects = false
                connection.connectTimeout = connectTimeoutMillis
                connection.readTimeout = readTimeoutMillis
                connection.useCaches = false
                connection.setRequestProperty("Accept", "application/json")
                if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    require(method == "POST")
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.setFixedLengthStreamingMode(bytes.size)
                    connection.outputStream.use { it.write(bytes) }
                }
                val status = connection.responseCode
                val stream = if (status >= 400) connection.errorStream else connection.inputStream
                val response = if (stream == null) "" else stream.use {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = it.read(buffer)
                        if (count < 0) break
                        if (count > maximumResponseBytes - output.size()) throw IOException("Battle server response is too large")
                        output.write(buffer, 0, count)
                    }
                    output.toString("UTF-8")
                }
                if (status !in 200..299) throw BattleHttpException(status, response)
                val contentType = connection.contentType?.substringBefore(';')?.trim()
                if (!contentType.equals("application/json", ignoreCase = true)) throw IOException("Battle server did not return JSON")
                response
            } finally {
                connection.disconnect()
            }
        }
}
