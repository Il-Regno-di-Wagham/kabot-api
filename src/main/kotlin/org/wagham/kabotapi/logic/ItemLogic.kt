package org.wagham.kabotapi.logic

import kotlinx.coroutines.flow.Flow
import org.wagham.db.models.GenericSession
import org.wagham.db.models.Item
import org.wagham.db.models.Player
import org.wagham.db.models.embed.LabelStub
import org.wagham.kabotapi.entities.PaginatedList
import javax.management.Query

interface ItemLogic {

    /**
     * Retrieves all [Item]s in a guild.
     *
     * @param guildId the id of the guild.
     * @return a [Flow] of [Item].
     */
    fun getItems(guildId: String): Flow<Item>

    /**
     * Retrieves all [Item]s in a guild with a specified id.
     *
     * @param guildId the id of the guild.
     * @param ids a [Set] containing the ids of the items to retrieve.
     * @return a [Flow] of [Item].
     */
    fun getItems(guildId: String, ids: Set<String>): Flow<Item>

    /**
     * Retrieves all the [Item]s for which the item with the specified id is a craft material
     *
     * @param guildId the id of the guild.
     * @param itemId the id of the item.
     * @return a [Flow] of [Item]s.
     */
    fun isMaterialOf(guildId: String, itemId: String): Flow<Item>

    /**
     * Returns all the items in a guild, where [Item.normalizedName] matches the normalized [query] (if present) and that
     * have the [label] passed as parameter (if present) with support for pagination.
     *
     * @param guildId the id of the guild where to retrieve the sessions.
     * @param label a [LabelStub] that the retrieved items should have. If null, the items for any label will be returned.
     * @param query a query (prefix) to search by normalized name. If null, the items with any name will be returned.
     * @param limit the maximum numbers of elements to be included in the page.
     * @param skip the number of elements to skip to go to the starting element of the page.
     * @return a [PaginatedList] of [Item].
     */
    suspend fun searchItems(guildId: String, label: LabelStub? = null, query: String? = null, limit: Int? = null, skip: Int? = null): PaginatedList<Item>

}