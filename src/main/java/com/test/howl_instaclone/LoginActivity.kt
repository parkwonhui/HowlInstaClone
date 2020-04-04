package com.test.howl_instaclone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class LoginActivity : AppCompatActivity() {
    var auth : FirebaseAuth? = null
    var googleSignInClient : GoogleSignInClient? = null
    var GOOGLE_LOGIN_CODE = 9001
    var callbackManager : CallbackManager? = null
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
        login_facebook_btn.setOnClickListener{
            facebookLogin()
        }
        // Specifies that email info is requested by your application. Note that we don't recommend keying user by email address since email address might change.
        // Keying user by ID is the preferable approach.
        var gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // google api key
            .requestEmail() // receive google email
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        //printHashKey()
        callbackManager = CallbackManager.Factory.create()
    }

    override fun onStart() {
        super.onStart()
        // 자동 로그인 되도록 만듬
        moveMainPage(auth?.currentUser)
    }

    fun printHashKey() {
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = String(Base64.encode(md.digest(), 0))
                Log.i("TEST_LOG", "printHashKey() Hash Key: $hashKey")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("TEST_LOG", "printHashKey()", e)
        } catch (e: Exception) {
            Log.e("TEST_LOG", "printHashKey()", e)
        }

    }

    fun googleLogin() {
        var signInIntent = googleSignInClient?.signInIntent
        startActivityForResult(signInIntent, GOOGLE_LOGIN_CODE)
    }

    fun facebookLogin() {
        // request facebook authority
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))

        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                Log.d("TEST_LOG", "onSuccess");
                handleFacebookAccessToken(result?.accessToken)
            }

            override fun onCancel() {
                Log.d("TEST_LOG", "onCancel")
            }

            override fun onError(error: FacebookException?) {
                Log.d("TEST_LOG", "onError"+error?.message)
                Log.d("TEST_LOG", "onError"+error?.toString())
            }

        });
    }

    fun handleFacebookAccessToken(token : AccessToken?) {
        var credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth?.signInWithCredential(credential)?.addOnCompleteListener{
                task ->
            if (task.isSuccessful) {
                // Login
                Log.d("TEST_LOG", "SUCCESS")
                moveMainPage(task.result!!.user)
            } else {
                Log.d("TEST_LOG", "ERROR")
                // Show the error message
                Toast.makeText(this, task.exception?.message,
                    Toast.LENGTH_LONG).show()
            }
        }    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, requestCode, data)

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
            finish()
        }
    }
}
