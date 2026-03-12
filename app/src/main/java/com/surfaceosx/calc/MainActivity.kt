package com.surfaceosx.calc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.*
import java.util.Stack

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvMode: TextView

    private var currentExpression = StringBuilder()
    private var isNewOperation = true
    private var isDecimalPressed = false
    private var memory: Double = 0.0

    private var isScientificMode = false
    private var isSecondMode = false      // режим 2nd
    private var isHypMode = false         // режим гиперболических функций
    private var isDegMode = true          // true = DEG, false = RAD
    private var isFEMode = false          // для F-E (пока не используется)

    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        tvMode = findViewById(R.id.tvMode)

        // Отступ под статус-бар для корневого layout
        val rootLayout = findViewById<LinearLayout>(R.id.root_layout)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, statusBarHeight, v.paddingRight, v.paddingBottom)
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
        clearExpression()
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
            val btn2nd = findViewById<Button>(R.id.btn2nd)
            btn2nd?.setBackgroundColor(0xFF8C00.toInt()) // оранжевый (неактивный)
        } else {
            setBasicFunctionButtonListeners()
        }
    }

    // ---------- Обычные функции (немедленное вычисление) ----------
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
        btn2nd?.setBackgroundColor(0xFF8C00.toInt())
        btn2nd?.setOnClickListener {
            isSecondMode = !isSecondMode
            updateSecondModeButtons()
            btn2nd.setBackgroundColor(if (isSecondMode) 0xFF3a3a3a.toInt() else 0xFF8C00.toInt())
            Toast.makeText(this, "2nd режим: ${if (isSecondMode) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
        }

        // Кнопка Trigonometry ▼
        findViewById<Button>(R.id.btnTrigonometry)?.setOnClickListener { view ->
            showTrigonometryMenu(view)
        }

        // Кнопка f Function ▼
        findViewById<Button>(R.id.btnFunction)?.setOnClickListener { view ->
            showFunctionMenu(view)
        }

        // Кнопка DEG/RAD
        findViewById<Button>(R.id.btnDegRadToggle)?.setOnClickListener {
            isDegMode = !isDegMode
            (it as Button).text = if (isDegMode) "DEG" else "RAD"
            Toast.makeText(this, "Режим углов: ${if (isDegMode) "DEG" else "RAD"}", Toast.LENGTH_SHORT).show()
        }

        // Кнопка F-E
        findViewById<Button>(R.id.btnFEToggle)?.setOnClickListener {
            isFEMode = !isFEMode
            Toast.makeText(this, "Формат чисел: ${if (isFEMode) "экспоненциальный" else "обычный"}", Toast.LENGTH_SHORT).show()
            // TODO: реализовать переключение формата отображения чисел
        }

        // π и e – константы (добавляем в выражение)
        findViewById<Button>(R.id.btnPi)?.setOnClickListener {
            appendConstant(PI.toString())
        }
        findViewById<Button>(R.id.btnE)?.setOnClickListener {
            appendConstant(E.toString())
        }

        // Унарные функции – немедленное вычисление
        findViewById<Button>(R.id.btnAbs)?.setOnClickListener {
            applyUnaryOperation("abs") { abs(it) }
        }
        findViewById<Button>(R.id.btnExp)?.setOnClickListener {
            applyUnaryOperation("exp") { exp(it) }
        }
        findViewById<Button>(R.id.btnCubeRoot)?.setOnClickListener {
            applyUnaryOperation("∛") { cbrt(it) }
        }
        findViewById<Button>(R.id.btnFactorial)?.setOnClickListener {
            val value = currentValue().toInt()
            if (value >= 0) {
                var fact = 1.0
                for (i in 1..value) fact *= i
                tvResult.text = formatNumber(fact)
                addToHistory("n!($value)", fact)
                currentExpression.clear()
                currentExpression.append(fact)
                isNewOperation = true
                isDecimalPressed = false
            } else {
                tvResult.text = "Ошибка"
            }
        }

        // Скобки – добавляем в выражение
        findViewById<Button>(R.id.btnLeftParen)?.setOnClickListener {
            appendToExpression("(")
        }
        findViewById<Button>(R.id.btnRightParen)?.setOnClickListener {
            appendToExpression(")")
        }

        // Кнопка возведения в степень ^ (бинарный оператор)
        findViewById<Button>(R.id.btnPower)?.setOnClickListener {
            appendOperator("^")
        }

        // Кнопка log (в обычном режиме – немедленный log10, в режиме 2nd – бинарный log_y)
        findViewById<Button>(R.id.btnLog)?.setOnClickListener {
            if (isSecondMode) {
                appendToExpression("l")  // символ для log_y
            } else {
                applyUnaryOperation("log") { log10(it) }
            }
        }

        // Кнопка ln – немедленный натуральный логарифм
        findViewById<Button>(R.id.btnLn)?.setOnClickListener {
            applyUnaryOperation("ln") { ln(it) }
        }

        // Кнопка x² / x³ (немедленное вычисление в зависимости от 2nd)
        findViewById<Button>(R.id.btnSquare)?.setOnClickListener {
            if (isSecondMode) {
                applyUnaryOperation("x³") { it * it * it }
            } else {
                applyUnaryOperation("x²") { it * it }
            }
        }

        // Кнопка 10^ / 2^ (бинарный оператор, вставляем основание и символ степени)
        findViewById<Button>(R.id.btnBasePow)?.setOnClickListener {
            if (isSecondMode) {
                appendToExpression("2^")
            } else {
                appendToExpression("10^")
            }
        }

        // mod – бинарный оператор
        findViewById<Button>(R.id.btnMod)?.setOnClickListener {
            appendOperator("mod")
        }
    }

    // ---------- Тригонометрическое меню ----------
    private fun showTrigonometryMenu(anchor: View) {
        val inflater = LayoutInflater.from(this)
        val menuView = inflater.inflate(R.layout.trigonometry_menu, null)
        val displayMetrics = resources.displayMetrics
        val width = (450 * displayMetrics.density).toInt()
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
            btnHyp.setBackgroundColor(if (isHypMode) 0xFF8C00.toInt() else 0xFF3a3a3a.toInt())
            btn2nd.setBackgroundColor(if (isSecondMode) 0xFF8C00.toInt() else 0xFF3a3a3a.toInt())
        }

        updateTrigLabels()

        btn2nd.setOnClickListener {
            isSecondMode = !isSecondMode
            updateTrigLabels()
            findViewById<Button>(R.id.btn2nd)?.setBackgroundColor(if (isSecondMode) 0xFF3a3a3a.toInt() else 0xFF8C00.toInt())
            Toast.makeText(this, "2nd режим: ${if (isSecondMode) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
        }

        btnHyp.setOnClickListener {
            isHypMode = !isHypMode
            updateTrigLabels()
            Toast.makeText(this, "Hyp режим: ${if (isHypMode) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
        }

        btnSin.setOnClickListener {
            val symbol = btnSin.text.toString()
            applyTrigonometry(symbol) { x ->
                when (symbol) {
                    "sin" -> sin(if (isDegMode) Math.toRadians(x) else x)
                    "arcsin" -> {
                        val res = asin(x)
                        if (isDegMode) Math.toDegrees(res) else res
                    }
                    "sinh" -> sinh(x)
                    "arsinh" -> asinh(x)
                    else -> x
                }
            }
            popupWindow.dismiss()
        }

        btnCos.setOnClickListener {
            val symbol = btnCos.text.toString()
            applyTrigonometry(symbol) { x ->
                when (symbol) {
                    "cos" -> cos(if (isDegMode) Math.toRadians(x) else x)
                    "arccos" -> {
                        val res = acos(x)
                        if (isDegMode) Math.toDegrees(res) else res
                    }
                    "cosh" -> cosh(x)
                    "arcosh" -> acosh(x)
                    else -> x
                }
            }
            popupWindow.dismiss()
        }

        btnTan.setOnClickListener {
            val symbol = btnTan.text.toString()
            applyTrigonometry(symbol) { x ->
                when (symbol) {
                    "tan" -> tan(if (isDegMode) Math.toRadians(x) else x)
                    "arctan" -> {
                        val res = atan(x)
                        if (isDegMode) Math.toDegrees(res) else res
                    }
                    "tanh" -> tanh(x)
                    "artanh" -> atanh(x)
                    else -> x
                }
            }
            popupWindow.dismiss()
        }

        btnSec.setOnClickListener {
            val symbol = btnSec.text.toString()
            applyTrigonometry(symbol) { x ->
                when (symbol) {
                    "sec" -> 1 / cos(if (isDegMode) Math.toRadians(x) else x)
                    "arcsec" -> {
                        val res = acos(1 / x)
                        if (isDegMode) Math.toDegrees(res) else res
                    }
                    "sech" -> 1 / cosh(x)
                    "arsech" -> acosh(1 / x)
                    else -> x
                }
            }
            popupWindow.dismiss()
        }

        btnCsc.setOnClickListener {
            val symbol = btnCsc.text.toString()
            applyTrigonometry(symbol) { x ->
                when (symbol) {
                    "csc" -> 1 / sin(if (isDegMode) Math.toRadians(x) else x)
                    "arccsc" -> {
                        val res = asin(1 / x)
                        if (isDegMode) Math.toDegrees(res) else res
                    }
                    "csch" -> 1 / sinh(x)
                    "arcsch" -> asinh(1 / x)
                    else -> x
                }
            }
            popupWindow.dismiss()
        }

        btnCot.setOnClickListener {
            val symbol = btnCot.text.toString()
            applyTrigonometry(symbol) { x ->
                when (symbol) {
                    "cot" -> 1 / tan(if (isDegMode) Math.toRadians(x) else x)
                    "arccot" -> {
                        val res = atan(1 / x)
                        if (isDegMode) Math.toDegrees(res) else res
                    }
                    "coth" -> 1 / tanh(x)
                    "arcoth" -> atanh(1 / x)
                    else -> x
                }
            }
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 0)
    }

    // ---------- Меню функций ----------
    private fun showFunctionMenu(anchor: View) {
        val inflater = LayoutInflater.from(this)
        val menuView = inflater.inflate(R.layout.function_menu, null)
        val displayMetrics = resources.displayMetrics
        val width = (400 * displayMetrics.density).toInt()
        val popupWindow = PopupWindow(
            menuView,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.elevation = 10f

        val btnAbs = menuView.findViewById<Button>(R.id.btnMenuAbs)
        val btnFloor = menuView.findViewById<Button>(R.id.btnMenuFloor)
        val btnCeil = menuView.findViewById<Button>(R.id.btnMenuCeil)
        val btnRand = menuView.findViewById<Button>(R.id.btnMenuRand)
        val btnDms = menuView.findViewById<Button>(R.id.btnMenuDms)
        val btnDeg = menuView.findViewById<Button>(R.id.btnMenuDeg)

        btnAbs.setOnClickListener {
            applyUnaryOperation("abs") { abs(it) }
            popupWindow.dismiss()
        }

        btnFloor.setOnClickListener {
            applyUnaryOperation("floor") { floor(it) }
            popupWindow.dismiss()
        }

        btnCeil.setOnClickListener {
            applyUnaryOperation("ceil") { ceil(it) }
            popupWindow.dismiss()
        }

        btnRand.setOnClickListener {
            val randValue = (0..10000).random() / 10000.0
            tvResult.text = formatNumber(randValue)
            addToHistory("rand", randValue)
            currentExpression.clear()
            currentExpression.append(randValue)
            isNewOperation = true
            isDecimalPressed = false
            popupWindow.dismiss()
        }

        btnDms.setOnClickListener {
            Toast.makeText(this, "DMS conversion not implemented", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        btnDeg.setOnClickListener {
            isDegMode = !isDegMode
            findViewById<Button>(R.id.btnDegRadToggle)?.text = if (isDegMode) "DEG" else "RAD"
            Toast.makeText(this, "Режим углов: ${if (isDegMode) "DEG" else "RAD"}", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 0)
    }

    // Обновление текста кнопок в режиме 2nd (основной интерфейс)
    private fun updateSecondModeButtons() {
        findViewById<Button>(R.id.btnSquare)?.text = if (isSecondMode) "x³" else "x²"
        findViewById<Button>(R.id.btnLog)?.text = if (isSecondMode) "log_y" else "log"
        findViewById<Button>(R.id.btnBasePow)?.text = if (isSecondMode) "2^" else "10^"
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

    private fun onNumberClick(button: Button) {
        val digit = button.text.toString()
        if (isNewOperation) {
            currentExpression = StringBuilder()
            tvResult.text = ""
            isNewOperation = false
        }
        val displayDigit = if (digit == ",") "." else digit
        currentExpression.append(displayDigit)
        tvResult.append(displayDigit)
        isDecimalPressed = currentExpression.contains(".")
    }

    // ---------- Операторы + - × ÷ и = ----------
    private fun setOperatorButtonListeners() {
        findViewById<Button>(R.id.btnAdd)?.setOnClickListener { appendOperator("+") }
        findViewById<Button>(R.id.btnSubtract)?.setOnClickListener { appendMinus() } // унарный/бинарный минус
        findViewById<Button>(R.id.btnMultiply)?.setOnClickListener { appendOperator("×") }
        findViewById<Button>(R.id.btnDivide)?.setOnClickListener { appendOperator("÷") }

        findViewById<Button>(R.id.btnEquals)?.setOnClickListener {
            evaluateCurrentExpression()
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
            currentExpression.clear()
            currentExpression.append(memory)
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
        findViewById<Button>(R.id.btnMTriangle)?.setOnClickListener {
            Toast.makeText(this, "Память: ${formatNumber(memory)}", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Управление (C, CE, ⌫, ±, ,) ----------
    private fun setControlButtonListeners() {
        findViewById<Button>(R.id.btnClear)?.setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnCE)?.setOnClickListener { onCEClick() }
        findViewById<Button>(R.id.btnBackspace)?.setOnClickListener { onBackspaceClick() }
        findViewById<Button>(R.id.btnSign)?.setOnClickListener { applyUnaryOperation("±") { -it } }
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

    // ---------- Вспомогательные методы для построения выражения ----------
    private fun appendToExpression(text: String) {
        if (isNewOperation) {
            currentExpression = StringBuilder()
            tvResult.text = ""
            isNewOperation = false
        }
        currentExpression.append(text)
        tvResult.append(text)
        isDecimalPressed = currentExpression.contains(".")
    }

    private fun appendOperator(op: String) {
        appendToExpression(op)
    }

    private fun appendConstant(const: String) {
        if (isNewOperation) {
            currentExpression = StringBuilder()
            tvResult.text = ""
            isNewOperation = false
        }
        currentExpression.append(const)
        tvResult.append(const)
    }

    private fun appendMinus() {
        if (isNewOperation) {
            currentExpression = StringBuilder()
            tvResult.text = ""
            isNewOperation = false
        }
        currentExpression.append('-')
        tvResult.append("-")
        isDecimalPressed = false
    }

    private fun clearExpression() {
        currentExpression.clear()
        tvResult.text = ""
        isNewOperation = true
        isDecimalPressed = false
    }

    // ---------- Вычисление выражения для бинарных операций ----------
    private fun evaluateCurrentExpression() {
        var expr = currentExpression.toString()
        if (expr.isEmpty()) return
        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }
        if (openCount > closeCount) {
            expr += ")".repeat(openCount - closeCount)
            currentExpression = StringBuilder(expr)
            tvResult.text = expr
        }
        try {
            val result = evaluateExpression(expr)
            tvResult.text = formatNumber(result)
            addToHistory(expr, result)
            currentExpression.clear()
            currentExpression.append(result)
            isNewOperation = true
        } catch (e: Exception) {
            tvResult.text = "Ошибка"
            e.printStackTrace()
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Парсер выражений (обратная польская нотация) – без изменений
    private fun evaluateExpression(expression: String): Double {
        val output = mutableListOf<Any>()
        val stack = Stack<Any>()

        val precedence = mapOf(
            '+' to 1, '-' to 1,
            '×' to 2, '÷' to 2,
            '^' to 3, 'm' to 3, // mod
            'l' to 4, // log_y
            "sin" to 5, "cos" to 5, "tan" to 5,
            "arcsin" to 5, "arccos" to 5, "arctan" to 5,
            "sinh" to 5, "cosh" to 5, "tanh" to 5,
            "arsinh" to 5, "arcosh" to 5, "artanh" to 5,
            "sec" to 5, "csc" to 5, "cot" to 5,
            "arcsec" to 5, "arccsc" to 5, "arccot" to 5,
            "sech" to 5, "csch" to 5, "coth" to 5,
            "arsech" to 5, "arcsch" to 5, "arcoth" to 5,
            "ln" to 5, "log" to 5, "exp" to 5,
            "sqrt" to 5, "cbrt" to 5, "abs" to 5,
            "floor" to 5, "ceil" to 5,
            "n!" to 5, "x²" to 5, "x³" to 5,
            "%" to 5, "±" to 5
        )

        var i = 0
        val len = expression.length
        while (i < len) {
            val ch = expression[i]

            when {
                ch.isDigit() || ch == '.' || (ch == '-' && (i == 0 || expression[i-1] == '(' || expression[i-1] in setOf('+','-','×','÷','^','m','l','('))) -> {
                    val start = i
                    if (ch == '-') i++
                    while (i < len && (expression[i].isDigit() || expression[i] == '.')) i++
                    val numStr = expression.substring(start, i)
                    output.add(numStr.toDouble())
                    continue
                }
                ch in setOf('+', '-', '×', '÷', '^', 'm', 'l') -> {
                    while (stack.isNotEmpty() && stack.peek() !is String && stack.peek() != '(' && precedence[stack.peek() as Char]!! >= precedence[ch]!!) {
                        output.add(stack.pop())
                    }
                    stack.push(ch)
                }
                ch == '(' -> {
                    stack.push('(')
                }
                ch == ')' -> {
                    while (stack.isNotEmpty() && stack.peek() != '(') {
                        output.add(stack.pop())
                    }
                    if (stack.isNotEmpty() && stack.peek() == '(') {
                        stack.pop()
                    }
                    if (stack.isNotEmpty() && stack.peek() is String) {
                        output.add(stack.pop())
                    }
                }
                else -> {
                    if (ch.isLetter()) {
                        val start = i
                        while (i < len && expression[i].isLetter()) i++
                        val func = expression.substring(start, i)
                        if (func == "π") {
                            output.add(PI)
                        } else if (func == "e") {
                            output.add(E)
                        } else {
                            stack.push(func)
                        }
                        continue
                    } else {
                        i++
                    }
                }
            }
            i++
        }

        while (stack.isNotEmpty()) {
            output.add(stack.pop())
        }

        val valueStack = Stack<Double>()
        for (token in output) {
            when (token) {
                is Double -> valueStack.push(token)
                is String -> {
                    val arg = valueStack.pop()
                    val result = when (token) {
                        "sin" -> {
                            val rad = if (isDegMode) Math.toRadians(arg) else arg
                            sin(rad)
                        }
                        "cos" -> {
                            val rad = if (isDegMode) Math.toRadians(arg) else arg
                            cos(rad)
                        }
                        "tan" -> {
                            val rad = if (isDegMode) Math.toRadians(arg) else arg
                            tan(rad)
                        }
                        "arcsin" -> {
                            val res = asin(arg)
                            if (isDegMode) Math.toDegrees(res) else res
                        }
                        "arccos" -> {
                            val res = acos(arg)
                            if (isDegMode) Math.toDegrees(res) else res
                        }
                        "arctan" -> {
                            val res = atan(arg)
                            if (isDegMode) Math.toDegrees(res) else res
                        }
                        "sinh" -> sinh(arg)
                        "cosh" -> cosh(arg)
                        "tanh" -> tanh(arg)
                        "arsinh" -> asinh(arg)
                        "arcosh" -> acosh(arg)
                        "artanh" -> atanh(arg)
                        "sec" -> {
                            val rad = if (isDegMode) Math.toRadians(arg) else arg
                            1 / cos(rad)
                        }
                        "csc" -> {
                            val rad = if (isDegMode) Math.toRadians(arg) else arg
                            1 / sin(rad)
                        }
                        "cot" -> {
                            val rad = if (isDegMode) Math.toRadians(arg) else arg
                            1 / tan(rad)
                        }
                        "arcsec" -> {
                            val res = acos(1 / arg)
                            if (isDegMode) Math.toDegrees(res) else res
                        }
                        "arccsc" -> {
                            val res = asin(1 / arg)
                            if (isDegMode) Math.toDegrees(res) else res
                        }
                        "arccot" -> {
                            val res = atan(1 / arg)
                            if (isDegMode) Math.toDegrees(res) else res
                        }
                        "sech" -> 1 / cosh(arg)
                        "csch" -> 1 / sinh(arg)
                        "coth" -> 1 / tanh(arg)
                        "arsech" -> acosh(1 / arg)
                        "arcsch" -> asinh(1 / arg)
                        "arcoth" -> atanh(1 / arg)
                        "ln" -> ln(arg)
                        "log" -> log10(arg)
                        "exp" -> exp(arg)
                        "sqrt" -> sqrt(arg)
                        "cbrt" -> cbrt(arg)
                        "abs" -> abs(arg)
                        "floor" -> floor(arg)
                        "ceil" -> ceil(arg)
                        "n!" -> {
                            var fact = 1.0
                            for (j in 1..arg.toInt()) fact *= j
                            fact
                        }
                        "x²" -> arg * arg
                        "x³" -> arg * arg * arg
                        "%" -> arg / 100.0
                        "±" -> -arg
                        else -> throw IllegalArgumentException("Неизвестная функция: $token")
                    }
                    valueStack.push(result)
                }
                is Char -> {
                    val b = valueStack.pop()
                    val a = valueStack.pop()
                    val result = when (token) {
                        '+' -> a + b
                        '-' -> a - b
                        '×' -> a * b
                        '÷' -> a / b
                        '^' -> a.pow(b)
                        'm' -> a % b
                        'l' -> ln(b) / ln(a) // log_y
                        else -> throw IllegalArgumentException("Неизвестный оператор: $token")
                    }
                    valueStack.push(result)
                }
            }
        }
        return valueStack.pop()
    }

    // ---------- Унарные операции (немедленное вычисление) ----------
    private fun applyUnaryOperation(symbol: String, operation: (Double) -> Double) {
        val value = currentValue()
        val result = operation(value)
        if (result.isNaN() || result.isInfinite()) {
            tvResult.text = "Ошибка"
        } else {
            tvResult.text = formatNumber(result)
            addToHistory("$symbol(${formatNumber(value)})", result)
            currentExpression.clear()
            currentExpression.append(result)
        }
        isNewOperation = true
        isDecimalPressed = false
    }

    private fun applyTrigonometry(symbol: String, operation: (Double) -> Double) {
        applyUnaryOperation(symbol, operation)
    }

    // ---------- Остальные методы ----------
    private fun currentValue(): Double {
        return tvResult.text.toString().toDoubleOrNull() ?: 0.0
    }

    private fun formatNumber(value: Double): String {
        // TODO: учесть isFEMode для экспоненциального формата
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun onClearClick() {
        clearExpression()
    }

    private fun onCEClick() {
        clearExpression()
    }

    private fun onBackspaceClick() {
        if (currentExpression.isEmpty()) return
        val lastIndex = currentExpression.length - 1
        if (currentExpression[lastIndex] == '(') {
            currentExpression.deleteCharAt(lastIndex)
            var pos = currentExpression.length - 1
            while (pos >= 0 && currentExpression[pos].isLetter()) {
                pos--
            }
            if (pos + 1 < currentExpression.length) {
                currentExpression.delete(pos + 1, currentExpression.length)
            }
            if (currentExpression.isEmpty()) {
                tvResult.text = ""
                isNewOperation = true
                isDecimalPressed = false
                return
            }
        } else {
            currentExpression.deleteCharAt(lastIndex)
        }
        tvResult.text = currentExpression.toString()
        isDecimalPressed = currentExpression.contains(".")
    }

    private fun onDotClick() {
        if (isDecimalPressed) return
        if (isNewOperation) {
            currentExpression = StringBuilder("0.")
            tvResult.text = "0."
            isNewOperation = false
        } else {
            currentExpression.append(".")
            tvResult.append(".")
        }
        isDecimalPressed = true
    }
}