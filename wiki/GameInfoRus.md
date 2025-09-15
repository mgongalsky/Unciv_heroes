### Анализ кода сериализации `GameInfo`

Этот класс отвечает за сериализацию и десериализацию игрового состояния (`GameInfo`) для сохранений игры. В процессе используется несколько интерфейсов и механизмов Kotlin для управления совместимостью, transient-полями, инициализацией и настройкой объектов. Давайте разберем основные моменты:

---

### 1. **Ключевые классы и интерфейсы**
1. **`IsPartOfGameInfoSerialization`**:
    - Интерфейс, который помечает классы, участвующие в сериализации игры.
    - Все данные, связанные с состоянием игры, наследуют этот интерфейс.

2. **`HasGameInfoSerializationVersion`**:
    - Интерфейс для хранения текущей версии совместимости.
    - Поле `version` позволяет отслеживать изменения формата сериализации.

3. **`GameInfo`**:
    - Главный класс, содержащий всю информацию о состоянии игры.
    - Содержит поля как для сериализации, так и для временных данных (`@Transient`).

4. **`GameInfoPreview`**:
    - Упрощённая версия `GameInfo` для предварительного просмотра или работы с мультиплеерными сохранениями.

---

### 2. **Поля `GameInfo`**
#### **Сериализуемые поля:**
Эти поля записываются в JSON:
- `version`: Информация о совместимости, основанная на `CompatibilityVersion`.
- `civilizations`: Список цивилизаций (`CivilizationInfo`).
- `barbarians`: Состояние варваров.
- `religions`: Словарь религий.
- `difficulty`: Уровень сложности.
- `tileMap`: Карта игры.
- `gameParameters`: Параметры игры.
- Прочие поля, такие как `turns`, `currentPlayer`, `gameId`.

#### **Временные поля (`@Transient`):**
Эти поля не сериализуются:
- `difficultyObject`: Объект сложности, рассчитываемый на основе текущего правила (`Ruleset`).
- `speed`: Скорость игры.
- `currentPlayerCiv`: Текущая цивилизация игрока.
- Прочие вспомогательные данные, такие как `ruleSet`, `simulateMaxTurns`.

> **Важно**: Эти поля инициализируются вручную в методе `setTransients`.

---

### 3. **Сериализация**
#### **Сохранение (`Serialization`)**
1. Поля из секции "Serialized" записываются в JSON.
2. Поля с `@Transient` пропускаются.
3. Если структура класса изменяется, увеличивается номер `CURRENT_COMPATIBILITY_NUMBER`, чтобы пометить несовместимость с предыдущими версиями.

Пример сохранения:
```kotlin
val gameInfo = GameInfo()
// Конвертация объекта в JSON
val json = Json.encodeToString(gameInfo)
```

#### **Загрузка (`Deserialization`)**
1. При загрузке файла JSON создается объект `GameInfo`.
2. Поля с `@Transient` остаются `null`, пока не будут инициализированы через `setTransients`.
3. Поле `version` помогает управлять обратной совместимостью, включая миграции данных.

---

### 4. **Обработка обратной совместимости**
Класс содержит механизмы для работы с файлами старых версий:
- **Поле `CURRENT_COMPATIBILITY_NUMBER`**:
    - Используется для проверки совместимости сохранений.
    - Если версия файла меньше текущей, запускаются процедуры миграции.
- **Миграционные функции**:
    - `convertFortify()`, `removeMissingModReferences()`, `convertOldGameSpeed()` и другие.
    - Эти функции адаптируют старую структуру данных под новые требования.

---

### 5. **Инициализация временных полей**
Метод `setTransients()`:
- Вызывается после загрузки данных.
- Настраивает временные поля (`@Transient`), такие как `difficultyObject`, `currentPlayerCiv`, `ruleSet`.

Пример:
```kotlin
fun setTransients() {
    tileMap.gameInfo = this
    for (civInfo in civilizations) civInfo.gameInfo = this
    difficultyObject = ruleSet.difficulties[difficulty]!!
    speed = ruleSet.speeds[gameParameters.speed]!!
    // Дополнительная настройка
}
```

---

### 6. **Проблемы с `lateinit`**
- Поля с `lateinit` не сериализуются и могут оставаться неинициализированными, что приводит к `UninitializedPropertyAccessException`.
- Временные поля (например, `currentPlayerCiv`) должны быть инициализированы вручную в методе `setTransients`.

---

### 7. **Упрощение просмотра данных**
Для анализа JSON-файлов можно написать скрипт, который:
1. Загружает JSON-файл.
2. Визуализирует его структуру.
3. Отображает данные, упрощая их отладку.

Пример на Python:
```python
import json

with open("save_file.json", "r") as file:
    data = json.load(file)

def print_structure(d, indent=0):
    for key, value in d.items() if isinstance(d, dict) else enumerate(d):
        print("  " * indent + str(key) + ": ", end="")
        if isinstance(value, (dict, list)):
            print()
            print_structure(value, indent + 1)
        else:
            print(value)

print_structure(data)
```

---

### 8. **Следующие шаги**
1. Сделать все `civilizationInfo` transient.
2. Убедиться, что `setTransients` корректно инициализирует временные поля.
3. Добавить тесты для проверки сериализации и десериализации, особенно для миграции старых версий.
4. Написать утилиту для просмотра JSON.

Если нужно углубить анализ или реализовать скрипт, дайте знать!
