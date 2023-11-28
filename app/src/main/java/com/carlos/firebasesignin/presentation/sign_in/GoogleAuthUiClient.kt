package com.carlos.firebasesignin.presentation.sign_in

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.carlos.firebasesignin.utils.Constants.Server_Client_Id
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class GoogleAuthUiClient(
    private val context: Context,
    private val oneTapClient: SignInClient
) {

    private val auth = Firebase.auth

    suspend fun signIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            null
        }

        return result?.pendingIntent?.intentSender
    }

    suspend fun signInWithIntent(intent: Intent): SignInResult {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        return try {
            val user = auth.signInWithCredential(googleCredentials).await().user
            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        username = displayName!!,
                        profilePictureUrl = photoUrl?.toString()

                    )
                },
                errorMessage = null
            )
        } catch (e: Exception){
            if (e is CancellationException) throw e
            e.message?.let {
                SignInResult(
                    data = null,
                    errorMessage = it
                )
            }
        }!!
    }

    suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
        }
    }

    fun getSignedInUser(): UserData? = auth.currentUser?.run {
        UserData(
            userId = uid,
            username = displayName!!,
            profilePictureUrl = photoUrl?.toString()

        )
    }

    private fun buildSignInRequest(): BeginSignInRequest {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.builder()
                    // Verifica se a autenticação é verdadeira
                    .setSupported(true)
                    // Código para verificiar se sempre quer mostrar a lsita de usuários.
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(Server_Client_Id)
                    .build()
            )
            // Caso tenha apenas uma conta, fara o login automatico.
            .setAutoSelectEnabled(true)
            .build()

    }


}