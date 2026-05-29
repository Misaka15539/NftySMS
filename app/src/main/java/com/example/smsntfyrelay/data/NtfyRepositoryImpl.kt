package com.example.smsntfyrelay.data

import com.example.smsntfyrelay.domain.AuthConfig
import com.example.smsntfyrelay.domain.RelayResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// ---------------------------------------------------------------------------
// NtfyRepositoryImpl — OkHttp 4-backed implementation of NtfyRepository
// ---------------------------------------------------------------------------

@Singleton
class NtfyRepositoryImpl @Inject constructor() : NtfyRepository {

    // ---------------------------------------------------------------------------
    // OkHttpClient instances — one per SSL-validation setting to avoid rebuilding
    // ---------------------------------------------------------------------------

    /** Standard client with full TLS certificate validation (default). */
    private val validatingClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Trust-all client used only when the user has explicitly disabled SSL
     * validation in settings. Accepts any certificate and skips hostname
     * verification.
     *
     * WARNING: This client must only be constructed and used when the user has
     * explicitly opted in via the settings toggle. It is intentionally insecure.
     */
    private val trustAllClient: OkHttpClient by lazy {
        val trustAllManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
        }

        val noOpHostnameVerifier = HostnameVerifier { _, _ -> true }

        OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllManager)
            .hostnameVerifier(noOpHostnameVerifier)
            .build()
    }

    // ---------------------------------------------------------------------------
    // post — build and execute the HTTP request on Dispatchers.IO
    // ---------------------------------------------------------------------------

    override suspend fun post(
        topicUrl: String,
        title: String,
        body: String,
        auth: AuthConfig?,
        sslValidation: Boolean,
    ): RelayResult = withContext(Dispatchers.IO) {
        val client = if (sslValidation) validatingClient else trustAllClient

        val requestBody = body.toRequestBody("text/plain".toMediaType())

        val requestBuilder = Request.Builder()
            .url(topicUrl)
            .post(requestBody)
            .header("Content-Type", "text/plain")
            .header("Title", title)

        // Add Authorization header based on AuthConfig type
        when (auth) {
            is AuthConfig.Bearer -> requestBuilder.header("Authorization", "Bearer ${auth.token}")
            is AuthConfig.Basic  -> requestBuilder.header(
                "Authorization",
                Credentials.basic(auth.username, auth.password),
            )
            is AuthConfig.None, null -> { /* omit Authorization header */ }
        }

        val request = requestBuilder.build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                if (code in 200..299) {
                    RelayResult.Success(code)
                } else {
                    RelayResult.HttpError(code)
                }
            }
        } catch (_: SocketTimeoutException) {
            RelayResult.Timeout
        } catch (_: IOException) {
            RelayResult.Timeout
        }
    }
}
