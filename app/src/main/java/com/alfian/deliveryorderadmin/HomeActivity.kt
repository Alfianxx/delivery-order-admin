package com.alfian.deliveryorderadmin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.alfian.deliveryorderadmin.adapter.PdfDocumentAdapter
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.common.PDFUtils
import com.alfian.deliveryorderadmin.model.*
import com.alfian.deliveryorderadmin.model.eventbus.CategoryClick
import com.alfian.deliveryorderadmin.model.eventbus.ChangeMenuClick
import com.alfian.deliveryorderadmin.model.eventbus.PrintOrderEvent
import com.alfian.deliveryorderadmin.model.eventbus.ToastEvent
import com.alfian.deliveryorderadmin.remote.IFCMService
import com.alfian.deliveryorderadmin.remote.RetrofitFCMClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itextpdf.text.*
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfWriter
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToLong

class HomeActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 7171
    private var menuClick: Int = -1
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView

    private var imgUpload: ImageView? = null
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iFcmService: IFCMService
    private var imgUri: Uri? = null
    private lateinit var storage: FirebaseStorage
    private var storageReference: StorageReference? = null

    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_home)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        fab_chat.setOnClickListener { startActivity(Intent(this, ChatListActivity::class.java)) }

        init()

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_category, R.id.nav_item_list, R.id.nav_order, R.id.nav_shipper
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        navView.setNavigationItemSelectedListener { p0 ->
            p0.isChecked = true
            drawerLayout.closeDrawers()
            if (p0.itemId == R.id.nav_sign_out) {
                signOut()
            } else if (p0.itemId == R.id.nav_category) {
                if (menuClick != p0.itemId) {
                    navController.popBackStack() //Clear back stack
                    navController.navigate(R.id.nav_category)
                }
            } else if (p0.itemId == R.id.nav_shipper) {
                if (menuClick != p0.itemId) {
                    navController.popBackStack() //Clear back stack
                    navController.navigate(R.id.nav_shipper)
                }
            } else if (p0.itemId == R.id.nav_order) {
                if (menuClick != p0.itemId) {
                    navController.popBackStack() //Clear back stack
                    navController.navigate(R.id.nav_order)
                }
            }
//            else if (p0.itemId == R.id.nav_news) {    // news
//                showSendNewsDialog()
//            }
            else if (p0.itemId == R.id.nav_location) {
                showLocationDialog()
            }

            menuClick = p0.itemId
            true
        }

        //View
        val headerView = navView.getHeaderView(0)
        val txtUser = headerView.findViewById<View>(R.id.txt_user) as TextView
        val txtAddress = headerView.findViewById<View>(R.id.tvAddress) as TextView
        Common.setSpanString("Halo ", Common.currentServerUser!!.name, txtUser)



        menuClick = R.id.nav_category //Default

        checkOpenOrderFragment()

        getAddressFromFirebase { address ->
            Common.setSpanString("Alamat :", address, txtAddress)
        }

    }

    private fun coordinateToAddress(lat: Double?, lng: Double?): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""


        if (lat != null && lng != null){
            addresses = geocoder.getFromLocation(lat, lng, 1)

            if (addresses.isNotEmpty()) {
                address = addresses[0]
                addressText = address.getAddressLine(0)
            } else{
                addressText = "alamat tidak ditemukan"
            }
        }
        val resultAddress = addressText.split(",")

        return resultAddress[0] + resultAddress[1]
    }

    private fun setAddressToFirebase(lat: Double, lng: Double) {
        val shopModel = ShopModel(coordinateToAddress(lat, lng))
        FirebaseDatabase.getInstance().getReference(Common.SHOP_REF)
            .child(Common.currentServerUser?.shop!!)
            .child(Common.SHOP_ADMIN)
            .setValue(
                shopModel
            )
    }

    private fun getAddressFromFirebase(listener: (String) -> Unit) {

        FirebaseDatabase.getInstance().getReference(Common.SHOP_REF)
            .child(Common.MYSHOP)
            .child(Common.SHOP_ADMIN)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shopModel = snapshot.getValue(ShopModel::class.java)
                    shopModel?.address?.let { listener(it) }
                }

                override fun onCancelled(error: DatabaseError) = Unit

            })
    }

    private fun showLocationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("update location")
            .setMessage("Apakah Anda ingin mengupdate lokasi ?")

        builder.setNegativeButton("NO") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("YES") { _, _ ->

            Dexter.withContext(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                        val fusedLocationProviderClient =
                            LocationServices.getFusedLocationProviderClient(this@HomeActivity)
                        // check permission
                        if (ActivityCompat.checkSelfPermission(
                                this@HomeActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this@HomeActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {

                            return
                        }
                        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->

                            if (location != null) {
                                //update address to firebase
                                setAddressToFirebase(location.latitude, location.longitude)
                                // Update coordinate to Firebase
                                FirebaseDatabase.getInstance()
                                    .getReference(Common.SHOP_REF)
                                    .child(Common.currentServerUser?.shop!!)
                                    .child(Common.LOCATION_REF)
                                    .setValue(
                                        ShopLocationModel(
                                            location.latitude,
                                            location.longitude
                                        )
                                    )

                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this@HomeActivity,
                                            "update location successfully!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this@HomeActivity,
                                            it.message,
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                    }

                            } else {
                                Toast.makeText(
                                    this@HomeActivity,
                                    "location null ! try again !",
                                    Toast.LENGTH_LONG
                                ).show()
                            }


                        }.addOnFailureListener {
                            Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                        Toast.makeText(
                            this@HomeActivity,
                            " you must allow this permission",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: PermissionRequest?,
                        p1: PermissionToken?
                    ) {

                    }

                }).check()

        }
        val dialog = builder.create()
        dialog.show()

    }

    private fun init() {
        iFcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        subscribeToTopic(Common.getNewOrderTopic())
        updateToken()

        dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage("Please wait....")
            .create()
    }

    private fun showSendNewsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("News System")
            .setMessage("Send news notification to all client")
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_news_system, null)

        //Views
        val edtTitle = itemView.findViewById<View>(R.id.edt_title) as EditText
        val edtContent = itemView.findViewById<View>(R.id.edt_content) as EditText
        val edtLink = itemView.findViewById<View>(R.id.edt_link) as EditText

        val rdiNone = itemView.findViewById<View>(R.id.rdi_none) as RadioButton
        val rdiLink = itemView.findViewById<View>(R.id.rdi_link) as RadioButton
        val rdiUpload = itemView.findViewById<View>(R.id.rdi_image) as RadioButton

        imgUpload = itemView.findViewById(R.id.img_upload) as ImageView

        //Event
        rdiNone.setOnClickListener {
            edtLink.visibility = View.GONE
            imgUpload!!.visibility = View.GONE
        }
        rdiLink.setOnClickListener {
            edtLink.visibility = View.VISIBLE
            imgUpload!!.visibility = View.GONE
        }
        rdiUpload.setOnClickListener {
            edtLink.visibility = View.GONE
            imgUpload!!.visibility = View.VISIBLE
        }
        imgUpload!!.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }

        builder.setView(itemView)
        builder.setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("SEND") { _, _ ->
            if (rdiNone.isChecked)
                sendNews(edtTitle.text.toString(), edtContent.text.toString())
            else if (rdiLink.isChecked)
                sendNews(
                    edtTitle.text.toString(),
                    edtContent.text.toString(),
                    edtLink.text.toString()
                )
            else if (rdiUpload.isChecked) {
                if (imgUri != null) {
                    val dialog = AlertDialog.Builder(this).setMessage("Uploading...").create()
                    dialog.show()
                    val fileName = UUID.randomUUID().toString()
                    val newsImage = storageReference!!.child("news/$fileName")
                    newsImage.putFile(imgUri!!)
                        .addOnFailureListener { e: Exception ->
                            dialog.dismiss()
                            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                        }.addOnSuccessListener {
                            dialog.dismiss()
                            newsImage.downloadUrl.addOnSuccessListener { uri ->
                                sendNews(
                                    edtTitle.text.toString(),
                                    edtContent.text.toString(),
                                    uri.toString()
                                )
                            }
                        }.addOnProgressListener { taskSnapshot ->
                            val progress =
                                (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).roundToLong()
                                    .toDouble()
                            dialog.setMessage(StringBuilder("Uploading: $progress %"))
                        }
                }
            }
        }
        val dialog = builder.create()
        dialog.show()

    }

    private fun sendNews(title: String, content: String, url: String) {
        val notificationData: MutableMap<String, String> = HashMap()
        notificationData[Common.NOTI_TITLE] = title
        notificationData[Common.NOTI_CONTENT] = content
        notificationData[Common.IS_SEND_IMAGE] = "true"
        notificationData[Common.IMAGE_URL] = url

        val fcmSendData = FCMSendData(Common.getNewsTopic(), notificationData)
        val dialog = AlertDialog.Builder(this).setMessage("Waiting...").create()
        dialog.show()
        compositeDisposable.addAll(
            iFcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t: FCMResponse? ->
                    dialog.dismiss()
                    if (t!!.message_id != 0L)
                        Toast.makeText(this@HomeActivity, "News has been sent", Toast.LENGTH_LONG)
                            .show()
                    else
                        Toast.makeText(this@HomeActivity, "News send failed", Toast.LENGTH_LONG)
                            .show()
                }, { t: Throwable? ->
                    dialog.dismiss()
                    Toast.makeText(this@HomeActivity, t!!.message, Toast.LENGTH_LONG).show()
                })
        )
    }

    private fun sendNews(title: String, content: String) {
        val notificationData: MutableMap<String, String> = HashMap()
        notificationData[Common.NOTI_TITLE] = title
        notificationData[Common.NOTI_CONTENT] = content
        notificationData[Common.IS_SEND_IMAGE] = "false"
        val fcmSendData = FCMSendData(Common.getNewsTopic(), notificationData)
        val dialog = AlertDialog.Builder(this).setMessage("Waiting...").create()
        dialog.show()
        compositeDisposable.addAll(
            iFcmService.sendNotification(fcmSendData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t: FCMResponse? ->
                    dialog.dismiss()
                    if (t!!.message_id != 0L)
                        Toast.makeText(this@HomeActivity, "News has been sent", Toast.LENGTH_LONG)
                            .show()
                    else
                        Toast.makeText(this@HomeActivity, "News send failed", Toast.LENGTH_LONG)
                            .show()
                }, { t: Throwable? ->
                    dialog.dismiss()
                    Toast.makeText(this@HomeActivity, t!!.message, Toast.LENGTH_LONG).show()
                })
        )
    }

    private fun checkOpenOrderFragment() {
        val isOpenNewOrder = intent.extras!!.getBoolean(Common.IS_OPEN_ACTIVITY_NEW_ORDER, false)
        if (isOpenNewOrder) {
            navController.popBackStack()
            navController.navigate(R.id.nav_order)
            menuClick = R.id.nav_order
        }
    }

    private fun updateToken() {
        FirebaseInstallations.getInstance().getToken(true)
            .addOnFailureListener { e ->
                Toast.makeText(
                    this@HomeActivity,
                    "" + e.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnSuccessListener { instanceIdResult ->
                Log.d("aan", instanceIdResult.token)
                Common.updateToken(this@HomeActivity, instanceIdResult.token, true, false)
            }
    }

    private fun subscribeToTopic(newOrderTopic: String) {
        FirebaseMessaging.getInstance()
            .subscribeToTopic(newOrderTopic)
            .addOnFailureListener { message ->
                Toast.makeText(
                    this@HomeActivity,
                    "" + message.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnCompleteListener { task ->
                if (!task.isSuccessful)
                    Toast.makeText(this@HomeActivity, "Subscribe topic failed!", Toast.LENGTH_SHORT)
                        .show()
            }
    }

    private fun signOut() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sign out")
            .setMessage("Do you really want to exit?")
            .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
            .setPositiveButton("OK") { _, _ ->
                Common.itemSelected = null
                Common.categorySelected = null
                Common.currentServerUser = null
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this@HomeActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().removeAllStickyEvents() //Fixed
        EventBus.getDefault().unregister(this)
        compositeDisposable.clear()
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategoryClick(event: CategoryClick) {
        if (event.isSuccess) {
            if (menuClick != R.id.nav_item_list) {
                navController.navigate(R.id.nav_item_list)
                menuClick = R.id.nav_item_list
            }
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onChangeMenuEvent(event: ChangeMenuClick) {
        if (!event.isFromItemList) {
            //Clear
            navController.popBackStack(R.id.nav_category, true)
            navController.navigate(R.id.nav_category)
        }
        menuClick = -1
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onToastEvent(event: ToastEvent) {
        when (event.action) {
            Common.ACTION.CREATE -> {
                Toast.makeText(this, "Create Success", Toast.LENGTH_SHORT).show()
            }
            Common.ACTION.UPDATE -> {
                Toast.makeText(this, "Update Success", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Delete Success", Toast.LENGTH_SHORT).show()
            }
        }
        EventBus.getDefault().postSticky(ChangeMenuClick(event.isBackFromItemList))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                imgUri = data.data
                imgUpload!!.setImageURI(imgUri)
            }
        }
    }

    //Print
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onPrintEventListener(event: PrintOrderEvent) {
        createPDFFile(event.path, event.orderModel)
    }

    private fun createPDFFile(path: String, orderModel: OrderModel) {
        dialog.show()
        if (File(path).exists())
            File(path).delete()
        try {
            val document = Document() //use from package itextpdf
            //save
            PdfWriter.getInstance(document, FileOutputStream(path))
            //Open
            document.open()

            //Setting
            document.pageSize = (PageSize.A4)
            document.addCreationDate()
            document.addAuthor("Alfian")
            document.addCreator(Common.currentServerUser!!.name)

            //font setting
            val colorAccent = BaseColor(0, 153, 204, 255)
            val fontSize = 20.0f

            //Custom font
            val fontName =
                BaseFont.createFont("assets/fonts/brandon_medium.otf", "UTF-8", BaseFont.EMBEDDED)

            //Create title of document
            val titleFont = Font(fontName, 36.0f, Font.NORMAL, BaseColor.BLACK)
            PDFUtils.addNewItem(document, "Detail Pesanan", Element.ALIGN_CENTER, titleFont)

            //add more
            val orderNumberFont = Font(fontName, fontSize, Font.NORMAL, colorAccent)
            PDFUtils.addNewItem(document, "No. Pesanan:", Element.ALIGN_LEFT, orderNumberFont)

            val orderNumberValueFont = Font(fontName, fontSize, Font.NORMAL, BaseColor.BLACK)
            PDFUtils.addNewItem(
                document,
                orderModel.key!!,
                Element.ALIGN_LEFT,
                orderNumberValueFont
            )
            PDFUtils.addLineSeparator(document)

            //Date
            PDFUtils.addNewItem(document, "Waktu Pesan:", Element.ALIGN_LEFT, orderNumberFont)
            PDFUtils.addNewItem(
                document, SimpleDateFormat("dd-MM-yyyy").format(orderModel.createDate),
                Element.ALIGN_LEFT, orderNumberValueFont
            )
            PDFUtils.addLineSeparator(document)

            //Account name
            PDFUtils.addNewItem(document, "Nama:", Element.ALIGN_LEFT, orderNumberFont)
            PDFUtils.addNewItem(
                document, orderModel.userName!!,
                Element.ALIGN_LEFT, orderNumberValueFont
            )
            PDFUtils.addLineSeparator(document)

            //Product Detail
            PDFUtils.addLineSpace(document)
            PDFUtils.addNewItem(document, "Detail Produk", Element.ALIGN_CENTER, titleFont)
            PDFUtils.addLineSeparator(document)

            //Use RxJava, fetch image from url and add to PDF
            Observable.fromIterable(orderModel.cartItemList)
                .flatMap { cartItem: CartItem ->
                    Common.getBitmapFromUrl(
                        this@HomeActivity,
                        cartItem,
                        document
                    )
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ cartItem -> //on next

                    //Each item we will add detail
                    //Item Name
                    PDFUtils.addNewItemWithLeftAndRight(
                        document,
                        cartItem.itemName!!,
                        "", // kanan pdf nama produk
                        titleFont,
                        orderNumberValueFont
                    )
                    //Item size and addon
                    // TODO : Hapus nanti : size
//                    PDFUtils.addNewItemWithLeftAndRight(
//                        document,
//                        "Size",
//                        Common.formatSizeJsonToString(cartItem.foodSize!!)!!,
//                        titleFont,
//                        orderNumberValueFont
//                    )
                    // TODO : Hapus nanti : addon
//                    PDFUtils.addNewItemWithLeftAndRight(
//                        document,
//                        "Addon",
//                        Common.formatAddonJsonToString(cartItem.foodAddon!!)!!,
//                        titleFont,
//                        orderNumberValueFont
//                    )

                    //Item price
                    //format : 1*30 = 30
                    PDFUtils.addNewItemWithLeftAndRight(
                        document,
                        StringBuilder("Harga: ").append(cartItem.itemQuantity)
                            .append(" x ")
                            .append(cartItem.itemExtraPrice.toInt() + cartItem.itemPrice.toInt())
                            .toString(),
                        StringBuilder().append(cartItem.itemQuantity * (cartItem.itemExtraPrice + cartItem.itemPrice).toInt())
                            .toString(),
                        titleFont,
                        orderNumberValueFont
                    )

                    //last separator
                    PDFUtils.addLineSeparator(document)


                }, { t -> //On Error
                    dialog.dismiss()
                    Toast.makeText(this@HomeActivity, t.message!!, Toast.LENGTH_SHORT).show()
                },
                    { //On Complete

                        //when all product detail is wrote, we will append total
                        PDFUtils.addLineSpace(document)
                        PDFUtils.addLineSpace(document)
                        PDFUtils.addNewItemWithLeftAndRight(
                            document,
                            "Total",
                            StringBuilder().append(orderModel.totalPayment.toInt()).toString(),
                            titleFont,
                            titleFont
                        )

                        //Close
                        document.close()
                        dialog.dismiss()
                        Toast.makeText(this@HomeActivity, "Success", Toast.LENGTH_SHORT).show()
                        printPDF()

                    })
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: DocumentException) {
            e.printStackTrace()
        }
    }

    private fun printPDF() {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        try {
            val printDocumentAdapter = PdfDocumentAdapter(
                this, StringBuilder(Common.getAppPath(this)).append(Common.FILE_PRINT).toString()
            )
            printManager.print("Document", printDocumentAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
