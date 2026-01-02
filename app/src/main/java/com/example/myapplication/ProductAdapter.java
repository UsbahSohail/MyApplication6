package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final List<Product> products;

    public ProductAdapter(List<Product> products) {
        this.products = products;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        holder.nameTextView.setText(product.getName());
        holder.priceTextView.setText(product.getPrice());
        holder.productImage.setImageResource(product.getImageResId());
        holder.addToCartButton.setOnClickListener(v ->
                Toast.makeText(v.getContext(), product.getName() + " added to cart", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ImageView productImage;
        final TextView nameTextView;
        final TextView priceTextView;
        final Button addToCartButton;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.imgProduct);
            nameTextView = itemView.findViewById(R.id.tvProductName);
            priceTextView = itemView.findViewById(R.id.tvProductPrice);
            addToCartButton = itemView.findViewById(R.id.btnAddToCart);
        }
    }
}

