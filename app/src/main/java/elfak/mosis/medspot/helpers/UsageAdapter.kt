package elfak.mosis.medspot.helpers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import elfak.mosis.medspot.R
import elfak.mosis.medspot.models.data.UsageItem
import java.text.SimpleDateFormat
import java.util.Locale

class UsageAdapter(private val items: List<UsageItem>) :
    RecyclerView.Adapter<UsageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.itemName)
        val time: TextView = view.findViewById(R.id.itemTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.usage_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        val item = items[pos]
        holder.name.text = item.name

        val time = item.timestamp?.toDate()

        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val formatted = formatter.format(time)

        holder.time.text = formatted
    }

    override fun getItemCount() = items.size
}