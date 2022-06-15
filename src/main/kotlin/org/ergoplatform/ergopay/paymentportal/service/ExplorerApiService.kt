package org.ergoplatform.ergopay.paymentportal.service

import okhttp3.OkHttpClient
import org.ergoplatform.appkit.RestApiErgoClient
import org.ergoplatform.explorer.client.DefaultApi
import org.springframework.stereotype.Service
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

@Service
class ExplorerApiService(private val okHttpClient: OkHttpClient) {
    val timeout = 30L // 30 seconds since Explorer can be slooooow

    private val api by lazy {
        buildExplorerApi(RestApiErgoClient.defaultMainnetExplorerUrl)
    }

    private fun buildExplorerApi(url: String) = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            okHttpClient.newBuilder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS).build()
        )
        .build()
        .create(DefaultApi::class.java)

    private fun <T> wrapCall(call: () -> Call<T>): T {
        val explorerCall = call().execute()

        if (!explorerCall.isSuccessful)
            throw IOException("Error calling Explorer: ${explorerCall.errorBody()}")

        return explorerCall.body()!!
    }

    fun getTransactionInfo(txId: String) =
        wrapCall {
            api.getApiV1TransactionsP1(txId)
        }

    fun getBoxInformation(boxId: String) =
        wrapCall {
            api.getApiV1BoxesP1(boxId)
        }
}
