package com.example.gpstreasurehunt

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.gpstreasurehunt.models.WaypointModel
import kotlinx.android.synthetic.main.activity_main.view.*


class WaypointFragment : DialogFragment() {

    interface OnInputListener {
        fun sendInput(input: Boolean, model: WaypointModel)
    }
    var onInputListener: OnInputListener? = null

    lateinit var customView: View;

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Simply return the already inflated custom view
        var latText = customView.findViewById<TextView>(R.id.Latitude)
        var longText = customView.findViewById<TextView>(R.id.Longitude)
        var digButton = customView.findViewById<Button>(R.id.digButton)
        val argsParam = "waypoint"
        if (arguments != null) {
            var model = requireArguments().getParcelable<WaypointModel>(argsParam)
            val latStr = model!!.getLatitude().toString()
            val longStr = model!!.getLongitude().toString()
            latText!!.text = latStr
            longText!!.text = longStr
        }

        digButton.setOnClickListener {
            var model: WaypointModel? = null
            dismiss()
            if (arguments != null) {
                model = requireArguments().getParcelable<WaypointModel>(argsParam)!!
                onInputListener?.sendInput(true, model)
            }
        }
        return customView
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = requireActivity().layoutInflater.inflate(R.layout.waypoint_fragment, null)
        return AlertDialog.Builder(requireContext()).setView(customView).create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onInputListener = activity as OnInputListener?
        } catch (e: ClassCastException) {
            Log.e(TAG, "onAttach: " + e.message)
        }
    }


}
/*

 */