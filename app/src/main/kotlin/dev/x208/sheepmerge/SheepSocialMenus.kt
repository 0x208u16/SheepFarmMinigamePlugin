package dev.x208.sheepmerge

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

internal object SheepSocialMenus {
    private const val PAGE_SIZE = 31

    @JvmStatic
    fun isSocialsMenuTitle(title: String?): Boolean = SheepMergeManager.SOCIALS_MENU_TITLE == title

    @JvmStatic
    fun openSocialsMenu(player: Player?) = openSocialsMenu(player, 0)

    @JvmStatic
    fun refreshOpenSocialsMenuItems(player: Player?, inventory: Inventory?) {
        if (player == null || inventory == null) return
        populateSocialsMenuItems(player, inventory, currentPage(player))
    }

    private fun openSocialsMenu(player: Player?, requestedPage: Int) {
        if (player == null) return
        val inventory = Bukkit.createInventory(null, 54, SheepMergeManager.SOCIALS_MENU_TITLE)
        populateSocialsMenuItems(player, inventory, requestedPage)
        player.openInventory(inventory)
    }

    private fun populateSocialsMenuItems(player: Player, inventory: Inventory, requestedPage: Int) {
        val owners = Bukkit.getOnlinePlayers()
            .filter { SheepMergeManager.socialsCanListOwner(player, it) }
            .sortedWith { left, right -> left.name.compareTo(right.name, ignoreCase = true) }
        val totalPages = maxOf(1, kotlin.math.ceil(owners.size / PAGE_SIZE.toDouble()).toInt())
        val page = requestedPage.coerceIn(0, totalPages - 1)
        SheepRuntimeUiState.socialsPages()[player.uniqueId] = page

        setMenuItemIfChanged(inventory, SheepMergeManager.SOCIALS_PREVIOUS_PAGE_SLOT, MenuItemFactory.create(
            Material.ARROW, "Previous Page", listOf(
                "Page ${page + 1} / $totalPages",
                if (page > 0) "Click to go back" else "Already at first page"
            )
        ))
        setMenuItemIfChanged(inventory, SheepMergeManager.SOCIALS_NEXT_PAGE_SLOT, MenuItemFactory.create(
            Material.ARROW, "Next Page", listOf(
                "Page ${page + 1} / $totalPages",
                if (page + 1 < totalPages) "Click to advance" else "Already at last page"
            )
        ))
        val topLore = mutableListOf("Top players by Coins:").apply {
            addAll(SheepMergeManager.getTopPointsLines(5))
        }
        setMenuItemIfChanged(inventory, SheepMergeManager.SOCIALS_TOP_POINTS_SLOT,
            MenuItemFactory.create(Material.GOLD_INGOT, "Top Coins", topLore))
        val visiting = SheepMergeManager.socialsIsVisitingAnotherFarm(player)
        setMenuItemIfChanged(inventory, SheepMergeManager.SOCIALS_RETURN_HOME_SLOT, MenuItemFactory.create(
            Material.COMPASS, "Return To Your Farm", listOf(
                if (visiting) "You are currently visiting." else "You are already in your own world.",
                "Click to return home"
            )
        ))
        setMenuItemIfChanged(inventory, SheepMergeManager.SOCIALS_BACK_SLOT,
            MenuItemFactory.create(Material.ARROW, "Back", listOf("Click: Open menu")))

        socialVisitSlots.forEach { inventory.setItem(it, null) }
        val start = page * PAGE_SIZE
        for (offset in 0 until PAGE_SIZE) {
            val owner = owners.getOrNull(start + offset) ?: break
            val slot = socialVisitSlots.getOrNull(offset) ?: break
            setMenuItemIfChanged(inventory, slot, createVisitItem(owner))
        }
    }

    @JvmStatic
    fun handleSocialsMenuClick(player: Player?, slot: Int, clickedItem: ItemStack?) {
        if (player == null) return
        when (slot) {
            SheepMergeManager.SOCIALS_PREVIOUS_PAGE_SLOT -> return openSocialsMenu(player, currentPage(player) - 1)
            SheepMergeManager.SOCIALS_NEXT_PAGE_SLOT -> return openSocialsMenu(player, currentPage(player) + 1)
            SheepMergeManager.SOCIALS_BACK_SLOT -> return SheepMergeManager.openUpgradeMenu(player)
            SheepMergeManager.SOCIALS_RETURN_HOME_SLOT -> {
                if (SheepMergeManager.socialsIsVisitingAnotherFarm(player)) {
                    SheepMergeManager.socialsReturnHome(player)
                } else {
                    player.sendMessage(SheepMergeManager.hint("You are already in your own world."))
                }
                return
            }
        }
        val ownerId = visitOwnerId(clickedItem) ?: return
        val owner = Bukkit.getPlayer(ownerId)
        if (owner == null || !owner.isOnline) {
            player.sendMessage(SheepMergeManager.warning("That player is no longer online."))
            return openSocialsMenu(player, currentPage(player))
        }
        if (!SheepMergeManager.socialsCanVisit(player, ownerId)) {
            player.sendMessage(SheepMergeManager.warning("That farm is closed to visitors."))
            return openSocialsMenu(player, currentPage(player))
        }
        SheepMergeManager.socialsVisit(player, owner)
    }

    private fun createVisitItem(owner: Player): ItemStack {
        val head = ItemStack(Material.PLAYER_HEAD, 1)
        val meta = head.itemMeta as? SkullMeta
            ?: return MenuItemFactory.create(Material.PLAYER_HEAD, "Visit ${owner.name}", listOf("Click to visit"))
        meta.owningPlayer = owner
        meta.setDisplayName("Visit ${owner.name}")
        meta.lore = listOf("Farm owner: ${owner.name}", "Click to visit")
        meta.persistentDataContainer.set(
            SheepMergeManager.socialsVisitOwnerKey(), PersistentDataType.STRING, owner.uniqueId.toString()
        )
        head.itemMeta = meta
        return head
    }

    private fun visitOwnerId(item: ItemStack?): UUID? {
        if (item?.type != Material.PLAYER_HEAD) return null
        val raw = item.itemMeta?.persistentDataContainer?.get(
            SheepMergeManager.socialsVisitOwnerKey(), PersistentDataType.STRING
        ) ?: return null
        return runCatching { UUID.fromString(raw) }.getOrNull()
    }

    private fun currentPage(player: Player): Int =
        SheepRuntimeUiState.socialsPages().getOrDefault(player.uniqueId, 0).coerceAtLeast(0)

    private fun setMenuItemIfChanged(inventory: Inventory, slot: Int, next: ItemStack?) {
        if (slot !in 0 until inventory.size) return
        val current = inventory.getItem(slot)
        if (current == null && next == null) return
        if (current != null && next != null && current.isSimilar(next) && current.amount == next.amount) return
        inventory.setItem(slot, next)
    }

    private val socialVisitSlots = (10 until 44).filter { it % 9 != 8 }
}