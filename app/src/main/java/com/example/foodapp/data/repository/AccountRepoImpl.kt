package com.example.foodapp.data.repository

import android.net.Uri
import com.example.foodapp.data.model.Account
import com.example.foodapp.domain.repository.AccountRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest

import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AccountRepoImpl @Inject constructor(

) : AccountRepository{
    override val currentUser: Flow<Account?>
        get() = callbackFlow {
            val listener =
                FirebaseAuth.AuthStateListener { auth ->
                    this.trySend(auth.currentUser?.toAppUser())
                }
            Firebase.auth.addAuthStateListener(listener)
            awaitClose { Firebase.auth.removeAuthStateListener(listener) }
        }.distinctUntilChanged()



    override val currentUserId: String?
        get() = Firebase.auth.currentUser?.uid

    override suspend fun getUserToken(): String? {
        val currentUser = Firebase.auth.currentUser ?: return null
        return currentUser.getIdToken(false).await().token
    }

    override suspend fun isEmailVerified(): Boolean {
        val user = Firebase.auth.currentUser ?: return false
        try {
            user.reload().await()
            return user.isEmailVerified
        } catch (e: Exception) {

            return false
        }
    }

    override fun sendVerifyEmail() {
        Firebase.auth.currentUser?.sendEmailVerification()
    }



    override fun hasUser(): Boolean {
        return Firebase.auth.currentUser != null
    }

    override fun getUserProfile(): Account {
        return Firebase.auth.currentUser?.toAppUser() ?: Account()
    }

    override suspend fun updatePassword(newPassword: String) {
        Firebase.auth.currentUser?.updatePassword(newPassword)?.await()
    }

    override suspend fun createAccountWithEmail(email: String, password: String) {
        Firebase.auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun updateProfile(name: String) {
        val userProfileChangeRequest = userProfileChangeRequest {
            displayName = name
        }
        Firebase.auth.currentUser?.updateProfile(userProfileChangeRequest)?.await()
    }


    override suspend fun forgetPassword(email: String) {
        Firebase.auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun reAuthenticateWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.currentUser?.reauthenticate(credential)?.await()
    }

    override suspend fun resetPassword(obb: String, newPassword: String) {

        Firebase.auth.confirmPasswordReset(obb, newPassword).await()
    }

    override suspend fun linkAccountWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.currentUser?.linkWithCredential(firebaseCredential)?.await()
    }

    override suspend fun reAuthenticateWithEmail(email: String, password: String) {
        val credential = EmailAuthProvider.getCredential(email, password)
        Firebase.auth.currentUser?.reauthenticate(credential)?.await()
    }

    override fun isGoogleLinked(): Boolean {
        val user = Firebase.auth.currentUser
        return user?.providerData?.any { it.providerId == "google.com" } == true
    }

    override suspend fun linkAccountWithEmail(email: String, password: String) {
        val credential = EmailAuthProvider.getCredential(email, password)
        Firebase.auth.currentUser?.linkWithCredential(credential)?.await()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(firebaseCredential).await()
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signOut() {

        Firebase.auth.signOut()
    }

    override suspend fun deleteAccount() {
        Firebase.auth.currentUser?.delete()?.await()
    }

    override suspend fun getUserRole(): String? {
        val currentUser = Firebase.auth.currentUser ?: return null
        return currentUser.getIdToken(false).await().claims["role"] as String?
    }



    override suspend fun reloadToken() {
        Firebase.auth.currentUser?.getIdToken(true)?.await()
    }
}

fun FirebaseUser.toAppUser(): Account {
    return Account(
        id = this.uid,
        email = this.email ?: "",
        phoneNumber = this.phoneNumber?: "",
        displayName = this.displayName ?: "",
        avatar = this.photoUrl.toString()
    )
}
