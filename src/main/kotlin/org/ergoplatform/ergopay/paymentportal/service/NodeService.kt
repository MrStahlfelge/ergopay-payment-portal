package org.ergoplatform.ergopay.paymentportal.service

import okhttp3.OkHttpClient
import org.ergoplatform.appkit.ErgoClient
import org.ergoplatform.appkit.NetworkType
import org.ergoplatform.appkit.RestApiErgoClient
import org.ergoplatform.restapi.client.ErgoTransactionOutput
import org.ergoplatform.restapi.client.UtxoApi
import org.springframework.stereotype.Service
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Service
class NodeService(
    private val okHttpClient: OkHttpClient
) {
    val nodeApiUrl = "http://213.239.193.208:9053/" // change this to your own node

    fun getErgoClient(): ErgoClient {
        return RestApiErgoClient.createWithHttpClientBuilder(
            nodeApiUrl,
            NetworkType.MAINNET,
            "",
            RestApiErgoClient.defaultMainnetExplorerUrl,
            okHttpClient.newBuilder()
        )
    }

    fun getBoxDataById(boxId: String): ErgoTransactionOutput? {
        return try {
            val utxoApi = getNodeRetrofit().create(UtxoApi::class.java)
            val apiCall = utxoApi.getBoxById(boxId).execute()

            apiCall.body()
        } catch (t: Throwable) {
            return null
        }
    }

    private fun getNodeRetrofit() = Retrofit.Builder()
        .baseUrl(nodeApiUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient.newBuilder().connectTimeout(5, TimeUnit.SECONDS).build())
        .build()
}