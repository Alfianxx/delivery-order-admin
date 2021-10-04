package com.alfian.deliveryorderadmin.model.eventbus

import com.alfian.deliveryorderadmin.model.ShipperModel

class UpdateActiveEvent(var shipperModel: ShipperModel, var active:Boolean) {
}