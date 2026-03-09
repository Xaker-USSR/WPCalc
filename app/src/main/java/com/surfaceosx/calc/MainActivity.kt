package com.surfaceosx.calc

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvMode: TextView  // можно использовать для отображения "обычный"

    private var operand1: Double = 0.0
    private var operand2: Double = 0.0
    private var operator: String = ""
    private var isNewOperation = true
    private var isDecimalPressed = false

    // Переменная для памяти
    private var memory: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)
        tvMode = findViewById(R.id.tvMode)

        // Кнопка меню
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.mode_menu, popup.menu)
            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.mode_standard -> {
                        tvMode.text = "обычный"
                        // Здесь можно будет переключить видимость кнопок
                    }
                    R.id.mode_scientific -> {
                        tvMode.text = "инженерный"
                        Toast.makeText(this, "Инженерный режим в разработке", Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            popup.show()
        }

        setNumberButtonListeners()
        setOperatorButtonListeners()
        setMemoryButtonListeners()
        setFunctionButtonListeners()
        setControlButtonListeners()
    }

    private fun setNumberButtonListeners() {
        val numberIds = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        numberIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                onNumberClick(it as Button)
            }
        }
    }

    private fun setOperatorButtonListeners() {
        val operatorIds = listOf(
            R.id.btnAdd, R.id.btnSubtract, R.id.btnMultiply, R.id.btnDivide
        )
        operatorIds.forEach { id ->
            findViewById<Button>(id).setOnClickListener {
                onOperatorClick(it as Button)
            }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener {
            onEqualsClick()
        }
    }

    private fun setMemoryButtonListeners() {
        findViewById<Button>(R.id.btnMC).setOnClickListener { memory = 0.0 }
        findViewById<Button>(R.id.btnMR).setOnClickListener {
            tvResult.text = formatNumber(memory)
            isNewOperation = true
            isDecimalPressed = memory.toString().contains(".")
        }
        findViewById<Button>(R.id.btnMPlus).setOnClickListener {
            memory += currentValue()
        }
        findViewById<Button>(R.id.btnMMinus).setOnClickListener {
            memory -= currentValue()
        }
        findViewById<Button>(R.id.btnMS).setOnClickListener {
            memory = currentValue()
        }
        findViewById<Button>(R.id.btnMTriangle).setOnClickListener {
            Toast.makeText(this, "Память: ${formatNumber(memory)}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setFunctionButtonListeners() {
        findViewById<Button>(R.id.btnPercent).setOnClickListener {
            applyUnaryOperation { it / 100 }
        }
        findViewById<Button>(R.id.btnSqrt).setOnClickListener {
            applyUnaryOperation { sqrt(it) }
        }
        findViewById<Button>(R.id.btnSquare).setOnClickListener {
            applyUnaryOperation { it * it }
        }
        findViewById<Button>(R.id.btnReciprocal).setOnClickListener {
            if (currentValue() == 0.0) {
                tvResult.text = "Ошибка"
                return@setOnClickListener
            }
            applyUnaryOperation { 1 / it }
        }
    }

    private fun setControlButtonListeners() {
        findViewById<Button>(R.id.btnClear).setOnClickListener { onClearClick() }
        findViewById<Button>(R.id.btnCE).setOnClickListener { onCEClick() }
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { onBackspaceClick() }
        findViewById<Button>(R.id.btnSign).setOnClickListener { onSignClick() }
        findViewById<Button>(R.id.btnDot).setOnClickListener { onDotClick() }
    }

    // Получить текущее значение с дисплея
    private fun currentValue(): Double {
        return tvResult.text.toString().toDoubleOrNull() ?: 0.0
    }

    // Применить унарную операцию (%, √, x², 1/x)
    private fun applyUnaryOperation(operation: (Double) -> Double) {
        val value = currentValue()
        val result = operation(value)
        tvResult.text = formatNumber(result)
        isNewOperation = true // после унарной операции можно начинать новое число
        isDecimalPressed = result.toString().contains(".")
    }

    // Форматирование числа: убираем .0 если целое
    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
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

        // Если уже есть оператор, вычисляем промежуточный результат
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

    private fun onEqualsClick() {
        val currentText = tvResult.text.toString()
        if (operator.isEmpty() || currentText.isEmpty()) return

        operand2 = currentText.toDouble()
        compute()
        operator = ""
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
        // Clear Entry: сбрасывает только текущее число
        tvResult.text = "0"
        isNewOperation = true
        isDecimalPressed = false
    }

    private fun onBackspaceClick() {
        val text = tvResult.text.toString()
        if (text.length > 1) {
            val newText = text.dropLast(1)
            tvResult.text = newText
            // Если после удаления остался только минус (для отрицательных), превращаем в 0
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