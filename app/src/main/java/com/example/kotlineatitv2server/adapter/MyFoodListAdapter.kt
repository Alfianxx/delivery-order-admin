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
import com.example.kotlineatitv2server.callback.IRecyclerItemClickListener
import com.example.kotlineatitv2server.common.Common
import com.example.kotlineatitv2server.model.FoodModel

class MyFoodListAdapter (internal var context: Context,
                         internal var foodList: List<FoodModel>) :
    RecyclerView.Adapter<MyFoodListAdapter.MyViewHolder>()  {



    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(foodList[position].image).into(holder.imgFoodImage!!)
        holder.txtFoodName!!.text = foodList[position].name
        holder.txtFoodPrice!!.text = StringBuilder("$").append(foodList[position].price.toString())

        //Event
        holder.setListener(object: IRecyclerItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                Common.foodSelected = foodList[pos]
                Common.foodSelected!!.key = pos.toString()

            }

        })


    }



    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyFoodListAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_food_item,parent,false))

    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    fun getItemAtPosition(pos: Int): FoodModel {
        return foodList[pos]
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        var txtFoodName: TextView?=null
        var txtFoodPrice: TextView?=null

        var imgFoodImage: ImageView?=null

        private var listener: IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener)
        {
            this.listener = listener
        }

        init{
            txtFoodName = itemView.findViewById(R.id.txt_food_name) as TextView
            txtFoodPrice = itemView.findViewById(R.id.txt_food_price) as TextView
            imgFoodImage = itemView.findViewById(R.id.img_food_image) as ImageView


            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!,adapterPosition)
        }

    }
}

