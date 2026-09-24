package com.example.checkpoint.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.checkpoint.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Helper per la gestione dell'autenticazione tramite Google Sign-In con Android CredentialManager API.
 */
class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Avvia il flusso di autenticazione Google e restituisce il token ID se l'operazione ha successo.
     */
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
