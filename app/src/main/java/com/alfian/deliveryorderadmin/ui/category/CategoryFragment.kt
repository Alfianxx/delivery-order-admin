package com.alfian.deliveryorderadmin.ui.category

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alfian.deliveryorderadmin.R
import com.alfian.deliveryorderadmin.adapter.MyCategoriesAdapter
import com.alfian.deliveryorderadmin.callback.IMyButtonCallback
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.common.MySwipeHelper
import com.alfian.deliveryorderadmin.model.CategoryModel
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

class CategoryFragment : Fragment() {

    private val PICK_IMAGE_REQUEST: Int = 1234
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyCategoriesAdapter?=null

    private var recyclerMenu: RecyclerView?=null

    internal var categoryModels:List<CategoryModel> = ArrayList()
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference:StorageReference
    private var imageUri: Uri?=null
    private lateinit var imgCategory:ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        categoryViewModel =
            ViewModelProvider(this).get(CategoryViewModel::class.java)

        val root = inflater.inflate(R.layout.fragment_category, container, false)

        initView(root)

        categoryViewModel.getMessageError().observe(viewLifecycleOwner, Observer {
            Toast.makeText(context,it,Toast.LENGTH_SHORT).show()
        })
        categoryViewModel.getCategoryList().observe(viewLifecycleOwner, Observer {
            dialog.dismiss()
            categoryModels = it
            adapter = MyCategoriesAdapter(requireContext(), categoryModels)
            recyclerMenu!!.adapter =  adapter
            recyclerMenu!!.layoutAnimation = layoutAnimationController
        })
        return root
    }

    private fun initView(root:View) {

        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference

        dialog = SpotsDialog.Builder().setContext(context).setCancelable(false).build()
        dialog.show()
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_item_from_left)

        recyclerMenu = root.findViewById(R.id.recycler_menu) as RecyclerView
        recyclerMenu!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)

        recyclerMenu!!.layoutManager = layoutManager
        recyclerMenu!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))

        val swipe = object: MySwipeHelper(requireContext(),recyclerMenu!!,200)
        {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {

                buffer.add(MyButton(context!!,
                    "Delete",
                    30,
                    0,
                    Color.parseColor("#333639"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {
                            Common.categorySelected = categoryModels[pos]

                            showDeleteDialog()
                        }

                    }))

                buffer.add(MyButton(context!!,
                    "Update",
                    30,
                    0,
                    Color.parseColor("#560027"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {
                            Common.categorySelected = categoryModels[pos]

                            showUpdateDialog()
                        }

                    }))
            }

        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.action_bar_menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_create)
        {
            showAddDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDeleteDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Delete Category")
        builder.setMessage("Do you really want to delete this category?")

        builder.setNegativeButton("CANCEL"){ dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("DELETE"){ _, _ ->

            deleteCategory()

        }


        val deleteDialog = builder.create()
        deleteDialog.show()
    }

    private fun deleteCategory() {
        FirebaseDatabase.getInstance()
            .getReference(Common.SHOP_REF)
            .child(Common.currentServerUser!!.shop!!)
            .child(Common.CATEGORY_REF)
            .child(Common.categorySelected!!.menuId!!)
            .removeValue()
            .addOnFailureListener{e-> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()}
            .addOnCompleteListener{
                categoryViewModel.loadCategory()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.DELETE,false))
            }
    }

    private fun showUpdateDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Update Category")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_category,null)
        val edtCategoryName = itemView.findViewById<View>(R.id.edt_category_name) as EditText
        imgCategory = itemView.findViewById<View>(R.id.img_category) as ImageView

        //Set data
        edtCategoryName.setText(Common.categorySelected!!.name)
        Glide.with(requireContext()).load(Common.categorySelected!!.image).into(imgCategory)

        //set event
        imgCategory.setOnClickListener{
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("CANCEL"){ dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("UPDATE"){ _, _ ->
            val updateData = HashMap<String,Any>()
            updateData["name"] = edtCategoryName.text.toString()
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
                        dialog.dismiss() //Fixed buh
                        imageFolder.downloadUrl.addOnSuccessListener{uri ->
                            updateData["image"] = uri.toString()
                            updateCategory(updateData)
                        }
                    }
            }
            else
            {
                updateCategory(updateData)
            }
        }
        
        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun updateCategory(updateData: java.util.HashMap<String, Any>) {
        FirebaseDatabase.getInstance()
            .getReference(Common.SHOP_REF)
            .child(Common.currentServerUser!!.shop!!)
            .child(Common.CATEGORY_REF)
            .child(Common.categorySelected!!.menuId!!)
            .updateChildren(updateData)
            .addOnFailureListener{e-> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()}
            .addOnCompleteListener{
                categoryViewModel.loadCategory()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.UPDATE,false))
            }
    }

    private fun showAddDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Add Category")
        builder.setMessage("Please fill information")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_update_category,null)
        val edtCategoryName = itemView.findViewById<View>(R.id.edt_category_name) as EditText
        imgCategory = itemView.findViewById<View>(R.id.img_category) as ImageView

        //Set data
        Glide.with(requireContext()).load(R.drawable.ic_baseline_image_grey_24).into(imgCategory)

        //set event
        imgCategory.setOnClickListener{
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)
        }

        builder.setNegativeButton("CANCEL"){ dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("CREATE"){ _, _ ->


            val categoryModel = CategoryModel()
            categoryModel.name = edtCategoryName.text.toString()
            categoryModel.items = ArrayList()


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
                        dialog.dismiss() //dialog, not dialog interface
                        imageFolder.downloadUrl.addOnSuccessListener{uri ->
                            categoryModel.image = uri.toString()
                            addCategory(categoryModel)
                        }
                    }
            }
            else
            {
                addCategory(categoryModel)
            }
        }

        builder.setView(itemView)
        val updateDialog = builder.create()
        updateDialog.show()
    }

    private fun addCategory(categoryModel: CategoryModel) {
        FirebaseDatabase.getInstance()
            .getReference(Common.SHOP_REF)
            .child(Common.currentServerUser!!.shop!!)
            .child(Common.CATEGORY_REF)
            .push()
            .setValue(categoryModel)
            .addOnFailureListener{e-> Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()}
            .addOnCompleteListener{
                categoryViewModel.loadCategory()
                EventBus.getDefault().postSticky(ToastEvent(Common.ACTION.CREATE,false))
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if (data != null && data.data != null)
            {
                imageUri = data.data
                imgCategory.setImageURI(imageUri)
            }
        }
    }
}
