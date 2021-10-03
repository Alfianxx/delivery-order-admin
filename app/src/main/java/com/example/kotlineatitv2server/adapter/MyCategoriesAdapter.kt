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
import com.example.kotlineatitv2server.model.CategoryModel
import com.example.kotlineatitv2server.model.eventbus.CategoryClick
import org.greenrobot.eventbus.EventBus

class MyCategoriesAdapter (internal var context: Context,
                           internal var categoriesList: List<CategoryModel>) :
    RecyclerView.Adapter<MyCategoriesAdapter.MyViewHolder>()  {
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(categoriesList[position].image).into(holder.categoryImage!!)
        holder.categoryName!!.text = categoriesList[position].name

        //Event
        holder.setListener(object: IRecyclerItemClickListener {
            override fun onItemClick(view: View, pos: Int) {
                Common.categorySelected = categoriesList[pos]
                EventBus.getDefault().postSticky(CategoryClick(true, categoriesList[pos]))
            }

        })
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyCategoriesAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_category_item,parent,false))

    }

    override fun getItemCount(): Int {
        return categoriesList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(categoriesList.size == 1)
            Common.DEFAULT_COLUMN_COUNT
        else{
            if(categoriesList.size % 2 == 0 )
                Common.DEFAULT_COLUMN_COUNT
            else
                if (position > 1 && position == categoriesList.size-1) Common.FULL_WIDTH_COLUMN else Common.DEFAULT_COLUMN_COUNT
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!,adapterPosition)
        }

        var categoryName: TextView?=null

        var categoryImage: ImageView?=null

        private var listener:IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener)
        {
            this.listener = listener
        }

        init{
            categoryName = itemView.findViewById(R.id.category_name) as TextView
            categoryImage = itemView.findViewById(R.id.category_image) as ImageView
            itemView.setOnClickListener(this)
        }

    }
}