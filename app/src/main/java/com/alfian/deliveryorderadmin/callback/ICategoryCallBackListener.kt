package com.alfian.deliveryorderadmin.callback

import com.alfian.deliveryorderadmin.model.CategoryModel

interface ICategoryCallBackListener {
    fun onCategoryLoadSuccess(categoriesList:List<CategoryModel>)
    fun onCategoryLoadFailed(message:String)
}