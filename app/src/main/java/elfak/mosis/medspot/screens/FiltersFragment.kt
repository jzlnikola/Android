package elfak.mosis.medspot.screens

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.medspot.R
import elfak.mosis.medspot.models.FilterItemsViewModel
import elfak.mosis.medspot.models.LocationViewModel
import elfak.mosis.medspot.models.data.MarkerItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FiltersFragment : Fragment() {
    private var valuesDate: ArrayList<String> = arrayListOf("", "")
    private lateinit var etRadius: EditText
    private lateinit var spinnerType: AutoCompleteTextView
    private lateinit var txtInputLayoutSpinner: TextInputLayout
    private lateinit var fromDateBtn: Button
    private lateinit var toDateBtn: Button
    private lateinit var filterBtn: Button
    private lateinit var etFromDate: EditText
    private lateinit var etToDate: EditText
    private var names: ArrayList<String> = arrayListOf(
        "Prva pomoć",
        "Defibrilator",
        "Zavoji",
        "EKG uređaj",
        "EpiPen",
        "Aparat za merenje šećera",
        "Aparat za pritisak",
        "Pulsni oksimetar",
        "Inhalator",
        "Toplomer"
    )
    private var itemName: String = ""
    private var db = Firebase.firestore
    private var markers: ArrayList<MarkerItem> = arrayListOf()
    private val filterViewModel: FilterItemsViewModel by activityViewModels()
    private val locationViewModel: LocationViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_filters, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etRadius = requireView().findViewById(R.id.editText_search_radius)
        spinnerType = requireView().findViewById(R.id.spinner)
        fromDateBtn = requireView().findViewById(R.id.button_from_date)
        toDateBtn = requireView().findViewById(R.id.button_to_date)
        filterBtn = requireView().findViewById(R.id.button_filter)
        etFromDate = requireView().findViewById(R.id.editText_from_date)
        etToDate = requireView().findViewById(R.id.editText_to_date)

        val adapter = ArrayAdapter(
            requireView().context,
            android.R.layout.simple_spinner_dropdown_item,
            names as List<Any?>
        )
        spinnerType.setAdapter(adapter)
        spinnerType.setOnItemClickListener { adapterview, view, i, l ->
            itemName = adapterview.getItemAtPosition(i).toString()
        }

        fromDateBtn.setOnClickListener { openDateDialog(true) }
        toDateBtn.setOnClickListener { openDateDialog(false) }
        addListenerDate(etFromDate)
        addListenerDate(etToDate)
        filterBtn.setOnClickListener { filterItems() }
    }

    private fun addListenerDate(o: EditText) {
        o.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                text: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(text: Editable?) {
                if (text.toString() != "") {
                    when (o) {
                        etFromDate -> valuesDate[0] = text.toString()
                        etToDate -> valuesDate[1] = text.toString()
                    }
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openDateDialog(from: Boolean) {
        val currentDate = LocalDateTime.now()
        if (from) {
            val dateListener = OnDateSetListener { _, year, month, day ->
                val m = month + 1
                val d = "$year-$m-$day"
                etFromDate.text = d.toEditable()
            }
            val dialog = DatePickerDialog(
                requireView().context, R.style.DialogTheme, dateListener,
                currentDate.year, currentDate.monthValue - 1, currentDate.dayOfMonth
            )
            dialog.show()
        } else {
            val dateListener = OnDateSetListener { _, year, month, day ->
                val m = month + 1
                val d = "$year-$m-$day"
                etToDate.text = d.toEditable()
            }
            val dialog = DatePickerDialog(
                requireView().context, R.style.DialogTheme, dateListener,
                currentDate.year, currentDate.monthValue - 1, currentDate.dayOfMonth
            )
            dialog.show()
        }
    }

    private fun filterItems() {

        var query: Query = db.collection("markers")

        val dates = ArrayList<String>()
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var date1: Date? = null
        var date2: Date? = null
        val today = Date()
        try {
            date1 = input.parse(valuesDate[0])
            date2 = input.parse(valuesDate[1])
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        if(date1!=null) {

            val cal1 = Calendar.getInstance()
            cal1.time = date1
            if (date2 == null)
                date2 = today

            val cal2 = Calendar.getInstance()
            cal2.time = date2

            while (!cal1.after(cal2)) {
                val output = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dates.add(output.format(cal1.time))
                cal1.add(Calendar.DATE, 1)
            }

            query = query.whereIn("dateCreated", dates)
        }

        val radiusText = etRadius.text.toString().trim()
        val radius = radiusText.toDoubleOrNull()

        if(radius!=null)
        {
             val myLoc = GeoLocation(
                locationViewModel.latitude.value!!.toDouble(),
                locationViewModel.longitude.value!!.toDouble()
            )

            val bounds = GeoFireUtils.getGeoHashQueryBounds(myLoc, radius)
            val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
            for (b in bounds) {
                val q = db.collection("markers")
                    .orderBy("hash")
                    .startAt(b.startHash)
                    .endAt(b.endHash)
                tasks.add(q.get())
            }

            Tasks.whenAllComplete(tasks)
                .addOnCompleteListener {
                    val matchingDocs: MutableList<DocumentSnapshot> = ArrayList()
                    for (task in tasks) {
                        val snap = task.result
                        Toast.makeText(requireContext(), "Kockica vratila: ${snap!!.size()} dokumenata", Toast.LENGTH_LONG).show()
                        for (doc in snap!!.documents) {
                            val lat = doc.get("latitude").toString().toDouble()
                            val lng = doc.get("longitude").toString().toDouble()

                            val docLocation = GeoLocation(lat, lng)
                            val distanceInM =
                                GeoFireUtils.getDistanceBetween(docLocation, myLoc)
                            if (distanceInM <= radius) {
                                matchingDocs.add(doc)
                            }


                        }
                    }

                    var filtered = matchingDocs.toList()

                    if (itemName.isNullOrBlank().not()) {
                        filtered = filtered.filter { doc ->
                            doc.getString("name") == itemName
                        }
                    }

                    if (date1 != null && dates.isNotEmpty()) {
                        filtered = filtered.filter { doc ->
                            val d = doc.getString("dateCreated")
                            d != null && dates.contains(d)
                        }
                    }

                    for (d in filtered) {
                        val marker = MarkerItem(
                            d.get("name").toString(),
                            d.get("longitude").toString(),
                            d.get("latitude").toString(),
                            d.get("user").toString(),
                            d.get("points").toString().toInt(),
                            d.get("rating").toString().toDouble(),
                            d.get("ratingCount").toString().toInt(),
                            d.get("dateCreated").toString(),
                            d.get("hash").toString(),
                            d.id
                        )
                        markers.add(marker)
                    }
                    filterViewModel.setItems(markers)
                    filterViewModel.setFlag("no")
                    markers.clear()
                    findNavController().navigate(R.id.action_FiltersFragment_to_MapFragment)
                }
        }
        else
        {
            if(itemName.isNullOrBlank() == false)
            {
                query = query.whereEqualTo("name", itemName)
            }

            if(date1!=null) {
                query = query.whereIn("dateCreated", dates)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val result = query.get().await()

                if (result != null) {
                    for (d in result.documents) {
                        val marker = MarkerItem(
                            d.get("name").toString(),
                            d.get("longitude").toString(),
                            d.get("latitude").toString(),
                            d.get("user").toString(),
                            d.get("points").toString().toInt(),
                            d.get("rating").toString().toDouble(),
                            d.get("ratingCount").toString().toInt(),
                            d.get("dateCreated").toString(),
                            d.get("hash").toString(),
                            d.id
                        )
                        markers.add(marker)
                    }
                    filterViewModel.setItems(markers)
                    filterViewModel.setFlag("no")
                    markers.clear()
                    findNavController().navigate(R.id.action_FiltersFragment_to_MapFragment)
                }
            }
        }
    }
}

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)