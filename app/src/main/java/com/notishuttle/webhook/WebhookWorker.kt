package com.notishuttle.webhook

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One worker instance per notification. WorkManager persists the request and
 * retries with exponential backoff, so deliveries survive process death and reboot.
 */
class WebhookWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val payload = inputData.getString(KEY_PAYLOAD) ?: return Result.failure()
        val secret = inputData.getString(KEY_SECRET) ?: ""
        val headers = inputData.getString(KEY_HEADERS) ?: ""

        return try {
            val ok = withContext(Dispatchers.IO) {
                WebhookSender.send(url, payload, secret, headers)
            }
            if (ok) Result.success() else Result.retry()
        } catch (t: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_URL = "url"
        const val KEY_PAYLOAD = "payload"
        const val KEY_SECRET = "secret"
        const val KEY_HEADERS = "headers"
        const val TAG = "notishuttle_delivery"
    }
}
