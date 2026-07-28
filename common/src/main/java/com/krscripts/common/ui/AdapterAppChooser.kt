package com.krscripts.common.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.krscripts.common.R
import kotlinx.coroutines.*
import java.util.*
import java.util.Locale.getDefault

class AdapterAppChooser(
        private val context: Context,
        private var apps: ArrayList<AppInfo>,
        private val multiple: Boolean
) : BaseAdapter(), Filterable {
    interface SelectStateListener {
        fun onSelectChange(selected: List<AppInfo>)
    }

    open class AppInfo {
        var appName: String = ""
        var packageName: String = ""

        // 是否未找到此应用
        var notFound: Boolean = false
        var selected: Boolean = false
    }

    private var selectStateListener: SelectStateListener? = null
    private var filter: Filter? = null
    internal var filterApps: ArrayList<AppInfo> = apps
    private val mLock = Any()

    private class ArrayFilter(private var adapter: AdapterAppChooser) : Filter() {
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            @Suppress("UNCHECKED_CAST")
            adapter.filterApps = results!!.values as ArrayList<AppInfo>
            if (results.count > 0) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyDataSetInvalidated()
            }
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val prefix: String = constraint?.toString() ?: ""

            if (prefix.isEmpty()) {
                val list: ArrayList<AppInfo>
                synchronized(adapter.mLock) {
                    list = ArrayList<AppInfo>(adapter.apps)
                }
                results.values = list
                results.count = list.size
            } else {
                val prefixString = prefix.lowercase(getDefault())

                val values: ArrayList<AppInfo>
                synchronized(adapter.mLock) {
                    values = ArrayList<AppInfo>(adapter.apps)
                }
                val selected = adapter.getSelectedItems()

                val count = values.size
                val newValues = ArrayList<AppInfo>()

                for (i in 0 until count) {
                    val value = values[i]
                    if (selected.contains(value)) {
                        newValues.add(value)
                    } else {
                        val labelText = value.appName.lowercase(getDefault())
                        val valueText = value.packageName.lowercase(getDefault())
                        if (labelText.contains(prefixString)) {
                            newValues.add(value)
                        } else if (valueText.contains(prefixString)) {
                            newValues.add(value)
                        }
                    }
                }

                results.values = newValues
                results.count = newValues.size
            }

            return results
        }
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = ArrayFilter(this)
        }
        return filter!!
    }

    private val iconCaches = LruCache<String, Drawable>(100)

    init {
        filterApps.sortBy { !it.selected }
    }

    override fun getCount(): Int {
        return filterApps.size
    }

    override fun getItem(position: Int): AppInfo {
        return filterApps[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    private suspend fun loadIcon(app: AppInfo): Drawable? = withContext(Dispatchers.IO) {
        val packageName = app.packageName
        val icon: Drawable? = iconCaches.get(packageName)
        if (icon == null && !app.notFound) {
            try {
                val installInfo = context.packageManager.getPackageInfo(packageName, 0)
                iconCaches.put(
                    packageName,
                    installInfo.applicationInfo!!.loadIcon(context.packageManager)
                )
            } catch (_: Exception) {
                app.notFound = true
            } finally {
            }
            iconCaches.get(packageName)
        } else {
            icon
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_chooser_item, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        holder.checkBox?.visibility = if (multiple) View.VISIBLE else View.GONE
        holder.radioButton?.visibility = if (multiple) View.GONE else View.VISIBLE
        holder.imgView?.visibility = View.VISIBLE

        updateRow(position, view, holder)
        return view
    }

    fun updateRow(position: Int, convertView: View, holder: ViewHolder) {
        val item = getItem(position)

        val packageName = item.packageName
        holder.packageName = packageName

        convertView.setOnClickListener {
            if (multiple || item.selected) {
                if (multiple) {
                    item.selected = !item.selected
                    holder.checkBox?.isChecked = item.selected
                }
            } else {
                val current = apps.find { it.selected }
                current?.selected = false
                item.selected = true
                notifyDataSetChanged()
            }
            selectStateListener?.onSelectChange(getSelectedItems())
        }

        holder.run {
            itemTitle?.text = item.appName
            itemDesc?.text = item.packageName
            checkBox?.isChecked = item.selected
            radioButton?.isChecked = item.selected

            val imgView = imgView!!
            imgView.tag = packageName
            scope.launch(Dispatchers.Main) {
                val icon = loadIcon(item)
                if (icon != null && imgView.tag == packageName) {
                    imgView.setImageDrawable(icon)
                }
            }
        }
    }

    fun setSelectAllState(allSelected: Boolean) {
        apps.forEach {
            it.selected = allSelected
        }
        notifyDataSetChanged()
    }

    fun setSelectStateListener(selectStateListener: SelectStateListener?) {
        this.selectStateListener = selectStateListener
    }

    fun getSelectedItems(): List<AppInfo> {
        return apps.filter { it.selected }
    }

    class ViewHolder(view: View) {
        internal val scope = MainScope()
        internal var packageName: String? = null
        internal var itemTitle: TextView? = view.findViewById(R.id.ItemTitle)
        internal var itemDesc: TextView? = view.findViewById(R.id.ItemDesc)
        internal var imgView: ImageView? = view.findViewById(R.id.ItemIcon)
        internal var checkBox: CheckBox? = view.findViewById(R.id.ItemCheckBox)
        internal var radioButton: RadioButton? = view.findViewById(R.id.ItemRadioButton)
    }
}
