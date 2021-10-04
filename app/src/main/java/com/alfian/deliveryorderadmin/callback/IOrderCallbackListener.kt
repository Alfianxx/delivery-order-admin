package com.alfian.deliveryorderadmin.callback

import com.alfian.deliveryorderadmin.model.OrderModel


interface IOrderCallbackListener {
    fun onOrderLoadSuccess(orderModel :List<OrderModel>)
    fun onOrderLoadFailed(message:String)
}