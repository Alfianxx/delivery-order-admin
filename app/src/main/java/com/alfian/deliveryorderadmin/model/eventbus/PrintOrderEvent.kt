package com.alfian.deliveryorderadmin.model.eventbus

import com.alfian.deliveryorderadmin.model.OrderModel

class PrintOrderEvent(var path:String,var orderModel: OrderModel)