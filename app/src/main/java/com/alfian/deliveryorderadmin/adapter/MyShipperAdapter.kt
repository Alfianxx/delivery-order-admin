package com.alfian.deliveryorderadmin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.alfian.deliveryorderadmin.R
import com.alfian.deliveryorderadmin.model.ShipperModel
import com.alfian.deliveryorderadmin.model.eventbus.UpdateActiveEvent
import org.greenrobot.eventbus.EventBus

class MyShipperAdapter (internal var context: Context,
                        private var shipperList: List<ShipperModel>): RecyclerView.Adapter<MyShipperAdapter.MyViewHolder>() {
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var txtName: TextView = itemView.findViewById(R.id.txt_name)
        var txtPhone: TextView = itemView.findViewById(R.id.txt_phone)
        var btnEnable: SwitchCompat = itemView.findViewById(R.id.btn_enable)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_shipper,parent,false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return shipperList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.txtName.text = shipperList[position].name
        holder.txtPhone.text = shipperList[position].phone
        holder.btnEnable.isChecked = shipperList[position].isActive

        //Event
        holder.btnEnable.setOnCheckedChangeListener{ _, b ->
            EventBus.getDefault().postSticky(UpdateActiveEvent(shipperList[position],b))
        }
    }
}