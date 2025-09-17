# Анализ системы анимации собираемых ресурсов - 16 сентября 2025

## Цель сессии
Реализовать анимированное исчезновение собираемых предметов (Gold, Food, Production, Science, Happiness) при их сборе юнитами.

## Ключевые открытия архитектуры

### 1. Разделение типов ресурсов
**Важное понимание**: В Unciv существует два принципиально разных типа "ресурсов":

#### A. Настоящие ресурсы (`resource` field)
- **Примеры**: Железо, уголь, нефть, пшеница
- **Характеристика**: Постоянные месторождения на карте
- **Добыча**: Требует строительство соответствующего improvement'а (шахты, фермы)
- **UI представление**: `resourceImage` в TileGroup
- **Расположение**: Левый верхний угол тайла
- **Отображение**: Через `ImageGetter.getResourceImage()`

#### B. Собираемые предметы (`improvement` field)
- **Примеры**: Gold, Food, Production, Science, Happiness
- **Характеристика**: Одноразовые предметы, лежащие на земле
- **Добыча**: Подбираются при перемещении юнита на тайл
- **UI представление**: `improvementIcon` в TileGroupIcons
- **Расположение**: Левый нижний угол тайла  
- **Отображение**: Через `ImageGetter.getImprovementIcon()`

### 2. Структура UI компонентов

#### TileGroup.kt
```kotlin
var resourceImage: Actor? = null  // Для постоянных ресурсов (железо, уголь)
var resource: String? = null      // Название ресурса
```

#### TileGroupIcons.kt
```kotlin
var improvementIcon: Actor? = null  // Для улучшений и собираемых предметов
```

### 3. Архитектурная проблема анимации

#### Проблема разделения логики и UI
- **Логика сбора**: В классе `Visitable.visit()` 
- **UI анимация**: В классе `TileGroupIcons`
- **Проблема**: Прямой вызов UI из логики нарушает архитектуру

#### Первоначальное решение (неудачное)
```kotlin
// В Visitable.visit() - ПЛОХО!
currentIcon.addAction(Actions.fadeOut(1.0f))
```
**Проблемы**:
- Нарушение разделения слоев
- Прямая зависимость логики от UI
- Сложности с управлением жизненным циклом

#### Флаговое решение (реализовано)
```kotlin
// В TileInfo.kt - флаги состояния
@Transient var isItemBeingCollected = false
@Transient var collectionCallback: (() -> Unit)? = null  
@Transient var improvementAnimationStarted = false

// В Visitable.visit() - установка флагов
parentTile.isItemBeingCollected = true
parentTile.collectionCallback = { /* логика сбора */ }

// В TileGroupIcons.updateImprovementIcon() - проверка флагов
if (tileInfo.isItemBeingCollected && improvementIcon != null) {
    // запуск анимации
}
```

## Реализованная система

### Компоненты системы

#### 1. TileInfo.kt - Флаги состояния
```kotlin
// Флаги анимации для собираемых предметов
@Transient var isItemBeingCollected = false
@Transient var collectionCallback: (() -> Unit)? = null
@Transient var improvementAnimationStarted = false
```

#### 2. Visitable.kt - Логика сбора
```kotlin
fun visit(unit: MapUnit): Boolean {
    when (visitability) {
        Visitability.takeable -> {
            // Проверка на повторный запуск
            if (parentTile.isItemBeingCollected) return false
            
            // Установка флагов для UI
            parentTile.isItemBeingCollected = true
            parentTile.collectionCallback = {
                collectTakeableItem(unit)      // Фактический сбор
                parentTile.removeImprovement() // Удаление с карты
                resetAnimationFlags()          // Сброс флагов
            }
            return true
        }
    }
}
```

