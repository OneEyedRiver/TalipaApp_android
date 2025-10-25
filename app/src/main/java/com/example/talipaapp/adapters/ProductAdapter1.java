package com.example.talipaapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talipaapp.ApiService;
import com.example.talipaapp.R;
import com.example.talipaapp.models.Product;
import com.example.talipaapp.network.ApiResponse;
import com.example.talipaapp.network.RetrofitClient;
import com.example.talipaapp.store.EditProductActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductAdapter1 extends RecyclerView.Adapter<ProductAdapter1.ProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private String token;


    public ProductAdapter1(Context context, List<Product> productList, String token) {
        this.context = context;
        this.productList = productList;
        this.token = token;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product1, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.txtName.setText(product.getProduct_name());
        holder.txtCategory.setText(product.getProduct_category());
        holder.txtPrice.setText("₱" + product.getProduct_price());

        // Load image with Glide
        Glide.with(context)
                .load(product.getProduct_image() != null ? product.getProduct_image() : R.drawable.ic_launcher_background)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgProduct);

        // Edit button (no function yet)
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditProductActivity.class);
            intent.putExtra("product_id", product.getId());
            Log.d("EDIT", "Edit clicked for product: " + product.getId());
            context.startActivity(intent);
        });

        // Delete button (no function yet)
        holder.btnDelete.setOnClickListener(v -> {

            int productId = product.getId();


            Log.d("TOKEN_CHECK", "Bearer " + this.token);


            ApiService api = RetrofitClient.getInstance().create(ApiService.class);

            Call<ApiResponse> call = api.deleteProduct("Bearer " + this.token, productId);
            call.enqueue(new Callback<ApiResponse>() {

                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        productList.remove(holder.getAdapterPosition());
                        notifyItemRemoved(holder.getAdapterPosition());
                    } else {
                        try {
                            Log.e("DELETE", "Raw error: " + response.errorBody().string());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(context, "Delete failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }


                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) {
                    Toast.makeText(context, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });


    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtCategory, txtPrice;
        ImageView imgProduct;
        Button btnEdit, btnDelete;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtProductName);
            txtCategory = itemView.findViewById(R.id.txtProductCategory);
            txtPrice = itemView.findViewById(R.id.txtProductPrice);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    private void destroyProduct(){


    }
}