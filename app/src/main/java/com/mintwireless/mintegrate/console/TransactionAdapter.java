package com.mintwireless.mintegrate.console;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mintwireless.mintegrate.core.responses.GetTransactionsResponse;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Jialian on 6/05/16.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public interface ItemOnClickListener {
        void onItemClick(GetTransactionsResponse.TransactionSummary transactionSummary);
    }

    private ItemOnClickListener itemOnClickListener;
    private List<GetTransactionsResponse.TransactionSummary> transactionSummaryList;

    public TransactionAdapter(ItemOnClickListener itemOnClickListener) {
        this.itemOnClickListener = itemOnClickListener;
        this.transactionSummaryList = new ArrayList<>();
    }

    public void clear() {
        transactionSummaryList.clear();
    }

    @Override
    public TransactionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.transaction_view, parent, false);
        return new TransactionViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TransactionViewHolder holder, int position) {

        final GetTransactionsResponse.TransactionSummary transactionSummary =
                transactionSummaryList.get(position);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemOnClickListener != null)
                    itemOnClickListener.onItemClick(transactionSummary);
            }
        });

        int maskedPANLength = transactionSummary.getMaskedPAN().length();
        String last4Digits = transactionSummary.getMaskedPAN().substring(maskedPANLength - 4);

        holder.cardType.setText(transactionSummary.getApplicationLabel());
        holder.amount.setText(formatCurrency(Double.valueOf(transactionSummary.getAmount()) / 100));
        holder.maskedPan.setText("-" + last4Digits);

        boolean isApproved = transactionSummary.getPaymentStatus() == GetTransactionsResponse.TransactionSummary.PAYMENT_STATUS.PAYMENT_APPROVED;
        boolean isPending = transactionSummary.getPaymentStatus() == GetTransactionsResponse.TransactionSummary.PAYMENT_STATUS.PAYMENT_PENDING_SIGNATURE;
        boolean isRefund = transactionSummary.isRefund() && isApproved;

        if (isRefund) {
            holder.status.setImageResource(R.drawable.ico_refunded);
        } else {
            if (isApproved) {
                holder.status.setImageResource(R.drawable.icon_paid);
            } else if (isPending) {
                holder.status.setImageResource(R.drawable.ico_pending);
            } else {
                holder.status.setImageResource(R.drawable.icon_declined);
            }
        }

    }

    @Override
    public int getItemCount() {
        int itemCount = 0;
        if (transactionSummaryList != null)
            itemCount = transactionSummaryList.size();
        return itemCount;
    }

    private String formatCurrency(double paramDouble) {
        String str = new String("");
        DecimalFormat localDecimalFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));
        str = "$" + localDecimalFormat.format(paramDouble);
        return str;
    }

    public void addToTransactionsList(GetTransactionsResponse.TransactionSummary transactionSummary) {
        this.transactionSummaryList.add(transactionSummary);
    }

    public String getLastTransactionRequestID() {
        int itemCount = getItemCount();
        if (itemCount != 0)
            return transactionSummaryList.get(itemCount - 1).getTransactionRequestID();
        return "";
    }

    public class TransactionViewHolder extends RecyclerView.ViewHolder {

        public CardView cardView;
        public TextView amount, cardType, maskedPan;
        public ImageView status;

        public TransactionViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.card_view);
            amount = (TextView) itemView.findViewById(R.id.amount);
            cardType = (TextView) itemView.findViewById(R.id.card_type);
            maskedPan = (TextView) itemView.findViewById(R.id.masked_pan);
            status = (ImageView) itemView.findViewById(R.id.status_icon);
        }
    }
}
