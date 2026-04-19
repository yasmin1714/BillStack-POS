package com.example.billstack

import BarcodeAnalyzer
import DatabaseHelper
import Product
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.graphics.Color
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.billstack.databinding.FragmentGenerateBillsBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.core.graphics.toColorInt

class GenerateBills : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private val billItems = mutableListOf<Product>()
    private var currentTotal: Double = 0.0

    private var _binding: FragmentGenerateBillsBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService

    @RequiresApi(Build.VERSION_CODES.R)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val ARG_PARAM1 = null
            param1 = it.getString(ARG_PARAM1)
            val ARG_PARAM2 = null
            param2 = it.getString(ARG_PARAM2)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateBillsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkCameraPermission()

        val dbHelper = DatabaseHelper(requireContext())
        val dbBillHelper = BillDatabaseHelper(requireContext())

        binding.addProduct.setOnClickListener {
            val id = binding.scannedProductId.text.toString().trim()
            if (id == "Scanned ID" || id.isEmpty()) {
                Toast.makeText(context, "Please scan a barcode first", Toast.LENGTH_LONG).show()
            } else {
                try {
                    val returnedProduct = dbHelper.getProductById(id)
                    if (returnedProduct != null) {
                        billItems.add(returnedProduct)
                        updateBillDisplay()
                        binding.scannedProductId.text = ""
                    } else {
                        Toast.makeText(context, "Product not found!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.saveBill.setOnClickListener {
            if (billItems.isEmpty()) {
                Toast.makeText(context, "Cannot save an empty bill!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val itemsCsv = billItems.joinToString(separator = ",") { it.name }
                val pricesCsv = billItems.joinToString(separator = ",") { String.format("%.2f", it.price) }
                val totalString = String.format("%.2f", currentTotal)
                val id = dbBillHelper.insertBill(itemsCsv, pricesCsv, totalString)
                if (id != -1L) {
                    Toast.makeText(context, "Bill saved successfully! ID: $id", Toast.LENGTH_SHORT).show()
                    billItems.clear()
                    updateBillDisplay()
                } else {
                    Toast.makeText(context, "Error: Failed to insert bill into database", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving bill: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateBillDisplay() {
        if (billItems.isEmpty()) {
            binding.productListView.text = ""
            binding.emptyTextView2.visibility = View.VISIBLE
            return
        }

        val builder = SpannableStringBuilder()
        var runningTotal = 0.0
        builder.append("SUMMARY\n", StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(RelativeSizeSpan(1.2f), 0, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.append("──────────────────────────\n\n")
        for (item in billItems) {
            val start = builder.length
            builder.append("${item.name}\n", StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            val details = "1 unit   ×   Rs. ${String.format("%.2f", item.price)}\n\n"
            builder.append(details)
            builder.setSpan(
                ForegroundColorSpan("#757575".toColorInt()),
                start + item.name.length + 1,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                RelativeSizeSpan(0.9f),
                start + item.name.length + 1,
                builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            runningTotal += item.price
        }
        builder.append("──────────────────────────\n")
        val totalCount = "Total Items: ${billItems.size}\n"
        builder.append(totalCount)
        val grandTotalStr = "GRAND TOTAL: Rs. ${String.format("%.2f", runningTotal)}"
        val totalStart = builder.length
        builder.append(grandTotalStr)
        builder.setSpan(StyleSpan(Typeface.BOLD), totalStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(RelativeSizeSpan(1.1f), totalStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        builder.setSpan(ForegroundColorSpan(Color.BLACK), totalStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        currentTotal = runningTotal
        binding.productListView.text = builder
        binding.emptyTextView2.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodeValue ->
                        activity?.runOnUiThread {
                            _binding?.let { b -> b.scannedProductId.text = barcodeValue; Toast.makeText(context,"Barcode Identified",Toast.LENGTH_SHORT).show(); b.root.performHapticFeedback(
                                android.view.HapticFeedbackConstants.CONFIRM,
                                android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            )}
                        }
                    })
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("GenerateBills", "Binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}