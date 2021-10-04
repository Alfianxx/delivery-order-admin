package com.alfian.deliveryorderadmin.callback

import com.alfian.deliveryorderadmin.model.ShippingOrderModel

interface ISingleShippingOrderCallbackListener {
    fun onSingleShippingOrderSuccess(shippingOrderModel: ShippingOrderModel)
}