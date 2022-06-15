package org.ergoplatform.ergopay.paymentportal

import okhttp3.OkHttpClient
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PaymentportalApplication{
	private val okHttpClient = OkHttpClient()

	@Bean
	fun getOkHttpClient(): OkHttpClient {
		// OKHttpCliebnt should be used as a Singleton - this is Spring's way to do this
		return okHttpClient
	}
}

fun main(args: Array<String>) {
	runApplication<PaymentportalApplication>(*args)
}
