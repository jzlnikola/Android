package elfak.mosis.medspot.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel: ViewModel() {

    private val _longitude = MutableLiveData<String>()
    val longitude: LiveData<String> = _longitude

    private val _latitude = MutableLiveData<String>()
    val latitude: LiveData<String> = _latitude

    private val _itemName = MutableLiveData<String>()
    val itemName: LiveData<String> = _itemName

    val _itemRating = MutableLiveData<String>()
    val itemRating: LiveData<String> = _itemRating

    private val _points = MutableLiveData<Int>()
    val points: LiveData<Int> = _points

    fun setLocation(lon: String, lat: String){
        _longitude.value = lon
        _latitude.value = lat
    }

    fun setItemName(name: String){
        _itemName.value = name
    }

    fun setItemRating(rating: String){
        _itemRating.value = rating
    }

    fun setPoints(points: Int){
        _points.value = points
    }
}