#### 3. TileGroupIcons.kt - UI анимация
```kotlin
private fun updateImprovementIcon(showResourcesAndImprovements: Boolean, viewingCiv: CivilizationInfo?) {
    // Проверка флагов анимации
    if (tileInfo.isItemBeingCollected && improvementIcon != null && !tileInfo.improvementAnimationStarted) {
        val currentIcon = improvementIcon!!
        tileInfo.improvementAnimationStarted = true
        
        // Комплексная анимация
        val fadeAction = Actions.sequence(
            Actions.parallel(
                Actions.fadeOut(1.5f),              // Затухание
                Actions.scaleTo(0.5f, 0.5f, 1.5f),  // Уменьшение
                Actions.rotateBy(180f, 1.5f)        // Поворот  
            ),
            Actions.run {
                tileInfo.collectionCallback?.invoke() // Выполнение сбора
            }
        )
        
        currentIcon.addAction(fadeAction)
        return // Не обновлять иконку во время анимации
    }
    
    // Обычная логика создания/обновления иконки
    // ...
}
```

#### 4. MapUnit.kt - Триггер обновления
```kotlin
fun visitPlace(tile: TileInfo) {
    if (civInfo.isMajorCiv() && tile.improvement != null && currentTile == tile) {
        tile.visitable!!.visit(this)
        
        // Важно: принудительное обновление UI
        if (UncivGame.Current.worldScreen != null) {
            UncivGame.Current.worldScreen!!.shouldUpdate = true
        }
    }
}
```

## Текущие проблемы

### 1. Смешение концепций ресурсов
- **Путаница**: resourceImage vs improvementIcon
- **Решение**: Четкое разделение - собираемые предметы ТОЛЬКО как improvements

### 2. Проблемы с анимацией
- **Статус**: Флаговая система реализована, но анимация не работает стабильно
- **Возможные причины**:
  - Проблемы с жизненным циклом UI компонентов
  - Неправильная синхронизация update циклов
  - Конфликты с другими UI обновлениями

### 3. Отладочная информация
- **Добавлено**: Extensive logging для отслеживания состояний
- **Результат**: Флаги устанавливаются, но анимация не всегда срабатывает

## Преимущества текущего подхода

### 1. Чистота архитектуры
- **Разделение**: Логика и UI полностью разделены
- **Флаги**: Состояние хранится в модели данных
- **Паттерн**: Соответствует существующему `shouldUpdate` подходу Unciv

### 2. Расширяемость
- **Callback система**: Легко добавлять сложную логику после анимации
- **Флаги**: Можно добавлять новые типы анимаций
- **Универсальность**: Работает для любых improvement'ов

### 3. Отладочность
- **Логирование**: Подробные debug сообщения на каждом этапе
- **Состояние**: Видимые флаги для отслеживания проблем

## План дальнейшей работы

### Приоритет 1: Исправление анимации
1. **Исследовать** жизненный цикл TileGroupIcons
2. **Проверить** порядок вызовов update методов
3. **Устранить** конфликты с другими UI обновлениями

### Приоритет 2: Упрощение системы
1. **Удалить** неиспользуемые resourceImage флаги
2. **Сосредоточиться** только на improvementIcon
3. **Убрать** тестовый код добавления Gold improvements

### Приоритет 3: Тестирование
1. **Создать** тестовые сценарии для разных типов предметов
2. **Проверить** работу с разными типами юнитов
3. **Убедиться** в корректном сбросе флагов

## Файлы, измененные в процессе работы
- `core/src/com/unciv/logic/map/TileInfo.kt` - добавлены флаги анимации
- `core/src/com/unciv/logic/map/Visitable.kt` - флаговая система сбора  
- `core/src/com/unciv/ui/tilegroups/TileGroupIcons.kt` - UI анимация
- `core/src/com/unciv/logic/map/MapUnit.kt` - триггер visitPlace
- `android/assets/jsons/Civ V - Vanilla/TileImprovements.json` - тестовые предметы

## Заключение
**Архитектурно система спроектирована правильно**, но требует доработки в части синхронизации UI обновлений. Флаговый подход является правильным направлением для Unciv, но нуждается в отладке механизма запуска анимаций.

**Следующий шаг**: Сосредоточиться на исправлении анимации improvementIcon'ов, временно игнорируя resourceImage.