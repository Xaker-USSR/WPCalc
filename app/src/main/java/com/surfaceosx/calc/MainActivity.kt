package com.surfaceosx.calc  // замените на свой пакет

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    // UI элементы
    private lateinit var textViewResult: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var panelStandard: LinearLayout
    private lateinit var panelScientific: LinearLayout
    private lateinit var panelProgrammer: LinearLayout
    private lateinit var panelConverter: LinearLayout

    // Для стандартного и научного режимов
    private var operand1: Double? = null
    private var operator: String? = null
    private var newNumber = true

    // Для памяти
    private var memoryValue: Double = 0.0

    // Для конвертера
    private lateinit var spinnerCategory: Spinner
    private lateinit var editFrom: EditText
    private lateinit var spinnerFromUnit: Spinner
    private lateinit var editTo: EditText
    private lateinit var spinnerToUnit: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация виджетов
        textViewResult = findViewById(R.id.textViewResult)
        tabLayout = findViewById(R.id.tabLayout)
        panelStandard = findViewById(R.id.panelStandard)
        panelScientific = findViewById(R.id.panelScientific)
        panelProgrammer = findViewById(R.id.panelProgrammer)
        panelConverter = findViewById(R.id.panelConverter)

        // Переключение режимов
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        panelStandard.visibility = View.VISIBLE
                        panelScientific.visibility = View.GONE
                        panelProgrammer.visibility = View.GONE
                        panelConverter.visibility = View.GONE
                    }
                    1 -> {
                        panelStandard.visibility = View.GONE
                        panelScientific.visibility = View.VISIBLE
                        panelProgrammer.visibility = View.GONE
                        panelConverter.visibility = View.GONE
                    }
                    2 -> {
                        panelStandard.visibility = View.GONE
                        panelScientific.visibility = View.GONE
                        panelProgrammer.visibility = View.VISIBLE
                        panelConverter.visibility = View.GONE
                    }
                    3 -> {
                        panelStandard.visibility = View.GONE
                        panelScientific.visibility = View.GONE
                        panelProgrammer.visibility = View.GONE
                        panelConverter.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Инициализация всех режимов
        initStandardMode()
        initScientificMode()
        initConverterMode()
    }

    // ---------- Standard Mode ----------
    private fun initStandardMode() {
        val numberIds = listOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3,
            R.id.button4, R.id.button5, R.id.button6, R.id.button7,
            R.id.button8, R.id.button9
        )
        numberIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener { numberClick(it) }
        }

        findViewById<Button>(R.id.buttonPlus).setOnClickListener { operatorClick("+") }
        findViewById<Button>(R.id.buttonMinus).setOnClickListener { operatorClick("-") }
        findViewById<Button>(R.id.buttonMultiply).setOnClickListener { operatorClick("*") }
        findViewById<Button>(R.id.buttonDivide).setOnClickListener { operatorClick("/") }
        findViewById<Button>(R.id.buttonEquals).setOnClickListener { equalsClick() }
        findViewById<Button>(R.id.buttonClear).setOnClickListener { clearClick() }
        findViewById<Button>(R.id.buttonPlusMinus).setOnClickListener { plusMinusClick() }
        findViewById<Button>(R.id.buttonPercent).setOnClickListener { percentClick() }
        findViewById<Button>(R.id.buttonDot).setOnClickListener { dotClick() }
    }

    private fun numberClick(view: View) {
        val button = view as Button
        val digit = button.text.toString()
        val currentText = textViewResult.text.toString()

        if (newNumber) {
            textViewResult.text = digit
            newNumber = false
        } else {
            textViewResult.text = currentText + digit
        }
    }

    private fun operatorClick(op: String) {
        val currentText = textViewResult.text.toString()
        if (!newNumber) {
            operand1 = currentText.toDoubleOrNull()
            operator = op
            newNumber = true
        } else {
            operator = op
        }
    }

    private fun equalsClick() {
        val operand2 = textViewResult.text.toString().toDoubleOrNull()
        if (operand1 != null && operand2 != null && operator != null) {
            val result = when (operator) {
                "+" -> operand1!! + operand2
                "-" -> operand1!! - operand2
                "*" -> operand1!! * operand2
                "/" -> if (operand2 != 0.0) operand1!! / operand2 else Double.NaN
                "^" -> operand1!!.pow(operand2)
                "root" -> operand2.pow(1.0 / operand1!!) // y√x: operand1 = степень, operand2 = число
                else -> 0.0
            }
            textViewResult.text = result.toString()
            operand1 = result
            operator = null
            newNumber = true
        }
    }

    private fun clearClick() {
        textViewResult.text = "0"
        operand1 = null
        operator = null
        newNumber = true
    }

    private fun plusMinusClick() {
        val current = textViewResult.text.toString().toDoubleOrNull()
        if (current != null) {
            textViewResult.text = (-current).toString()
        }
    }

    private fun percentClick() {
        val current = textViewResult.text.toString().toDoubleOrNull()
        if (current != null && operand1 != null) {
            val percentValue = operand1!! * current / 100
            textViewResult.text = percentValue.toString()
            newNumber = true
        } else {
            textViewResult.text = (current?.div(100))?.toString() ?: "0"
        }
    }

    private fun dotClick() {
        val currentText = textViewResult.text.toString()
        if (!currentText.contains(".")) {
            textViewResult.text = "$currentText."
            newNumber = false
        }
    }

    // ---------- Scientific Mode ----------
    private fun initScientificMode() {
        // Унарные функции
        findViewById<Button>(R.id.buttonSin).setOnClickListener { applyFunction("sin") }
        findViewById<Button>(R.id.buttonCos).setOnClickListener { applyFunction("cos") }
        findViewById<Button>(R.id.buttonTan).setOnClickListener { applyFunction("tan") }
        findViewById<Button>(R.id.buttonSinh).setOnClickListener { applyFunction("sinh") }
        findViewById<Button>(R.id.buttonCosh).setOnClickListener { applyFunction("cosh") }
        findViewById<Button>(R.id.buttonTanh).setOnClickListener { applyFunction("tanh") }

        findViewById<Button>(R.id.buttonLn).setOnClickListener { applyFunction("ln") }
        findViewById<Button>(R.id.buttonLog).setOnClickListener { applyFunction("log") }

        findViewById<Button>(R.id.buttonSquare).setOnClickListener { applyFunction("square") }
        findViewById<Button>(R.id.buttonCube).setOnClickListener { applyFunction("cube") }
        findViewById<Button>(R.id.buttonInverse).setOnClickListener { applyFunction("inverse") }
        findViewById<Button>(R.id.buttonSqrt).setOnClickListener { applyFunction("sqrt") }
        findViewById<Button>(R.id.buttonCbrt).setOnClickListener { applyFunction("cbrt") }
        findViewById<Button>(R.id.buttonFactorial).setOnClickListener { applyFunction("factorial") }

        findViewById<Button>(R.id.buttonExp).setOnClickListener { applyFunction("exp") }
        findViewById<Button>(R.id.buttonTenPower).setOnClickListener { applyFunction("tenPow") }

        // Бинарные операторы
        findViewById<Button>(R.id.buttonPowerY).setOnClickListener { operatorClick("^") }
        findViewById<Button>(R.id.buttonYRootX).setOnClickListener { operatorClick("root") }

        // Константы
        findViewById<Button>(R.id.buttonPi).setOnClickListener { insertConstant("π") }
        findViewById<Button>(R.id.buttonE).setOnClickListener { insertConstant("e") }

        // Память
        findViewById<Button>(R.id.buttonMC).setOnClickListener { memoryClear() }
        findViewById<Button>(R.id.buttonMR).setOnClickListener { memoryRecall() }
        findViewById<Button>(R.id.buttonMPlus).setOnClickListener { memoryAdd() }
        findViewById<Button>(R.id.buttonMMinus).setOnClickListener { memorySubtract() }

        // Заглушки для скобок и 2nd
        findViewById<Button>(R.id.button2nd).setOnClickListener {
            Toast.makeText(this, "2nd function not implemented", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.buttonLeftParen).setOnClickListener {
            Toast.makeText(this, "( not implemented", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.buttonRightParen).setOnClickListener {
            Toast.makeText(this, ") not implemented", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.buttonLeftParen2).setOnClickListener {
            Toast.makeText(this, "( not implemented", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.buttonRightParen2).setOnClickListener {
            Toast.makeText(this, ") not implemented", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.buttonEE).setOnClickListener {
            Toast.makeText(this, "EE not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyFunction(func: String) {
        val current = textViewResult.text.toString().toDoubleOrNull()
        if (current != null) {
            val result = when (func) {
                "sin" -> sin(current)
                "cos" -> cos(current)
                "tan" -> tan(current)
                "sinh" -> sinh(current)
                "cosh" -> cosh(current)
                "tanh" -> tanh(current)
                "ln" -> ln(current)
                "log" -> log10(current)
                "square" -> current * current
                "cube" -> current * current * current
                "inverse" -> 1.0 / current
                "sqrt" -> sqrt(current)
                "cbrt" -> cbrt(current)
                "factorial" -> factorial(current.toLong()).toDouble()
                "exp" -> exp(current)
                "tenPow" -> 10.0.pow(current)
                else -> current
            }
            textViewResult.text = result.toString()
            newNumber = true
        }
    }

    private fun factorial(n: Long): Long {
        return if (n <= 1) 1 else n * factorial(n - 1)
    }

    private fun insertConstant(constant: String) {
        val value = when (constant) {
            "π" -> PI
            "e" -> E
            else -> 0.0
        }
        if (newNumber) {
            textViewResult.text = value.toString()
            newNumber = false
        } else {
            textViewResult.text = textViewResult.text.toString() + value.toString()
        }
    }

    // ---------- Memory Functions ----------
    private fun memoryClear() {
        memoryValue = 0.0
        Toast.makeText(this, "Memory cleared", Toast.LENGTH_SHORT).show()
    }

    private fun memoryRecall() {
        textViewResult.text = memoryValue.toString()
        newNumber = true
    }

    private fun memoryAdd() {
        val current = textViewResult.text.toString().toDoubleOrNull()
        if (current != null) {
            memoryValue += current
        }
    }

    private fun memorySubtract() {
        val current = textViewResult.text.toString().toDoubleOrNull()
        if (current != null) {
            memoryValue -= current
        }
    }

    // ---------- Converter Mode ----------
    private fun initConverterMode() {
        spinnerCategory = findViewById(R.id.spinnerCategory)
        editFrom = findViewById(R.id.editFrom)
        spinnerFromUnit = findViewById(R.id.spinnerFromUnit)
        editTo = findViewById(R.id.editTo)
        spinnerToUnit = findViewById(R.id.spinnerToUnit)

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val unitsArray = when (position) {
                    0 -> R.array.length_units
                    1 -> R.array.weight_units
                    2 -> R.array.temperature_units
                    else -> R.array.length_units
                }
                val adapter = ArrayAdapter.createFromResource(
                    this@MainActivity,
                    unitsArray,
                    android.R.layout.simple_spinner_item
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFromUnit.adapter = adapter
                spinnerToUnit.adapter = adapter
                convert()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        editFrom.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                convert()
            }
        })

        spinnerFromUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                convert()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerToUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                convert()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun convert() {
        val fromValue = editFrom.text.toString().toDoubleOrNull() ?: 0.0
        val category = spinnerCategory.selectedItemPosition
        val fromUnit = spinnerFromUnit.selectedItemPosition
        val toUnit = spinnerToUnit.selectedItemPosition

        val result = when (category) {
            0 -> convertLength(fromValue, fromUnit, toUnit)
            1 -> convertWeight(fromValue, fromUnit, toUnit)
            2 -> convertTemperature(fromValue, fromUnit, toUnit)
            else -> 0.0
        }
        editTo.setText(result.toString())
    }

    private fun convertLength(value: Double, from: Int, to: Int): Double {
        val factors = doubleArrayOf(1.0, 1000.0, 1609.34) // meters, km, miles
        val valueInMeters = value * factors[from]
        return valueInMeters / factors[to]
    }

    private fun convertWeight(value: Double, from: Int, to: Int): Double {
        val factors = doubleArrayOf(1.0, 0.001, 0.453592) // kg, g, lb (в kg)
        val valueInKg = value * factors[from]
        return valueInKg / factors[to]
    }

    private fun convertTemperature(value: Double, from: Int, to: Int): Double {
        // Все в Celsius, потом в целевую
        val inCelsius = when (from) {
            0 -> value                 // Celsius
            1 -> (value - 32) * 5.0/9.0 // Fahrenheit
            2 -> value - 273.15         // Kelvin
            else -> value
        }
        return when (to) {
            0 -> inCelsius
            1 -> inCelsius * 9.0/5.0 + 32
            2 -> inCelsius + 273.15
            else -> inCelsius
        }
    }
}