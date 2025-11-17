package elfak.mosis.medspot.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import android.preference.PreferenceManager
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.medspot.R
import elfak.mosis.medspot.helpers.CustomInfoWindow
import elfak.mosis.medspot.models.FilterItemsViewModel
import elfak.mosis.medspot.models.LocationViewModel
import elfak.mosis.medspot.models.data.Item
import elfak.mosis.medspot.models.data.MarkerItem
import elfak.mosis.medspot.models.data.UsageItem
import elfak.mosis.medspot.models.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlin.math.log
import com.google.firebase.Timestamp

class MapFragment : Fragment() {

    private lateinit var map: MapView
    private val locationViewModel: LocationViewModel by activityViewModels()
    private val filterViewModel: FilterItemsViewModel by activityViewModels()
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private var auth : FirebaseAuth = Firebase.auth
    private var db = Firebase.firestore
    private lateinit var collectBtn: Button
    private lateinit var ratingBar: RatingBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(filterViewModel.flag.value == "yes"){
            getAllMarkers()
        }
        if(filterViewModel.items.value == null){
            getAllMarkers()
        }

        map = requireView().findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val ctx: Context? = requireActivity().applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences((ctx!!)))

        map.setMultiTouchControls(true)
        if(ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissionlauncher.launch(
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        else{
            setMyLoactionOverlay()
        }


        val startPoint: GeoPoint = if (!::myLocationOverlay.isInitialized || myLocationOverlay.myLocation == null)
            GeoPoint(43.3209, 21.8958)
        else
            GeoPoint(myLocationOverlay.myLocation.latitude, myLocationOverlay.myLocation.longitude)

        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        val mRotationGestureOverlay = RotationGestureOverlay(context, map)
        mRotationGestureOverlay.isEnabled = true
        map.setMultiTouchControls(true)
        map.overlays.add(mRotationGestureOverlay)

        val fabAdd: FloatingActionButton = requireView().findViewById(R.id.fab_add)
        fabAdd.setOnClickListener{addItem()}
        val fabFilter: FloatingActionButton = requireView().findViewById(R.id.fab_filter)
        fabFilter.setOnClickListener{filterItem()}

        val nameObserver = Observer<String> { newValue ->
            if(newValue != "no"){
                val marker = Marker(map)
                val point =
                    locationViewModel.latitude.value?.toDouble()
                        ?.let { locationViewModel.longitude.value?.toDouble()
                            ?.let { it1 -> GeoPoint(it, it1) } }
                marker.position = point
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                map.overlays.add(marker)
                marker.title = locationViewModel.itemName.value
                marker.snippet = locationViewModel.itemRating.value

                var drawableImage: Int? = null
                when(locationViewModel.itemName.value){
                    "Defibrilator" -> drawableImage = R.drawable.defibrilator_marker
                    "EpiPen" -> drawableImage = R.drawable.epipen_marker
                    "EKG uređaj" -> drawableImage = R.drawable.ekg_marker
                    "Pulsni oksimetar" -> drawableImage = R.drawable.oksimetar_marker
                    "Inhalator" -> drawableImage = R.drawable.inhalator_marker
                    "Aparat za pritisak" -> drawableImage = R.drawable.pritisak_marker
                    "Aparat za merenje šećera" -> drawableImage = R.drawable.secer_marker
                    "Prva pomoć" -> drawableImage = R.drawable.pp_marker
                    "Zavoji" -> drawableImage = R.drawable.zavoji_marker
                    "Toplomer" -> drawableImage = R.drawable.toplomer_marker
                }
                val image: Drawable? = ResourcesCompat.getDrawable(resources, drawableImage!!, null)
                marker.icon = image

                marker.infoWindow = CustomInfoWindow(map)

                collectBtn  = marker.infoWindow.view.findViewById(R.id.bubble_moreinfo)
                collectBtn.visibility = View.GONE

                val hash = GeoFireUtils.getGeoHashForLocation(
                    GeoLocation(
                        locationViewModel.latitude.value!!.toDouble(),
                        locationViewModel.longitude.value!!.toDouble()))

                val m = MarkerItem(locationViewModel.itemName.value,
                    locationViewModel.longitude.value,
                    locationViewModel.latitude.value,
                    auth.currentUser?.email,
                    locationViewModel.points.value,
                    0.0,
                    0,
                    LocalDate.now().toString(),
                    hash,
                    null)

                db.collection("markers").add(m)
                    .addOnSuccessListener {
                        Toast.makeText(view.context, "Successfully posted!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener{
                        Toast.makeText(view.context, "Failed!", Toast.LENGTH_SHORT).show()
                    }
                locationViewModel.setItemName("no")
            }
        }
        locationViewModel.itemName.observe(viewLifecycleOwner, nameObserver)

        locationViewModel.itemRating.observe(viewLifecycleOwner) { markerId ->
            if (markerId != null) {
                refreshMarkerRating(markerId)
            }
        }

        val itemsObserver = Observer<ArrayList<MarkerItem>> {newValue ->
            if(newValue.isNotEmpty()){
                removeAllMarkers()
                for(m in filterViewModel.items.value!!){
                    val marker = Marker(map)
                    marker.id = m.id
                    val point = m.latitude?.let { it1 -> m.longitude?.let { it2 -> GeoPoint(it1.toDouble(), it2.toDouble()) } }
                    marker.position = point
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    map.overlays.add(marker)
                    marker.title = m.name

                    val avgRating = m.rating
                    marker.snippet =
                        if (avgRating == null || avgRating == 0.0)
                            "⭐ Not rated"
                        else
                            "⭐ ${String.format("%.1f", avgRating)}"

                    var drawableImage: Int? = null
                    when(m.name){
                        "Defibrilator" -> drawableImage = R.drawable.defibrilator_marker
                        "EpiPen" -> drawableImage = R.drawable.epipen_marker
                        "EKG uređaj" -> drawableImage = R.drawable.ekg_marker
                        "Pulsni oksimetar" -> drawableImage = R.drawable.oksimetar_marker
                        "Inhalator" -> drawableImage = R.drawable.inhalator_marker
                        "Aparat za pritisak" -> drawableImage = R.drawable.pritisak_marker
                        "Aparat za merenje šećera" -> drawableImage = R.drawable.secer_marker
                        "Prva pomoć" -> drawableImage = R.drawable.pp_marker
                        "Zavoji" -> drawableImage = R.drawable.zavoji_marker
                        "Toplomer" -> drawableImage = R.drawable.toplomer_marker
                    }
                    val image: Drawable? = ResourcesCompat.getDrawable(resources, drawableImage!!, null)
                    marker.icon = image

                    marker.infoWindow = CustomInfoWindow(map)

                    collectBtn  = marker.infoWindow.view.findViewById(R.id.bubble_moreinfo)
                    if(m.user == auth.currentUser?.email){
                        collectBtn.visibility = View.GONE
                    }
                    collectBtn.setOnClickListener{collectItem(m, marker)}
                }
                filterViewModel.setItems(ArrayList())
                filterViewModel.setFlag("yes")
            }
        }
        filterViewModel.items.observe(viewLifecycleOwner, itemsObserver)
    }

    private fun getAllMarkers(){
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    db.collection("markers")
                        .get()
                        .await()
                }

                if(result != null){
                    for(d in result.documents){
                        val marker = Marker(map)
                        marker.id = d.id
                        val point = GeoPoint(d.get("latitude").toString().toDouble(), d.get("longitude").toString().toDouble())
                        marker.position = point
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        map.overlays.add(marker)
                        marker.title = d.get("name").toString()

                        val avgRating = d.getDouble("rating")
                        marker.snippet =
                            if (avgRating == null || avgRating == 0.0)
                                "⭐ Not rated"
                            else
                                "⭐ ${String.format("%.1f", avgRating)}"

                        var drawableImage: Int? = null
                        when(d.get("name").toString()){
                            "Defibrilator" -> drawableImage = R.drawable.defibrilator_marker
                            "EpiPen" -> drawableImage = R.drawable.epipen_marker
                            "EKG uređaj" -> drawableImage = R.drawable.ekg_marker
                            "Pulsni oksimetar" -> drawableImage = R.drawable.oksimetar_marker
                            "Inhalator" -> drawableImage = R.drawable.inhalator_marker
                            "Aparat za pritisak" -> drawableImage = R.drawable.pritisak_marker
                            "Aparat za merenje šećera" -> drawableImage = R.drawable.secer_marker
                            "Prva pomoć" -> drawableImage = R.drawable.pp_marker
                            "Zavoji" -> drawableImage = R.drawable.zavoji_marker
                            "Toplomer" -> drawableImage = R.drawable.toplomer_marker
                        }
                        val image: Drawable? = ResourcesCompat.getDrawable(resources, drawableImage!!, null)
                        marker.icon = image

                        marker.infoWindow = CustomInfoWindow(map)

                        collectBtn  = marker.infoWindow.view.findViewById(R.id.bubble_moreinfo)
                        if(d.get("user").toString() == auth.currentUser?.email){
                            collectBtn.visibility = View.GONE
                        }
                        val m = MarkerItem(d.get("name").toString(), d.get("longitude").toString(),
                            d.get("latitude").toString(), d.get("user").toString(), d.get("points").toString().toInt(), d.get("rating").toString().toDouble(),
                            d.get("ratingCount").toString().toInt(), d.get("dateCreated").toString(), d.get("hash").toString(), d.id)
                        collectBtn.setOnClickListener{collectItem(m, marker)}
                    }
                }
            }
            catch (e: Exception) {
                Log.w("MARKER", "ERROR", e)
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun collectItem(item: MarkerItem, marker: Marker){

        if(myLocationOverlay.myLocation == null)
            return

        val myLoc =  GeoLocation(myLocationOverlay.myLocation.latitude, myLocationOverlay.myLocation.longitude)
        val itemLat = item.latitude!!.toDouble()
        val itemLong = item.longitude!!.toDouble()
        val itemLoc = GeoLocation(itemLat, itemLong)
        val radius = 3.0
        val distance = GeoFireUtils.getDistanceBetween(itemLoc, myLoc)

        Log.d("DISTANCE", distance.toString())
        if(distance <= radius){
            val itemId = item.id
            val itemPoints = item.points
            val usedItem = UsageItem(item.name,
                itemId,
                Timestamp.now()
            )


            db.collection("usageHistory").document(auth.currentUser!!.uid).collection("items").add(usedItem)
                .addOnSuccessListener { Log.d("MARKER", "Success writing into collectedItems") }
                .addOnFailureListener{ Log.d("MARKER", "Fail writing into collectedItems") }

            val userID: String = auth.currentUser?.uid ?: ""
            var userPoints: Int?
            val database = Firebase.database("https://nijo-medspot-id-default-rtdb.europe-west1.firebasedatabase.app/")
                .reference.child("users").child(userID)

            database.get()
                .addOnSuccessListener { result ->
                    val user = result.getValue<User>()
                    userPoints = itemPoints?.let { user?.points?.plus(it) }
                    database.child("points").setValue(userPoints)
                }
                .addOnFailureListener{
                    Log.d("USER", "Fail to update!") }

            Toast.makeText(view?.context, "Collected!", Toast.LENGTH_SHORT).show()
            marker.closeInfoWindow()
            RatingDialog(marker.id).show(
                (map.context as AppCompatActivity).supportFragmentManager,
                "ratingDialog"
            )

        }
        else{
            Toast.makeText(view?.context, "You need to get closer!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeAllMarkers() {
        val markers = map.overlays.filterIsInstance<Marker>().toMutableList()
        markers.forEach { m -> map.overlays.remove(m) }
        map.invalidate()
    }

    private fun addItem(){
        val loc = myLocationOverlay.myLocation
        if(loc != null){
            locationViewModel.setLocation(loc.longitude.toString(), loc.latitude.toString())
            AddFragment().show(childFragmentManager, "Add item dialog")
        }
        else{
            Toast.makeText(view?.context, "Turn on location!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterItem(){
        val loc = myLocationOverlay.myLocation
        if(loc != null){
            locationViewModel.setLocation(loc.longitude.toString(), loc.latitude.toString())
            findNavController().navigate(R.id.action_MapFragment_to_FiltersFragment)
        }
        else{
            Toast.makeText(view?.context, "Turn on location!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshMarkerRating(markerId: String) {
        val ref = Firebase.firestore.collection("markers").document(markerId)

        ref.get().addOnSuccessListener { doc ->
            val avgRating = doc.getDouble("rating") ?: 0.0

            val newSnippet = if (avgRating == 0.0) {
                "Not rated"
            } else {
                "Rating: ${String.format("%.1f", avgRating)} ★"
            }

            // Pronađi odgovarajući marker
            for (overlay in map.overlays) {
                if (overlay is Marker && overlay.id == markerId) {
                    overlay.snippet = newSnippet
                    overlay.showInfoWindow() // odmah osveži prikaz
                    break
                }
            }
        }
    }

    private fun setMyLoactionOverlay(){
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(activity), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)
    }

    private val requestPermissionlauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ){ isGranted: Boolean ->
            if(isGranted){
                setMyLoactionOverlay()
            }
        }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}