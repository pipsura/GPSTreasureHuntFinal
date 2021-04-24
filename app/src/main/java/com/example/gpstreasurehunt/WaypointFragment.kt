package com.example.gpstreasurehunt

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.icu.math.BigDecimal
import android.icu.math.BigDecimal.ROUND_CEILING
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
import java.math.RoundingMode

/**
 * WaypointFragment.kt
 * Version 1.0
 * Last modified: 24/04/21
 * @author William Harvey
 *
 *
 * Class for the pop-up dialog fragment when a waypoint is clicked.
 * Implements DialogFragment.
 */
class WaypointFragment : DialogFragment() {

    /**
     * Sets up an interface so data can be returned back to the main activity
     *
     */
    interface OnInputListener {
        fun sendInput(input: Boolean, model: WaypointModel)
    }
    var onInputListener: OnInputListener? = null

    lateinit var customView: View

    /**
     * Sets up the dialog when it is created.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return The view of the dialog
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val latText = customView.findViewById<TextView>(R.id.Latitude)
        val longText = customView.findViewById<TextView>(R.id.Longitude)
        val digButton = customView.findViewById<Button>(R.id.digButton)
        val argsParam = "waypoint"
        if (arguments != null) {
            //Setting the TextViews text as the lat and long from the WapointModel that was parsed as args
            val model = requireArguments().getParcelable<WaypointModel>(argsParam)
            val latStr = (BigDecimal(model!!.getLatitude()).setScale(2, ROUND_CEILING)).toString()
            val longStr =(BigDecimal(model.getLongitude()).setScale(2, ROUND_CEILING)).toString()
            latText!!.text = latStr
            longText!!.text = longStr
        }

        //Setting the action for when the user presses dig
        digButton.setOnClickListener {
            var model: WaypointModel? = null
            dismiss()
            if (arguments != null) {
                //Starts the treasure hunt functions in MainActivity
                model = requireArguments().getParcelable<WaypointModel>(argsParam)!!
                onInputListener?.sendInput(true, model)
            }
        }
        return customView
    }

    /**
     * When the dialog is created set the view.
     *
     * @param savedInstanceState - Arguments
     * @return The dialog fragment displayed
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        customView = requireActivity().layoutInflater.inflate(R.layout.waypoint_fragment, null)
        return AlertDialog.Builder(requireContext()).setView(customView).create()
    }

    /**
     * Sets up the button listener to return data.
     *
     * @param context
     */
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