package com.alfian.deliveryorderadmin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alfian.deliveryorderadmin.common.Common
import com.alfian.deliveryorderadmin.model.ServerUserModel
import com.firebase.ui.auth.AuthUI
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.layout_register.*
import java.util.*

// Untuk Login
class MainActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth? = null
    private var listener: FirebaseAuth.AuthStateListener? = null
    private var dialog: AlertDialog? = null
    private var serverRef:DatabaseReference? = null
    private var providers : List<AuthUI.IdpConfig>? = null

    companion object{
        private const val APP_REQUEST_CODE = 7171
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth!!.addAuthStateListener(listener!!)
    }

    override fun onStop() {
        firebaseAuth!!.removeAuthStateListener(listener!!)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        init()

    }

    private fun init() {
        providers = listOf(AuthUI.IdpConfig.PhoneBuilder().build(),
        AuthUI.IdpConfig.EmailBuilder().build())

        serverRef = FirebaseDatabase.getInstance().getReference(Common.SERVER_REF)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                checkServerUserFromFirebase(user)
            } else {
                phoneLogin()
            }
        }
    }

    private fun checkServerUserFromFirebase(user: FirebaseUser) {
        dialog!!.show()
        serverRef!!.child(user.uid)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    dialog!!.dismiss()
                    Toast.makeText(this@MainActivity,""+p0.message,Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists())
                    {
                        val userModel = dataSnapshot.getValue(ServerUserModel::class.java)
                        if (userModel!!.isActive)
                        {
                            goToHomeActivity(userModel)
                        }
                        else{
                            dialog!!.dismiss()
                            Toast.makeText(this@MainActivity,"Anda harus izin ke Admin",Toast.LENGTH_SHORT).show()

                        }
                    }
                    else{
                        dialog!!.dismiss()
                        showRegisterDialog(user)
                    }
                }

            })
    }

    private fun goToHomeActivity(userModel: ServerUserModel) {
        dialog!!.dismiss()
        Common.currentServerUser = userModel
        val myIntent = Intent(this,HomeActivity::class.java)
        var isOpenActivityNewOrder = false
        if (intent != null && intent.extras != null)
            isOpenActivityNewOrder = intent!!.extras!!.getBoolean(Common.IS_OPEN_ACTIVITY_NEW_ORDER,false)
        myIntent.putExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER,isOpenActivityNewOrder)
        startActivity(myIntent)
        finish()
    }

    private fun showRegisterDialog(user: FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Registrasi")
        builder.setMessage("Mohon Isi Data Anda")

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null)
        val phoneInputLayout = itemView.findViewById<View>(R.id.phone_input_layout) as TextInputLayout
        val edtName = itemView.findViewById<View>(R.id.edt_name) as EditText
        val edtPhone = itemView.findViewById<View>(R.id.edt_phone) as EditText

        //set data
        if (user.phoneNumber == null || TextUtils.isEmpty(user.phoneNumber))
        {
            phoneInputLayout.hint = "Email"
            edtPhone.setText(user.email)
            edtName.setText(user.displayName)
        }
        else
            edtPhone.setText(user.phoneNumber)

        builder.setNegativeButton("Batal") { dialogInterface, _ -> dialogInterface.dismiss() }
            .setPositiveButton("Registrasi") { _, _ ->
                if (TextUtils.isEmpty(edtName.text)) {
                    Toast.makeText(this, "Isi nama anda", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val serverUserModel = ServerUserModel()
                serverUserModel.uid = user.uid
                serverUserModel.name = edtName.text.toString()
                serverUserModel.phone = edtPhone.text.toString()
                serverUserModel.isActive = true   //Ganti false kalo mau cek

                dialog!!.show()
                serverRef!!.child(serverUserModel.uid!!)
                    .setValue(serverUserModel)
                    .addOnFailureListener { e ->
                        dialog!!.dismiss()
                        Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener {
                        dialog!!.dismiss()
                        Toast.makeText(
                            this,
                            "Registrasi Berhasil",
                            Toast.LENGTH_SHORT
                        ).show()
                        checkServerUserFromFirebase(user)
                    }
            }

        builder.setView(itemView)

        val registerDialog = builder.create()
        registerDialog.show()
    }

    private fun phoneLogin() {
        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers!!)
            .setTheme(R.style.LoginTheme)
            .setLogo(R.drawable.delivery_logo)
            .setIsSmartLockEnabled(false)   // beda
            .build(),APP_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_REQUEST_CODE)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                val user = FirebaseAuth.getInstance().currentUser
            }
            else
            {
                Toast.makeText(this,"Failed to sign in",Toast.LENGTH_SHORT).show()
            }
        }
    }
}
