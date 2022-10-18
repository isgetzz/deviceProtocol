package com.cc.device.dialog

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatDialog
import com.cc.device.R
import com.cc.device.databinding.DialogSelectProtocolBinding

/**
 * @Author      : cc
 * @Date        : on 2022-10-17 17:57.
 * @Description :选择协议 ota
 */
class SelectDialog(context: Context, private val onClickListener: (String, String) -> Unit) :
    AppCompatDialog(context, R.style.DialogTheme),
    View.OnClickListener {
    private val viewBind = DialogSelectProtocolBinding.inflate(layoutInflater)

    init {
        setContentView(viewBind.root)
        viewBind.btAffirm.setOnClickListener(this)
        setCanceledOnTouchOutside(false)
        setCancelable(false)
    }


    override fun onClick(v: View) {
        if (v.id == viewBind.btAffirm.id) {
            onClickListener("1", "1")
            dismiss()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dismiss()
    }
}