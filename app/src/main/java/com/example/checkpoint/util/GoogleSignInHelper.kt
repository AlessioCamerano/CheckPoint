package com.example.checkpoint.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.example.checkpoint.R

/**
 * Helper per gestire l'autenticazione con Google tramite CredentialManager.
 */
class GoogleSignInHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            when (val credential = result.credential) {
                is GoogleIdTokenCredential -> {
                    credential.idToken
                }
                else -> {
                    // Gestione fallback per il tipo TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            googleIdTokenCredential.idToken
                        } catch (e: Exception) {
                            Log.e("GoogleSignInHelper", "Failed to parse Google ID Token from data", e)
                            null
                        }
                    } else {
                        Log.e("GoogleSignInHelper", "Unexpected credential type: ${credential.type}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Error during sign in: ${e.message}", e)
            null
        }
    }
}
