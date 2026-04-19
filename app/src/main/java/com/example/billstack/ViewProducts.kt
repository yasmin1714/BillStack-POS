package com.example.billstack

import DatabaseHelper
import Product
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.billstack.databinding.FragmentViewProductsBinding
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
class ViewProducts : Fragment() {

    private var _binding: FragmentViewProductsBinding? = null
    // This getter is safe ONLY between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // FIX 1: Initialize the binding here!
        _binding = FragmentViewProductsBinding.inflate(inflater, container, false)

        // FIX 2: Return binding.root, NOT the manual inflation
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dbHelper = DatabaseHelper(requireContext())
        val products = dbHelper.getAllProducts()

        if (products.isEmpty()) {
            binding.emptyTextView.visibility = View.VISIBLE
        } else {
            binding.emptyTextView.visibility = View.GONE
            displayProducts(products)
        }
    }

    private fun displayProducts(products: List<Product>) {
        if (products.isEmpty()) {
            binding.productListView.text = "No products available."
            binding.productListView.gravity = Gravity.CENTER
            return
        }

        val builder = SpannableStringBuilder()

        for (product in products) {
            val start = builder.length

            // 1. PRODUCT NAME & PRICE HEADER
            // Format: ● PRODUCT NAME  |  Rs. 0.00
            val header = "● ${product.name.uppercase()}  |  Rs. ${String.format("%.2f", product.price)}\n"
            builder.append(header)

            // Bold the header and make it slightly larger
            builder.setSpan(StyleSpan(Typeface.BOLD), start, start + header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(RelativeSizeSpan(1.1f), start, start + header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(Color.BLACK), start, start + header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // 2. ID (Subtle secondary info)
            val idString = "   ID: ${product.id}\n"
            builder.append(idString)
            builder.setSpan(ForegroundColorSpan(Color.parseColor("#757575")), builder.length - idString.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(RelativeSizeSpan(0.85f), builder.length - idString.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // 3. DESCRIPTION
            val descString = "   ${product.description}\n\n"
            builder.append(descString)
            builder.setSpan(ForegroundColorSpan(Color.parseColor("#424242")), builder.length - descString.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // 4. DIVIDER LINE
            builder.append("──────────────────────────\n\n")
        }

        binding.productListView.text = builder
        binding.productListView.gravity = Gravity.START
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up to avoid memory leaks
    }
}