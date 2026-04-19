package com.example.billstack

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.billstack.databinding.FragmentViewBillsBinding // Ensure you use ViewBinding

class ViewBills : Fragment() {

    private var _binding: FragmentViewBillsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: BillDatabaseHelper // Replace with your actual DB class name

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewBillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = BillDatabaseHelper(requireContext())
        displaySavedBills()
    }

    private fun displaySavedBills() {
        val bills = dbHelper.getAllBills()

        if (bills.isEmpty()) {
            binding.productListView.text = ""
            binding.emptyTextView4.visibility = View.VISIBLE
            return
        }

        binding.emptyTextView4.visibility = View.GONE
        val builder = SpannableStringBuilder()

        for (bill in bills) {
            val start = builder.length

            // 1. HEADER: Bill ID and Date
            val header = "BILL #${bill.id}  •  ${bill.date}\n"
            builder.append(header)
            builder.setSpan(StyleSpan(Typeface.BOLD), start, start + header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(Color.parseColor("#2E7D32")), start, start + header.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // 2. ITEMS LIST (Cleaning CSV formatting for display)
            val itemsFormatted = "Items: ${bill.items.replace(",", ", ")}\n"
            builder.append(itemsFormatted)
            builder.setSpan(ForegroundColorSpan(Color.parseColor("#616161")), builder.length - itemsFormatted.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(RelativeSizeSpan(0.9f), builder.length - itemsFormatted.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // 3. TOTAL
            val totalStr = "TOTAL AMOUNT: Rs. ${bill.billTotal}\n"
            builder.append(totalStr)
            builder.setSpan(StyleSpan(Typeface.BOLD), builder.length - totalStr.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(Color.BLACK), builder.length - totalStr.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            // 4. DIVIDER
            builder.append("──────────────────────────\n\n")
        }

        binding.productListView.text = builder
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}