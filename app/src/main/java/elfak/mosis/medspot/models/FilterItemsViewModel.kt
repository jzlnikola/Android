package elfak.mosis.medspot.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import elfak.mosis.medspot.models.data.MarkerItem

class FilterItemsViewModel: ViewModel() {

    private val _items = MutableLiveData<ArrayList<MarkerItem>>()
    val items: LiveData<ArrayList<MarkerItem>> = _items

    private val _flag = MutableLiveData<String>()
    val flag: LiveData<String> = _flag

    fun setItems(items: ArrayList<MarkerItem>){
        _items.value = ArrayList(items)
    }

    fun setFlag(f: String){
        _flag.value = f
    }
}