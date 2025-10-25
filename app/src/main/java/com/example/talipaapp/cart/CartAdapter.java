package com.example.talipaapp.cart;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.talipaapp.ApiService;
import com.example.talipaapp.R;
import com.example.talipaapp.models.CartItem;
import com.example.talipaapp.network.ApiResponse;
import com.example.talipaapp.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private Context context;
    private List<CartItem> cartList;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;

    public CartAdapter(Context context, List<CartItem> cartList) {
        this.context = context;
        this.cartList = cartList;
        this.apiService = RetrofitClient.getInstance().create(ApiService.class);
        this.sharedPreferences = context.getSharedPreferences("com.example.talipaapp", Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartList.get(position);

        holder.tvName.setText(item.getProduct_name());
        holder.tvStoreName.setText(item.getStore_name());
        holder.tvPrice.setText("₱" + item.getTotal_price());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));

        String imageUrl = item.getProduct_image();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_placeholder);
        }

        // ✅ Handle + and - buttons
        holder.btnPlus.setOnClickListener(v -> {
            int newQty = item.getQuantity() + 1;
            updateQuantity(item, newQty, holder);
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                int newQty = item.getQuantity() - 1;
                updateQuantity(item, newQty, holder);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            String token = "Bearer " + sharedPreferences.getString("token", "");
            int position1 = holder.getAdapterPosition();

            if (position1 != RecyclerView.NO_POSITION) {
                CartItem itemToDelete = cartList.get(position1);

                Call<ApiResponse> call = apiService.deleteCart_droid(token, itemToDelete.getId());
                call.enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                            // ✅ Remove from list and update adapter
                            cartList.remove(position1);
                            notifyItemRemoved(position1);
                            notifyItemRangeChanged(position1, cartList.size());

                            // ✅ Update SharedPreferences
                            SharedPrefManager prefManager = new SharedPrefManager(context);
                            prefManager.saveCart(cartList);

                            Toast.makeText(context, "Item removed from cart", Toast.LENGTH_SHORT).show();

                            // ✅ Update total price display
                            if (context instanceof CartActivity) {
                                ((CartActivity) context).updateTotalPrice();
                            }

                        } else {
                            Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        holder.itemView.setOnClickListener(v -> {
            if (context instanceof CartActivity) {
                ((CartActivity) context).showStoreOnMap(item);
            }
        });


    }

    @Override
    public int getItemCount() {
        return cartList.size();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvName, tvPrice, tvQuantity, tvStoreName;
        ImageButton btnPlus, btnMinus, btnDelete;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvName = itemView.findViewById(R.id.tvName);
            tvStoreName= itemView.findViewById(R.id.textViewStoreName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    // ✅ Fixed updateQuantity
    private void updateQuantity(CartItem item, int newQuantity, CartViewHolder holder) {
        String token = "Bearer " + sharedPreferences.getString("token", "");

        Call<ApiResponse> call = apiService.updateCart_droid(
                token,
                item.getId(),
                newQuantity
        );

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ApiResponse apiResponse = response.body();

                    item.setQuantity(apiResponse.getQuantity());
                    item.setProduct_price(apiResponse.getProduct_price());
                    item.setTotal_price(apiResponse.getTotal_price());

                    holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
                    holder.tvPrice.setText("₱" + item.getTotal_price());

                    // Persist updated cart to SharedPreferences (minimal needed change)
                    new SharedPrefManager(context).saveCart(cartList);

                    Toast.makeText(context, "Cart updated!", Toast.LENGTH_SHORT).show();

                    if (context instanceof CartActivity) {
                        ((CartActivity) context).updateTotalPrice();
                    }
                } else {
                    Toast.makeText(context, "Failed to update cart", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    public List<CartItem> getCartList() {
        return cartList;
    }

}
