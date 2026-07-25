package com.krscripts.common.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.krscripts.common.R
import com.krscripts.common.model.SelectItem
import java.util.*
import java.util.Locale.getDefault

class AdapterItemChooser(private val context: Context, private var items: ArrayList<SelectItem>, private val multiple: Boolean) : BaseAdapter(), Filterable {
    interface SelectStateListener {
        fun onSelectChange(selected: List<SelectItem>)
    }

    private var selectStateListener: SelectStateListener? = null
    private var filter: Filter? = null
    internal var filterItems: ArrayList<SelectItem> = items
    private val mLock = Any()

    private class ArrayFilter(private var adapter: AdapterItemChooser) : Filter() {
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            adapter.filterItems = results!!.values as ArrayList<SelectItem>
            if (results.count > 0) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyDataSetInvalidated()
            }
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = Filter.FilterResults()
            val prefix: String = constraint?.toString() ?: ""

            if (prefix.isEmpty()) {
                val list: ArrayList<SelectItem>
                synchronized(adapter.mLock) {
                    list = ArrayList<SelectItem>(adapter.items)
                }
                results.values = list
                results.count = list.size
            } else {
                val prefixString = prefix.lowercase(getDefault())

                val values: ArrayList<SelectItem>
                synchronized(adapter.mLock) {
                    values = ArrayList<SelectItem>(adapter.items)
                }
                val selected = adapter.getSelectedItems()

                val count = values.size
                val newValues = ArrayList<SelectItem>()

                for (i in 0 until count) {
                    val value = values[i]
                    if (selected.contains(value)) {
                        newValues.add(value)
                    } else {
                        val valueText = if (value.title == null) "" else value.title!!.lowercase(
                            getDefault()
                        )

                        // First match against the whole, non-splitted value
                        if (valueText.contains(prefixString)) {
                            newValues.add(value)
                        } else {
                            val words = valueText.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                            val wordCount = words.size

                            // Start at index 0, in case valueText starts with space(s)
                            for (k in 0 until wordCount) {
                                if (words[k].contains(prefixString)) {
                                    newValues.add(value)
                                    break
                                }
                            }
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

    override fun getCount(): Int {
        return filterItems.size
    }

    override fun getItem(position: Int): SelectItem {
        return filterItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
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

        updateRow(position, view, holder)
        return view
    }

    fun updateRow(position: Int, convertView: View, holder: ViewHolder) {
        val item = getItem(position)

        convertView.setOnClickListener {
            if (multiple) {
                item.selected = !item.selected
                holder.checkBox?.isChecked = item.selected
            } else {
                if (item.selected) {
                    return@setOnClickListener
                } else {
                    val current = items.find { it.selected }
                    current?.selected = false
                    item.selected = true
                    notifyDataSetChanged()
                }
            }
            selectStateListener?.onSelectChange(getSelectedItems())
        }

        holder.itemTitle?.text = item.title
        holder.itemDesc?.run{
            if (item.title.isNullOrEmpty()) {
                text = item.title
            } else {
                visibility = View.GONE
            }
        }
        holder.checkBox?.isChecked = item.selected
        holder.radioButton?.isChecked = item.selected
    }

    fun setSelectAllState(allSelected: Boolean) {
        items.forEach {
            it.selected = allSelected
        }
        notifyDataSetChanged()
    }

    fun setSelectStateListener(selectStateListener: SelectStateListener?) {
        this.selectStateListener = selectStateListener
    }

    fun getSelectedItems(): List<SelectItem> {
        return items.filter { it.selected }
    }

    fun getSelectStatus (): BooleanArray {
        return items.map { it.selected }.toBooleanArray()
    }

    class ViewHolder(view: View) {
        internal var itemTitle: TextView? = view.findViewById(R.id.ItemTitle)
        internal var itemDesc: TextView? = view.findViewById(R.id.ItemDesc)
        internal var checkBox: CheckBox? = view.findViewById(R.id.ItemCheckBox)
        internal var radioButton: RadioButton? = view.findViewById(R.id.ItemRadioButton)
    }
}
