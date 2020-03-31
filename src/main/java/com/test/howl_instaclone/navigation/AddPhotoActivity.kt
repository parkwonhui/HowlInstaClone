package com.test.howl_instaclone.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.test.howl_instaclone.R
import com.test.howl_instaclone.navigation.model.ContentDTO
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage ? = null
    var photoUri : Uri?= null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore ? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        Log.d("TEST_LOG", "AddPhotoActivity start")
        // Initiate
        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        // Open the album
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)

        // add image upload event
        addphoto_btn_upload.setOnClickListener{
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            if (resultCode == Activity.RESULT_OK) {
                // This is path to the selected image
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)
            } else {
                // Exit the addPhotoActivity if you leave the album without selecting it
                finish()

            }
        }
    }

    fun contentUpload() {
        Log.d("TEST_LOG", "contentUpload()");
        // Make filename
        var timestamp  = SimpleDateFormat("yyyMMdd_HHmmsss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        Log.d("TEST_LOG", "timestamp:"+timestamp)
        Log.d("TEST_LOG", "imageFileName:"+imageFileName)
        Log.d("TEST_LOG", "storageRef:"+storageRef)

        // 업로드 방식 2가지 callback or promise
        // Promise method 구글에서 권한하는 방식
        storageRef?.putFile(photoUri!!)?.continueWithTask { task : Task<UploadTask.TaskSnapshot> ->
            Log.d("TEST_LOG", "storageRef.downloadUrl:"+storageRef.downloadUrl)
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()
                // Insert downloadUrl of image
                contentDTO.imageUrl = uri.toString()
                Log.d("TEST_LOG", "contentDTO.imageUrl:"+contentDTO.imageUrl )

            // Insert uid of user
                contentDTO.uid = auth?.currentUser?.uid
                Log.d("TEST_LOG", "contentDTO.uid :"+contentDTO.uid )

            // Insert UserId
                contentDTO.userId = auth?.currentUser?.email
            Log.d("TEST_LOG", "contentDTO.uid :"+contentDTO.uid )

            // Insert explain of content
                contentDTO.explain = addphoto_edit_explain.toString()
            Log.d("TEST_LOG", "contentDTO.explain:"+contentDTO.explain)

                // Insert timestamp
                contentDTO.timestamp = System.currentTimeMillis()
            Log.d("TEST_LOG", "contentDTO.timestamp:"+contentDTO.timestamp)


            firestore?.collection("images")?.document()?.set(contentDTO)

                // 정상적으로 닫혔다는 의미
                setResult(Activity.RESULT_OK)

                finish()
        }

        // Callback method
        /*storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            /*Toast.makeText(this, getString(R.string.upload_success
            ), Toast.LENGTH_LONG).show()*/
            storageRef.downloadUrl.addOnSuccessListener {
                uri -> var contentDTO = ContentDTO()

                // Insert downloadUrl of image
                contentDTO.imageUrl = uri.toString()

                // Insert uid of user
                contentDTO.uid = auth?.currentUser?.uid

                // Insert UserId
                contentDTO.userId = auth?.currentUser?.email

                // Insert explain of content
                contentDTO.explain = addphoto_edit_explain.toString()

                // Insert timestamp
                contentDTO.timestamp = System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)

                // 정상적으로 닫혔다는 의미
                setResult(Activity.RESULT_OK)

                finish()
            }
        }*/
    }
}
