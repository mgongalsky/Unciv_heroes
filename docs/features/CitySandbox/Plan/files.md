# Фактический перечень файлов

Все пути относительно корня проекта.

## Созданы

- core/src/com/unciv/ui/cityscreen/CityScreenContext.kt — источники разрешений городского UI.
- core/src/com/unciv/ui/cityscreen/CitySandbox.kt — минимальный сценарий в памяти и фабрика экрана.
- desktop/src/com/unciv/app/desktop/DesktopLauncherCity.kt — отдельный desktop main.

## Изменены

- core/src/com/unciv/ui/cityscreen/CityScreen.kt — явный контекст, пересоздание, отключение отметок обучения для песочницы.
- core/src/com/unciv/ui/cityscreen/ConstructionInfoTable.kt — признак хода из экрана.
- core/src/com/unciv/ui/cityscreen/SpecialistAllocationTable.kt — признак хода из экрана.
- core/src/com/unciv/ui/cityscreen/CityScreenCityPickerTable.kt — сохранение контекста после переименования.
- core/src/com/unciv/ui/cityscreen/CityScreenTileTable.kt — сохранение контекста после покупки и перехода.
- desktop/src/com/unciv/app/desktop/CityScreenGoldenChecks.kt — standaloneLoadingScreen и проверки автономности.
- desktop/src/com/unciv/app/desktop/UiGoldenTestRunner.kt — сценарий city-sandbox до сценариев с загрузкой партии.
- tests/src/com/unciv/testing/UiGoldenTest.kt — citySandboxRender и citySandbox.
- tests/src/com/unciv/testing/UiGoldenUpdateTest.kt — отдельный метод обновления будущего эталона; не запускался.

## Создана документация

- docs/features/CitySandbox/Research/RESEARCH.md
- docs/features/CitySandbox/Plan/PLAN.md
- docs/features/CitySandbox/Plan/STEPS.md
- docs/features/CitySandbox/Plan/STEP_01_Isolation.md
- docs/features/CitySandbox/Plan/STEP_02_Verification.md
- docs/features/CitySandbox/Plan/files.md
- docs/features/CitySandbox/CODE_GUIDE.md

Удалений и переносов нет. Прежние изменения пользователя, бандлы и сгенерированные результаты проверок не включаются в изменения задачи. Ветка feature/city-sandbox создана от feature/battle-linux; коммит и публикация не выполнялись.
