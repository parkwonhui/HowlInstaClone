package com.test.howl_instaclone.navigation

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.test.howl_instaclone.LoginActivity
import com.test.howl_instaclone.MainActivity
import com.test.howl_instaclone.R
import com.test.howl_instaclone.navigation.model.ContentDTO
import com.test.howl_instaclone.navigation.model.FollowDTO
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    var fragmentView : View?= null
    var firestore : FirebaseFirestore? = null
    var uid : String?= null
    var auth : FirebaseAuth? = null
    var currentUserUid : String ?= null
    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        Log.d("TEST_LOG", "!!! userId:"+arguments?.getString("userId"))


        if (uid == currentUserUid) {
            // MyPage
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.signout)
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
        } else {
            // Other User Page
            fragmentView?.account_btn_follow_signout?.text = getString(R.string.follow)
            var mainActivity = (activity as MainActivity)
            mainActivity?.toolbar_username?.text = arguments?.getString("userId")
            mainActivity?.toolbar_btn_back?.setOnClickListener {
                mainActivity.bottom_navigation.selectedItemId = R.id.action_home
            }
            mainActivity?.toolbar_title_image?.visibility = View.GONE
            mainActivity?.toolbar_username?.visibility = View.VISIBLE
            mainActivity?.toolbar_btn_back?.visibility = View.VISIBLE
            // Follow 버튼에 이벤트 추가
            fragmentView?.account_btn_follow_signout?.setOnClickListener {
                Log.d("TEST_LOG", "click follow btn")
                requestFollow()
            }
        }

        fragmentView?.account_recyclerView?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.account_recyclerView?.layoutManager = GridLayoutManager(activity!!,3)

        fragmentView?.account_iv_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        getProfileImage()
        getFollowerAndFollowing()
        return fragmentView
    }

    // 화면에 카운터가 변화됨
    fun getFollowerAndFollowing() {
        // 내 페이지를 클릭 했을 땐 내uid, 상대방 페이지 클릭 시 상대방 페이지
        // snapshot으로 값을 실시간으로 불러옴
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener {
            documentSnapshot, firebaseFirestoreException ->
            // followDTO로 documentSnapshot 받아오기
            var followDTO = documentSnapshot?.toObject(FollowDTO::class.java)
            // following count에 값 출력
            if (followDTO?.followingCount != null) {
                fragmentView?.account_tv_following_count?.text = followDTO?.followingCount?.toString()

            }
            // follow count도 출력
            if (followDTO?.followerCount != null) {
                fragmentView?.account_tv_following_count?.text = followDTO?.followerCount?.toString()
                // follow를 하고 있으면 button이 변함
                if (followDTO?.followers?.containsKey(currentUserUid!!)) {
                    fragmentView?.account_btn_follow_signout?.text =getString(R.string.follow_cancel)
                    fragmentView?.account_btn_follow_signout?.background?.colorFilter =
                        PorterDuffColorFilter(Color.parseColor("#0fff0000"), PorterDuff.Mode.SRC_OVER)
                } else {
                    fragmentView?.account_btn_follow_signout?.text =getString(R.string.follow)
                    // 상대방 유저 fragment일 때 백그라운드 컬러를 없앤다
                    if (uid != currentUserUid) {
                        fragmentView?.account_btn_follow_signout?.background?.colorFilter = null
                    }
                }
            }

        }
    }


    fun requestFollow() {
        // Save data my account  나의 계정에서 상대가 누구를 follow하는지
        // db에 값이 없을 경우 만들어준다
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)

        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            Log.d("TEST_LOG", "followDTO == null:"+(followDTO == null))
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                // 중복 팔로잉 방지를 위해 상대방 uid를 넣어줌
                followDTO!!.followers[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }
            Log.d("TEST_LOG", "followDTO.followings.containsKey(uid):"+(followDTO.followings.containsKey(uid)))

            // 상대방 키가 있을 경우(내가 팔로우 한 상태)
            if (followDTO.followings.containsKey(uid)) {
                // It remove following third person when a third person follow me
                followDTO?.followingCount = followDTO?.followingCount - 1
                // 상대 uid 제거
                followDTO?.followers?.remove(uid)
            } else {
                // It add following third person when a third person follow me
                followDTO?.followingCount = followDTO?.followingCount + 1
                // 상대방 uid 추가
                followDTO.followers[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            // transaction 닫아주기
            return@runTransaction
        }
        // Save data to third person, 내가 팔로잉 할 상대방 계정에 접근
        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            // 값이 없을 경우
            Log.d("TEST_LOG","followDTO == null:"+(followDTO == null))
            Log.d("TEST_LOG", "followDTO!!.followers.containsKey(currentUserUid):"+(followDTO!!.followers.containsKey(currentUserUid)))
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                // 상대방에게 나의 uid 넣어주기
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            // 상대방 계정에 내가 follow를 했을 경우
            if (followDTO!!.followers.containsKey(currentUserUid)) {
                // 팔로우 취소
                followDTO?.followerCount = followDTO!!.followerCount - 1
                // 나의 uid를 입력
                followDTO?.followers?.remove(currentUserUid!!)
            } else {
                // 팔로우를 하지 않았을 경우
                // It add my follower when I follow a third person do not
                followDTO?.followerCount = followDTO!!.followerCount + 1
                // 나의 uid 추가
                followDTO!!.followers[currentUserUid!!] = true

            }
            // DB 값 저장
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    // 올린 프로필 이미지를 다운로드
    fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener{
            documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null) return@addSnapshotListener
            if (documentSnapshot.data != null) {
                var url = documentSnapshot?.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.account_iv_profile!!)
            }
        }

    }



    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                //Sometimes, This code return null of querySnapshot when it signout
                if (querySnapshot == null) {
                    return@addSnapshotListener
                }

                // Get data
                for (snapshot in querySnapshot.documents) {
                    contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                }
                fragmentView?.account_tv_post_count?.text = contentDTOs.size.toString()
                // 리사이클뷰 새로고침
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3
            var imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView : ImageView) : RecyclerView.ViewHolder(imageView) {
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl).apply(RequestOptions().centerCrop()).into(imageView)
        }
    }

}