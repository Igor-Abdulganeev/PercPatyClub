package ru.igorabdulganeev.percpatyclub

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val provider: OAuthProvider.Builder = OAuthProvider.newBuilder("github.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.login_button)

        button.setOnClickListener {

            val result = auth.pendingAuthResult

            if (result == null) {
                auth.startActivityForSignInWithProvider(this, provider.build())
                    .addOnSuccessListener {
                        Log.v(TAG, "start Success ${it.additionalUserInfo?.username}")
                    }
                    .addOnFailureListener {
                        Log.v(TAG, "start Failure ${it.localizedMessage}")
                    }
            } else {
                result
                    .addOnSuccessListener {
                        Log.v(TAG, "result Success ${it.user}")
                    }
                    .addOnFailureListener {
                        Log.v(TAG, "result Failure ${it.localizedMessage}")
                    }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}