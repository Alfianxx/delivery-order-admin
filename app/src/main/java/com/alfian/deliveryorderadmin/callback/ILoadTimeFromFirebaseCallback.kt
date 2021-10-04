package com.alfian.deliveryorderadmin.callback

interface ILoadTimeFromFirebaseCallback {
    fun onLoadOnlyTimeSuccess(estimatedTimeMs:Long)
    fun onLoadTimeFailed(message:String)
}