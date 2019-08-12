package com.smart.hero.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smart.hero.R
import com.smart.hero.data.model.Record
import java.lang.Exception

class RecordAdapter(private val mContext: Context, private val list: List<Record>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return RecordHolder(LayoutInflater.from(mContext).inflate(R.layout.item_record, parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(genericHolder: RecyclerView.ViewHolder, position: Int) {
        val holder = genericHolder as RecordHolder
        holder.fillFields(list[position], position, mContext)
    }

    override fun getItemId(position: Int): Long {
        try {
            return list[position].id_historial.toLong()
        } catch (e:Exception) {
            e.printStackTrace()
        }
        return 0
    }
}

class RecordHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val layRecord: LinearLayout = view.findViewById(R.id.layRecord)
    private val textTipo: TextView = view.findViewById(R.id.textTipo)
    private val textDetalle: TextView = view.findViewById(R.id.textDetalle)

    fun fillFields(record: Record, position: Int, mContext: Context){
        if (position % 2 != 0) layRecord.setBackgroundColor(mContext.resources.getColor(R.color.colorHint))
        textTipo.text = record.tipo
        textDetalle.text = record.detalle
    }
}