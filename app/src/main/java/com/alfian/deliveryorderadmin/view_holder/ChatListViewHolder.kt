package com.alfian.deliveryorderadmin.view_holder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alfian.deliveryorderadmin.R
import com.alfian.deliveryorderadmin.callback.IRecyclerItemClickListener

class ChatListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var txtEmail:TextView = itemView.findViewById(R.id.txt_email) as TextView
    var txtChatMessage:TextView = itemView.findViewById(R.id.txt_chat_message) as TextView

    private var listener: IRecyclerItemClickListener?=null
    fun setListener(listener: IRecyclerItemClickListener)
    {
        this.listener = listener
    }

    init {
        itemView.setOnClickListener { view -> listener!!.onItemClick(view,adapterPosition) }
    }
}