package com.example.expensecalculator.Authentication

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val auth = FirebaseAuth.getInstance()


    var name by mutableStateOf("")
    fun onNameChange(v:String){name=v}

    val isLoggedIn get() = auth.currentUser != null

    fun onEmailChange(v: String) { email = v }
    fun onPasswordChange(v: String) { password = v }

    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                onSuccess()
            } catch (e: Exception) {
                val msg = "Login failed: ${e.message}"
                errorMessage = msg
                onError(msg)
            } finally {
                isLoading = false
            }
        }
    }
    fun register(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: return@launch

                // Update Firebase display name
                val profileUpdate = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                result.user?.updateProfile(profileUpdate)?.await()

                // Save to Firestore
                saveUserProfile(uid, name, email)
                onSuccess()
            } catch (e: Exception) {
                onError("Registration failed: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun forgotPassword(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                auth.sendPasswordResetEmail(email).await()
                onSuccess()
            } catch (e: Exception) {
                onError("Failed: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: return@launch

                // Save profile only if new user
                if (result.additionalUserInfo?.isNewUser == true) {
                    saveUserProfile(
                        uid = user.uid,
                        name = user.displayName ?: "User",
                        email = user.email ?: ""
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                onError("Google sign-in failed: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun getGoogleSignInClient(context: Context) =
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("1087169199906-elmu6f6qdlut9lnbs5dl22j25h2fifj3.apps.googleusercontent.com") // from google-services.json
                .requestEmail()
                .build()
        )

    private suspend fun saveUserProfile(uid: String, name: String, email: String) {
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email
        )
        db.collection("users").document(uid).set(user).await()
    }
}