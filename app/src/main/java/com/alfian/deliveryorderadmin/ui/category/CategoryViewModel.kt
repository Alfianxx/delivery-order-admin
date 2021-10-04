package com.alfian.deliveryorderadmin.ui.category

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alfian.deliveryorderadmin.callback.ICategoryCallBackListener
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.model.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CategoryViewModel : ViewModel(), ICategoryCallBackListener {

    override fun onCategoryLoadSuccess(categoriesList: List<CategoryModel>) {
        categoriesListMutable!!.value = categoriesList
    }

    override fun onCategoryLoadFailed(message: String) {
        messageError.value = message
    }

    private var categoriesListMutable : MutableLiveData<List<CategoryModel>>?=null
    private var messageError:MutableLiveData<String> = MutableLiveData()
    private var categoryCallBackListener: ICategoryCallBackListener = this

    fun getCategoryList() :MutableLiveData<List<CategoryModel>>{
        if(categoriesListMutable == null)
        {
            categoriesListMutable = MutableLiveData()
            loadCategory()
        }
        return categoriesListMutable!!
    }

    fun getMessageError():MutableLiveData<String>{
        return messageError
    }

    fun loadCategory() {
        val tempList = ArrayList<CategoryModel>()

        try {

            Log.d("abcd", "loadCategory: current restaurant = ${Common.currentServerUser?.restaurant}")

            val categoryRef = FirebaseDatabase.getInstance().getReference(Common.RESTAURANT_REF)
                .child(Common.currentServerUser!!.restaurant!!)
//                .child("restauranta")
                .child(Common.CATEGORY_REF)

            categoryRef.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    categoryCallBackListener.onCategoryLoadFailed((p0.message))
                }
                override fun onDataChange(p0: DataSnapshot) {
                    // looping data dari firebase
                    for (itemSnapshot in p0.children)
                    {
                        val model = itemSnapshot.getValue<CategoryModel>(CategoryModel::class.java)
                        model!!.menu_id = itemSnapshot.key
                        tempList.add(model)
                    }
                    categoryCallBackListener.onCategoryLoadSuccess(tempList)
                }

            })

        } catch (e: Exception) {
            Log.d("abcd", "loadCategory: ${e.message}")
        }

    }


}

