package com.alfian.deliveryorderadmin.model

class ShippingOrderModel {
    var key:String?=null
    var shipperPhone:String?=null
    var shipperName:String?=null
    var currentLat= 0.0
    var currentLng= 0.0
    var orderModel:OrderModel?=null
    var isStartTrip=false
    var shopKey:String?=null
}