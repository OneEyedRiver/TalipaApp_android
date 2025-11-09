package com.example.talipaapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talipaapp.ProductDetailActivity;
import com.example.talipaapp.R;
import com.example.talipaapp.models.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private Context context;
    private List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.txtName.setText(product.getProduct_name());
        holder.txtCategory.setText(product.getProduct_category());
        holder.txtPrice.setText("₱" + product.getProduct_price());
        holder.txtUnit.setText("per " + product.getProduct_unit());
        Glide.with(context)
                .load(product.getProduct_image())
                .placeholder(R.drawable.imagesnon)
                .into(holder.imgProduct);

        // Handle click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);

            intent.putExtra("productId", product.getId());
            intent.putExtra("sellerId", product.getSeller_id());
            intent.putExtra("storeId", product.getStore_id());
            intent.putExtra("productName", product.getProduct_name());
            intent.putExtra("productCategory", product.getProduct_category());
            intent.putExtra("price", product.getProduct_price());
            intent.putExtra("unit", product.getProduct_unit());
            intent.putExtra("freshness", product.getProduct_freshness());
            intent.putExtra("qty", product.getProduct_qty());
            intent.putExtra("harvestDate", product.getHarvest_date());
            intent.putExtra("deliverAvailability", product.isDeliver_availability());
            intent.putExtra("pickupAvailability", product.isPick_up_availability());
            intent.putExtra("isAvailable", product.isIs_available());
            intent.putExtra("productImage", product.getProduct_image());
            intent.putExtra("productDescription", product.getProduct_description());

            // ✅ Pass store coordinates
            intent.putExtra("latitude", product.getStoreLatitude());
            intent.putExtra("longitude", product.getStoreLongitude());



            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtCategory, txtPrice, txtUnit;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtName);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtUnit = itemView.findViewById(R.id.txtUnit);
        }
    }
}