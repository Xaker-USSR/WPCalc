package com.surfaceosx.calc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import kotlin.math.*
import java.util.Stack

enum class CalculatorMode { STANDARD, SCIENTIFIC, PROGRAMMER }
enum class NumberBase { HEX, DEC, OCT, BIN }
enum class ShiftMode { ARITHMETIC, LOGICAL, ROTATE, ROTATE_CARRY }
enum class WordSize { QWORD, DWORD, WORD, BYTE }

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvMode: TextView
    private lateinit var drawerLayout: DrawerLayout

    private var currentExpression = StringBuilder()
    private var isNewOperation = true
    private var isDecimalPressed = false
    private var memory: Double = 0.0

    private var currentMode = CalculatorMode.STANDARD
    private var isSecondMode = false
    private var isHypMode = false
    private var isDegMode = true
    private var isFEMode = false

    private var operand1: Double = 0.0
    private var operand2: Double = 0.0
    private var operator: String = ""

    // Для режима программиста
    private var currentBase = NumberBase.DEC
    private var currentWordSize = WordSize.QWORD
    private var currentBits = 64
    private lateinit var tvHexValue: TextView
    private lateinit var tvDecValue: TextView
    private lateinit var tvOctValue: TextView
    private lateinit var tvBinValue: TextView

    // Компоненты битовой панели
    private lateinit var standardKeyboard: View
    private lateinit var bitKeyboard: View
    private lateinit var bitKeyboardContainer: LinearLayout
    private val bitButtons = mutableListOf<Button>()

    private var shiftMode = ShiftMode.LOGICAL

    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        tvMode = findViewById(R.id.tvMode)
        drawerLayout = findViewById(R.id.drawer_layout)

        // Navigation Drawer
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        headerView.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
            drawerLayout.closeDrawers()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_standard -> {
                    switchMode(CalculatorMode.STANDARD)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_scientific -> {
                    switchMode(CalculatorMode.SCIENTIFIC)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_programmer -> {
                    switchMode(CalculatorMode.PROGRAMMER)
                    drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }

        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val rootLayout = findViewById<LinearLayout>(R.id.root_layout)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, statusBarHeight, v.paddingRight, v.paddingBottom)
            insets
        }

        setupHistoryButton()
        loadMode(currentMode)
    }

    // ---------- Переключение режимов ----------
    private fun switchMode(mode: CalculatorMode) {
        currentMode = mode
        isSecondMode = false
        isHypMode = false
        loadMode(mode)
        tvMode.text = when (mode) {
            CalculatorMode.STANDARD -> getString(R.string.mode_standard)
            CalculatorMode.SCIENTIFIC -> getString(R.string.mode_scientific)
            CalculatorMode.PROGRAMMER -> getString(R.string.mode_programmer)
        }
    }

    private fun loadMode(mode: CalculatorMode) {
        val container = findViewById<FrameLayout>(R.id.buttonPanel)
        container.removeAllViews()
        val inflater = layoutInflater
        val view = when (mode) {
            CalculatorMode.STANDARD -> inflater.inflate(R.layout.basic_buttons, container, false)
            CalculatorMode.SCIENTIFIC -> inflater.inflate(R.layout.scientific_buttons, container, false)
            CalculatorMode.PROGRAMMER -> inflater.inflate(R.layout.programmer_buttons, container, false)
        }
        container.addView(view)
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        setNumberButtonListeners()
        setOperatorButtonListeners()
        setMemoryButtonListeners()
        setControlButtonListeners()

        when (currentMode) {
            CalculatorMode.STANDARD -> setBasicFunctionButtonListeners()
            CalculatorMode.SCIENTIFIC -> {
                setScientificButtonListeners()
                updateSecondModeButtons()
                val btn2nd = findViewById<Button>(R.id.btn2nd)
                btn2nd?.setBackgroundColor(0xFF8C00)
            }
            CalculatorMode.PROGRAMMER -> setProgrammerButtonListeners()
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
        val btn2nd = findViewById<Button>(R.id.btn2nd)
        btn2nd?.setBackgroundColor(0xFF8C00)
        btn2nd?.setOnClickListener {
            isSecondMode = !isSecondMode
            updateSecondModeButtons()
            btn2nd.setBackgroundColor(if (isSecondMode) 0xFF3a3a3a.toInt() else 0xFF8C00)
            Toast.makeText(this, "2nd режим: ${if (isSecondMode) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnTrigonometry)?.setOnClickListener { view ->
            showTrigonometryMenu(view)
        }
        findViewById<Button>(R.id.btnFunction)?.setOnClickListener { view ->
            showFunctionMenu(view)
        }
        findViewById<Button>(R.id.btnDegRadToggle)?.setOnClickListener {
            isDegMode = !isDegMode
            (it as Button).text = if (isDegMode) "DEG" else "RAD"
            Toast.makeText(this, "Режим углов: ${if (isDegMode) "DEG" else "RAD"}", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnFEToggle)?.setOnClickListener {
            isFEMode = !isFEMode
            Toast.makeText(this, "Формат чисел: ${if (isFEMode) "экспоненциальный" else "обычный"}", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnPi)?.setOnClickListener {
            appendConstant(PI.toString())
        }
        findViewById<Button>(R.id.btnE)?.setOnClickListener {
            appendConstant(E.toString())
        }
        findViewById<Button>(R.id.btnAbs)?.setOnClickListener {
            applyUnaryOperation("abs") { abs(it) }
        }
        findViewById<Button>(R.id.btnExp)?.setOnClickListener {
            applyUnaryOperation("exp") { exp(it) }
        }
        findViewById<Button>(R.id.btnCubeRoot)?.setOnClickListener {
            applyUnaryOperation("∛") { cbrt(it) }
        }
        findViewById<Button>(R.id.btnLeftParen)?.setOnClickListener {
            appendToExpression("(")
        }
        findViewById<Button>(R.id.btnRightParen)?.setOnClickListener {
            appendToExpression(")")
        }
        findViewById<Button>(R.id.btnFactorial)?.setOnClickListener {
            val value = currentValue().toInt()
            if (value >= 0) {
                var fact = 1.0
                for (j in 1..value) fact *= j
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
        findViewById<Button>(R.id.btnPower)?.setOnClickListener {
            appendOperator("^")
        }
        findViewById<Button>(R.id.btnLog)?.setOnClickListener {
            if (isSecondMode) {
                appendToExpression("l")
            } else {
                applyUnaryOperation("log") { log10(it) }
            }
        }
        findViewById<Button>(R.id.btnLn)?.setOnClickListener {
            applyUnaryOperation("ln") { ln(it) }
        }
        findViewById<Button>(R.id.btnSquare)?.setOnClickListener {
            if (isSecondMode) {
                applyUnaryOperation("x³") { it * it * it }
            } else {
                applyUnaryOperation("x²") { it * it }
            }
        }
        findViewById<Button>(R.id.btnBasePow)?.setOnClickListener {
            if (isSecondMode) {
                appendToExpression("2^")
            } else {
                appendToExpression("10^")
            }
        }
        findViewById<Button>(R.id.btnMod)?.setOnClickListener {
            appendOperator("mod")
        }
    }

    // ---------- Режим программиста ----------
    private fun setProgrammerButtonListeners() {
        tvHexValue = findViewById(R.id.tvHexValue)
        tvDecValue = findViewById(R.id.tvDecValue)
        tvOctValue = findViewById(R.id.tvOctValue)
        tvBinValue = findViewById(R.id.tvBinValue)

        // Радиокнопки СС
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupBase)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val oldBase = currentBase
            currentBase = when (checkedId) {
                R.id.radioHex -> NumberBase.HEX
                R.id.radioDec -> NumberBase.DEC
                R.id.radioOct -> NumberBase.OCT
                R.id.radioBin -> NumberBase.BIN
                else -> NumberBase.DEC
            }
            val currentText = tvResult.text.toString()
            val longValue = try {
                when (oldBase) {
                    NumberBase.HEX -> currentText.toLong(16)
                    NumberBase.DEC -> currentText.toLong()
                    NumberBase.OCT -> currentText.toLong(8)
                    NumberBase.BIN -> currentText.toLong(2)
                }
            } catch (e: NumberFormatException) {
                0L
            }
            tvResult.text = when (currentBase) {
                NumberBase.HEX -> longValue.toString(16).uppercase()
                NumberBase.DEC -> longValue.toString()
                NumberBase.OCT -> longValue.toString(8)
                NumberBase.BIN -> longValue.toString(2)
            }
            updateAllBaseDisplays(maskValue(longValue))
        }
        radioGroup.check(R.id.radioDec)

        // Кнопки A-F
        findViewById<Button>(R.id.btnA)?.setOnClickListener { appendHexDigit('A') }
        findViewById<Button>(R.id.btnB)?.setOnClickListener { appendHexDigit('B') }
        findViewById<Button>(R.id.btnC)?.setOnClickListener { appendHexDigit('C') }
        findViewById<Button>(R.id.btnD)?.setOnClickListener { appendHexDigit('D') }
        findViewById<Button>(R.id.btnE)?.setOnClickListener { appendHexDigit('E') }
        findViewById<Button>(R.id.btnF)?.setOnClickListener { appendHexDigit('F') }

        // QWORD – меню выбора размера
        findViewById<Button>(R.id.btnQword)?.setOnClickListener { view ->
            showWordSizeMenu(view)
        }

        // MS, треугольник
        findViewById<Button>(R.id.btnMS_prog)?.setOnClickListener {
            memory = currentValue()
            Toast.makeText(this, "MS: ${formatNumber(memory)}", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnTriangle_prog)?.setOnClickListener {
            Toast.makeText(this, "Память: ${formatNumber(memory)}", Toast.LENGTH_SHORT).show()
        }

        // Переключатели режимов ввода
        standardKeyboard = findViewById(R.id.standardKeyboard)
        bitKeyboard = findViewById(R.id.bitKeyboard)
        initBitKeyboard()
        val btnStandardMode = findViewById<ImageButton>(R.id.btnStandardMode)
        val btnBitMode = findViewById<ImageButton>(R.id.btnBitMode)
        btnStandardMode.setOnClickListener {
            standardKeyboard.visibility = View.VISIBLE
            bitKeyboard.visibility = View.GONE
            btnStandardMode.setColorFilter(0xFFFFFFFF.toInt())
            btnBitMode.setColorFilter(0xFF888888.toInt())
        }
        btnBitMode.setOnClickListener {
            standardKeyboard.visibility = View.GONE
            bitKeyboard.visibility = View.VISIBLE
            updateBitButtons()
            btnStandardMode.setColorFilter(0xFF888888.toInt())
            btnBitMode.setColorFilter(0xFFFFFFFF.toInt())
        }
        btnStandardMode.performClick()

        // Bitwise и Bit Shift
        findViewById<Button>(R.id.btnBitwise)?.setOnClickListener { view ->
            showBitwiseMenu(view)
        }
        findViewById<Button>(R.id.btnBitShift)?.setOnClickListener { view ->
            showBitShiftMenu(view)
        }
    }

    // ---------- Битовая панель ----------
    private fun initBitKeyboard() {
        bitKeyboardContainer = bitKeyboard.findViewById(R.id.bitKeyboardContainer)
        createBitButtons(currentBits)
        updateBitButtons()
    }

    private fun createBitButtons(bits: Int) {
        bitButtons.clear()
        bitKeyboardContainer.removeAllViews()
        val columns = 8
        val rows = bits / columns
        for (row in 0 until rows) {
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                weightSum = columns.toFloat()
            }
            for (col in 0 until columns) {
                val bitIndex = row * columns + col
                val cellLayout = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(4, 4, 4, 4)
                }
                val button = Button(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                    text = "0"
                    textSize = 12f   // уменьшен размер шрифта
                    setBackgroundColor(0xFF3a3a3a.toInt())
                    setTextColor(android.graphics.Color.WHITE)
                    setOnClickListener { onBitClick(bitIndex) }
                }
                val label = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = bitIndex.toString()
                    textSize = 8f    // уменьшен размер шрифта
                    gravity = android.view.Gravity.CENTER
                    setTextColor(android.graphics.Color.LTGRAY)
                }
                cellLayout.addView(button)
                cellLayout.addView(label)
                rowLayout.addView(cellLayout)
                bitButtons.add(button)
            }
            bitKeyboardContainer.addView(rowLayout)
        }
    }

    private fun updateBitButtons() {
        if (!::bitKeyboardContainer.isInitialized || bitKeyboard.visibility != View.VISIBLE) return
        val value = currentValueLong()
        for (i in bitButtons.indices) {
            val bit = (value shr i) and 1L
            bitButtons[i].text = bit.toString()
            bitButtons[i].setBackgroundColor(if (bit == 1L) 0xFF8C00 else 0xFF3a3a3a.toInt())
        }
    }

    private fun onBitClick(bitIndex: Int) {
        val value = currentValueLong()
        val newValue = value xor (1L shl bitIndex)
        updateDisplayAndAllBases(maskValue(newValue))
        addToHistory("Bit $bitIndex toggle", maskValue(newValue).toDouble())
    }

    // ---------- Маскирование в зависимости от WordSize ----------
    private fun maskValue(value: Long): Long {
        return when (currentWordSize) {
            WordSize.QWORD -> value
            WordSize.DWORD -> value and 0xFFFFFFFFuL.toLong()
            WordSize.WORD -> value and 0xFFFFuL.toLong()
            WordSize.BYTE -> value and 0xFFuL.toLong()
        }
    }

    // ---------- Меню выбора размера слова ----------
    private fun showWordSizeMenu(anchor: View) {
        val inflater = LayoutInflater.from(this)
        val parent = anchor.parent as? ViewGroup ?: anchor.rootView as ViewGroup
        val menuView = inflater.inflate(R.layout.word_size_menu, parent, false)
        val popupWindow = PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.elevation = 10f

        menuView.findViewById<Button>(R.id.btnQword).setOnClickListener {
            currentWordSize = WordSize.QWORD
            currentBits = 64
            findViewById<Button>(R.id.btnQword)?.text = "QWORD"
            updateAfterWordSizeChange()
            popupWindow.dismiss()
        }
        menuView.findViewById<Button>(R.id.btnDword).setOnClickListener {
            currentWordSize = WordSize.DWORD
            currentBits = 32
            findViewById<Button>(R.id.btnQword)?.text = "DWORD"
            updateAfterWordSizeChange()
            popupWindow.dismiss()
        }
        menuView.findViewById<Button>(R.id.btnWord).setOnClickListener {
            currentWordSize = WordSize.WORD
            currentBits = 16
            findViewById<Button>(R.id.btnQword)?.text = "WORD"
            updateAfterWordSizeChange()
            popupWindow.dismiss()
        }
        menuView.findViewById<Button>(R.id.btnByte).setOnClickListener {
            currentWordSize = WordSize.BYTE
            currentBits = 8
            findViewById<Button>(R.id.btnQword)?.text = "BYTE"
            updateAfterWordSizeChange()
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 0)
    }

    private fun updateAfterWordSizeChange() {
        if (::bitKeyboardContainer.isInitialized) {
            createBitButtons(currentBits)
            updateBitButtons()
        }
        val masked = maskValue(currentValueLong())
        updateDisplayAndAllBases(masked)
        Toast.makeText(this, "Размер слова: ${currentWordSize.name}", Toast.LENGTH_SHORT).show()
    }

    // ---------- Bitwise и Bit Shift меню ----------
    private fun showBitwiseMenu(anchor: View) {
        val inflater = LayoutInflater.from(this)
        val parent = anchor.parent as? ViewGroup ?: anchor.rootView as ViewGroup
        val menuView = inflater.inflate(R.layout.bitwise_menu, parent, false)
        val popupWindow = PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.elevation = 10f

        menuView.findViewById<Button>(R.id.btnAnd).setOnClickListener {
            appendOperator("&")
            popupWindow.dismiss()
        }
        menuView.findViewById<Button>(R.id.btnOr).setOnClickListener {
            appendOperator("|")
            popupWindow.dismiss()
        }
        menuView.findViewById<Button>(R.id.btnXor).setOnClickListener {
            appendOperator("^")
            popupWindow.dismiss()
        }
        menuView.findViewById<Button>(R.id.btnNot).setOnClickListener {
            val value = currentValueLong()
            val result = maskValue(value.inv())
            updateDisplayAndAllBases(result)
            addToHistory("NOT($value)", result.toDouble())
            isNewOperation = true
            isDecimalPressed = false
            popupWindow.dismiss()
        }
        menuView.findViewById<Button>(R.id.btnNand).setOnClickListener {
            Toast.makeText(this, "NAND (заглушка)", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }
        menuView.findViewById<Button>(R.id.btnNor).setOnClickListener {
            Toast.makeText(this, "NOR (заглушка)", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 0)
    }

    private fun showBitShiftMenu(anchor: View) {
        val inflater = LayoutInflater.from(this)
        val parent = anchor.parent as? ViewGroup ?: anchor.rootView as ViewGroup
        val menuView = inflater.inflate(R.layout.bit_shift_menu, parent, false)
        val radioGroup = menuView.findViewById<RadioGroup>(R.id.radioGroupShiftMode)
        val popupWindow = PopupWindow(menuView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.elevation = 10f

        when (shiftMode) {
            ShiftMode.ARITHMETIC -> radioGroup.check(R.id.radioArithShift)
            ShiftMode.LOGICAL -> radioGroup.check(R.id.radioLogicalShift)
            ShiftMode.ROTATE -> radioGroup.check(R.id.radioRotateShift)
            ShiftMode.ROTATE_CARRY -> radioGroup.check(R.id.radioRotateCarryShift)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            shiftMode = when (checkedId) {
                R.id.radioArithShift -> ShiftMode.ARITHMETIC
                R.id.radioLogicalShift -> ShiftMode.LOGICAL
                R.id.radioRotateShift -> ShiftMode.ROTATE
                R.id.radioRotateCarryShift -> ShiftMode.ROTATE_CARRY
                else -> ShiftMode.LOGICAL
            }
            Toast.makeText(this, "Режим сдвига: $shiftMode", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(anchor, 0, 0)
    }

    // ---------- Тригонометрическое меню ----------
    private fun showTrigonometryMenu(anchor: View) {
        val inflater = LayoutInflater.from(this)
        val parent = anchor.parent as? ViewGroup ?: anchor.rootView as ViewGroup
        val menuView = inflater.inflate(R.layout.trigonometry_menu, parent, false)
        val width = (450 * resources.displayMetrics.density).toInt()
        val popupWindow = PopupWindow(menuView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
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
            btnHyp.setBackgroundColor(if (isHypMode) 0xFF8C00 else 0xFF3a3a3a.toInt())
            btn2nd.setBackgroundColor(if (isSecondMode) 0xFF8C00 else 0xFF3a3a3a.toInt())
        }

        updateTrigLabels()

        btn2nd.setOnClickListener {
            isSecondMode = !isSecondMode
            updateTrigLabels()
            findViewById<Button>(R.id.btn2nd)?.setBackgroundColor(if (isSecondMode) 0xFF3a3a3a.toInt() else 0xFF8C00)
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
                    "arcsin" -> if (isDegMode) Math.toDegrees(asin(x)) else asin(x)
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
                    "arccos" -> if (isDegMode) Math.toDegrees(acos(x)) else acos(x)
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
                    "arctan" -> if (isDegMode) Math.toDegrees(atan(x)) else atan(x)
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
                    "sec" -> 1.0 / cos(if (isDegMode) Math.toRadians(x) else x)
                    "arcsec" -> if (isDegMode) Math.toDegrees(acos(1.0 / x)) else acos(1.0 / x)
                    "sech" -> 1.0 / cosh(x)
                    "arsech" -> acosh(1.0 / x)
                    else -> x
                }
            }
            popupWindow.dismiss()
        }

        btnCsc.setOnClickListener {
            val symbol = btnCsc.text.toString()
            applyTrigonometry(symbol) { x ->
                when (symbol) {
                    "csc" -> 1.0 / sin(if (isDegMode) Math.toRadians(x) else x)
                    "arccsc" -> if (isDegMode) Math.toDegrees(asin(1.0 / x)) else asin(1.0 / x)
                    "csch" -> 1.0 / sinh(x)
                    "arcsch" -> asinh(1.0 / x)
                    else -> x
                }
            }
            popupWindow.dismiss()
        }

        btnCot.setOnClickListener {
            val symbol = btnCot.text.toString()
            applyTrigonometry(symbol) { x ->
                when (symbol) {
                    "cot" -> 1.0 / tan(if (isDegMode) Math.toRadians(x) else x)
                    "arccot" -> if (isDegMode) Math.toDegrees(atan(1.0 / x)) else atan(1.0 / x)
                    "coth" -> 1.0 / tanh(x)
                    "arcoth" -> atanh(1.0 / x)
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
        val parent = anchor.parent as? ViewGroup ?: anchor.rootView as ViewGroup
        val menuView = inflater.inflate(R.layout.function_menu, parent, false)
        val width = (400 * resources.displayMetrics.density).toInt()
        val popupWindow = PopupWindow(menuView, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
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
            if (currentMode == CalculatorMode.PROGRAMMER) {
                updateAllBaseDisplays(randValue.toLong())
            }
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
        if (currentMode == CalculatorMode.PROGRAMMER && !isDigitValidForBase(digit.first())) {
            Toast.makeText(this, "Недопустимая цифра для текущей системы счисления", Toast.LENGTH_SHORT).show()
            return
        }
        if (isNewOperation) {
            currentExpression = StringBuilder()
            tvResult.text = ""
            isNewOperation = false
        }
        val displayDigit = if (digit == ",") "." else digit
        currentExpression.append(displayDigit)
        tvResult.append(displayDigit)
        isDecimalPressed = currentExpression.contains(".")
        if (currentMode == CalculatorMode.PROGRAMMER) {
            updateAllBaseDisplays(currentValueLong())
        }
    }

    // ---------- Операторы ----------
    private fun setOperatorButtonListeners() {
        findViewById<Button>(R.id.btnAdd)?.setOnClickListener { appendOperator("+") }
        findViewById<Button>(R.id.btnSubtract)?.setOnClickListener { appendMinus() }
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
            if (currentMode == CalculatorMode.PROGRAMMER) {
                updateAllBaseDisplays(memory.toLong())
            }
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

    // ---------- Управление ----------
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
        if (isNewOperation && currentExpression.isNotEmpty()) {
            // Если после вычисления результата нажат оператор, начинаем новое выражение
            val lastResult = currentExpression.toString()
            currentExpression.clear()
            currentExpression.append(lastResult)
            isNewOperation = false
        }
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
        if (isNewOperation && currentExpression.isNotEmpty()) {
            // Если после вычисления результата нажат минус, начинаем новое выражение
            val lastResult = currentExpression.toString()
            currentExpression.clear()
            currentExpression.append(lastResult)
            isNewOperation = false
        }
        if (isNewOperation) {
            currentExpression = StringBuilder()
            tvResult.text = ""
            isNewOperation = false
        }
        currentExpression.append('-')
        tvResult.append("-")
        isDecimalPressed = false
    }

    private fun appendHexDigit(digit: Char) {
        if (currentBase != NumberBase.HEX) {
            Toast.makeText(this, "Доступно только в HEX режиме", Toast.LENGTH_SHORT).show()
            return
        }
        if (isNewOperation) {
            currentExpression = StringBuilder()
            tvResult.text = ""
            isNewOperation = false
        }
        currentExpression.append(digit)
        tvResult.append(digit.toString())
        isDecimalPressed = false
        if (currentMode == CalculatorMode.PROGRAMMER) {
            updateAllBaseDisplays(currentValueLong())
        }
    }

    private fun clearExpression() {
        currentExpression.clear()
        tvResult.text = ""
        isNewOperation = true
        isDecimalPressed = false
        if (currentMode == CalculatorMode.PROGRAMMER) {
            updateAllBaseDisplays(0L)
        }
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
            // Сохраняем результат как текущее выражение
            currentExpression.clear()
            currentExpression.append(result)
            operand1 = result
            operator = ""
            isNewOperation = true
        } catch (e: Exception) {
            tvResult.text = "Ошибка"
            e.printStackTrace()
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ---------- Парсер выражений (обратная польская нотация) ----------
    private fun evaluateExpression(expression: String): Double {
        val output = mutableListOf<Any>()
        val stack = Stack<Any>()

        val precedence = mapOf(
            '+' to 1, '-' to 1,
            '×' to 2, '÷' to 2,
            '^' to 3,
            '&' to 3, '|' to 2,
            "mod" to 3, "l" to 4,
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
                ch.isDigit() || ch == '.' || (ch == '-' && (i == 0 || expression[i-1] == '(' || expression[i-1] in setOf('+','-','×','÷','^','&','|','<','>','('))) -> {
                    val start = i
                    if (ch == '-') i++
                    while (i < len && (expression[i].isDigit() || expression[i] == '.')) i++
                    val numStr = expression.substring(start, i)
                    output.add(numStr.toDouble())
                    continue
                }
                ch in setOf('+', '-', '×', '÷', '^', '&', '|') -> {
                    while (stack.isNotEmpty() && stack.peek() !is String && stack.peek() != '(' && precedence[stack.peek() as Char]!! >= precedence[ch]!!) {
                        output.add(stack.pop())
                    }
                    stack.push(ch)
                }
                ch == '(' -> stack.push('(')
                ch == ')' -> {
                    while (stack.isNotEmpty() && stack.peek() != '(') {
                        output.add(stack.pop())
                    }
                    if (stack.isNotEmpty() && stack.peek() == '(') stack.pop()
                    if (stack.isNotEmpty() && stack.peek() is String) output.add(stack.pop())
                }
                ch == '<' || ch == '>' -> {
                    if (i + 1 < len && expression[i + 1] == ch) {
                        val op = expression.substring(i, i + 2)
                        while (stack.isNotEmpty() && stack.peek() !is String && stack.peek() != '(' && precedence[stack.peek() as Char]!! >= 3) {
                            output.add(stack.pop())
                        }
                        stack.push(op)
                        i++
                    }
                }
                else -> {
                    if (ch.isLetter()) {
                        val start = i
                        while (i < len && expression[i].isLetter()) i++
                        when (val word = expression.substring(start, i)) {
                            "π" -> output.add(PI)
                            "e" -> output.add(E)
                            "mod", "l" -> {
                                while (stack.isNotEmpty() && stack.peek() !is String && stack.peek() != '(' && precedence[stack.peek() as Char]!! >= precedence[word]!!) {
                                    output.add(stack.pop())
                                }
                                stack.push(word)
                            }
                            else -> stack.push(word)
                        }
                        continue
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
                    when (token) {
                        "<<", ">>" -> {
                            val b = valueStack.pop().toInt()
                            val a = valueStack.pop().toLong()
                            val result = when (shiftMode) {
                                ShiftMode.ARITHMETIC -> if (token == "<<") (a shl b).toDouble() else (a shr b).toDouble()
                                ShiftMode.LOGICAL -> if (token == "<<") (a shl b).toDouble() else (a ushr b).toDouble()
                                ShiftMode.ROTATE -> {
                                    val bits = 64
                                    val res = if (token == "<<") (a shl b) or (a ushr (bits - b)) else (a ushr b) or (a shl (bits - b))
                                    maskValue(res).toDouble()
                                }
                                ShiftMode.ROTATE_CARRY -> {
                                    val bits = 64
                                    val res = if (token == "<<") (a shl b) or (a ushr (bits - b)) else (a ushr b) or (a shl (bits - b))
                                    maskValue(res).toDouble()
                                }
                            }
                            valueStack.push(result)
                        }
                        "mod", "l" -> {
                            val b = valueStack.pop()
                            val a = valueStack.pop()
                            val result = when (token) {
                                "mod" -> a % b
                                "l" -> ln(b) / ln(a)
                                else -> throw IllegalArgumentException()
                            }
                            valueStack.push(result)
                        }
                        else -> {
                            val arg = valueStack.pop()
                            val result = when (token) {
                                "sin" -> sin(if (isDegMode) Math.toRadians(arg) else arg)
                                "cos" -> cos(if (isDegMode) Math.toRadians(arg) else arg)
                                "tan" -> tan(if (isDegMode) Math.toRadians(arg) else arg)
                                "arcsin" -> if (isDegMode) Math.toDegrees(asin(arg)) else asin(arg)
                                "arccos" -> if (isDegMode) Math.toDegrees(acos(arg)) else acos(arg)
                                "arctan" -> if (isDegMode) Math.toDegrees(atan(arg)) else atan(arg)
                                "sinh" -> sinh(arg); "cosh" -> cosh(arg); "tanh" -> tanh(arg)
                                "arsinh" -> asinh(arg); "arcosh" -> acosh(arg); "artanh" -> atanh(arg)
                                "sec" -> 1.0 / cos(if (isDegMode) Math.toRadians(arg) else arg)
                                "csc" -> 1.0 / sin(if (isDegMode) Math.toRadians(arg) else arg)
                                "cot" -> 1.0 / tan(if (isDegMode) Math.toRadians(arg) else arg)
                                "arcsec" -> if (isDegMode) Math.toDegrees(acos(1.0 / arg)) else acos(1.0 / arg)
                                "arccsc" -> if (isDegMode) Math.toDegrees(asin(1.0 / arg)) else asin(1.0 / arg)
                                "arccot" -> if (isDegMode) Math.toDegrees(atan(1.0 / arg)) else atan(1.0 / arg)
                                "sech" -> 1.0 / cosh(arg); "csch" -> 1.0 / sinh(arg); "coth" -> 1.0 / tanh(arg)
                                "arsech" -> acosh(1.0 / arg); "arcsch" -> asinh(1.0 / arg); "arcoth" -> atanh(1.0 / arg)
                                "ln" -> ln(arg); "log" -> log10(arg); "exp" -> exp(arg)
                                "sqrt" -> sqrt(arg); "cbrt" -> cbrt(arg); "abs" -> abs(arg)
                                "floor" -> floor(arg); "ceil" -> ceil(arg)
                                "n!" -> { var f = 1.0; for (j in 1..arg.toInt()) f *= j; f }
                                "x²" -> arg * arg; "x³" -> arg * arg * arg
                                "%" -> arg / 100.0; "±" -> -arg
                                else -> throw IllegalArgumentException("Неизвестная функция: $token")
                            }
                            valueStack.push(result)
                        }
                    }
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
                        '&' -> (a.toLong() and b.toLong()).toDouble()
                        '|' -> (a.toLong() or b.toLong()).toDouble()
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
        if (currentMode == CalculatorMode.PROGRAMMER) {
            updateAllBaseDisplays(result.toLong())
        }
    }

    private fun applyTrigonometry(symbol: String, operation: (Double) -> Double) {
        applyUnaryOperation(symbol, operation)
    }

    // ---------- Вспомогательные методы для чисел ----------
    private fun currentValue(): Double {
        return tvResult.text.toString().toDoubleOrNull() ?: 0.0
    }

    private fun currentValueLong(): Long {
        val raw = try {
            when (currentBase) {
                NumberBase.HEX -> tvResult.text.toString().toLong(16)
                NumberBase.DEC -> tvResult.text.toString().toLong()
                NumberBase.OCT -> tvResult.text.toString().toLong(8)
                NumberBase.BIN -> tvResult.text.toString().toLong(2)
            }
        } catch (e: NumberFormatException) {
            0L
        }
        return maskValue(raw)
    }

    private fun updateAllBaseDisplays(value: Long) {
        tvHexValue.text = value.toString(16).uppercase()
        tvDecValue.text = value.toString()
        tvOctValue.text = value.toString(8)
        tvBinValue.text = value.toString(2)
        updateBitButtons()
    }

    private fun updateDisplayAndAllBases(value: Long) {
        tvResult.text = when (currentBase) {
            NumberBase.HEX -> value.toString(16).uppercase()
            NumberBase.DEC -> value.toString()
            NumberBase.OCT -> value.toString(8)
            NumberBase.BIN -> value.toString(2)
        }
        updateAllBaseDisplays(value)
    }

    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    private fun isDigitValidForBase(digit: Char): Boolean {
        return when (currentBase) {
            NumberBase.HEX -> digit in '0'..'9' || digit in 'A'..'F' || digit in 'a'..'f'
            NumberBase.DEC -> digit in '0'..'9'
            NumberBase.OCT -> digit in '0'..'7'
            NumberBase.BIN -> digit in '0'..'1'
        }
    }

    // ---------- Обработчики управления ----------
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
                if (currentMode == CalculatorMode.PROGRAMMER) {
                    updateAllBaseDisplays(0L)
                }
                return
            }
        } else {
            currentExpression.deleteCharAt(lastIndex)
        }
        tvResult.text = currentExpression.toString()
        isDecimalPressed = currentExpression.contains(".")
        if (currentMode == CalculatorMode.PROGRAMMER) {
            updateAllBaseDisplays(currentValueLong())
        }
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
        if (currentMode == CalculatorMode.PROGRAMMER) {
            updateAllBaseDisplays(currentValueLong())
        }
    }
}