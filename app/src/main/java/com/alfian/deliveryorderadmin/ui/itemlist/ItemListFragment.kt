package com.alfian.deliveryorderadmin.ui.itemlist


import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alfian.deliveryorderadmin.R
import com.alfian.deliveryorderadmin.adapter.MyItemListAdapter
import com.alfian.deliveryorderadmin.callback.IMyButtonCallback
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.common.MySwipeHelper
import com.alfian.deliveryorderadmin.model.ItemModel
import com.alfian.deliveryorderadmin.model.eventbus.ChangeMenuClick
import com.alfian.deliveryorderadmin.model.eventbus.ToastEvent
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dmax.dialog.SpotsDialog
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ItemListFragment : Fragment() {

    private var imageUri: Uri?=null
    private val PICK_IMAGE_REQUEST: Int=1234
    private lateinit var itemListViewModel: ItemListViewModel

    var recyclerItemList : RecyclerView?=null
    private var layoutAnimationController:LayoutAnimationController?=null

    var adapter : MyItemListAdapter?=null
    var itemModelList :List<ItemModel> = ArrayList()


    //Variable
    private var imgItem:ImageView?=null
    private lateinit var storage:FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var dialog:android.app.AlertDialog

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.item_list_menu,menu)

        //Create search view
        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as androidx.appcompat.widget.SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName!!))
        //Event
        searchView.setOnQueryTextListener(object:androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(search: String?): Boolean {
                startSearchItem(search!!)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }

        })
        //clear text when click to clear button
        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener {
            val ed = searchView.findViewById<View>(R.id.search_src_text) as EditText
            //clear text
            ed.setText("")
            //Clear query
            searchView.setQuery("",false)
            //collapse the action view
            searchView.onActionViewCollapsed()
            //collapse the search widget
            menuItem.collapseActionView()
            //restore result to original
            itemListViewModel.getMutableItemModelListData().value = Common.categorySelected!!.items
        }
    }

    private fun startSearchItem(s: String) {
        val resultItem: MutableList<ItemModel> = ArrayList()
        for (i in Common.categorySelected!!.items!!.indices)
        {
            val itemModel = Common.categorySelected!!.items!![i]
            if (itemModel.name!!.toLowerCase(Locale.ROOT).contains(s.toLowerCase(Locale.ROOT))) {   // aslinya tanpa parameter locale
                //Here we will save index of search result item
                itemModel.positionInList = 1
                resultItem.add(itemModel)
            }
        }
        //Update search result
        itemListViewModel.getMutableItemModelListData().value = resultItem
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        itemListViewModel =
            ViewModelProvider(this).get(ItemListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_item_list,container,false)
        initViews(root)
        itemListViewModel.getMutableItemModelListData().observe(viewLifecycleOwner, Observer {
            if(it!=null) {
                itemModelList = it
                adapter = MyItemListAdapter(requireContext(), itemModelList)
                recyclerItemList!!.adapter = adapter
                recyclerItemList!!.layoutAnimation = layoutAnimationController
            }
        })
        return root
    }

    private fun initViews(root: View?){

        setHasOptionsMenu(true) //Enable options menu on Fragment

        dialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        recyclerItemList = root!!.findViewById(R.id.recycler_item_list) as RecyclerView
        recyclerItemList!!.setHasFixedSize(true)
        recyclerItemList!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)


        (activity as AppCompatActivity).supportActionBar!!.title = Common.categorySelected!!.name

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        val swipe = object: MySwipeHelper(requireContext(),recyclerItemList!!,width/6)
        {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(context!!,
                    "Delete",
                    30,
                    0,
                    Color.parseColor("#9b0000"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {

                            Common.itemSelected = itemModelList[pos]
                            val builder = AlertDialog.Builder(context!!)
                            builder.setTitle("Delete")
                                .setMessage("Do you really want to delete item ?")
                                .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
                                .setPositiveButton("DELETE") { _, _ ->
                                    val itemModel = adapter!!.getItemAtPosition(pos)
                                    if (itemModel.positionInList == -1)
                                        Common.categorySelected!!.items!!.removeAt(pos)
                                    else
                                        Common.categorySelected!!.items!!.removeAt(itemModel.positionInList)
                                    updateItem(
                                        Common.categorySelected!!.items,
                                        Common.ACTION.DELETE
                                    )
                                }

                            val deleteDialog = builder.create()
                            deleteDialog.show()

                        }

                    })
                )

                buffer.add(MyButton(context!!,
                    "Update",
                    30,
                    0,
                    Color.parseColor("#560027"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {

                            val itemModel = adapter!!.getItemAtPosition(pos)
                            if (itemModel.positionInList == -1)
                                showUpdateDialog(pos,itemModel)
                            else
                                showUpdateDialog(itemModel.positionInList,itemModel)
                        }

                    })
                )

                //TODO FIX : hapus tombol size dan addon
