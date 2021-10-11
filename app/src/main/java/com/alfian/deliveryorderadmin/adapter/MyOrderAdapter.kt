package com.alfian.deliveryorderadmin.adapter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alfian.deliveryorderadmin.R
import com.alfian.deliveryorderadmin.callback.IRecyclerItemClickListener
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.model.CartItem
import com.alfian.deliveryorderadmin.model.OrderModel
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat

class MyOrderAdapter (internal var context: Context,
                      internal var orderList: MutableList<OrderModel>) :
    RecyclerView.Adapter<MyOrderAdapter.MyViewHolder>() {

    private var simpleDateFormat:SimpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")

    class MyViewHolder (itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var txtTime: TextView?=null
        var txtOrderNumber: TextView?=null
        var txtOrderStatus: TextView?=null
        var txtNumItem: TextView?=null
        var txtName: TextView?=null

        var imgItem: ImageView?=null

        private var iRecyclerItemClickListener: IRecyclerItemClickListener?=null

        fun setListener(iRecyclerItemClickListener: IRecyclerItemClickListener)
        {
            this.iRecyclerItemClickListener = iRecyclerItemClickListener
        }

        init {
            imgItem = itemView.findViewById(R.id.img_item_image) as ImageView

            txtTime = itemView.findViewById(R.id.txt_time) as TextView
            txtOrderNumber = itemView.findViewById(R.id.txt_order_number) as TextView
            txtOrderStatus = itemView.findViewById(R.id.txt_order_status) as TextView
            txtNumItem = itemView.findViewById(R.id.txt_num_item) as TextView
            txtName = itemView.findViewById(R.id.txt_name) as TextView

            itemView.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            iRecyclerItemClickListener!!.onItemClick(p0!!,adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_order_item,parent,false))
    }

    override fun getItemCount(): Int {
        return orderList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(orderList[position].cartItemList!![0].itemImage)
            .into(holder.imgItem!!)
        holder.txtOrderNumber!!.text = orderList[position].key
        Common.setSpanStringColor("Order date ",simpleDateFormat.format(orderList[position].createDate),
        holder.txtTime,Color.parseColor("#333639"))

        Common.setSpanStringColor("Order status ",Common.convertStatusToString(orderList[position].orderStatus),
            holder.txtOrderStatus,Color.parseColor("#005758"))

        Common.setSpanStringColor("Num of items ",if (orderList[position].cartItemList == null) "0"
            else orderList[position].cartItemList!!.size.toString(),
            holder.txtNumItem,Color.parseColor("#00574B"))

        Common.setSpanStringColor("Name ",orderList[position].userName,
            holder.txtName,Color.parseColor("#006061"))

        holder.setListener(object:IRecyclerItemClickListener{
            override fun onItemClick(view: View, pos: Int) {
                showDialog(orderList[pos].cartItemList)
            }
        })
    }

    private fun showDialog(cartItemList: List<CartItem>?) {
        val layoutDialog = LayoutInflater.from(context).inflate(R.layout.layout_dialog_order_detail,null)
        val builder = AlertDialog.Builder(context)
        builder.setView(layoutDialog)
        
        val btnOk = layoutDialog.findViewById<View>(R.id.btn_ok) as Button
        val recyclerOrderDetail = layoutDialog.findViewById<View>(R.id.recycler_order_detail) as RecyclerView
        recyclerOrderDetail.setHasFixedSize(true)
        val layoutManger = LinearLayoutManager(context)
        recyclerOrderDetail.layoutManager = layoutManger
        recyclerOrderDetail.addItemDecoration(DividerItemDecoration(context,layoutManger.orientation))
        val adapter = MyOrderDetailAdapter(context,cartItemList!!.toMutableList())
        recyclerOrderDetail.adapter = adapter

        //Show dialog
        val dialog = builder.create()
        dialog.show()
        //custom dialog
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)

        btnOk.setOnClickListener { dialog.dismiss() }
    }

    fun getItemAtPosition(pos: Int): OrderModel {
        return orderList[pos]
    }

    fun removeItem(pos: Int) {
        orderList.removeAt(pos)
    }


}