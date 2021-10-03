package com.example.kotlineatitv2server.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.kotlineatitv2server.R
import com.example.kotlineatitv2server.model.eventbus.LoadOrderEvent
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_order_filter.*
import org.greenrobot.eventbus.EventBus

class BottomSheetOrderFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        placed_filter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(0))
            dismiss()
        }
        shipping_filter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(1))
            dismiss()
        }
        shipped_filter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(2))
            dismiss()
        }
        cancelled_filter.setOnClickListener {
            EventBus.getDefault().postSticky(LoadOrderEvent(-1))
            dismiss()
        }
    }

    companion object{
        val instance:BottomSheetOrderFragment?=null
        get() = field ?: BottomSheetOrderFragment()
    }

}