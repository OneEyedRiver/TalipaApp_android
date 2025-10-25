package com.example.talipaapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talipaapp.R;
import com.example.talipaapp.models.Fast.StoreFast;
import com.example.talipaapp.models.Fast.StoreProductRow;

import java.util.List;
public class StoreTableAdapter extends RecyclerView.Adapter<StoreTableAdapter.ViewHolder> {

    private List<StoreProductRow> data;

    public StoreTableAdapter(List<StoreProductRow> data) {
        this.data = data;
    }

    public void setData(List<StoreProductRow> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_store_table, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        StoreProductRow row = data.get(position);
        holder.tvStoreName.setText(row.storeName);
        holder.tvAddress.setText(row.storeAddress);
        holder.tvProductName.setText(row.productName);
        holder.tvPrice.setText("₱" + row.price);


        holder.itemView.setOnClickListener(v -> {
            if (onRowClickListener != null) {
                onRowClickListener.onRowClick(row);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStoreName, tvAddress, tvProductName, tvPrice;

        public ViewHolder(View itemView) {
            super(itemView);
            tvStoreName = itemView.findViewById(R.id.tvStoreName);
            tvAddress = itemView.findViewById(R.id.tvAddress); // <-- add this
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
    public interface OnRowClickListener {
        void onRowClick(StoreProductRow row);
    }

    private OnRowClickListener onRowClickListener;

    public void setOnRowClickListener(OnRowClickListener listener) {
        this.onRowClickListener = listener;
    }
}
