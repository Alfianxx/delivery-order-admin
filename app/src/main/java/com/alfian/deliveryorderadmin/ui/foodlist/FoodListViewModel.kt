package com.alfian.deliveryorderadmin.ui.foodlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.model.FoodModel

class FoodListViewModel : ViewModel(){

    private var mutableFoodModelListData : MutableLiveData<List<FoodModel>>?=null

    fun getMutableFoodModelListData(): MutableLiveData<List<FoodModel>> {
        if (mutableFoodModelListData == null)
            mutableFoodModelListData = MutableLiveData()
        mutableFoodModelListData!!.value = Common.categorySelected!!.foods
        return mutableFoodModelListData!!
    }
}