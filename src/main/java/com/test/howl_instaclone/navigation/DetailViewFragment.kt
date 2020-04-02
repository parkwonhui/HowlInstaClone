package com.test.howl_instaclone.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.util.Util
import com.google.firebase.firestore.FirebaseFirestore
import com.test.howl_instaclone.R
import com.test.howl_instaclone.navigation.model.ContentDTO
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {
    var firestore : FirebaseFirestore?= null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("TEST_LOG", "DetailViewFragment onCreateView call")
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)

        view.detailviewfragment_recyclerview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var contentDTOs : ArrayList<ContentDTO> = arrayListOf()
        var contentUidList : ArrayList<String> = arrayListOf()
        init {
            firestore?.collection("images")?.orderBy("timestamp")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                Log.d("TEST_LOG", "DetailViewRecyclerViewAdapter init")
                // 스냅샷 찍기
                contentDTOs.clear()
                contentUidList.clear()
                // snapshot에 데이터를 순서대로 읽기
                for (snapshot in querySnapshot!!.documents) {
                    Log.d("TEST_LOG", "snapshot 데이터 읽기!!!")
                    var item = snapshot.toObject(ContentDTO::class.java)
                    contentDTOs.add(item!!)
                    contentUidList.add(snapshot.id)
                }
                // 값 새로 고침
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view);
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }
        // 서버에서 받아온 데이터 매핑
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as CustomViewHolder).itemView
            Log.d("test_log","데이터 받아왔나?"+viewHolder.detailviewitem_profile_textview.text);

            // UserId
            viewHolder.detailviewitem_profile_textview.text = contentDTOs!![position].userId
            // Image
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewHolder.detailviewitem_imageview_content)
            // Explain of content
            viewHolder.detailviewitem_explain_textview.text = contentDTOs!![position].explain
            // likes
            viewHolder.detailviewitem_favoritecounter_textview.text = "Likes" + contentDTOs!![position].favoriteCount
            // ProfileImage
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl).into(viewHolder.detailviewitem_profile_image)
        }
    }

}