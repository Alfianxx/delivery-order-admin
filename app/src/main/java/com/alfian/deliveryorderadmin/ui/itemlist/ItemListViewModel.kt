package com.alfian.deliveryorderadmin.ui.itemlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.model.ItemModel

class ItemListViewModel : ViewModel(){

    private var mutableItemModelListData : MutableLiveData<List<ItemModel>>?=null

    fun getMutableItemModelListData(): MutableLiveData<List<ItemModel>> {
        if (mutableItemModelListData == null)
            mutableItemModelListData = MutableLiveData()
        mutableItemModelListData!!.value = Common.categorySelected!!.items
        return mutableItemModelListData!!
    }
}