//                buffer.add(MyButton(context!!,
//                    "Size",
//                    30,
//                    0,
//                    Color.parseColor("#12005e"),
//                    object : IMyButtonCallback {
//                        override fun onClick(pos: Int) {
//
//
//                            val foodModel = adapter!!.getItemAtPosition(pos)
//                            if (foodModel.positionInList == -1)
//                                Common.itemSelected = itemModelList!![pos]
//                            else
//                                Common.itemSelected = foodModel
//                            startActivity(Intent(context,SizeAddonEditActivity::class.java))
//                            if (foodModel.positionInList == -1)
//                                EventBus.getDefault().postSticky(AddonSizeEditEvent(false,pos))
//                            else
//                                EventBus.getDefault().postSticky(AddonSizeEditEvent(false,foodModel.positionInList))
//
//                        }
//
//                    })
//                )
//
//                buffer.add(MyButton(context!!,
//                    "Addon",
//                    30,
//                    0,
//                    Color.parseColor("#333639"),
//                    object : IMyButtonCallback {
//                        override fun onClick(pos: Int) {
//
//                            val foodModel = adapter!!.getItemAtPosition(pos)
//                            if (foodModel.positionInList == -1)
//                                Common.itemSelected = itemModelList!![pos]
//                            else
//                                Common.itemSelected = foodModel
//                            startActivity(Intent(context,SizeAddonEditActivity::class.java))
//                            if (foodModel.positionInList == -1)
//                                EventBus.getDefault().postSticky(AddonSizeEditEvent(true,pos))
//                            else
//                                EventBus.getDefault().postSticky(AddonSizeEditEvent(true,foodModel.positionInList))
//
//                        }
//
//                    })
//                )

            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_create)
            showAddItemDialog()
        return super.onOptionsItemSelected(item)
    }

    private fun showAddItemDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Buat Produk")
        builder.setMessage("Isi Data")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_item,null)

        val edtItemName = itemView.findViewById<View>(R.id.edt_item_name) as EditText
        val edtItemPrice = itemView.findViewById<View>(R.id.edt_item_price) as EditText
        val edtItemDescription = itemView.findViewById<View>(R.id.edt_item_description) as EditText
        imgItem = itemView.findViewById<View>(R.id.img_item_image) as ImageView

        //Set data

        Glide.with(requireContext()).load(R.drawable.ic_baseline_image_grey_24).into(imgItem!!)

        //Set Event
        imgItem!!.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("Batal") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("Buat"){ dialogInterface, _ ->

            val updateItem = ItemModel()
            updateItem.name = edtItemName.text.toString()
            updateItem.price = if(TextUtils.isEmpty(edtItemPrice.text))
                0
            else
                edtItemPrice.text.toString().toLong()
            updateItem.description = edtItemDescription.text.toString()
            //TODO : tambah id atau key
            val uid = UUID.randomUUID()
            updateItem.id = uid.toString()
            Log.d("aan", "showAddItemDialog: uuid = $uid")

            if (imageUri != null)
            {
                dialog.setMessage("Uploading....")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                    .addOnFailureListener{e ->
                        dialog.dismiss()
                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener { taskSnapshot ->
                        val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                        dialog.setMessage("Uploaded $progress")
                    }
                    .addOnSuccessListener {
                        dialogInterface.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener{uri ->
                            dialog.dismiss()
                            updateItem.image = uri.toString()
                            if (Common.categorySelected!!.items == null)
                                Common.categorySelected!!.items = ArrayList()
                            Common.categorySelected!!.items!!.add(updateItem)
                            updateItem(Common.categorySelected!!.items!!,Common.ACTION.CREATE)
                        }
                    }

            }
            else
            {
                Common.categorySelected!!.items!!.add(updateItem)
                updateItem(Common.categorySelected!!.items!!,Common.ACTION.CREATE)
            }
        }
        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun showUpdateDialog(pos: Int, itemModel: ItemModel) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_item,null)

        val edtItemName = itemView.findViewById<View>(R.id.edt_item_name) as EditText
        val edtItemPrice = itemView.findViewById<View>(R.id.edt_item_price) as EditText
        val edtItemDescription = itemView.findViewById<View>(R.id.edt_item_description) as EditText
        imgItem = itemView.findViewById<View>(R.id.img_item_image) as ImageView

        //Set data
        edtItemName.setText(StringBuilder("").append(itemModel.name))
        edtItemPrice.setText(StringBuilder("").append(itemModel.price))
        edtItemDescription.setText(StringBuilder("").append(itemModel.description))
        Glide.with(requireContext()).load(itemModel.image).into(imgItem!!)

        //Set Event
        imgItem!!.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("UPDATE"){ dialogInterface, _ ->

            val updateItem = itemModel
            updateItem.name = edtItemName.text.toString()
            updateItem.price = if(TextUtils.isEmpty(edtItemPrice.text))
                0
            else
                edtItemPrice.text.toString().toLong()
            updateItem.description = edtItemDescription.text.toString()

            if (imageUri != null)
            {
                dialog.setMessage("Uploading....")
                dialog.show()

                val imageName = UUID.randomUUID().toString()
                val imageFolder = storageReference.child("images/$imageName")
                imageFolder.putFile(imageUri!!)
                    .addOnFailureListener{e ->
                        dialog.dismiss()
                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnProgressListener { taskSnapshot ->
                        val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                        dialog.setMessage("Uploaded $progress")
                    }
                    .addOnSuccessListener {
                        dialogInterface.dismiss()
                        imageFolder.downloadUrl.addOnSuccessListener{uri ->
                            dialog.dismiss()
                            updateItem.image = uri.toString()
                            Common.categorySelected!!.items!![pos] = updateItem
                            updateItem(Common.categorySelected!!.items!!,Common.ACTION.UPDATE)
                        }
                    }

            }
            else
            {
                Common.categorySelected!!.items!![pos] = updateItem
                updateItem(Common.categorySelected!!.items!!,Common.ACTION.UPDATE)
            }
        }
        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if (data != null && data.data != null)
            {
                imageUri = data.data
                imgItem!!.setImageURI(imageUri)
            }
        }
    }

    private fun updateItem(items: MutableList<ItemModel>?, action: Common.ACTION) {
        val updateData = HashMap<String,Any>()
        updateData["items"] = items!!

        FirebaseDatabase.getInstance()
            .getReference(Common.SHOP_REF)
            .child(Common.currentServerUser!!.shop!!)
            .child(Common.CATEGORY_REF)
            .child(Common.categorySelected!!.menuId!!)
            .updateChildren(updateData)
            .addOnFailureListener { e -> Toast.makeText(requireContext(),""+e.message,Toast.LENGTH_SHORT).show() }
            .addOnCompleteListener { task ->
                if (task.isSuccessful)
                {
                    itemListViewModel.getMutableItemModelListData()
                    EventBus.getDefault().postSticky(ToastEvent(action,true))
                }
            }
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }
}
