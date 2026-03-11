package com.surfaceosx.calc

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.*
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvMode: TextView
    private lateinit var tvDegRad: TextView
    private lateinit var tvFE: TextView

    private var operand1: Double = 0.0
    private var operand2: Double = 0.0
    private var operator: String = ""
    private var isNewOperation = true
    private var isDecimalPressed = false
    private var memory: Double = 0.0

    private var isScientificMode = false
    private var isSecondMode = false      // режим 2nd
    private var isHypMode = false         // режим гиперболических функций
    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        tvMode = findViewById(R.id.tvMode)
        tvDegRad = findViewById(R.id.tvDegRad)
        tvFE = findViewById(R.id.tvFE)

        // Отступ под статус-бар
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            val btnMenu = findViewById<View>(R.id.btnMenu)
            val tvMode = findViewById<View>(R.id.tvMode)
            btnMenu.setPadding(btnMenu.paddingLeft, statusBarHeight, btnMenu.paddingRight, btnMenu.paddingBottom)
            tvMode.setPadding(tvMode.paddingLeft, statusBarHeight, tvMode.paddingRight, tvMode.paddingBottom)
            insets
        }

        setupMenu()
        setupHistoryButton()
        loadMode(isScientificMode)
    }

    // ---------- Переключение режимов ----------
    private fun setupMenu() {
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.mode_menu, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.mode_standard -> switchMode(false)
                    R.id.mode_scientific -> switchMode(true)
                }
                true
            }
            popup.show()
        }
    }

    private fun switchMode(scientific: Boolean) {
        isScientificMode = scientific
        isSecondMode = false
        isHypMode = false
        loadMode(scientific)
        tvMode.text = if (scientific) "инженерный" else "обычный"
    }

    private fun loadMode(scientific: Boolean) {
        val container = findViewById<FrameLayout>(R.id.buttonPanel)
        container.removeAllViews()
        val inflater = layoutInflater
        val view = if (scientific) {
            inflater.inflate(R.layout.scientific_buttons, container, false)
        } else {
            inflater.inflate(R.layout.basic_buttons, container, false)
        }
        container.addView(view)
        setupButtonListeners()
    }

    // ---------- Инициализация слушателей ----------
    private fun setupButtonListeners() {
        setNumberButtonListeners()
        setOperatorButtonListeners()
        setMemoryButtonListeners()
        setControlButtonListeners()
        if (isScientificMode) {
            setScientificButtonListeners()
            updateSecondModeButtons()
        } else {
            setBasicFunctionButtonListeners()
        }
    }

    // ---------- Обычные функции ----------
    private fun setBasicFunctionButtonListeners() {
        findViewById<Button>(R.id.btnPercent)?.setOnClickListener {
            applyUnaryOperation("%") { it / 100 }
        }
        findViewById<Button>(R.id.btnSqrt)?.setOnClickListener {
            applyUnaryOperation("√") { sqrt(it) }
        }
        findViewById<Button>(R.id.btnSquare)?.setOnClickListener {
            applyUnaryOperation("x²") { it * it }
        }
        findViewById<Button>(R.id.btnReciprocal)?.setOnClickListener {
            if (currentValue() == 0.0) {
                tvResult.text = "Ошибка"
                return@setOnClickListener
            }
            applyUnaryOperation("1/x") { 1 / it }
        }
    }

    // ---------- Научные функции ----------
    private fun setScientificButtonListeners() {
        // Кнопка 2nd (основная)
        val btn2nd = findViewById<Button>(R.id.btn2nd)
        btn2nd?.setBackgroundColor(0xFF8C00.toInt()) // начальный цвет (черный)
        btn2nd?.setOnClickListener {
            isSecondMode = !isSecondMode
            updateSecondModeButtons()
            // Меняем фон кнопки в зависимости от состояния
            btn2nd.setBackgroundColor(if (isSecondMode) 0xFF3a3a3a.toInt() else  0xFF8C00.toInt())
            Toast.makeText(this, "2nd режим: ${if (isSecondMode) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
        }

        // Кнопка Trigonometry ▼
        findViewById<Button>(R.id.btnTrigonometry)?.setOnClickListener { view ->
            showTrigonometryMenu(view)
        }

        findViewById<Button>(R.id.btnFunction)?.setOnClickListener {
            Toast.makeText(this, "Function menu (в разработке)", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnPi)?.setOnClickListener {
            val current = tvResult.text.toString()
            tvResult.text = if (isNewOperation) PI.toString() else current + PI
            isNewOperation = false
            isDecimalPressed = true
        }

        findViewById<Button>(R.id.btnE)?.setOnClickListener {
            val current = tvResult.text.toString()
            tvResult.text = if (isNewOperation) E.toString() else current + E
            isNewOperation = false
            isDecimalPressed = true
        }

        findViewById<Button>(R.id.btnAbs)?.setOnClickListener {
            applyUnaryOperation("|x|") { abs(it) }
        }

        findViewById<Button>(R.id.btnExp)?.setOnClickListener {
            applyUnaryOperation("exp") { exp(it) }
        }

        findViewById<Button>(R.id.btnMod)?.setOnClickListener {
            onOperatorClickWithSymbol("mod")
        }

        findViewById<Button>(R.id.btnCubeRoot)?.setOnClickListener {
            applyUnaryOperation("∛") { cbrt(it) }
        }

        findViewById<Button>(R.id.btnLeftParen)?.setOnClickListener {
            tvResult.append("(")
        }

        findViewById<Button>(R.id.btnRightParen)?.setOnClickListener {
            tvResult.append(")")
        }

        findViewById<Button>(R.id.btnFactorial)?.setOnClickListener {
            val value = currentValue().toInt()
            if (value >= 0) {
                var fact = 1L
                for (i in 1..value) fact *= i
                tvResult.text = fact.toString()
                addToHistory("$value!", fact.toDouble())
                isNewOperation = true
            } else {
                tvResult.text = "Ошибка"
            }
        }

        findViewById<Button>(R.id.btnPower)?.setOnClickListener {
            onOperatorClickWithSymbol("^")
        }

        // Кнопка log (меняется в режиме 2nd)
        findViewById<Button>(R.id.btnLog)?.setOnClickListener {
            if (isSecondMode) {
                onOperatorClickWithSymbol("logy")
            } else {
                applyUnaryOperation("log") { log10(it) }
            }
        }

        findViewById<Button>(R.id.btnLn)?.setOnClickListener {
            applyUnaryOperation("ln") { ln(it) }
        }

        // Кнопка x² / x³ (меняется в режиме 2nd)
        findViewById<Button>(R.id.btnSquare)?.setOnClickListener {
            if (isSecondMode) {
                applyUnaryOperation("x³") { it * it * it }
            } else {
                applyUnaryOperation("x²") { it * it }
            }
        }

        // Кнопка 10^x / 2^x (меняется в режиме 2nd)
        findViewById<Button>(R.id.btn10x)?.setOnClickListener {
            if (isSecondMode) {
                applyUnaryOperation("2^x") { 2.0.pow(it) }
            } else {
                applyUnaryOperation("10^x") { 10.0.pow(it) }
            }
        }
    }

    // Меню тригонометрии
    private fun showTrigonometryMenu(anchor: View) {
        val inflater = LayoutInflater.from(this)
        val menuView = inflater.inflate(R.layout.trigonometry_menu, null)
        val displayMetrics = resources.displayMetrics
        val width = (450 * displayMetrics.density).toInt() // 450dp в пикселях
        val popupWindow = PopupWindow(
            menuView,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.elevation = 10f

        val btn2nd = menuView.findViewById<Button>(R.id.btnMenu2nd)
        val btnSin = menuView.findViewById<Button>(R.id.btnMenuSin)
        val btnCos = menuView.findViewById<Button>(R.id.btnMenuCos)
        val btnTan = menuView.findViewById<Button>(R.id.btnMenuTan)
        val btnHyp = menuView.findViewById<Button>(R.id.btnMenuHyp)
        val btnSec = menuView.findViewById<Button>(R.id.btnMenuSec)
        val btnCsc = menuView.findViewById<Button>(R.id.btnMenuCsc)
        val btnCot = menuView.findViewById<Button>(R.id.btnMenuCot)

        fun updateTrigLabels() {
            // Тексты кнопок в зависимости от комбинации hyp и 2nd
            btnSin.text = when {
                isHypMode && isSecondMode -> "arsinh"
                isHypMode -> "sinh"
                isSecondMode -> "arcsin"
                else -> "sin"
            }
            btnCos.text = when {
                isHypMode && isSecondMode -> "arcosh"
                isHypMode -> "cosh"
                isSecondMode -> "arccos"
                else -> "cos"
            }
            btnTan.text = when {
                isHypMode && isSecondMode -> "artanh"
                isHypMode -> "tanh"
                isSecondMode -> "arctan"
                else -> "tan"
            }
            btnSec.text = when {
                isHypMode && isSecondMode -> "arsech"
                isHypMode -> "sech"
                isSecondMode -> "arcsec"
                else -> "sec"
            }
            btnCsc.text = when {
                isHypMode && isSecondMode -> "arcsch"
                isHypMode -> "csch"
                isSecondMode -> "arccsc"
                else -> "csc"
            }
            btnCot.text = when {
                isHypMode && isSecondMode -> "arcoth"
                isHypMode -> "coth"
                isSecondMode -> "arccot"
                else -> "cot"
            }
            // Подсветка hyp: оранжевый при включении
            btnHyp.setBackgroundColor(if (isHypMode) 0xFF8C00.toInt() else 0xFF3a3a3a.toInt())
            // Подсветка 2nd: оранжевый при включении
            btn2nd.setBackgroundColor(if (isSecondMode) 0xFF8C00.toInt() else 0xFF3a3a3a.toInt())
        }

        updateTrigLabels()

        btn2nd.setOnClickListener {
            isSecondMode = !isSecondMode
            updateTrigLabels()
            Toast.makeText(this, "2nd режим: ${if (isSecondMode) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
        }

        btnHyp.setOnClickListener {
            isHypMode = !isHypMode
            updateTrigLabels()
            Toast.makeText(this, "Hyp режим: ${if (isHypMode) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
        }

        // sin
        btnSin.setOnClickListener {
            val symbol = btnSin.text.toString()
            val operation: (Double) -> Double = when {
                isHypMode && isSecondMode -> { x -> asinh(x) }
                isHypMode -> { x -> sinh(x) }
                isSecondMode -> { x -> asin(x) }
                else -> { x -> sin(x) }
            }
            applyTrigonometry(symbol, operation)
            popupWindow.dismiss()
        }

        // cos
        btnCos.setOnClickListener {
            val symbol = btnCos.text.toString()
            val operation: (Double) -> Double = when {
                isHypMode && isSecondMode -> { x -> acosh(x) }
                isHypMode -> { x -> cosh(x) }
                isSecondMode -> { x -> acos(x) }
                else -> { x -> cos(x) }
            }
            applyTrigonometry(symbol, operation)
            popupWindow.dismiss()
        }

        // tan
        btnTan.setOnClickListener {
            val symbol = btnTan.text.toString()
            val operation: (Double) -> Double = when {
                isHypMode && isSecondMode -> { x -> atanh(x) }
                isHypMode -> { x -> tanh(x) }
                isSecondMode -> { x -> atan(x) }
                else -> { x -> tan(x) }
            }
            applyTrigonometry(symbol, operation)
            popupWindow.dismiss()
        }

        // sec
        btnSec.setOnClickListener {
            val symbol = btnSec.text.toString()
            val operation: (Double) -> Double = when {
                isHypMode && isSecondMode -> { x -> 1.0 / cosh(x) }  // arsech можно уточнить
                isHypMode -> { x -> 1.0 / cosh(x) }
                isSecondMode -> { x -> acos(1.0 / x) }
                else -> { x -> 1.0 / cos(x) }
            }
            applyTrigonometry(symbol, operation)
            popupWindow.dismiss()
        }

        // csc
        btnCsc.setOnClickListener {
            val symbol = btnCsc.text.toString()
            val operation: (Double) -> Double = when {
                isHypMode && isSecondMode -> { x -> 1.0 / sinh(x) }  // arcsch
                isHypMode -> { x -> 1.0 / sinh(x) }
                isSecondMode -> { x -> asin(1.0 / x) }
                else -> { x -> 1.0 / sin(x) }
            }
            applyTrigonometry(symbol, operation)
            popupWindow.dismiss()
        }

        // cot
        btnCot.setOnClickListener {
            val symbol = btnCot.text.toString()
            val operation: (Double) -> Double = when {
                isHypMode && isSecondMode -> { x -> 1.0 / tanh(x) }  // arcoth
                isHypMode -> { x -> 1.0 / tanh(x) }
                isSecondMode -> { x -> atan(1.0 / x) }
                else -> { x -> 1.0 / tan(x) }
            }
            applyTrigonometry(symbol, operation)
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 0)
    }


    // Обновление текста кнопок в режиме 2nd
    private fun updateSecondModeButtons() {
        findViewById<Button>(R.id.btnSquare)?.text = if (isSecondMode) "x³" else "x²"
        findViewById<Button>(R.id.btn10x)?.text = if (isSecondMode) "2^x" else "10^x"
        findViewById<Button>(R.id.btnLog)?.text = if (isSecondMode) "log_y X" else "log"
    }

    // ---------- Цифровые кнопки ----------
    private fun setNumberButtonListeners() {
        val numberIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        numberIds.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener {
                onNumberClick(it as Button)
            }
        }
    }

    // ---------- Операторы + = ----------
    private fun setOperatorButtonListeners() {
        val operatorIds = listOf(
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide
        )
        operatorIds.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener {
                onOperatorClick(it as Button)
            }
        }

        findViewById<Button>(R.id.btnEquals)?.setOnClickListener {
            onEqualsClick()
        }
    }

    // ---------- Кнопки памяти ----------
    private fun setMemoryButtonListeners() {
        findViewById<Button>(R.id.btnMC)?.setOnClickListener { memory = 0.0 }
        findViewById<Button>(R.id.btnMR)?.setOnClickListener {
            tvResult.text = formatNumber(memory)
            isNewOperation = true
            isDecimalPressed = memory.toString().contains(".")
            addToHistory("MR →", memory)
        }
        findViewById<Button>(R.id.btnMPlus)?.setOnClickListener {
            memory += currentValue()
        }
        findViewById<Button>(R.id.btnMMinus)?.setOnClickListener {
            memory -= currentValue()
        }
        findViewById<Button>(R.id.btnMS)?.setOnClickListener {
            memory = currentValue()
        }
        findViewById<Button>(R.id.btnMTriangle)?.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.memory_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_mc -> memory = 0.0
                    R.id.action_mr -> {
                        tvResult.text = formatNumber(memory)
                        isNewOperation = true
                        isDecimalPressed = memory.toString().contains(".")
                        addToHistory("MR →", memory)
                    }
                    R.id.action_mplus -> memory += currentValue()
                    R.id.action_mminus -> memory -= currentValue()
                    R.id.action_ms -> memory = currentValue()
                }
                true
            }
            popup.show()
        }
    }

    // ---------- Управление (C, CE, ⌫, ±, ,) ----------
    private fun setControlButtonListeners() {
        findViewById<Button>(R.id.btnClear)?.setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnCE)?.setOnClickListener { onCEClick() }
        findViewById<Button>(R.id.btnBackspace)?.setOnClickListener { onBackspaceClick() }
        findViewById<Button>(R.id.btnSign)?.setOnClickListener { onSignClick() }
        findViewById<Button>(R.id.btnDot)?.setOnClickListener { onDotClick() }
    }

    // ---------- История ----------
    private fun setupHistoryButton() {
        findViewById<ImageButton>(R.id.btnHistory).setOnClickListener {
            showHistoryDialog()
        }
    }

    private fun showHistoryDialog() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "История пуста", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("История вычислений")
            .setItems(historyList.toTypedArray()) { _, _ -> }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun addToHistory(expression: String, result: Double) {
        val entry = "$expression = ${formatNumber(result)}"
        historyList.add(0, entry)
        if (historyList.size > 20) {
            historyList.removeAt(historyList.size - 1)
        }
    }

    // ---------- Вспомогательные методы ----------
    private fun currentValue(): Double {
        return tvResult.text.toString().toDoubleOrNull() ?: 0.0
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun applyUnaryOperation(symbol: String, operation: (Double) -> Double) {
        val value = currentValue()
        val result = operation(value)
        if (result.isNaN() || result.isInfinite()) {
            tvResult.text = "Ошибка"
        } else {
            tvResult.text = formatNumber(result)
            addToHistory("$symbol(${formatNumber(value)})", result)
        }
        isNewOperation = true
        isDecimalPressed = false
    }

    private fun applyTrigonometry(symbol: String, operation: (Double) -> Double) {
        applyUnaryOperation(symbol, operation)
    }

    private fun onNumberClick(button: Button) {
        val digit = button.text.toString()
        val currentText = tvResult.text.toString()

        when {
            isNewOperation -> {
                tvResult.text = if (digit == ",") "0." else digit
                isNewOperation = false
            }
            currentText == "0" && digit != "," -> {
                tvResult.text = digit
            }
            else -> {
                tvResult.append(if (digit == ",") "." else digit)
            }
        }
        isDecimalPressed = tvResult.text.contains(".")
    }

    private fun onOperatorClick(button: Button) {
        val currentText = tvResult.text.toString()
        if (currentText.isEmpty()) return

        if (operator.isNotEmpty() && !isNewOperation) {
            operand2 = currentText.toDouble()
            compute()
        } else {
            operand1 = currentText.toDouble()
        }

        operator = when (button.id) {
            R.id.btnAdd -> "+"
            R.id.btnSubtract -> "-"
            R.id.btnMultiply -> "×"
            R.id.btnDivide -> "/"
            else -> ""
        }
        isNewOperation = true
        isDecimalPressed = false
    }

    private fun onOperatorClickWithSymbol(symbol: String) {
        val currentText = tvResult.text.toString()
        if (currentText.isEmpty()) return
        if (operator.isNotEmpty() && !isNewOperation) {
            operand2 = currentText.toDouble()
            compute()
        } else {
            operand1 = currentText.toDouble()
        }
        operator = symbol
        isNewOperation = true
        isDecimalPressed = false
    }

    private fun onEqualsClick() {
        val currentText = tvResult.text.toString()
        if (operator.isEmpty() || currentText.isEmpty()) return

        val left = operand1
        val right = currentText.toDouble()
        val op = operator

        operand2 = right
        compute()

        if (tvResult.text != "Ошибка") {
            val expression = "${formatNumber(left)} $op ${formatNumber(right)}"
            addToHistory(expression, operand1)
        }
    }

    private fun compute() {
        val result = when (operator) {
            "+" -> operand1 + operand2
            "-" -> operand1 - operand2
            "×" -> operand1 * operand2
            "/" -> {
                if (operand2 == 0.0) {
                    tvResult.text = "Ошибка"
                    return
                } else {
                    operand1 / operand2
                }
            }
            "^" -> operand1.pow(operand2)
            "mod" -> operand1 % operand2
            "logy" -> {
                if (operand1 <= 0.0 || operand1 == 1.0 || operand2 <= 0.0) {
                    tvResult.text = "Ошибка"
                    return
                }
                ln(operand2) / ln(operand1)
            }
            else -> operand2
        }
        tvResult.text = formatNumber(result)
        operand1 = result
        isNewOperation = true
    }

    private fun onClearClick() {
        tvResult.text = "0"
        operand1 = 0.0
        operand2 = 0.0
        operator = ""
        isNewOperation = true
        isDecimalPressed = false
    }

    private fun onCEClick() {
        tvResult.text = "0"
        isNewOperation = true
        isDecimalPressed = false
    }

    private fun onBackspaceClick() {
        val text = tvResult.text.toString()
        if (text.length > 1) {
            val newText = text.dropLast(1)
            tvResult.text = newText
            if (newText == "-") {
                tvResult.text = "0"
            }
        } else {
            tvResult.text = "0"
        }
        isDecimalPressed = tvResult.text.contains(".")
    }

    private fun onSignClick() {
        val value = currentValue()
        val newValue = -value
        tvResult.text = formatNumber(newValue)
        isDecimalPressed = newValue.toString().contains(".")
        addToHistory("±(${formatNumber(value)})", newValue)
    }

    private fun onDotClick() {
        if (isDecimalPressed) return
        val currentText = tvResult.text.toString()
        if (isNewOperation) {
            tvResult.text = "0."
            isNewOperation = false
        } else {
            tvResult.append(".")
        }
        isDecimalPressed = true
    }
}