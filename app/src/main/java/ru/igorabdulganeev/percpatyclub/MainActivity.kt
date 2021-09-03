package ru.igorabdulganeev.percpatyclub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import ru.igorabdulganeev.percpatyclub.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val auth = Firebase.auth.apply {
        setLanguageCode("ru")
    }
    private val providerGithub: OAuthProvider.Builder = OAuthProvider.newBuilder("github.com")
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var loginName: String

    private lateinit var verificationSms: String
    private val callbackPhone = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            firebaseAuthWithPhone(credential)
        }

        override fun onVerificationFailed(exception: FirebaseException) {
            var message = ""
            when (exception) {
                is FirebaseAuthInvalidCredentialsException -> {
                    message = "неверный номер телефона"
                }
                is FirebaseTooManyRequestsException -> {
                    message = "квота СМС была использована"
                }
            }
            updateUI(null, message)
        }

        override fun onCodeAutoRetrievalTimeOut(message: String) {
            super.onCodeAutoRetrievalTimeOut(message)
            updateUI(null, message)
        }

        override fun onCodeSent(
            verification: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            super.onCodeSent(verification, token)
            verificationSms = verification
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loginName = resources.getString(R.string.default_login_name)
        binding.loginNameTextview.text = loginName

        val googleSignInOptions: GoogleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.id_token))
                .requestEmail()
                .build()
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        binding.loginGithubButton.setOnClickListener {
            updateUI(null, "")
            loginGithub()
        }
        binding.loginGoogleButton.setOnClickListener {
            updateUI(null, "")
            loginGoogle()
        }
        binding.loginPhoneButton.setOnClickListener {
            updateUI(null, "")
            loginPhone(binding.phoneEdittext.text.toString())
        }
        binding.smsCodeButton.setOnClickListener {
            checkSmsCode(binding.smsCodeEdittext.text.toString())
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser, null)
    }

    private fun loginPhone(phoneNumber: String) {
        phoneNumberVerification(phoneNumber)
    }

    private fun phoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder()
            .setPhoneNumber(phoneNumber)
            .setTimeout(120, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbackPhone)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun checkSmsCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationSms, code)
        firebaseAuthWithPhone(credential)
    }

    private fun loginGoogle() {
        signIn()
    }

    private fun loginGithub() {
        val result = auth.pendingAuthResult

        if (result == null) {
            auth.startActivityForSignInWithProvider(this, providerGithub.build())
                .addOnSuccessListener {
                    Log.v(TAG, "start Success ${it.additionalUserInfo?.username}")
                    updateUI(it.user, null)
                }
                .addOnFailureListener {
                    Log.v(TAG, "start Failure ${it.localizedMessage}")
                }
        } else {
            result
                .addOnSuccessListener {
                    Log.v(TAG, "result Success ${it.user}")
                    updateUI(it.user, null)
                }
                .addOnFailureListener {
                    Log.v(TAG, "result Failure ${it.localizedMessage}")
                }
        }

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user, null)
                } else {
                    updateUI(null, "ошибка авторизации")
                }
            }
    }

    private fun firebaseAuthWithPhone(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) {
                if (it.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user, null)
                } else {
                    updateUI(null, it.exception?.localizedMessage)
                }
            }
            .addOnFailureListener {
                updateUI(null, "addOnFailureListener ${it.localizedMessage}")
            }
            .addOnCanceledListener {
                updateUI(null, "addOnCanceledListener")
            }
    }

    private fun updateUI(user: FirebaseUser?, message: String?) {

        binding.loginNameTextview.text =
            user?.displayName ?: user?.phoneNumber ?: message
                    ?: resources.getString(R.string.error_login_name)
    }

    override fun onDestroy() {
        auth.signOut()
        _binding = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val GOOGLE_SIGN_IN = 9001
    }
}