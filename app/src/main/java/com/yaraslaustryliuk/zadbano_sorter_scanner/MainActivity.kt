/*
 * © 2025 Yaraslau Stryliuk
 * All rights reserved.
 * Zadbano Sorter Scanner
 */

package com.yaraslaustryliuk.zadbano_sorter_scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Основная активность приложения Zadbano Sorter Scanner
 */
class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var barcodeRecyclerView: RecyclerView
    private lateinit var barcodeAdapter: BarcodeAdapter
    private lateinit var barcodeListHeaderTextView: MaterialTextView
    private lateinit var clearButton: MaterialButton
    private lateinit var generatePdfButton: MaterialButton
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraToggleButton: SwitchMaterial
    private lateinit var manualInputEditText: TextInputEditText
    private lateinit var addManualCodeButton: MaterialButton

    // Barcode display components
    private lateinit var barcodeDisplayLayout: LinearLayout
    private lateinit var generatedCodeTextView: MaterialTextView
    private lateinit var generatedCodeImageView: ImageView
    private lateinit var saveCodeButton: MaterialButton
    private lateinit var shareCodeButton: MaterialButton
    private lateinit var closeDisplayButton: MaterialButton

    // Data
    private val scannedBarcodes = mutableListOf<BarcodeItem>()
    private var isScanningPaused = false
    private val scanPauseHandler = Handler(Looper.getMainLooper())

    private val TAG = "MainActivity"

    // Permission launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            showSnackbar("Разрешение на камеру необходимо для сканирования", true)
            showPermissionRationale("Камера")
        }
    }

    private val requestManageStoragePermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                showSnackbar("Разрешение на управление хранилищем получено")
            } else {
                showSnackbar("Разрешение не получено. Сохранение PDF может быть ограничено", true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        loadScannedBarcodes()
        updateBarcodeListHeader()

        cameraExecutor = Executors.newSingleThreadExecutor()
        requestCameraPermission()
    }

    /**
     * Инициализация всех View компонентов
     */
    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        barcodeRecyclerView = findViewById(R.id.barcodeRecyclerView)
        barcodeListHeaderTextView = findViewById(R.id.barcodeListHeaderTextView)
        clearButton = findViewById(R.id.clearButton)
        generatePdfButton = findViewById(R.id.generatePdfButton)
        manualInputEditText = findViewById(R.id.manualInputEditText)
        addManualCodeButton = findViewById(R.id.addManualCodeButton)
        cameraToggleButton = findViewById(R.id.cameraToggleButton)

        // Barcode display components
        barcodeDisplayLayout = findViewById(R.id.barcodeDisplayLayout)
        generatedCodeTextView = findViewById(R.id.generatedCodeTextView)
        generatedCodeImageView = findViewById(R.id.generatedCodeImageView)
        saveCodeButton = findViewById(R.id.saveCodeButton)
        shareCodeButton = findViewById(R.id.shareCodeButton)
        closeDisplayButton = findViewById(R.id.closeDisplayButton)
    }

    /**
     * Настройка RecyclerView и адаптера
     */
    private fun setupRecyclerView() {
        barcodeAdapter = BarcodeAdapter(
            onGenerateCode128Click = { codeValue ->
                generateAndDisplayCode128(codeValue)
            },
            onDeleteClick = { position ->
                deleteBarcodeItem(position)
            },
            onEditCommentClick = { position, currentComment ->
                showEditCommentDialog(position, currentComment)
            }
        )
        barcodeRecyclerView.layoutManager = LinearLayoutManager(this)
        barcodeRecyclerView.adapter = barcodeAdapter
    }

    /**
     * Настройка обработчиков кликов
     */
    private fun setupClickListeners() {
        // Camera toggle
        cameraToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bindCameraUseCases()
            } else {
                stopCamera()
            }
        }

        // Clear button
        clearButton.setOnClickListener {
            clearScannedBarcodes()
        }

        // Generate PDF button
        generatePdfButton.setOnClickListener {
            generatePdfForAllBarcodes()
        }

        // Add manual code button
        addManualCodeButton.setOnClickListener {
            val code = manualInputEditText.text.toString().trim()
            if (code.isNotEmpty()) {
                addBarcodeToList(code)
                manualInputEditText.text?.clear()
            } else {
                showSnackbar("Пожалуйста, введите код", true)
            }
        }

        // Barcode display dialog buttons
        closeDisplayButton.setOnClickListener {
            barcodeDisplayLayout.visibility = View.GONE
        }

        // Camera toggle initial state
        if (cameraToggleButton.isChecked) {
            startCamera()
        }
    }

    /**
     * Показать диалог редактирования комментария
     */
    private fun showEditCommentDialog(position: Int, currentComment: String) {
        val editText = TextInputEditText(this).apply {
            setText(currentComment)
            hint = "Введите комментарий"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Редактировать комментарий")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newComment = editText.text.toString().trim()
                updateBarcodeComment(position, newComment)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Обновить комментарий к коду
     */
    private fun updateBarcodeComment(position: Int, newComment: String) {
        if (position >= 0 && position < scannedBarcodes.size) {
            val oldItem = scannedBarcodes[position]
            val newItem = oldItem.copy(comment = newComment)
            scannedBarcodes[position] = newItem
            barcodeAdapter.submitList(scannedBarcodes.toList())
            saveScannedBarcodes()
            showSnackbar("Комментарий обновлен")
        }
    }

    /**
     * Запрос разрешения на использование камеры
     */
    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationale("Камера")
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Удаление элемента из списка
     */
    private fun deleteBarcodeItem(position: Int) {
        if (position >= 0 && position < scannedBarcodes.size) {
            val deletedCode = scannedBarcodes[position].code
            scannedBarcodes.removeAt(position)
            barcodeAdapter.submitList(scannedBarcodes.toList())
            saveScannedBarcodes()
            updateBarcodeListHeader()
            showSnackbar("Код \"$deletedCode\" удален")
        }
    }

    /**
     * Запрос разрешения на управление внешним хранилищем
     */
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Необходимо разрешение")
                    .setMessage("Для сохранения PDF-файлов требуется разрешение на управление всеми файлами.")
                    .setPositiveButton("Разрешить") { _, _ ->
                        try {
                            val intentAppSpecific = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intentAppSpecific.data = Uri.parse("package:${applicationContext.packageName}")
                            requestManageStoragePermission.launch(intentAppSpecific)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            requestManageStoragePermission.launch(intent)
                        }
                    }
                    .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    /**
     * Показать объяснение необходимости разрешения
     */
    private fun showPermissionRationale(permissionName: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Разрешение $permissionName отклонено")
            .setMessage("Пожалуйста, предоставьте разрешение на $permissionName в настройках приложения, чтобы использовать эту функцию.")
            .setPositiveButton("Открыть настройки") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Запуск камеры
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Привязка использования камеры
     */
    private fun bindCameraUseCases() {
        if (!::cameraProvider.isInitialized) {
            Log.e(TAG, "CameraProvider не инициализирован при попытке привязать use cases.")
            return
        }

        cameraProvider.unbindAll()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcodes ->
                    if (barcodes.isNotEmpty() && !isScanningPaused) {
                        for (barcode in barcodes) {
                            addBarcodeToList(barcode.rawValue ?: "Неизвестный код")
                        }
                    }
                })
            }

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            previewView.visibility = View.VISIBLE
        } catch (exc: Exception) {
            Log.e(TAG, "Ошибка при привязке вариантов использования камеры", exc)
            showSnackbar("Ошибка при включении камеры: ${exc.message}", true)
        }
    }

    /**
     * Остановка камеры
     */
    private fun stopCamera() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
            previewView.visibility = View.INVISIBLE
        }
    }

    /**
     * Добавить штрихкод в список
     */
    private fun addBarcodeToList(code: String) {
        // Валидация кода
        if (!BarcodeItem.isValidCode(code)) {
            showSnackbar("Неверный формат кода", true)
            return
        }

        // Проверка на запрещенные коды
        if (BarcodeItem.isCodeBlocked(code)) {
            showSnackbar("⛔ Код заблокирован фильтром: $code", true)
            return
        }

        // Проверка на дубликаты
        if (scannedBarcodes.any { it.code == code }) {
            showSnackbar("Код уже существует в списке", true)
            return
        }

        // Добавление кода
        val newItem = BarcodeItem(code)
        scannedBarcodes.add(0, newItem)
        barcodeAdapter.submitList(scannedBarcodes.toList())
        saveScannedBarcodes()
        updateBarcodeListHeader()
        showSnackbar("✅ Отсканирован: $code")

        // Пауза сканирования на 2 секунды
        isScanningPaused = true
        scanPauseHandler.postDelayed({
            isScanningPaused = false
        }, 2000)
    }

    /**
     * Обновить заголовок списка кодов
     */
    private fun updateBarcodeListHeader() {
        val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        barcodeListHeaderTextView.text = "📋 Отсканированные коды ($today): ${scannedBarcodes.size}"
    }

    /**
     * Очистить список отсканированных кодов
     */
    private fun clearScannedBarcodes() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Очистить историю")
            .setMessage("Вы уверены, что хотите очистить всю историю сканирований?")
            .setPositiveButton("Да") { _, _ ->
                scannedBarcodes.clear()
                barcodeAdapter.submitList(emptyList())
                saveScannedBarcodes()
                updateBarcodeListHeader()
                showSnackbar("История очищена")
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * Сохранить список кодов в SharedPreferences
     */
    private fun saveScannedBarcodes() {
        val sharedPrefs = getSharedPreferences("barcode_history", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val json = Gson().toJson(scannedBarcodes)
        editor.putString("history_list", json)
        editor.apply()
    }

    /**
     * Загрузить список кодов из SharedPreferences
     */
    private fun loadScannedBarcodes() {
        val sharedPrefs = getSharedPreferences("barcode_history", MODE_PRIVATE)
        val json = sharedPrefs.getString("history_list", null)
        if (json != null) {
            try {
                val type = object : TypeToken<MutableList<BarcodeItem>>() {}.type
                val loadedList: MutableList<BarcodeItem> = Gson().fromJson(json, type)
                scannedBarcodes.clear()
                scannedBarcodes.addAll(loadedList)
                barcodeAdapter.submitList(scannedBarcodes.toList())
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при загрузке истории: ${e.message}")
            }
        }
    }

    /**
     * Анализатор штрихкодов для камеры
     */
    private inner class BarcodeAnalyzer(private val listener: (List<Barcode>) -> Unit) : ImageAnalysis.Analyzer {
        private val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_CODE_128, Barcode.FORMAT_QR_CODE)
            .build()
        private val scanner = BarcodeScanning.getClient(options)

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            listener(barcodes)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("BarcodeAnalyzer", "Barcode scanning failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    /**
     * Генерация и отображение штрихкода Code 128
     */
    private fun generateAndDisplayCode128(codeValue: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(
                    codeValue,
                    BarcodeFormat.CODE_128,
                    800,
                    300
                )

                withContext(Dispatchers.Main) {
                    generatedCodeTextView.text = "Сгенерированный код: $codeValue"
                    generatedCodeImageView.setImageBitmap(bitmap)
                    barcodeDisplayLayout.visibility = View.VISIBLE

                    setupBarcodeDisplayButtons(bitmap)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Ошибка при генерации штрихкода: ${e.message}", true)
                    Log.e("CodeGenerator", "Error generating barcode", e)
                }
            }
        }
    }

    /**
     * Настройка кнопок в диалоге отображения штрихкода
     */
    private fun setupBarcodeDisplayButtons(bitmap: Bitmap) {
        saveCodeButton.setOnClickListener {
            saveImageToGallery(bitmap, "barcode_code128_${System.currentTimeMillis()}.png")
        }

        shareCodeButton.setOnClickListener {
            shareImage(bitmap, "barcode_code128_${System.currentTimeMillis()}.png")
        }
    }

    /**
     * Сохранение изображения в галерею
     */
    private fun saveImageToGallery(bitmap: Bitmap, filename: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentResolver = contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    showSnackbar("Изображение сохранено в Галерею")
                }
            } ?: showSnackbar("Не удалось сохранить изображение", true)
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val imageFile = File(imagesDir, filename)
            try {
                FileOutputStream(imageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    showSnackbar("Изображение сохранено: ${imageFile.absolutePath}")
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = Uri.fromFile(imageFile)
                    sendBroadcast(mediaScanIntent)
                }
            } catch (e: IOException) {
                showSnackbar("Ошибка сохранения: ${e.message}", true)
                Log.e("SaveImage", "Error saving image", e)
            }
        }
    }

    /**
     * Поделиться изображением
     */
    private fun shareImage(bitmap: Bitmap, filename: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val imagesDir = File(applicationContext.cacheDir, "images")
            imagesDir.mkdirs()
            val imageFile = File(imagesDir, filename)

            try {
                FileOutputStream(imageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }

                val imageUri: Uri = FileProvider.getUriForFile(
                    applicationContext,
                    "com.yaraslaustryliuk.zadbano_sorter_scanner.provider",
                    imageFile
                )

                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Поделиться изображением"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Ошибка при подготовке изображения к отправке: ${e.message}", true)
                    Log.e("ShareImage", "Error preparing image for sharing", e)
                }
            }
        }
    }

    /**
     * Генерация PDF со всеми штрихкодами
     */
    private fun generatePdfForAllBarcodes() {
        if (scannedBarcodes.isEmpty()) {
            showSnackbar("Список отсканированных кодов пуст", true)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val barcodeEncoder = BarcodeEncoder()

                val pageWidth = (8.27 * 72).toInt()
                val pageHeight = (11.69 * 72).toInt()
                val marginX = (0.5 * 72).toInt()
                val marginY = (0.5 * 72).toInt()
                val maxCodeWidth = (pageWidth / 2) - (marginX * 2)
                val maxCodeHeight = (pageHeight / 3) - (marginY * 2)

                scannedBarcodes.forEachIndexed { index, barcodeItem ->
                    try {
                        val barcodeBitmap = barcodeEncoder.encodeBitmap(
                            barcodeItem.code,
                            BarcodeFormat.CODE_128,
                            1600,
                            600
                        )

                        val scaledBitmap = scaleBitmapToFit(barcodeBitmap, maxCodeWidth, maxCodeHeight)
                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                        val page = pdfDocument.startPage(pageInfo)
                        val canvas = page.canvas

                        // Заголовок
                        val headerPaint = Paint().apply {
                            color = Color.BLACK
                            textSize = 10f
                            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                        }
                        canvas.drawText("len jest silnikiem postępu", marginX.toFloat(), marginY.toFloat() - 5, headerPaint)

                        // Штрихкод
                        val bitmapY = marginY.toFloat()
                        canvas.drawBitmap(scaledBitmap, marginX.toFloat(), bitmapY, null)

                        // Код (как обычно)
                        val textPaint = Paint().apply {
                            color = Color.BLACK
                            textSize = 16f
                            typeface = Typeface.DEFAULT
                        }
                        canvas.drawText(barcodeItem.code, marginX.toFloat(), bitmapY + scaledBitmap.height + 20, textPaint)

                        // УЛУЧШЕННЫЙ комментарий (если есть) - ЖИРНЫЙ И БОЛЬШОЙ
                        if (barcodeItem.hasComment()) {
                            val commentPaint = Paint().apply {
                                color = Color.BLACK  // Изменили цвет на чёрный
                                textSize = 18f        // Увеличили размер с 14f до 18f
                                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)  // Сделали жирным
                            }
                            // УБРАЛИ слово "Комментарий:" - теперь только текст комментария
                            canvas.drawText(barcodeItem.comment, marginX.toFloat(), bitmapY + scaledBitmap.height + 50, commentPaint)
                        }

                        // Дата
                        val datePaint = Paint().apply {
                            color = Color.DKGRAY
                            textSize = 16f
                            textAlign = Paint.Align.RIGHT
                        }
                        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                        canvas.drawText(currentDate, (marginX + scaledBitmap.width).toFloat(), bitmapY + scaledBitmap.height + 20, datePaint)

                        pdfDocument.finishPage(page)
                    } catch (e: Exception) {
                        Log.e("PDF_Generator", "Error generating page for ${barcodeItem.code}: ${e.message}", e)
                    }
                }

                // Сохранение PDF в кеш директории
                val cacheDir = File(applicationContext.cacheDir, "pdfs")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
                val filename = "Zadbano_Sorter_Scanner_${dateFormat}.pdf"
                val pdfFile = File(cacheDir, filename)

                FileOutputStream(pdfFile).use { fos ->
                    pdfDocument.writeTo(fos)
                }
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    showSnackbar("PDF создан: ${pdfFile.name}")
                    sharePdfFile(pdfFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Ошибка при создании PDF: ${e.message}", true)
                    Log.e("PDF_Generator", "Error creating PDF", e)
                }
            }
        }
    }

    /**
     * Масштабирование битмапа под размер
     */
    private fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val ratio = Math.min(maxWidth.toFloat() / originalWidth, maxHeight.toFloat() / originalHeight)
        val newWidth = (originalWidth * ratio).toInt()
        val newHeight = (originalHeight * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Поделиться PDF файлом
     */
    private fun sharePdfFile(pdfFile: File) {
        try {
            val pdfUri: Uri = FileProvider.getUriForFile(
                applicationContext,
                "com.yaraslaustryliuk.zadbano_sorter_scanner.provider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (shareIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(shareIntent, "Поделиться PDF"))
            } else {
                showSnackbar("Не найдено приложений для отправки PDF", true)
            }
        } catch (e: Exception) {
            Log.e("SharePDF", "Error sharing PDF: ${e.message}", e)
            showSnackbar("Ошибка при отправке PDF: ${e.message}", true)
        }
    }

    /**
     * Показать Snackbar сообщение
     */
    private fun showSnackbar(message: String, isError: Boolean = false) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        if (isError) {
            try {
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.md_theme_light_error))
                snackbar.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onError))
            } catch (e: Exception) {
                // Fallback если цвета не найдены
                snackbar.setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                snackbar.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            }
        }
        snackbar.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        scanPauseHandler.removeCallbacksAndMessages(null)
    }
}