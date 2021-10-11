package com.alfian.deliveryorderadmin.model.eventbus

import com.alfian.deliveryorderadmin.common.Common


class ToastEvent(var action: Common.ACTION, var isBackFromItemList:Boolean)