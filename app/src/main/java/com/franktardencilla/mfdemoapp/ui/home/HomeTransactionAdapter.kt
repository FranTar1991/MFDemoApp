package com.franktardencilla.mfdemoapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.franktardencilla.mfdemoapp.R
import com.franktardencilla.mfdemoapp.domain.model.TransactionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeTransactionAdapter(
    private val onTransactionClick: (TransactionSummary) -> Unit
) : RecyclerView.Adapter<HomeTransactionAdapter.TransactionViewHolder>() {
    private val transactions = mutableListOf<TransactionSummary>()

    fun submitList(items: List<TransactionSummary>) {
        transactions.clear()
        transactions.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TransactionViewHolder,
        position: Int
    ) {
        holder.bind(transactions[position], onTransactionClick)
    }

    override fun getItemCount(): Int = transactions.size

    class TransactionViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val statusText = itemView.findViewById<TextView>(R.id.transactionStatusText)
        private val amountText = itemView.findViewById<TextView>(R.id.transactionAmountText)
        private val timeText = itemView.findViewById<TextView>(R.id.transactionTimeText)
        private val metadataText = itemView.findViewById<TextView>(R.id.transactionMetadataText)

        fun bind(
            transaction: TransactionSummary,
            onTransactionClick: (TransactionSummary) -> Unit
        ) {
            statusText.text = transaction.status.name
            amountText.text = transaction.amount.formatted()
            timeText.text = timeFormatter.format(Date(transaction.createdAtMillis))
            metadataText.text = listOf(
                "STAN ${transaction.stan ?: "none"}",
                transaction.entryMode?.displayName ?: "unknown entry",
                transaction.maskedPan?.value ?: "card unavailable"
            ).joinToString(separator = " | ")
            itemView.setOnClickListener {
                onTransactionClick(transaction)
            }
        }

        private companion object {
            val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        }
    }
}
