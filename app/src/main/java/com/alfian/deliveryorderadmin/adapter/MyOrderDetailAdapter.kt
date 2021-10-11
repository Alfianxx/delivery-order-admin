package com.alfian.deliveryorderadmin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alfian.deliveryorderadmin.R
import com.alfian.deliveryorderadmin.model.CartItem
import com.bumptech.glide.Glide
import com.google.gson.Gson

class MyOrderDetailAdapter(internal var context: Context, private var cartItemList:MutableList<CartItem>):RecyclerView.Adapter<MyOrderDetailAdapter.MyViewHolder>(){

    private val gson:Gson = Gson()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)
    {
        var txtItemName:TextView = itemView.findViewById(R.id.txt_item_name)

        var txtItemQuantity:TextView = itemView.findViewById(R.id.txt_item_quantity)
        var imgItem:ImageView = itemView.findViewById(R.id.img_item_image)
        var txtItemPrice:TextView = itemView.findViewById(R.id.txt_item_price_detail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.layout_order_detail_item,parent,false))
    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(cartItemList[position].itemImage)
            .into(holder.imgItem)
        holder.txtItemName.text = StringBuilder().append(cartItemList[position].itemName)
        holder.txtItemQuantity.text = StringBuilder("Jumlah:  ").append(cartItemList[position].itemQuantity)

        holder.txtItemPrice.text = StringBuilder("Harga:  ").append(cartItemList[position].itemPrice)

    }

}