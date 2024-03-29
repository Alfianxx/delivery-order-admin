package com.alfian.deliveryorderadmin.services

import android.content.Intent
import com.alfian.deliveryorderadmin.MainActivity
import com.alfian.deliveryorderadmin.common.Common
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MyFCMServices : FirebaseMessagingService() {
    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Common.updateToken(this,p0,true,false) //because we are in server app so server=true
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val dataReceive = remoteMessage.data
        if (dataReceive[Common.NOTI_TITLE]!! == "New Order")
        {
            //Create intent and call MainActivity
            //Because we need Common.currentUser is assign
            //So we must call MainActivity instead direct HomeActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER,true) //Pass key
            Common.showNotification(this, Random().nextInt(),
                dataReceive[Common.NOTI_TITLE],
                dataReceive[Common.NOTI_CONTENT],
                intent)
        }
        else
        Common.showNotification(this, Random().nextInt(),
            dataReceive[Common.NOTI_TITLE],
            dataReceive[Common.NOTI_CONTENT],
            null)
    }
}