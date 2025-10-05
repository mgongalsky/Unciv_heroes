package com.unciv.clean.app

import com.badlogic.gdx.utils.Json
import com.unciv.clean.adapters.events.InProcessDomainEventBus
import com.unciv.clean.adapters.persistence.SaveMigrationRegistry
import com.unciv.clean.adapters.persistence.VersionedSaveIO
import com.unciv.clean.application.ports.events.DomainEventBus
import com.unciv.clean.application.ports.events.DomainEventHandler
import com.unciv.clean.application.ports.persistence.SaveWritePolicy

/**
 * Единая точка сборки адаптеров/портов/флагов.
 * По умолчанию — NO-OP подписчики и LEGACY запись сейвов.
 */
class CompositionRoot(
    private val json: Json,
    // Флаги/конфиг можно получать из настроек игры/CLI/ENV
    private val featureNotificationsViaBus: Boolean = false,
    private val saveWritePolicy: SaveWritePolicy = SaveWritePolicy.LEGACY_BY_DEFAULT
) {
    // --- Event bus ---
    val handlers: List<DomainEventHandler> by lazy {
        if (!featureNotificationsViaBus) emptyList()
        else buildList<DomainEventHandler> {
            // Подключение обработчиков при включении флага
            // add(UiNotificationsEventHandlerV2(...))
        }
    }

    val eventBus: DomainEventBus by lazy {
        InProcessDomainEventBus(handlers, logger = null)
    }

    // --- Save IO ---
    val migrationRegistry = SaveMigrationRegistry()
    val saveIO: VersionedSaveIO by lazy {
        VersionedSaveIO(
            json = json,
            migrationRegistry = migrationRegistry,
            writePolicy = saveWritePolicy
        )
    }
}
