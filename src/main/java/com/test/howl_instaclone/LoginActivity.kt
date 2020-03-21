package com.test.howl_instaclone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        login_email_btn.setOnClickListener{
            signinAndSignup()
        }
        login_google_btn.setOnClickListener{
            googleLogin()
        }
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // google api key
            .requestEmail() // receive google email
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_LOGIN_CODE) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            // 구글에 이메일 정보 요청 성공 시
            if (result.isSuccess) {
                var account = result.signInAccount
                // Second step
                firebaseAuthWithGoogle(account)
            }
        }
    }

    fun firebaseAuthWithGoogle(account : GoogleSignInAccount?) {
        var credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth?.signInWithCredential(credential)?.addOnCompleteListener{
                task ->
            if (task.isSuccessful) {
                // Login
                moveMainPage(task.result!!.user)
            } else {
                // Show the error message
                Toast.makeText(this, task.exception?.message,
                    Toast.LENGTH_LONG).show()
            }
        }

    }

    fun signinAndSignup() {
        auth?.createUserWithEmailAndPassword(login_email_txt.text.toString(),
            login_password_txt.toString())?.addOnCompleteListener{
            task ->
            if (task.isSuccessful) {
                // Creating a user account
                moveMainPage(task.result!!.user)
            } else if (task.exception?.message.isNullOrEmpty()) {
                // Show the error message
                Toast.makeText(this, task.exception?.message,
                    Toast.LENGTH_LONG).show()

            } else {
                // Login if you have account
                signinEmail()
            }

        }
    }

    fun signinEmail() {
        auth?.signInWithEmailAndPassword(login_email_txt.text.toString(),
            login_password_txt.toString())?.addOnCompleteListener{
                task ->
            if (task.isSuccessful) {
                // Login
                moveMainPage(task.result!!.user)
            } else {
                // Show the error message
                Toast.makeText(this, task.exception?.message,
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    fun moveMainPage(user:FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java));
        }
    }
}
