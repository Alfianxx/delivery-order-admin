package com.alfian.deliveryorderadmin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alfian.deliveryorderadmin.R
import com.alfian.deliveryorderadmin.callback.IRecyclerItemClickListener
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.model.ItemModel
import com.bumptech.glide.Glide

class MyItemListAdapter (internal var context: Context,
                         internal var itemList: List<ItemModel>) :
    RecyclerView.Adapter<MyItemListAdapter.MyViewHolder>()  {

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(itemList[position].image).into(holder.imgItemImage!!)
        holder.txtItemName!!.text = itemList[position].name
        holder.txtItemPrice!!.text = StringBuilder("Rp ").append(itemList[position].price.toString())

        //Event
        holder.setListener(object: IRecyclerItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                Common.itemSelected = itemList[pos]
                Common.itemSelected!!.key = pos.toString()

            }

        })


    }



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyItemListAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_item,parent,false))

    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun getItemAtPosition(pos: Int): ItemModel {
        return itemList[pos]
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        var txtItemName: TextView?=null
        var txtItemPrice: TextView?=null

        var imgItemImage: ImageView?=null

        private var listener: IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener)
        {
            this.listener = listener
        }

        init{
            txtItemName = itemView.findViewById(R.id.txt_item_name) as TextView
            txtItemPrice = itemView.findViewById(R.id.txt_item_price) as TextView
            imgItemImage = itemView.findViewById(R.id.img_item_image) as ImageView


            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!,adapterPosition)
        }

    }
}

