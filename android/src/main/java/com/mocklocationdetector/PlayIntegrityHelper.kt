package com.mocklocationdetector

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.facebook.react.bridge.Promise
import java.util.UUID

object PlayIntegrityHelper {

    fun requestIntegrityToken(
        context: Context,
        cloudProjectNumber: Long,
        promise: Promise
    ) {
        try {
            val integrityManager = IntegrityManagerFactory.create(context)
            val nonce = UUID.randomUUID().toString().replace("-", "")

            val request = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(cloudProjectNumber)
                .setNonce(nonce)
                .build()

            integrityManager.requestIntegrityToken(request)
                .addOnSuccessListener { response ->
                    val token = response.token()
                    promise.resolve(token)
                }
                .addOnFailureListener { exception ->
                    promise.reject("INTEGRITY_REQUEST_FAILED", exception.message, exception)
                }
        } catch (t: Throwable) {
            promise.reject("INTEGRITY_UNAVAILABLE", t.message, t)
        }
    }
}
