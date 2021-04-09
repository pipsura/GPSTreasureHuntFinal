package com.example.gpstreasurehunt

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.gpstreasurehunt.models.WaypointModel

class WaypointFragment : DialogFragment() {


    lateinit var customView: View;

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Simply return the already inflated custom view
        var latText = customView.findViewById<TextView>(R.id.Latitude)
        var longText = customView.findViewById<TextView>(R.id.Longitude)
        val argsParam = "waypoint"
        if (arguments != null) {
            var model = requireArguments().getParcelable<WaypointModel>(argsParam)
            val latStr = model!!.getLatitude().toString()
            val longStr = model!!.getLongitude().toString()
            latText!!.text = latStr
            longText!!.text = longStr
        }
        return customView
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = requireActivity().layoutInflater.inflate(R.layout.waypoint_fragment, null)
        return AlertDialog.Builder(requireContext()).setView(customView).create()
    }


}
/*

 */