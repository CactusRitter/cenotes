package com.cenotes.app.sync

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * EXPERIMENT (branch experimento-drive): sign-in and the few Drive REST calls the sync needs, all inside the hidden
 * `appDataFolder` (scope `drive.appdata`, nothing else). No Drive SDK: OkHttp + org.json.
 */
object DriveScope {
    const val AppData = "https://www.googleapis.com/auth/drive.appdata"
}

/** What [DriveAuth.authorize] gives back: a token, or a consent screen the UI has to launch. */
sealed interface AuthStep {
    class Token(val accessToken: String) : AuthStep
    class NeedsConsent(val intentSender: IntentSender) : AuthStep
}

object DriveAuth {
    suspend fun authorize(context: Context): AuthStep = suspendCancellableCoroutine { cont ->
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveScope.AppData)))
            .build()
        Identity.getAuthorizationClient(context).authorize(request)
            .addOnSuccessListener { cont.resume(step(it)) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    /** After the consent screen: the result comes back in the activity result's intent. */
    fun fromIntent(context: Context, data: Intent?): AuthStep =
        step(Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data))

    private fun step(r: AuthorizationResult): AuthStep {
        val token = r.accessToken
        if (token != null) return AuthStep.Token(token)
        val pending = r.pendingIntent ?: error("No token and no consent screen")
        return AuthStep.NeedsConsent(pending.intentSender)
    }
}

class DriveFile(val id: String, val name: String, val modifiedTime: String, val size: Long)

class DriveApi(private val token: String, private val http: OkHttpClient = OkHttpClient()) {
    private val files = "https://www.googleapis.com/drive/v3/files"
    private val upload = "https://www.googleapis.com/upload/drive/v3/files"

    private fun Request.Builder.auth() = header("Authorization", "Bearer $token")

    private suspend fun <T> call(request: Request, read: (okhttp3.Response) -> T): T = withContext(Dispatchers.IO) {
        http.newCall(request).execute().use { r ->
            if (!r.isSuccessful) throw DriveException(r.code, r.body.string().take(400))
            read(r)
        }
    }

    suspend fun list(): List<DriveFile> {
        val out = ArrayList<DriveFile>()
        var page: String? = null
        do {
            val url = files.toHttpUrl().newBuilder()
                .addQueryParameter("spaces", "appDataFolder")
                .addQueryParameter("fields", "nextPageToken,files(id,name,modifiedTime,size)")
                .addQueryParameter("pageSize", "1000")
                .apply { page?.let { addQueryParameter("pageToken", it) } }
                .build()
            val json = call(Request.Builder().url(url).auth().build()) { JSONObject(it.body.string()) }
            val arr = json.optJSONArray("files") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val f = arr.getJSONObject(i)
                out += DriveFile(f.getString("id"), f.getString("name"), f.optString("modifiedTime"), f.optString("size", "0").toLong())
            }
            page = json.optString("nextPageToken").ifEmpty { null }
        } while (page != null)
        return out
    }

    /** Creates the file, or replaces its content when [existingId] is given. Returns the file id. */
    suspend fun put(name: String, bytes: ByteArray, existingId: String? = null): String {
        val meta = JSONObject().put("name", name).apply { if (existingId == null) put("parents", JSONArray().put("appDataFolder")) }
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(meta.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(bytes.toRequestBody("application/octet-stream".toMediaType()))
            .build()
        val url = (if (existingId == null) upload else "$upload/$existingId").toHttpUrl().newBuilder()
            .addQueryParameter("uploadType", "multipart").addQueryParameter("fields", "id").build()
        val builder = Request.Builder().url(url).auth()
        val req = (if (existingId == null) builder.post(body) else builder.patch(body)).build()
        return call(req) { JSONObject(it.body.string()).getString("id") }
    }

    suspend fun get(id: String): ByteArray {
        val url = "$files/$id".toHttpUrl().newBuilder().addQueryParameter("alt", "media").build()
        return call(Request.Builder().url(url).auth().build()) { it.body.bytes() }
    }

    suspend fun delete(id: String) {
        call(Request.Builder().url("$files/$id").auth().delete().build()) { }
    }
}

class DriveException(val code: Int, message: String) : Exception("Drive $code: $message")
