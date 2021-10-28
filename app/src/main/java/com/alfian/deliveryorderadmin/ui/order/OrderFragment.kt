package com.alfian.deliveryorderadmin.ui.order

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alfian.deliveryorderadmin.R
import com.alfian.deliveryorderadmin.TrackingOrderActivity
import com.alfian.deliveryorderadmin.adapter.MyOrderAdapter
import com.alfian.deliveryorderadmin.adapter.MyShipperSelectedAdapter
import com.alfian.deliveryorderadmin.callback.IMyButtonCallback
import com.alfian.deliveryorderadmin.callback.IShipperLoadCallbackListener
import com.alfian.deliveryorderadmin.common.BottomSheetOrderFragment
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.common.MySwipeHelper
import com.alfian.deliveryorderadmin.model.*
import com.alfian.deliveryorderadmin.model.eventbus.ChangeMenuClick
import com.alfian.deliveryorderadmin.model.eventbus.LoadOrderEvent
import com.alfian.deliveryorderadmin.model.eventbus.PrintOrderEvent
import com.alfian.deliveryorderadmin.remote.IFCMService
import com.alfian.deliveryorderadmin.remote.RetrofitFCMClient
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_order.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.alfian.deliveryorderadmin.MainActivity

import org.apache.poi.hssf.usermodel.HSSFRichTextString

import org.apache.poi.hssf.usermodel.HSSFCell

import org.apache.poi.hssf.usermodel.HSSFRow

import org.apache.poi.hssf.usermodel.HSSFSheet

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.IndexedColorMap
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class OrderFragment: Fragment(), IShipperLoadCallbackListener, OrderListener {
    private val compositeDisposable = CompositeDisposable()
    lateinit var iFcmService: IFCMService
    lateinit var recyclerOrder:RecyclerView
    private lateinit var layoutAnimationController: LayoutAnimationController

    private lateinit var orderViewModel: OrderViewModel

    private var adapter : MyOrderAdapter?=null

    private var myShipperSelectedAdapter: MyShipperSelectedAdapter? = null
    lateinit var shipperLoadCallbackListener:IShipperLoadCallbackListener
    private var recyclerShipper:RecyclerView?=null

    val columnName = 0
    val columnProduct = 1
    val columnPrice = 2
    val columnEmail = 3
    val columnAddress = 4

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_order,container,false)
        initViews(root)

        orderViewModel = ViewModelProvider(this).get(OrderViewModel::class.java)

        orderViewModel.messageError.observe(viewLifecycleOwner, Observer { s ->
            Toast.makeText(context,s,Toast.LENGTH_SHORT).show()
        })
        orderViewModel.getOrderModelList().observe(viewLifecycleOwner, Observer { orderList ->
            if (orderList != null)
            {
                adapter = MyOrderAdapter(requireContext(),orderList.toMutableList())
                recyclerOrder.adapter = adapter
                recyclerOrder.layoutAnimation = layoutAnimationController

                val orderListener: OrderListener = this
//                orderListener.onGetData()

               updateTextCounter()
            }
        })

        return root
    }

    private fun createWorkbook(): Workbook {
        // Creating excel workbook
        val workbook = XSSFWorkbook()

        //Creating first sheet inside workbook
        //Constants.SHEET_NAME is a string value of sheet name
        val sheet: Sheet = workbook.createSheet("Data Pelanggan")

        //Create Header Cell Style
        val cellStyle = getHeaderStyle(workbook)

        //Creating sheet header row
        createSheetHeader(cellStyle, sheet)

        //Adding data to the sheet
        for (i in 1..10) {
            addData(i, sheet)
        }

        return workbook
    }

    //tes excel
    private fun createSheetHeader(cellStyle: CellStyle, sheet: Sheet) {
        //setHeaderStyle is a custom function written below to add header style

        //Create sheet first row
        val row = sheet.createRow(0)

        //Header list
        val HEADER_LIST = listOf("Nama", "Product", "Harga", "Email/Hp", "Alamat", "Tanggal")    //todo ganti Header

        //Loop to populate each column of header row
        for ((index, value) in HEADER_LIST.withIndex()) {

            val columnWidth = (15 * 500)

            //index represents the column number
            sheet.setColumnWidth(index, columnWidth)

            //Create cell
            val cell = row.createCell(index)

            //value represents the header value from HEADER_LIST
            cell?.setCellValue(value)

            //Apply style to cell
            cell.cellStyle = cellStyle
        }
    }

    private fun getHeaderStyle(workbook: Workbook): CellStyle {

        //Cell style for header row
        val cellStyle: CellStyle = workbook.createCellStyle()

        //Apply cell color
        val colorMap: IndexedColorMap = (workbook as XSSFWorkbook).stylesSource.indexedColors
        var color = XSSFColor(IndexedColors.GREEN, colorMap).indexed
        cellStyle.fillForegroundColor = color
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

        //Apply font style on cell text
        val whiteFont = workbook.createFont()
        color = XSSFColor(IndexedColors.WHITE, colorMap).indexed
        whiteFont.color = color
        whiteFont.bold = true
        cellStyle.setFont(whiteFont)


        return cellStyle
    }

    override fun onGetData() {
        TODO("Not yet implemented")
    }

