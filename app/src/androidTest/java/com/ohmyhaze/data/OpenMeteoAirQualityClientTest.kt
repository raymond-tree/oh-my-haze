package com.ohmyhaze.data

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenMeteoAirQualityClientTest {
    private val checkedAt = Instant.parse("2026-10-10T15:00:00Z")
    private val location = SavedMonitoringLocation(
        label = "Kuala Lumpur",
        latitude = 3.139,
        longitude = 101.6869,
        selectionSource = LocationSelectionSource.BUNDLED_LOCALITY,
        selectedAt = checkedAt,
    )

    @Test
    fun requestUsesSavedCoordinatesAndCurrentUsAqiParameters() {
        val client = OpenMeteoAirQualityClient(nowProvider = { checkedAt })
        val url = client.buildUrl(location)
        val parameters = url.queryParameters()

        assertEquals("https", url.protocol)
        assertEquals("air-quality-api.open-meteo.com", url.host)
        assertEquals("/v1/air-quality", url.path)
        assertEquals("3.139", parameters["latitude"])
        assertEquals("101.6869", parameters["longitude"])
        assertEquals("us_aqi", parameters["current"])
        assertEquals("cams_global", parameters["domains"])
        assertEquals("auto", parameters["timezone"])
        assertEquals("unixtime", parameters["timeformat"])
        assertFalse(parameters.containsKey("hourly"))
        assertFalse(parameters.containsKey("apikey"))
    }

    @Test
    fun parsesUsAqiUnixTimeAndProviderTimezone() {
        val expectedTime = checkedAt.minusSeconds(3_600)
        val response = response(aqi = "155.5", epochSeconds = expectedTime.epochSecond)
        val client = clientReturning(200, response)

        val result = client.fetch(location)

        assertTrue(result is ApiCheckResult.Success)
        val estimate = (result as ApiCheckResult.Success).estimate
        assertEquals(155.5, estimate.sourceUsAqi, 0.0)
        assertEquals(expectedTime, estimate.modelValidAt)
        assertEquals(checkedAt, estimate.retrievedAt)
        assertEquals("Asia/Kuala_Lumpur", estimate.providerTimezone)
    }

    @Test
    fun malformedMissingNullAndNonNumericFieldsAreRejected() {
        val invalidBodies = listOf(
            "not json",
            "{}",
            "{\"timezone\":\"Asia/Kuala_Lumpur\",\"current\":null}",
            "{\"timezone\":\"Asia/Kuala_Lumpur\",\"current\":{\"time\":1}}",
            "{\"timezone\":\"Asia/Kuala_Lumpur\",\"current\":{\"time\":1,\"us_aqi\":null}}",
            "{\"timezone\":\"Asia/Kuala_Lumpur\",\"current\":{\"time\":1,\"us_aqi\":\"155\"}}",
            "{\"current\":{\"time\":1,\"us_aqi\":155}}",
            response(aqi = "-1", epochSeconds = checkedAt.epochSecond),
            response(aqi = "1e309", epochSeconds = checkedAt.epochSecond),
            response(aqi = "155", epochSeconds = checkedAt.epochSecond + 301),
        )

        invalidBodies.forEach { body ->
            val result = clientReturning(200, body).fetch(location)
            assertTrue("Expected invalid response for: $body", result is ApiCheckResult.Failure)
            assertEquals(CheckStatus.INVALID_RESPONSE, (result as ApiCheckResult.Failure).status)
        }
    }

    @Test
    fun validOldDuplicateAndOlderModelTimesRemainSuccessfulRetrievals() {
        val oldTime = checkedAt.minusSeconds(20 * 60 * 60)
        val body = response(aqi = "155", epochSeconds = oldTime.epochSecond)
        val client = clientReturning(200, body)

        val first = client.fetch(location) as ApiCheckResult.Success
        val duplicate = client.fetch(location) as ApiCheckResult.Success

        assertEquals(oldTime, first.estimate.modelValidAt)
        assertEquals(first.estimate.modelValidAt, duplicate.estimate.modelValidAt)
        assertEquals(checkedAt, first.checkedAt)
    }

    @Test
    fun nonSuccessHttpStatusIsReportedWithoutLeakingCoordinates() {
        val result = clientReturning(400, "{\"error\":true,\"reason\":\"invalid coordinates\"}")
            .fetch(location) as ApiCheckResult.Failure

        assertEquals(CheckStatus.HTTP_ERROR, result.status)
        assertFalse(result.safeMessage.orEmpty().contains("3.139"))
        assertFalse(result.safeMessage.orEmpty().contains("101.6869"))
    }

    @Test
    fun ioFailureIsReportedAsNetworkError() {
        val client = OpenMeteoAirQualityClient(
            nowProvider = { checkedAt },
            connectionFactory = { throw IOException("offline") },
        )

        val result = client.fetch(location) as ApiCheckResult.Failure

        assertEquals(CheckStatus.NETWORK_ERROR, result.status)
    }

    private fun clientReturning(status: Int, body: String): OpenMeteoAirQualityClient =
        OpenMeteoAirQualityClient(
            nowProvider = { checkedAt },
            connectionFactory = { url -> FakeConnection(url, status, body) },
        )

    private fun response(aqi: String, epochSeconds: Long): String =
        """{"timezone":"Asia/Kuala_Lumpur","current":{"time":$epochSeconds,"us_aqi":$aqi}}"""

    private fun URL.queryParameters(): Map<String, String> = query.split('&').associate { part ->
        val (key, value) = part.split('=', limit = 2)
        URLDecoder.decode(key, StandardCharsets.UTF_8.name()) to
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }

    private class FakeConnection(
        url: URL,
        private val status: Int,
        private val body: String,
    ) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = status
        override fun getInputStream() = ByteArrayInputStream(body.toByteArray())
        override fun getErrorStream() = ByteArrayInputStream(body.toByteArray())
    }
}
