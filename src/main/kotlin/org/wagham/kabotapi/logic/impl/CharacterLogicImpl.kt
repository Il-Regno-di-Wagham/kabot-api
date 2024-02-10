package org.wagham.kabotapi.logic.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.wagham.db.enums.CharacterStatus
import org.wagham.db.enums.TransactionType
import org.wagham.db.models.Character
import org.wagham.db.models.Errata
import org.wagham.db.models.Item
import org.wagham.db.models.dto.SessionOutcome
import org.wagham.db.models.embed.Transaction
import org.wagham.db.pipelines.characters.CharacterWithPlayer
import org.wagham.kabotapi.components.DatabaseComponent
import org.wagham.kabotapi.components.ExternalGateway
import org.wagham.kabotapi.entities.StatusResponse
import org.wagham.kabotapi.entities.dto.items.UpdateInventoryDto
import org.wagham.kabotapi.entities.dto.items.UpdateInventoryDto.Companion.InventoryUpdate
import org.wagham.kabotapi.exceptions.NotFoundException
import org.wagham.kabotapi.logic.CharacterLogic
import org.wagham.kabotapi.utils.transactionMoney
import java.util.*

class CharacterLogicImpl(
    private val database: DatabaseComponent,
    private val gateway: ExternalGateway
): CharacterLogic {
    override fun getActiveCharacters(guildId: String, playerId: String) =
        database.charactersScope.getActiveCharacters(guildId, playerId)

    override suspend fun getCharacter(guildId: String, playerId: String, characterId: String): Character {
        val character = database.charactersScope.getCharacter(guildId, characterId)
        if(character.player != playerId) {
            throw IllegalAccessException("You are not allowed to get this character")
        }
        return character
    }

    override fun getAllActiveCharacters(guildId: String): Flow<Character> =
        database.charactersScope.getAllCharacters(guildId, CharacterStatus.active)

    override fun getAllActiveCharactersWithPlayer(guildId: String): Flow<CharacterWithPlayer> =
        database.charactersScope.getCharactersWithPlayer(guildId, CharacterStatus.active)

    override suspend fun addErrata(guildId: String, characterId: String, errata: Errata): StatusResponse {
        val result = database.charactersScope.addErrata(guildId, characterId, errata)
        if(result.committed) {
            gateway.sendLevelUpInfo(guildId, listOf(SessionOutcome(characterId, errata.ms, errata.statusChange == CharacterStatus.dead)))
        }
        return StatusResponse(result.committed, result.exception?.message)
    }

    private suspend fun sellItem(guildId: String, character: Character, item: Item, qty: Int) {
        if(character.inventory.getOrDefault(item.name, 0) < qty) {
            throw IllegalArgumentException("You don't have enough ${item.name} to sell")
        }
        database.transaction(guildId) { session ->
            val itemRemoved = database.charactersScope.removeItemFromInventory(
                session,
                guildId,
                character.id,
                item.name,
                qty
            )
            val cost = requireNotNull(item.sell?.cost) { "This item cannot be sold" } * qty
            val moneyAdded = database.charactersScope.addMoney(session, guildId, character.id, cost)
            val transactionRegistered = database.transactionsScope.addTransactionForCharacter(
                session, guildId, character.id, Transaction(
                    Date(), null, "SELL", TransactionType.ADD, mapOf(transactionMoney to cost))
            ) && database.transactionsScope.addTransactionForCharacter(
                session, guildId, character.id, Transaction(Date(), null, "SELL", TransactionType.REMOVE, mapOf(item.name to qty.toFloat()))
            )
            itemRemoved && moneyAdded && transactionRegistered
        }.also {
            if (it.exception != null) throw it.exception!!
        }
    }

    override suspend fun updateInventory(guildId: String, playerId: String, characterId: String, updateData: UpdateInventoryDto) {
        val character = database.charactersScope.getCharacter(guildId, characterId)
        if(character.player != playerId) {
            throw IllegalAccessException("You are not allowed to get this character")
        }
        val item = database.itemsScope.getItems(guildId, setOf(updateData.itemId)).firstOrNull()
            ?: throw NotFoundException("Item not found: ${updateData.itemId}")

        when(updateData.operation) {
            InventoryUpdate.BUY -> throw IllegalStateException("Operation not implemented yet")
            InventoryUpdate.SELL -> sellItem(guildId, character, item, updateData.qty)
            InventoryUpdate.ASSIGN -> throw IllegalStateException("Operation not implemented yet")
            InventoryUpdate.TAKE -> throw IllegalStateException("Operation not implemented yet")
        }
    }
}