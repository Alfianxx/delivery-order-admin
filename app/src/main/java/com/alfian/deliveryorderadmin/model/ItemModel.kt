package com.alfian.deliveryorderadmin.model

class ItemModel {
    var key:String?=null
    var name:String?=null
    var image:String?=null
    var id:String?=null
    var description:String?=null
    var price:Long=0

    var ratingValue:Double = 0.toDouble()
    var ratingCount:Long = 0.toLong()

    var positionInList:Int=-1
}