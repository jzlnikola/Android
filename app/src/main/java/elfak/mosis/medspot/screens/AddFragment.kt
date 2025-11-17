package elfak.mosis.medspot.screens

import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.medspot.R
import elfak.mosis.medspot.models.LocationViewModel
import elfak.mosis.medspot.models.data.Item
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddFragment : DialogFragment() {

    private var auth: FirebaseAuth = Firebase.auth
    private var db = Firebase.firestore
    private var itemName: String = ""
    private val locationViewModel: LocationViewModel by activityViewModels()
    private lateinit var postBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getReceivedItems()

        postBtn = requireView().findViewById(R.id.button_post)
        postBtn.setOnClickListener{postItem()}
        postBtn.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
        else{
            dialog?.window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun getReceivedItems() {
        val userID: String = auth.currentUser?.uid ?: ""
        viewLifecycleOwner.lifecycleScope.launch {

            val names = listOf(
                "Prva pomoć",
                "Defibrilator",
                "Zavoji",
                "EKG uređaj",
                "EpiPen",
                "Aparat za merenje šećera",
                "Aparat za pritisak",
                "Pulsni oksimetar",
                "Inhalator",
                "Toplomer",
            )

            val spinner: AutoCompleteTextView = requireView().findViewById(R.id.spinner)
            spinner.isEnabled = false
            val adapter = ArrayAdapter(requireView().context, android.R.layout.simple_spinner_dropdown_item, names as List<Any?>)
            spinner.setAdapter(adapter)
            spinner.setOnItemClickListener { adapterview, view, i, l ->
                val txtInputLayoutSpinner: TextInputLayout = requireView().findViewById(R.id.textInputLayoutSpinner)
                itemName = adapterview.getItemAtPosition(i).toString()
            }
            spinner.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(text: Editable?) {
                    postBtn.isEnabled = text.toString() != ""
                }
            })
        }
    }

    private fun postItem(){
        val prioriyPoints = when(itemName) {
            "Defibrilator" -> 30
            "EpiPen" -> 30
            "EKG uređaj" -> 20
            "Pulsni oksimetar" -> 20
            "Inhalator" -> 20
            "Aparat za pritisak" -> 15
            "Aparat za merenje šećera" -> 15
            "Prva pomoć" -> 5
            "Zavoji" -> 5
            "Toplomer" -> 3

            else -> 0
        }

        val hash = GeoFireUtils.getGeoHashForLocation(
            GeoLocation(locationViewModel.latitude.value!!.toDouble(), locationViewModel.longitude.value!!.toDouble())
        )

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())

        val itemData = hashMapOf(
            "name" to itemName,
            "points" to prioriyPoints,
            "rating" to 0.0,
            "ratingCount" to 0,
            "latitude" to locationViewModel.latitude.value,
            "longitude" to locationViewModel.longitude.value,
            "user" to auth.currentUser?.uid,
            "dateCreated" to today,
            "hash" to hash
        )

        Toast.makeText(requireContext(), "Posting...", Toast.LENGTH_SHORT).show()
        db.collection("markers")
            .add(itemData)
            .addOnSuccessListener {
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Greška pri dodavanju!", Toast.LENGTH_SHORT).show()
            }

        prioriyPoints?.let { locationViewModel.setPoints(it) }
        itemName?.let { locationViewModel.setItemName(it) }
        Toast.makeText(requireContext(), "Aparat uspešno dodat!", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}