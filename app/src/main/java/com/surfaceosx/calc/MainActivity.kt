package com.surfaceosx.calc

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    // UI элементы
    private lateinit var textViewResult: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var panelStandard: LinearLayout
    private lateinit var panelScientific: LinearLayout
    private lateinit var panelProgrammer: LinearLayout
    private lateinit var panelConverter: LinearLayout

    // Для стандартного режима
    private var operand1: Double? = null
    private var operator: String? = null
    private var newNumber = true

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

        // Инициализация обработчиков для стандартного режима
        initStandardMode()

        // Инициализация конвертера
        initConverterMode()

        // Здесь можно добавить инициализацию научного и программистского режимов,
        initScientificMode()
        // когда будут добавлены кнопки.
    }

    private fun initStandardMode() {
        // Цифры
        val numberIds = listOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3,
            R.id.button4, R.id.button5, R.id.button6, R.id.button7,
            R.id.button8, R.id.button9
        )
        numberIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener { numberClick(it) }
        }

        // Операторы
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
            // Процент от первого операнда
            val percentValue = operand1!! * current / 100
            textViewResult.text = percentValue.toString()
            newNumber = true
        } else {
            // Если нет первого операнда, просто делим на 100
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

    // ---------- Sciencific Mode ---------
    private fun initScientificMode() {
        findViewById<Button>(R.id.buttonSin).setOnClickListener {
            applyFunction("sin")
        }
        findViewById<Button>(R.id.buttonCos).setOnClickListener {
            applyFunction("cos")
        }
        findViewById<Button>(R.id.buttonTan).setOnClickListener {
            applyFunction("tan")
        }
        findViewById<Button>(R.id.buttonLn).setOnClickListener {
            applyFunction("ln")
        }
        findViewById<Button>(R.id.buttonLog).setOnClickListener {
            applyFunction("log")
        }
        findViewById<Button>(R.id.buttonSquare).setOnClickListener {
            applyFunction("square")
        }
        // и так далее...
    }

    private fun applyFunction(func: String) {
        val current = textViewResult.text.toString().toDoubleOrNull()
        if (current != null) {
            val result = when (func) {
                "sin" -> kotlin.math.sin(current)
                "cos" -> kotlin.math.cos(current)
                "tan" -> kotlin.math.tan(current)
                "ln" -> kotlin.math.ln(current)
                "log" -> kotlin.math.log10(current)
                "square" -> current * current
                else -> current
            }
            textViewResult.text = result.toString()
            newNumber = true
        }
    }

    // ---------- Converter Mode ----------
    private fun initConverterMode() {
        spinnerCategory = findViewById(R.id.spinnerCategory)
        editFrom = findViewById(R.id.editFrom)
        spinnerFromUnit = findViewById(R.id.spinnerFromUnit)
        editTo = findViewById(R.id.editTo)
        spinnerToUnit = findViewById(R.id.spinnerToUnit)

        // Пример: обновление единиц измерения при выборе категории
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // В зависимости от категории подставляем соответствующие массивы единиц
                val unitsArray = when (position) {
                    0 -> R.array.length_units
                    1 -> R.array.weight_units   // нужно создать
                    2 -> R.array.temperature_units // нужно создать
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
                convert() // пересчитать
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Слушатели изменения полей
        editFrom.setOnEditorActionListener { _, _, _ ->
            convert()
            false
        }
        editFrom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
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
        // Простейшая конвертация на основе коэффициентов к метрам
        val factors = doubleArrayOf(1.0, 1000.0, 1609.34) // meters, km, miles
        val valueInMeters = value * factors[from]
        return valueInMeters / factors[to]
    }

    private fun convertWeight(value: Double, from: Int, to: Int): Double {
        // Заглушка
        return value
    }

    private fun convertTemperature(value: Double, from: Int, to: Int): Double {
        // Заглушка
        return value
    }
}