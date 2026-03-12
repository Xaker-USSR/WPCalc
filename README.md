WP10 Calculator for Android

Калькулятор для Android, выполненный в стиле Windows Phone 10. Проект включает два режима работы (обычный и инженерный), историю вычислений, поддержку памяти, тригонометрические и другие научные функции. Полностью адаптивный дизайн с тёмной темой и квадратными кнопками, характерными для WP10.
Скрины:
<img width="1080" height="2400" alt="изображение" src="https://github.com/user-attachments/assets/319e1ae5-b4d9-4457-b9f3-a313adaea360" />
<img width="1080" height="2400" alt="изображение" src="https://github.com/user-attachments/assets/3e77df73-ec7c-49c9-b250-1b77d955b889" />
<img width="2208" height="1840" alt="изображение" src="https://github.com/user-attachments/assets/59ac8235-f3df-4031-93ff-dee1626f5fab" />



Два режима работы:
  Обычный – базовые операции (+, -, ×, ÷, %, √, x², 1/x, ±, десятичная точка).

  Инженерный – расширенный набор функций:
     Тригонометрия (sin, cos, tan, sec, csc, cot) и обратные (arcsin, arccos, arctan, arcsec, arccsc, arccot).
     Гиперболические функции (sinh, cosh, tanh, sech, csch, coth) и обратные гиперболические.
     Логарифмы (ln, log₁₀, log по основанию y).
     Степени и корни (x², x³, √, ∛, x^y, 10^x, 2^x).
     Факториал (n!), модуль (|x|), округление вниз/вверх (⌊x⌋, ⌈x⌉), случайное число (rand).
     Константы π и e.
     Оператор остатка (mod).
  Переключение режимов через меню (кнопка с тремя полосками).
История вычислений сохраняет последние 20 операций (доступна по иконке справа вверху).
Память – кнопки MC, MR, M+, M-, MS и отображение текущего значения при нажатии на треугольник (▼).

Технологии

    Язык: Kotlin

    Минимальная версия Android: API 21 (Android 5.0)

    Используемые библиотеки: AndroidX (AppCompat, ConstraintLayout, GridLayout), Material Components.

    Архитектура: простая одноактивити с переключением фрагментов разметки.

Структура проекта
app/
├── src/
│   ├── main/
│   │   ├── java/com/surfaceosx/calc/
│   │   │   └── MainActivity.kt        # Основной код приложения
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml  # Корневая разметка
│   │   │   │   ├── basic_buttons.xml  # Кнопки обычного режима
│   │   │   │   ├── scientific_buttons.xml # Кнопки инженерного режима
│   │   │   │   ├── trigonometry_menu.xml # Меню тригонометрии
│   │   │   │   └── function_menu.xml  # Меню функций
│   │   │   ├── menu/
│   │   │   │   ├── mode_menu.xml      # Переключение режимов
│   │   │   │   └── memory_menu.xml    # (опционально) меню памяти
│   │   │   ├── drawable/
│   │   │   │   ├── ic_menu.xml        # Иконка гамбургер-меню
│   │   │   │   └── ic_history.xml     # Иконка истории
│   │   │   ├── values/
│   │   │   │   └── styles.xml         # Стили кнопок и темы
│   │   │   └── mipmap/...             # Иконки приложения
│   │   └── AndroidManifest.xml
│   └── ...
└── build.gradle

🚀 Сборка и запуск

    Клонируйте репозиторий или скопируйте файлы в новый проект Android Studio.

    Убедитесь, что в файле app/build.gradle указаны необходимые зависимости:
    groovy

    dependencies {
        implementation 'androidx.core:core-ktx:1.9.0'
        implementation 'androidx.appcompat:appcompat:1.5.1'
        implementation 'com.google.android.material:material:1.7.0'
        implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
        implementation 'androidx.gridlayout:gridlayout:1.0.0'
    }

    Синхронизируйте проект.

    Запустите на эмуляторе или реальном устройстве.

Автор
Проект разработан для проэкта surfaceosx. Исходный код предоставляется «как есть» для образовательных целей.

Лицензия
MIT License — можно свободно использовать, модифицировать и распространять с указанием авторства.
