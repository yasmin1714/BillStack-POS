package com.example.billstack

import BarcodeAnalyzer
import DatabaseHelper
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.billstack.databinding.FragmentCreateProductBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CreateProduct : Fragment() {

    // View Binding setup for Fragments
    private var _binding: FragmentCreateProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService

    // Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(context, "Camera permission is required to scan barcodes", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check for camera permission immediately
        checkCameraPermission()

        // Inside onViewCreated or onCreateView
        val dbHelper = DatabaseHelper(requireContext())

        binding.saveButton.setOnClickListener {
            val id = binding.productBarcode.text.toString().trim()
            val name = binding.productName.text.toString().trim()
            val priceString = binding.productPrice.text.toString().trim()
            val description = binding.productDesc.text.toString().trim()

            if (id.isEmpty() || id == "Scan a barcode") {
                Toast.makeText(context, "Please scan a barcode first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val price = priceString.toDoubleOrNull() ?: 0.0
            try {
                val result = dbHelper.addProduct(id, name, price, description)
                if (result == -1L) {
                    Toast.makeText(context, "Error: Product already exists!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Product saved successfully!", Toast.LENGTH_SHORT).show()
                    binding.productBarcode.text = ""
                    binding.productName.setText("")
                    binding.productPrice.setText("")
                    binding.productDesc.setText("")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview Setup
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Barcode Analysis Setup
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodeValue: String ->
                        // Update UI on the main thread
                        activity?.runOnUiThread {
                            binding.productBarcode.text = barcodeValue
                        }
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // Use viewLifecycleOwner in Fragments!
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("CreateProduct", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }


    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null // CRITICAL: Prevent memory leaks
    }
}