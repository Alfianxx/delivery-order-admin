package com.example.kotlineatitv2server.view_holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlineatitv2server.R
import de.hdodenhof.circleimageview.CircleImageView

class ChatPictureViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    var txtTime: TextView?=null
    var txtChatMessage: TextView?=null
    var txtEmail: TextView?=null
    private var profileImage: CircleImageView?=null
    var imgPreview: ImageView?=null

    init {
        txtChatMessage = itemView.findViewById(R.id.txt_chat_message) as TextView
        txtTime = itemView.findViewById(R.id.txt_time) as TextView
        txtEmail = itemView.findViewById(R.id.txt_email) as TextView

        profileImage = itemView.findViewById(R.id.profile_image) as CircleImageView
        imgPreview = itemView.findViewById(R.id.img_preview) as ImageView
    }
}