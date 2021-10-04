package com.alfian.deliveryorderadmin.callback

import android.widget.Button
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import com.alfian.deliveryorderadmin.model.OrderModel
import com.alfian.deliveryorderadmin.model.ShipperModel

interface IShipperLoadCallbackListener {
    fun onShipperLoadSuccess(shipperList:List<ShipperModel>)
    fun onShipperLoadSuccess(pos:Int, orderModel: OrderModel?,
                             shipperList: List<ShipperModel>?,
                             dialog: AlertDialog?,
                             ok:Button?, cancel:Button?,
                             rdi_shipping:RadioButton?,
                             rdi_shipped:RadioButton?,
                             rdi_cancelled:RadioButton?,
                             rdi_delete:RadioButton?,
                             rdi_restore_placed:RadioButton?)
    fun onShipperLoadFailed(message:String)
}