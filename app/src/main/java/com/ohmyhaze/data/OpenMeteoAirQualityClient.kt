package com.ohmyhaze.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.json.JSONObject

/** Small direct Open-Meteo HTTPS client. Call from a worker's IO dispatcher. */
class OpenMeteoAirQualityClient(
    private val nowProvider: () -> Instant = Instant::now,
    private val connectionFactory: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    },
) {
    fun buildUrl(location: SavedMonitoringLocation): URL {
        val query = listOf(
            "latitude" to location.latitude.toString(),
            "longitude" to location.longitude.toString(),
            "current" to "us_aqi",
            "domains" to "cams_global",
            "timezone" to "auto",
            "timeformat" to "unixtime",
        ).joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return URL("$ENDPOINT?$query")
    }

    fun fetch(location: SavedMonitoringLocation): ApiCheckResult {
        val connection = try {
            connectionFactory(buildUrl(location))
        } catch (_: IOException) {
            return networkFailure()
        }

        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = REQUEST_TIMEOUT_MILLIS
            connection.readTimeout = REQUEST_TIMEOUT_MILLIS
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")

            val status = connection.responseCode
            if (status !in 200..299) {
                ApiCheckResult.Failure(
                    status = CheckStatus.HTTP_ERROR,
                    safeMessage = "Open-Meteo returned HTTP $status.",
                )
            } else {
                val body = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val checkedAt = nowProvider()
                parseSuccess(body, checkedAt)
            }
        } catch (_: IOException) {
            networkFailure()
        } catch (_: Exception) {
            invalidResponse()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSuccess(body: String, checkedAt: Instant): ApiCheckResult {
        return try {
            val root = JSONObject(body)
            val current = root.optJSONObject("current") ?: return invalidResponse()
            val rawAqi = current.opt("us_aqi") as? Number ?: return invalidResponse()
            val sourceAqi = rawAqi.toDouble()
            if (!sourceAqi.isFinite() || sourceAqi < 0.0) return invalidResponse()

            val rawModelTime = current.opt("time") as? Number ?: return invalidResponse()
            val modelSeconds = rawModelTime.toDouble()
            if (!modelSeconds.isFinite() || modelSeconds % 1.0 != 0.0) return invalidResponse()
            if (modelSeconds < Long.MIN_VALUE.toDouble() || modelSeconds >= Long.MAX_VALUE.toDouble()) {
                return invalidResponse()
            }
            val modelValidAt = Instant.ofEpochSecond(modelSeconds.toLong())
            if (modelValidAt.isAfter(checkedAt.plus(MAX_FUTURE_MODEL_AGE))) return invalidResponse()

            val timezone = root.optString("timezone").takeIf { it.isNotBlank() } ?: return invalidResponse()
            ZoneId.of(timezone)
            ApiCheckResult.Success(
                estimate = AirQualityEstimate(
                    sourceUsAqi = sourceAqi,
                    modelValidAt = modelValidAt,
                    retrievedAt = checkedAt,
                    providerTimezone = timezone,
                ),
                checkedAt = checkedAt,
            )
        } catch (_: DateTimeException) {
            invalidResponse()
        } catch (_: Exception) {
            invalidResponse()
        }
    }

    private fun networkFailure() = ApiCheckResult.Failure(
        status = CheckStatus.NETWORK_ERROR,
        safeMessage = "Could not reach Open-Meteo. Try again later.",
    )

    private fun invalidResponse() = ApiCheckResult.Failure(
        status = CheckStatus.INVALID_RESPONSE,
        safeMessage = "Open-Meteo returned an invalid air-quality response.",
    )

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val ENDPOINT = "https://air-quality-api.open-meteo.com/v1/air-quality"
        const val REQUEST_TIMEOUT_MILLIS = 10_000
        val MAX_FUTURE_MODEL_AGE: Duration = Duration.ofMinutes(5)
    }
}
