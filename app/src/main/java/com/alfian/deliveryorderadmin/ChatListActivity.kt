package com.alfian.deliveryorderadmin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alfian.deliveryorderadmin.callback.IRecyclerItemClickListener
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.model.ChatInfoModel
import com.alfian.deliveryorderadmin.view_holder.ChatListViewHolder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import kotlinx.android.synthetic.main.activity_chat_list.*

class ChatListActivity : AppCompatActivity() {

    lateinit var database:FirebaseDatabase
    private lateinit var chatRef:DatabaseReference

    lateinit var adapter: FirebaseRecyclerAdapter<ChatInfoModel, ChatListViewHolder>
    lateinit var options: FirebaseRecyclerOptions<ChatInfoModel>

    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)
        initViews()
        loadChatList()
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    override fun onStop() {
        adapter.stopListening()
        super.onStop()
    }

    private fun loadChatList() {
        adapter = object:FirebaseRecyclerAdapter<ChatInfoModel,ChatListViewHolder>(options){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
                return ChatListViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.layout_message_list_item,parent,false))
            }

            override fun onBindViewHolder(
                holder: ChatListViewHolder,
                position: Int,
                model: ChatInfoModel
            ) {
                holder.txtEmail.text = model.createName
                holder.txtChatMessage.text = model.lastMessage

                holder.setListener(object : IRecyclerItemClickListener {
                    override fun onItemClick(view: View, pos: Int) {
                        //code late
                        //Toast.makeText(this@ChatListActivity,model.lastMessage,Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ChatListActivity,
                            ChatDetailActivity::class.java)
                        intent.putExtra(Common.KEY_CHAT_ROOM_ID,adapter.getRef(pos).key)
                        intent.putExtra(Common.KEY_CHAT_SENDER,model.createName)
                        startActivity(intent)
                    }

                })
            }

        }
        recycler_chat_list.adapter = adapter
    }

    private fun initViews() {
        database = FirebaseDatabase.getInstance()
        chatRef = database.getReference(Common.SHOP_REF)
            .child(Common.currentServerUser!!.shop!!)
            .child(Common.CHAT_REF)

        val query:Query = chatRef
        options = FirebaseRecyclerOptions.Builder<ChatInfoModel>()
            .setQuery(query,ChatInfoModel::class.java)
            .build()

        layoutManager = LinearLayoutManager(this)
        recycler_chat_list.layoutManager = layoutManager
        recycler_chat_list.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))

        toolbar.title = Common.currentServerUser!!.shop!!
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }
}