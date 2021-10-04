package com.example.kotlineatitv2server.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlineatitv2server.R
import com.example.kotlineatitv2server.model.CartItem
import com.google.gson.Gson

class MyOrderDetailAdapter(internal var context: Context, private var cartItemList:MutableList<CartItem>):RecyclerView.Adapter<MyOrderDetailAdapter.MyViewHolder>(){

    private val gson:Gson = Gson()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)
    {
        var txtFoodName:TextView = itemView.findViewById(R.id.txt_food_name)

        var txtFoodQuantity:TextView = itemView.findViewById(R.id.txt_food_quantity)
        var imgFoodImage:ImageView = itemView.findViewById(R.id.img_food_image)
        var txtFoodPrice:TextView = itemView.findViewById(R.id.txt_food_price_detail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.layout_order_detail_item,parent,false))
    }

    override fun getItemCount(): Int {
        return cartItemList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(cartItemList[position].foodImage)
            .into(holder.imgFoodImage)
        holder.txtFoodName.text = StringBuilder().append(cartItemList[position].foodName)
        holder.txtFoodQuantity.text = StringBuilder("Jumlah:  ").append(cartItemList[position].foodQuantity)

        holder.txtFoodPrice.text = StringBuilder("Harga:  ").append(cartItemList[position].foodPrice)

    }

}