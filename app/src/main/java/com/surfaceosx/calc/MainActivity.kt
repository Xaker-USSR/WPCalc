package com.surfaceosx.calc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
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
    private lateinit var tvDegRad: TextView
    private lateinit var tvFE: TextView

    private var currentExpression = StringBuilder()
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
            btn2nd?.setBackgroundColor(0xFF8C00.toInt()) // оранжевый по умолчанию (неактивный)
        } else {
            setBasicFunctionButtonListeners()
        }
    }

    // ---------- Обычные функции ----------
    private fun setBasicFunctionButtonListeners() {
        findViewById<Button>(R.id.btnPercent)?.setOnClickListener {
            appendFunction("%(")
        }
        findViewById<Button>(R.id.btnSqrt)?.setOnClickListener {
            appendFunction("√(")
        }
        findViewById<Button>(R.id.btnSquare)?.setOnClickListener {
            appendFunction("x²(")
        }
        findViewById<Button>(R.id.btnReciprocal)?.setOnClickListener {
            appendFunction("1/x(")
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

        findViewById<Button>(R.id.btnFunction)?.setOnClickListener {
            Toast.makeText(this, "Function menu (в разработке)", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnPi)?.setOnClickListener {
            appendConstant(PI.toString())
        }

        findViewById<Button>(R.id.btnE)?.setOnClickListener {
            appendConstant(E.toString())
        }

        findViewById<Button>(R.id.btnAbs)?.setOnClickListener {
            appendFunction("abs(")
        }

        findViewById<Button>(R.id.btnExp)?.setOnClickListener {
            appendFunction("exp(")
        }

        findViewById<Button>(R.id.btnMod)?.setOnClickListener {
            appendOperator("mod")
        }

        findViewById<Button>(R.id.btnCubeRoot)?.setOnClickListener {
            appendFunction("cbrt(")
        }

        findViewById<Button>(R.id.btnLeftParen)?.setOnClickListener {
            appendToExpression("(")
        }

        findViewById<Button>(R.id.btnRightParen)?.setOnClickListener {
            appendToExpression(")")
        }

        findViewById<Button>(R.id.btnFactorial)?.setOnClickListener {
            appendFunction("n!(")
        }

        // Кнопка возведения в степень ^ (бывшая x^y)
        findViewById<Button>(R.id.btnPower)?.setOnClickListener {
            onOperatorClickWithSymbol("^")  // используем существующий метод
        }

        findViewById<Button>(R.id.btnBasePow)?.setOnClickListener {
            if (isSecondMode) {
                appendToExpression("2^")
            } else {
                appendToExpression("10^")
            }
        }

        // Кнопка log (меняется в режиме 2nd)
        findViewById<Button>(R.id.btnLog)?.setOnClickListener {
            if (isSecondMode) {
                appendToExpression("l")  // символ для log_y (бинарный)
            } else {
                appendFunction("log(")
            }
        }

        findViewById<Button>(R.id.btnLn)?.setOnClickListener {
            appendFunction("ln(")
        }

        // Кнопка x² / x³ (меняется в режиме 2nd)
        findViewById<Button>(R.id.btnSquare)?.setOnClickListener {
            if (isSecondMode) {
                appendFunction("x³(")
            } else {
                appendFunction("x²(")
            }
        }

        // Кнопки 10^x и 2^x удалены
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
            appendFunction(btnSin.text.toString() + "(")
            popupWindow.dismiss()
        }

        btnCos.setOnClickListener {
            appendFunction(btnCos.text.toString() + "(")
            popupWindow.dismiss()
        }

        btnTan.setOnClickListener {
            appendFunction(btnTan.text.toString() + "(")
            popupWindow.dismiss()
        }

        btnSec.setOnClickListener {
            appendFunction(btnSec.text.toString() + "(")
            popupWindow.dismiss()
        }

        btnCsc.setOnClickListener {
            appendFunction(btnCsc.text.toString() + "(")
            popupWindow.dismiss()
        }

        btnCot.setOnClickListener {
            appendFunction(btnCot.text.toString() + "(")
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
            appendConstant(formatNumber(memory))
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
                        appendConstant(formatNumber(memory))
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
        findViewById<Button>(R.id.btnSign)?.setOnClickListener { appendFunction("±(") }
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

    private fun appendFunction(func: String) {
        appendToExpression("$func(")
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

    // ---------- Вычисление выражения ----------
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
                        "sin" -> sin(arg)
                        "cos" -> cos(arg)
                        "tan" -> tan(arg)
                        "arcsin" -> asin(arg)
                        "arccos" -> acos(arg)
                        "arctan" -> atan(arg)
                        "sinh" -> sinh(arg)
                        "cosh" -> cosh(arg)
                        "tanh" -> tanh(arg)
                        "arsinh" -> asinh(arg)
                        "arcosh" -> acosh(arg)
                        "artanh" -> atanh(arg)
                        "sec" -> 1 / cos(arg)
                        "csc" -> 1 / sin(arg)
                        "cot" -> 1 / tan(arg)
                        "arcsec" -> acos(1 / arg)
                        "arccsc" -> asin(1 / arg)
                        "arccot" -> atan(1 / arg)
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

    // ---------- Остальные методы ----------
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

    private fun onSignClick() {
        appendFunction("±(")
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

    private fun onOperatorClickWithSymbol(symbol: String) {
        appendToExpression(symbol)
    }
}