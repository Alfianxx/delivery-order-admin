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
import com.example.kotlineatitv2server.model.AddonModel
import com.example.kotlineatitv2server.model.CartItem
import com.example.kotlineatitv2server.model.SizeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyOrderDetailAdapter(internal var context: Context, private var cartItemList:MutableList<CartItem>):RecyclerView.Adapter<MyOrderDetailAdapter.MyViewHolder>(){

    private val gson:Gson = Gson()

    class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)
    {
        var txtFoodName:TextView = itemView.findViewById(R.id.txt_food_name)
        var txtFoodSize:TextView = itemView.findViewById(R.id.txt_size)
        var txtFoodAddon:TextView = itemView.findViewById(R.id.txt_food_add_on)
        var txtFoodQuantity:TextView = itemView.findViewById(R.id.txt_food_quantity)
        var imgFoodImage:ImageView = itemView.findViewById(R.id.img_food_image)
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
        holder.txtFoodQuantity.text = StringBuilder("Quantity:  ").append(cartItemList[position].foodQuantity)

        //Fix crash
        if (cartItemList[position].foodSize
                .equals("Default"))
            holder.txtFoodSize.text = StringBuilder("Size: Default")
        else{
            val sizeModel = gson.fromJson<SizeModel>(cartItemList[position].foodSize,SizeModel::class.java)
            holder.txtFoodSize.text = StringBuilder("Size: ").append(sizeModel.name)
        }

        if (!cartItemList[position].foodAddon.equals("Default"))
        {
            val addonModels : List<AddonModel> = gson.fromJson(cartItemList[position].foodAddon,
            object:TypeToken<List<AddonModel?>?>(){}.type)
            val addonString = StringBuilder()

            for(addonModel in addonModels) addonString.append(addonModel.name).append(",")
            addonString.delete(addonString.length-1,addonString.length) //Remove last ","
            holder.txtFoodAddon.text = StringBuilder("Addon: ").append(addonString)
        }
        else
        {
            holder.txtFoodAddon.text = StringBuilder("Addon: Default")
        }
    }

}