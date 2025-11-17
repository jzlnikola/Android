package elfak.mosis.medspot.helpers

import android.widget.Button
import elfak.mosis.medspot.R
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow


class CustomInfoWindow(mapView: MapView) : MarkerInfoWindow(R.layout.bubble, mapView) {

    private lateinit var collectBtn: Button

    override fun onOpen(item: Any?) {
        super.onOpen(item)
        val marker: Marker = item as Marker
        collectBtn  = marker.infoWindow.view.findViewById(R.id.bubble_moreinfo)
        //Log.d("BUTTON", collectBtn.toString())
    }
}