//    private fun addData(rowIndex: Int, sheet: Sheet) {
//
////        object : OrderListener {
////            override fun onGetData() {
////
////            }
////
////        }
//
//        //Create row based on row index
//        val row = sheet.createRow(rowIndex)
//
//        //Add data to each cell
//        createCell(row, 0, "value 1") //Column 1
//        createCell(row, 1, "value 2") //Column 2
//        createCell(row, 2, "value 3") //Column 3
//    }

    private fun addData(rowIndex: Int, sheet: Sheet) {
        //Create row based on row index
        //Add data to each cell
        //todo error background thread
        orderViewModel.getOrderModelList().observe(viewLifecycleOwner, Observer {

            //looping supaya hanya mengambil data sebulan terurut
            val listData = mutableListOf<OrderModel>()
            val sdf = SimpleDateFormat("dd-M-yyyy")
            val currentDate = sdf.format(Date())
            val currentMonth = currentDate.split("-")
            for (order in it) {
                val date = SimpleDateFormat("dd-MM-yyyy").format(order.createDate)
                val dateList = date.split("-")
                if ( currentMonth[1]+currentMonth[2] == dateList[1] + dateList[2]) {
                    listData.add(order)
                }
            }


            // todo pikir pikir
            var total = 0   // todo total item salah
            var price = 0
            for ((index, data) in listData.withIndex()) {
                val row = sheet.createRow(index + 1)

                val productCart = mutableListOf<CartItem>()
                var itemName = ""

                for ( i in data.cartItemList!!.indices) {
                    total += data.cartItemList!![i].itemQuantity
                    val jumlah = data.cartItemList!![i].itemQuantity
                    itemName += data.cartItemList!![i].itemName + " * $jumlah, "
                }
                Log.d("abcd", "total = $total")

                Log.d("abcd", "data quantity = ${data.cartItemList!![0].itemQuantity}")

                val date = SimpleDateFormat("dd-MM-yyyy").format(data.createDate)
                createCell(row, 0, data.userName)
                createCell(row, 1, itemName)
                createCell(row, 2, data.finalPayment.toInt().toString())
                createCell(row, 3, data.userPhone)
                createCell(row, 4, data.shippingAddress)
                createCell(row, 5, date)

                price += data.finalPayment.toInt()

                val rowTotal = sheet.createRow(listData.size + 1)
                createCell(rowTotal, 1, "$total item")
                createCell(rowTotal, 2, "Total Harga Rp.$price")
            }
        })
    }

    private fun createCell(row: Row, columnIndex: Int, value: String?) {
        val cell = row.createCell(columnIndex)
        cell?.setCellValue(value)
    }

    private fun createExcel(workbook: Workbook) {
        val str_path = Environment.getExternalStorageDirectory().toString()
        val file = File("$str_path/DataPelanggan/")    //.xlsx
        // buat folder
        if (!file.exists()) {
            file.mkdirs()
        }
        // buat file
        val myFile = File(file, getString(R.string.app_name) + ".xlsx")

        //Write workbook to file using FileOutputStream
        try {
            val fileOut = FileOutputStream(myFile)
            workbook.write(fileOut)
            fileOut.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // todo : part 2
    private fun getExcelFile(): File? {

        val str_path = Environment.getExternalStorageDirectory().toString()
        val file: File
        file = File(str_path, getString(R.string.app_name) + ".xlsx")

        //Check App Directory whether it exists or not
        file.let {
            //Check if file exists or not
            if (it.exists()) {
                //return excel file
                return it
            }
        }
        return null
    }

    private fun readExcelAsWorkbook(): Workbook?{
        //Reading excel file
        getExcelFile()?.let {
            try {

                //Reading excel file as stream
                val inputStream = FileInputStream(it)

                //Return workbook
                return WorkbookFactory.create(inputStream)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        return null
    }

    private fun getSheet(): Sheet? {

        //Getting workbook
        readExcelAsWorkbook()?.let { workbook ->

            //Checking that sheet exist
            //This function will also tell you total number of sheets
            if (workbook.numberOfSheets > 0) {

                //Return first sheet of excel. You can get all existing sheets
                return workbook.getSheetAt(0)
            }
        }

        return null
    }

    private fun getRow(){
        //get sheet
        getSheet()?.let{ sheet ->

            //To find total number of rows
            val totalRows = sheet.physicalNumberOfRows

            //Total number of cells of a row
            val totalColumns = sheet.getRow(0).physicalNumberOfCells

            //Get value of first cell from row
            val value = sheet.getRow(0).getCell(0)
            Log.d("abcd", "value = $value")
            Log.d("abcd", "totalcol = $totalColumns")
            Log.d("abcd", "row = $totalRows")

        }
    }

    //--------------------------------------------------------------------------------

    private fun initViews(root:View) {





        shipperLoadCallbackListener = this

        iFcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        setHasOptionsMenu(true)

        recyclerOrder = root.findViewById(R.id.recycler_order) as RecyclerView
        recyclerOrder.setHasFixedSize(true)
        recyclerOrder.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels

        val swipe = object: MySwipeHelper(requireContext(), recyclerOrder,width/6)
        {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(context!!,
                    "Print",
                    30,
                    0,
                    Color.parseColor("#8b0010"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {

                            //tes csv
//                            val csv: String =
//                                Environment.getExternalStorageDirectory().getAbsolutePath()
//                                    .toString() + "/MyCsvFile.csv" // Here csv file name is MyCsvFile.csv
//                            //by Hiting button csv will create inside phone storage.
//                            var writer: CSVWriter? = null
//                            try {
//                                writer = CSVWriter(FileWriter(csv))
//                                val data: MutableList<Array<String>> = ArrayList()
//                                data.add(arrayOf("Country", "Capital"))
//                                data.add(arrayOf("India", "New Delhi"))
//                                data.add(arrayOf("United States", "Washington D.C"))
//                                data.add(arrayOf("Germany", "Berlin"))
//                                writer.writeAll(data) // data is adding to csv
//                                writer.close()
////                                        callRead()
//                            } catch (e: IOException) {
//                                e.printStackTrace()
//                            }

                            //tes


                            // tes excel
//                            val workbook = HSSFWorkbook()
//                            val firstSheet = workbook.createSheet("Sheet No 1")
//                            val secondSheet = workbook.createSheet("Sheet No 2")
//                            val rowA = firstSheet.createRow(0)
//                            val cellA = rowA.createCell(0)
//                            cellA.setCellValue(HSSFRichTextString("Sheet One"))
//                            val rowB = secondSheet.createRow(0)
//                            val cellB = rowB.createCell(0)
//                            cellB.setCellValue(HSSFRichTextString("Sheet two"))
//                            var fos: FileOutputStream? = null
//                            try {
//                                val str_path = Environment.getExternalStorageDirectory().toString()
//                                val file: File
//                                file = File(str_path, getString(R.string.app_name) + ".xls")
//                                fos = FileOutputStream(file)
//                                workbook.write(fos)
//                            } catch (e: IOException) {
//                                e.printStackTrace()
//                            } finally {
//                                if (fos != null) {
//                                    try {
//                                        fos.flush()
//                                        fos.close()
//                                    } catch (e: IOException) {
//                                        e.printStackTrace()
//                                    }
//                                }
//                                Toast.makeText(
//                                    requireContext(),
//                                    "Excel Sheet Generated",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                            }




                            //membuat pdf
                            Dexter.withContext(activity)
                                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                .withListener(object:PermissionListener{
                                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {

                                        //send event to HomeActivity to process print
                                        EventBus.getDefault().postSticky(
                                            PrintOrderEvent(
                                            StringBuilder(Common.getAppPath(activity!!))
                                                .append(Common.FILE_PRINT).toString(),
                                            adapter!!.getItemAtPosition(pos)
                                        )
                                        )
                                    }

                                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                                        Toast.makeText(context,"You should accept this permission",Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onPermissionRationaleShouldBeShown(
                                        permission: PermissionRequest?,
                                        token: PermissionToken?
                                    ) {

                                    }

                                }).check()

                        }

                    })
                )

                // rute
//                buffer.add(MyButton(context!!,
//                    "Directions",
//                    30,
//                    0,
//                    Color.parseColor("#9b0000"),
//                    object : IMyButtonCallback {
//                        override fun onClick(pos: Int) {
//
//                            val orderModel = (recyclerOrder.adapter as MyOrderAdapter)
//                                .getItemAtPosition(pos)
//                            if (orderModel.orderStatus == 1) //Shipping
//                            {
//                                Common.currentOrderSelected = orderModel
//                                startActivity(Intent(context!!, TrackingOrderActivity::class.java))
//                            }
//                            else
//                            {
//                                Toast.makeText(context!!,StringBuilder("Your order has been ")
//                                    .append(Common.convertStatusToString(orderModel.orderStatus))
//                                    .append(". So you can't track directions"),
//                                Toast.LENGTH_SHORT).show()
//                            }
//
//                        }
//
//                    })
//                )

                //telepon
//                buffer.add(MyButton(context!!,
//                    "Call",
//                    30,
//                    0,
//                    Color.parseColor("#560027"),
//                    object : IMyButtonCallback {
//                        override fun onClick(pos: Int) {
//
//                            Dexter.withContext(activity)
//                                .withPermission(Manifest.permission.CALL_PHONE)
//                                .withListener(object:PermissionListener{
//                                    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
//                                        val orderModel = adapter!!.getItemAtPosition(pos)
//                                        val intent = Intent()
//                                        intent.action = Intent.ACTION_DIAL
//                                        intent.data = Uri.parse(StringBuilder("tel: ")
//                                            .append(orderModel.userPhone).toString())
//                                        startActivity(intent)
//                                    }
//
//                                    override fun onPermissionRationaleShouldBeShown(
//                                        permission: PermissionRequest?,
//                                        token: PermissionToken?
//                                    ) {
//
//                                    }
//
//                                    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
//                                        Toast.makeText(context,"You must accept this permission "+response!!.permissionName,
//                                            Toast.LENGTH_SHORT).show()
//                                    }
//
//                                }).check()
//
//                        }
//
//                    })
//                )

                buffer.add(MyButton(context!!,
                    "Hapus",
                    30,
                    0,
                    Color.parseColor("#12005e"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {

                            val orderModel = adapter!!.getItemAtPosition(pos)
                            val builder = AlertDialog.Builder(context!!)
                                .setTitle("Hapus")
                                .setMessage("Ingin Hapus Pesanan?")
                                .setNegativeButton("Batal"){ dialogInterface, _ -> dialogInterface.dismiss() }
                                .setPositiveButton("Hapus"){ dialogInterface, _ ->
                                    FirebaseDatabase.getInstance()
                                        .getReference(Common.SHOP_REF)
                                        .child(Common.currentServerUser!!.shop!!)
                                        .child(Common.ORDER_REF)
                                        .child(orderModel.key!!)
                                        .removeValue()
                                        .addOnFailureListener {
                                            Toast.makeText(context!!,""+it.message,Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnSuccessListener {
                                            adapter!!.removeItem(pos)
                                            adapter!!.notifyItemRemoved(pos)
                                            updateTextCounter()
                                            dialogInterface.dismiss()
                                            Toast.makeText(context!!,"Pesanan Terhapus!",Toast.LENGTH_SHORT).show()

                                        }
                                }

                            val dialog = builder.create()
                            dialog.show()

                            val btnNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                            btnNegative.setTextColor(Color.LTGRAY)
                            val btnPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                            btnPositive.setTextColor(Color.RED)


                        }

                    })
                )

                buffer.add(MyButton(context!!,
                    "Update",
                    30,
                    0,
                    Color.parseColor("#333639"),
                    object : IMyButtonCallback {
                        override fun onClick(pos: Int) {

                            showEditDialog(adapter!!.getItemAtPosition(pos),pos)

                        }

                    })
                )
            }

        }

    }

    private fun showEditDialog(orderModel: OrderModel, pos: Int) {
        val layoutDialog: View?
        val builder: AlertDialog.Builder?

        var rdiShipping:RadioButton?=null
        var rdiCancelled:RadioButton?=null
        var rdiShipped:RadioButton?=null
        var rdiDelete:RadioButton?=null
        var rdiRestorePlaced:RadioButton?=null

        when (orderModel.orderStatus) {
            -1 -> {
                layoutDialog = LayoutInflater.from(requireContext())
                    .inflate(R.layout.layout_dialog_cancelled,null)


                builder = AlertDialog.Builder(requireContext())
                    .setView(layoutDialog)

                rdiDelete = layoutDialog.findViewById<View>(R.id.rdi_delete) as RadioButton
                rdiRestorePlaced = layoutDialog.findViewById<View>(R.id.rdi_restore_placed) as RadioButton

            }
            0 -> {
                layoutDialog = LayoutInflater.from(requireContext())
                    .inflate(R.layout.layout_dialog_shipping,null)
                recyclerShipper = layoutDialog.findViewById(R.id.recycler_shipper) as RecyclerView //Add when shipping order status == 0
                builder = AlertDialog.Builder(requireContext(),
                    android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
                    .setView(layoutDialog)

                rdiShipping = layoutDialog.findViewById<View>(R.id.rdi_shipping) as RadioButton
                rdiCancelled = layoutDialog.findViewById<View>(R.id.rdi_cancelled) as RadioButton
            }
            else -> {
                layoutDialog = LayoutInflater.from(requireContext())
                    .inflate(R.layout.layout_dialog_shipped,null)
                builder = AlertDialog.Builder(requireContext())
                    .setView(layoutDialog)

                rdiShipped = layoutDialog.findViewById<View>(R.id.rdi_shipped) as RadioButton
                rdiCancelled = layoutDialog.findViewById<View>(R.id.rdi_cancelled) as RadioButton
            }
        }

        //View
        val btnOk = layoutDialog.findViewById<View>(R.id.btn_ok) as Button
        val btnCancel = layoutDialog.findViewById<View>(R.id.btn_cancel) as Button






        val txtStatus = layoutDialog.findViewById<View>(R.id.txt_status) as TextView

        //Set data
//        txtStatus.text = StringBuilder("Order Status(")
//            .append(Common.convertStatusToString(orderModel.orderStatus))
//            .append(")")

        //Create Dialog
        val dialog = builder.create()

        if (orderModel.orderStatus == 0) //shipping
            loadShipperList(pos,orderModel,dialog,btnOk,btnCancel,
            rdiShipping,rdiShipped,rdiCancelled,rdiDelete,rdiRestorePlaced)
        else
            showDialog(pos,orderModel,dialog,btnOk,btnCancel,
                rdiShipping,rdiShipped,rdiCancelled,rdiDelete,rdiRestorePlaced)

       
    }

    private fun loadShipperList(
        pos: Int,
        orderModel: OrderModel,
        dialog: AlertDialog,
        btnOk: Button,
        btnCancel: Button,
        rdiShipping: RadioButton?,
        rdiShipped: RadioButton?,
        rdiCancelled: RadioButton?,
        rdiDelete: RadioButton?,
        rdiRestorePlaced: RadioButton?
    ) {
        val tempList:MutableList<ShipperModel> = ArrayList()
        val shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHOP_REF)
            .child(Common.currentServerUser!!.shop!!)
            .child(Common.SHIPPER_REF)
        val shipperActive = shipperRef.orderByChild("active").equalTo(true)
        shipperActive.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                shipperLoadCallbackListener.onShipperLoadFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (shipperSnapshot in p0.children)
                {
                    val shipperModel = shipperSnapshot.getValue(ShipperModel::class.java)!!
                    shipperModel.key = shipperSnapshot.key
                    tempList.add(shipperModel)
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(pos,
                orderModel,
                tempList,
                dialog,
                btnOk,
                btnCancel,
                rdiShipping,rdiShipped,rdiCancelled,rdiDelete,rdiRestorePlaced)
            }

        })
    }

    private fun showDialog(
        pos: Int,
        orderModel: OrderModel,
        dialog: AlertDialog,
        btnOk: Button,
        btnCancel: Button,
        rdiShipping: RadioButton?,
        rdiShipped: RadioButton?,
        rdiCancelled: RadioButton?,
        rdiDelete: RadioButton?,
        rdiRestorePlaced: RadioButton?
    ) {
        dialog.show()
        //Custom dialog
        btnCancel.setOnClickListener { dialog.dismiss() }
        btnOk.setOnClickListener {

            if (rdiCancelled != null && rdiCancelled.isChecked)
            {
                updateOrder(pos,orderModel,-1)
                dialog.dismiss()
            }
            else if (rdiShipping != null && rdiShipping.isChecked)
            {

                val shipperModel: ShipperModel?
                if (myShipperSelectedAdapter != null)
                {
                    shipperModel = myShipperSelectedAdapter!!.selectedShipper
                    if (shipperModel != null)
                    {
                        createShippingOrder(pos,shipperModel,orderModel,dialog)
                    }
                    else
                        Toast.makeText(context,"Pilih Pengantar",Toast.LENGTH_SHORT).show()
                }
            }
            else if (rdiShipped != null && rdiShipped.isChecked)
            {
                updateOrder(pos,orderModel,2)
                dialog.dismiss()
            }
            else if (rdiRestorePlaced != null && rdiRestorePlaced.isChecked)
            {
                updateOrder(pos,orderModel,0)
                dialog.dismiss()
            }
            else if (rdiDelete != null && rdiDelete.isChecked)
            {
                deleteOrder(pos,orderModel)
                dialog.dismiss()
            }
        }
    }

    private fun createShippingOrder(
        pos:Int,
        shipperModel: ShipperModel,
        orderModel: OrderModel,
        dialog: AlertDialog
    ) {
        val shippingOrder = ShippingOrderModel()
        shippingOrder.shopKey = Common.currentServerUser!!.shop!!
        shippingOrder.shipperName = shipperModel.name
        shippingOrder.shipperPhone = shipperModel.phone
        shippingOrder.orderModel = orderModel
        shippingOrder.isStartTrip = false
        shippingOrder.currentLat = -1.0
        shippingOrder.currentLng = -1.0
        FirebaseDatabase.getInstance()
            .getReference(Common.SHOP_REF)
            .child(Common.currentServerUser!!.shop!!)
            .child(Common.SHIPPING_ORDER_REF)
            .child(orderModel.key!!) //change push() to key()
            .setValue(shippingOrder)
            .addOnFailureListener { e:Exception ->
                dialog.dismiss()
                Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { task: Task<Void?> ->
                if(task.isSuccessful)
                {
                    dialog.dismiss()
                    //Load token
                    FirebaseDatabase.getInstance()
                        .getReference(Common.TOKEN_REF)
                        .child(shipperModel.key!!)
                        .addListenerForSingleValueEvent(object:ValueEventListener{
                            override fun onCancelled(p0: DatabaseError) {
                                dialog.dismiss()
                                Toast.makeText(context,""+p0.message,Toast.LENGTH_SHORT).show()
                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                if (p0.exists())
                                {
                                    val tokenModel = p0.getValue(TokenModel::class.java)
                                    val notifData = HashMap<String,String>()
                                    notifData[Common.NOTI_TITLE] = "kamu ada pesanan baru harus diantarkan"
                                    notifData[Common.NOTI_CONTENT] = StringBuilder("Your have new order need ship to ")
                                        .append(orderModel.userPhone).toString()

                                    val sendData = FCMSendData(tokenModel!!.token!!,notifData)

                                    compositeDisposable.add(
                                        iFcmService.sendNotification(sendData)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({ fcmResponse ->
                                                dialog.dismiss()
                                                if (fcmResponse.success == 1)
                                                {
                                                    updateOrder(pos,orderModel,1)
                                                }
                                                else
                                                {
                                                    // ganti sementara
//                                                    Toast.makeText(context,"Failed to send notification ! Order wasn't update",Toast.LENGTH_SHORT).show()
                                                    Toast.makeText(context,"Pesanan di Teruskan ke Pengantar",Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                                {t ->
                                                    dialog.dismiss()
                                                    Toast.makeText(context,""+t.message,Toast.LENGTH_SHORT).show()
                                                })
                                    )
                                }
                                else
                                {
                                    dialog.dismiss()
                                    Toast.makeText(context,"Token not found",Toast.LENGTH_SHORT).show()
                                }
                            }

                        })


                }
            }
    }

    private fun deleteOrder(pos: Int, orderModel: OrderModel) {
        if (!TextUtils.isEmpty(orderModel.key))
        {


            FirebaseDatabase.getInstance()
                .getReference(Common.SHOP_REF)
                .child(Common.currentServerUser!!.shop!!)
                .child(Common.ORDER_REF)
                .child(orderModel.key!!)
                .removeValue()
                .addOnFailureListener { throwable -> Toast.makeText(requireContext(),""+throwable.message,
                    Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener {
                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)
                    updateTextCounter()
                    Toast.makeText(requireContext(),"Update Order success!",
                        Toast.LENGTH_SHORT).show()
                }
        }
        else
        {
            Toast.makeText(requireContext(),"Nomor pesanan tidak boleh kosong",Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOrder(pos: Int, orderModel: OrderModel, status: Int) {
        if (!TextUtils.isEmpty(orderModel.key))
        {
            val updateData = HashMap<String,Any>()
            updateData["orderStatus"] = status

            FirebaseDatabase.getInstance()
                .getReference(Common.SHOP_REF)
                .child(Common.currentServerUser!!.shop!!)
                .child(Common.ORDER_REF)
                .child(orderModel.key!!)
                .updateChildren(updateData)
                .addOnFailureListener { throwable -> Toast.makeText(requireContext(),""+throwable.message,
                Toast.LENGTH_SHORT).show() }
                .addOnSuccessListener {

                    val dialog = SpotsDialog.Builder().setContext(requireContext()).setCancelable(false).build()
                    dialog.show()

                    //Load token
                    FirebaseDatabase.getInstance()
                        .getReference(Common.TOKEN_REF)
                        .child(orderModel.userId!!)
                        .addListenerForSingleValueEvent(object:ValueEventListener{
                            override fun onCancelled(p0: DatabaseError) {
                                dialog.dismiss()
                                Toast.makeText(context,""+p0.message,Toast.LENGTH_SHORT).show()
                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                if (p0.exists())
                                {
                                    val tokenModel = p0.getValue(TokenModel::class.java)
                                    val notifData = HashMap<String,String>()
                                    notifData[Common.NOTI_TITLE] = "Pesanan telah diupdate"
                                    notifData[Common.NOTI_CONTENT] = StringBuilder("Pesananmu ")
                                        .append(orderModel.key)
                                        .append(" di update ke ")
                                        .append(Common.convertStatusToString(status)).toString()

                                    val sendData = FCMSendData(tokenModel!!.token!!,notifData)

                                    compositeDisposable.add(
                                        iFcmService.sendNotification(sendData)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe({ fcmResponse ->
                                                dialog.dismiss()
                                                if (fcmResponse.success == 1)
                                                {
                                                    Toast.makeText(context,"Update order successfully",Toast.LENGTH_SHORT).show()
                                                }
                                                else
                                                {
//                                                    Toast.makeText(context,"Failed to send notification",Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                                {t ->
                                                    dialog.dismiss()
                                                    Toast.makeText(context,""+t.message,Toast.LENGTH_SHORT).show()
                                                })
                                    )
                                }
                                else
                                {
                                    dialog.dismiss()
                                    Toast.makeText(context,"Token not found",Toast.LENGTH_SHORT).show()
                                }
                            }

                        })

                    adapter!!.removeItem(pos)
                    adapter!!.notifyItemRemoved(pos)
                    updateTextCounter()

                }
        }
        else
        {
            Toast.makeText(requireContext(),"Nomor Pesanan tidak boleh kosong",Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTextCounter() {
        txt_order_filter.text = StringBuilder("Orders (")
            .append(adapter!!.itemCount)
            .append(")")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.order_list_menu,menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_filter) {
            val bottomSheet = BottomSheetOrderFragment.instance
            bottomSheet!!.show(requireActivity().supportFragmentManager,"OrderList")
            true
        }else if (item.itemId == R.id.action_manage) {

//            val executor = Executors.newSingleThreadExecutor()
//            val handler = Handler(Looper.getMainLooper())
//            executor.execute {
//                createExcel(createWorkbook())
//                Log.d("abcd", "1..")
//                handler.post {
//                    Toast.makeText(requireContext(), "data berhasil disimpan", Toast.LENGTH_SHORT)
//                        .show()
//
//                    getRow()    // todo nanti
//
//                    Log.d("abcd", "2..")
//                }
//                Log.d("abcd", "3..")
//            }
            createExcel(createWorkbook())
            Log.d("abcd", "4..")
            true
        } else
            super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent::class.java))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent::class.java)
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)

        compositeDisposable.clear()

        super.onStop()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(ChangeMenuClick(true))
        super.onDestroy()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onLoadOrder(event: LoadOrderEvent)
    {
        orderViewModel.loadOrder(event.status)
    }

    override fun onShipperLoadSuccess(shipperList: List<ShipperModel>) {
        //Do nothing
    }

    override fun onShipperLoadSuccess(
        pos: Int,
        orderModel: OrderModel?,
        shipperList: List<ShipperModel>?,
        dialog: AlertDialog?,
        ok: Button?,
        cancel: Button?,
        rdi_shipping: RadioButton?,
        rdi_shipped: RadioButton?,
        rdi_cancelled: RadioButton?,
        rdi_delete: RadioButton?,
        rdi_restore_placed: RadioButton?
    ) {
        if (recyclerShipper != null)
        {
            recyclerShipper!!.setHasFixedSize(true)
            val layoutManager = LinearLayoutManager(context)
            recyclerShipper!!.layoutManager = layoutManager
            recyclerShipper!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))
            myShipperSelectedAdapter = MyShipperSelectedAdapter(requireContext(),shipperList!!)
            recyclerShipper!!.adapter = myShipperSelectedAdapter
        }

        showDialog(pos,orderModel!!,dialog!!,ok!!,cancel!!,rdi_shipping,rdi_shipped,rdi_cancelled,rdi_delete,rdi_restore_placed)
    }

    override fun onShipperLoadFailed(message: String) {
        Toast.makeText(requireContext(),message,Toast.LENGTH_SHORT).show()
    }

}