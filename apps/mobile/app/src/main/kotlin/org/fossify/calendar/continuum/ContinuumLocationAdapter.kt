package org.fossify.calendar.continuum

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import org.fossify.calendar.R

/** History plus live map suggestions as the user types in the location field. */
class ContinuumLocationAdapter(
    context: Context,
    private val history: List<String>,
) : ArrayAdapter<String>(context, R.layout.item_dropdown, ArrayList()) {

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val q = constraint?.toString()?.trim().orEmpty()
            val hist = if (q.isEmpty()) {
                history.take(8)
            } else {
                history.filter { it.contains(q, ignoreCase = true) }
            }
            val remote = if (q.length >= 2) {
                try {
                    ContinuumLocationSearch.suggest(context, q)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            val merged = LinkedHashSet<String>(hist.size + remote.size).apply {
                addAll(hist)
                addAll(remote)
            }.toList().take(12)
            return FilterResults().apply {
                values = merged
                count = merged.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            setNotifyOnChange(false)
            clear()
            @Suppress("UNCHECKED_CAST")
            addAll(results?.values as? List<String> ?: emptyList())
            notifyDataSetChanged()
        }
    }
}
