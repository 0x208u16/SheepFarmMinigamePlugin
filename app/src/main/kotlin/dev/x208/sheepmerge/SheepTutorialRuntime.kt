package dev.x208.sheepmerge

import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.math.BigInteger
import java.util.UUID

internal object SheepTutorialRuntime {
    enum class Action {
        SPAWN_SHEEP,
        SHEAR_SHEEP,
        MERGE_SHEEP,
        OPEN_UPGRADE_COMMAND,
        OPEN_SHOP_COMMAND,
        OPEN_PRESTIGE_COMMAND,
        OTHER_COMMAND,
    }

    private enum class Step {
        SPAWN,
        SHEAR,
        MERGE,
        OPEN_UPGRADES,
        BUY_REGULAR_UPGRADE,
        OPEN_QUESTS,
        USE_ABILITY,
        BUY_SHEAR_UPGRADE,
        OPEN_PRESTIGE,
        PRESTIGE_ONCE,
        COMPLETE,
    }

    private var shearTarget = 3
    private var spawnTarget = 3
    private var mergeTarget = 1
    private var menuSectionTarget = 7
    private var reminderDelayMs = 2L * 60L * 1000L
    private var reminderRepeatMs = 60_000L
    private var taskTitleRepeatMs = 12_000L
    private var statusFeedRepeatMs = 12_000L
    private var focusNotificationCooldownMs = 8_000L
    private var mergePointsReminderRepeatMs = 15_000L

    @JvmStatic
    fun configure(
        shearTarget: Int,
        spawnTarget: Int,
        mergeTarget: Int,
        menuSectionTarget: Int,
        reminderDelayMs: Long,
        reminderRepeatMs: Long,
        taskTitleRepeatMs: Long,
        statusFeedRepeatMs: Long,
        focusNotificationCooldownMs: Long,
        mergePointsReminderRepeatMs: Long,
    ) {
        this.shearTarget = shearTarget
        this.spawnTarget = spawnTarget
        this.mergeTarget = mergeTarget
        this.menuSectionTarget = menuSectionTarget
        this.reminderDelayMs = reminderDelayMs
        this.reminderRepeatMs = reminderRepeatMs
        this.taskTitleRepeatMs = taskTitleRepeatMs
        this.statusFeedRepeatMs = statusFeedRepeatMs
        this.focusNotificationCooldownMs = focusNotificationCooldownMs
        this.mergePointsReminderRepeatMs = mergePointsReminderRepeatMs
    }

    @JvmStatic
    fun shearCount(player: Player?): Int = player?.let { SheepTutorialState.getShears(it.uniqueId) } ?: 0

    @JvmStatic
    fun spawnCount(player: Player?): Int = player?.let { SheepTutorialState.getSpawns(it.uniqueId) } ?: 0

    @JvmStatic
    fun mergeCount(player: Player?): Int = player?.let { SheepTutorialState.getMerges(it.uniqueId) } ?: 0

    @JvmStatic
    fun start(player: Player?, resetProgress: Boolean) {
        if (player == null) return
        val playerId = player.uniqueId
        if (resetProgress) {
            resetProgress(playerId)
            SheepMergeManager.tutorialSaveData()
        }
        SheepTutorialState.setStartedAt(playerId, System.currentTimeMillis())
        SheepTutorialState.clearLastReminderTimestamp(playerId)
        if (!SheepFarmWorldCommand.teleportToTutorialWorld(player)) {
            player.sendMessage(SheepMergeManager.warning("Unable to open your tutorial world right now."))
            return
        }
        SheepMergeManager.addEggs(player, 10)
        sendTitle(player, "&eSheepMerge Tutorial", "&fFollow the steps to unlock your farm")
        player.sendMessage(SheepMergeManager.hint("Step 1: Spawn $spawnTarget sheep."))
        player.sendMessage(SheepMergeManager.hint("Step 2: Shear $shearTarget sheep."))
        player.sendMessage(SheepMergeManager.hint("Step 3: Merge $mergeTarget pair (SHIFT + RIGHT-CLICK)."))
        player.sendMessage(SheepMergeManager.hint("Step 4: Menus -> Upgrades, Quests, Shear Shop, Prestige."))
        player.sendMessage(SheepMergeManager.accent("Tip: /sheepmerge status shows your current step."))
        sendStatusFeed(player)
    }

    @JvmStatic
    fun tickReminder(player: Player?) {
        if (player == null) return
        val playerId = player.uniqueId
        if (!inProgress(player) || !inTutorialWorld(player)) {
            SheepTutorialState.clearRuntimeState(playerId)
            return
        }
        markRegularUpgradesIfComplete(player)
        if (SheepMergeManager.getShearShopLevel(player) > 0) markShearUpgraded(player)
        if (SheepMergeManager.getPrestigeLevel(player) > 0) markPrestigedOnce(player)

        val now = System.currentTimeMillis()
        tickTaskTitle(player, now)
        maybeSendMergePointsReminder(player, now)
        val startedAt = SheepTutorialState.ensureStartedAt(playerId, now)
        if (now - startedAt < reminderDelayMs) return
        val lastReminder = SheepTutorialState.getLastReminderTimestamp(playerId)
        if (now - lastReminder < reminderRepeatMs) return
        SheepTutorialState.setLastReminderTimestamp(playerId, now)
        player.sendMessage(SheepMergeManager.warning("Finish the tutorial to unlock your farm."))
        player.sendMessage(SheepMergeManager.hint("Next: ${nextStepLine(player)}"))
    }

    @JvmStatic
    fun markUpgradeOpened(player: Player?) = markSection(player, SheepTutorialState.Section.UPGRADE_OPENED, "Tutorial step done: Upgrades opened.")

    @JvmStatic
    fun markQuestOpened(player: Player?) = markSection(player, SheepTutorialState.Section.QUEST_OPENED, "Tutorial step done: Quests opened.")

    @JvmStatic
    fun markPrestigeOpened(player: Player?) = markSection(player, SheepTutorialState.Section.PRESTIGE_OPENED, "Tutorial step done: Prestige opened.")

    @JvmStatic
    fun markQuestUpgradesOpened(player: Player?) = markSection(player, SheepTutorialState.Section.QUEST_UPGRADES_OPENED, "Tutorial step done: Quest Upgrades opened.")

    @JvmStatic
    fun markAbilityUsed(player: Player?) = markSection(player, SheepTutorialState.Section.ABILITY_USED, "Tutorial step done: Ability used.")

    @JvmStatic
    fun markShearUpgraded(player: Player?) = markSection(player, SheepTutorialState.Section.SHEAR_UPGRADED, "Tutorial step done: Shear upgrade bought.")

    @JvmStatic
    fun markPrestigedOnce(player: Player?) = markSection(player, SheepTutorialState.Section.PRESTIGED_ONCE, "Tutorial step done: Prestiged once.")

    @JvmStatic
    fun markShearShopOpened(player: Player?) = markSection(player, SheepTutorialState.Section.SHEAR_SHOP_OPENED, "Tutorial step done: Shear Shop opened.")

    @JvmStatic
    fun markRegularUpgradesIfComplete(player: Player?) {
        if (!inProgress(player) || !inTutorialWorld(player) || player == null) return
        if (SheepMergeManager.getLimitUpgradeLevel(player) <= 0 && SheepMergeManager.getEggSpeedLevel(player) <= 0 &&
            SheepMergeManager.getWoolRegenLevel(player) <= 0 && SheepMergeManager.getHigherTierChanceLevel(player) <= 0
        ) return
        markSection(player, SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT, "Tutorial step done: Regular upgrade bought.")
    }

    @JvmStatic
    fun recordShear(player: Player?) = record(player, SheepTutorialState::getShears, SheepTutorialState::setShears, true)

    @JvmStatic
    fun recordSpawn(player: Player?) = record(player, SheepTutorialState::getSpawns, SheepTutorialState::setSpawns, true)

    @JvmStatic
    fun recordMerge(player: Player?) = record(player, SheepTutorialState::getMerges, SheepTutorialState::setMerges, false)

    @JvmStatic
    fun shouldRestrictActions(player: Player?): Boolean = inProgress(player) && inTutorialWorld(player)

    @JvmStatic
    fun blockAction(player: Player?, action: Action?, attemptedAction: String?): Boolean {
        if (!shouldRestrictActions(player) || player == null || action == null) return false
        val step = currentStep(player)
        if (isActionAllowed(step, action)) return false
        notifyOffTask(player, attemptedAction, step)
        return false
    }

    @JvmStatic
    fun blockRegularUpgradePurchase(player: Player?): Boolean =
        blockMenuPurchase(player, Step.BUY_REGULAR_UPGRADE, "Hotbar Slot 9 -> Upgrade Menu -> Buy one regular upgrade")

    @JvmStatic
    fun blockPrestigePurchase(player: Player?): Boolean =
        blockMenuPurchase(player, Step.PRESTIGE_ONCE, "Upgrade Menu -> Prestige Menu -> Prestige Reset")

    @JvmStatic
    fun blockShearUpgradePurchase(player: Player?): Boolean =
        blockMenuPurchase(player, Step.BUY_SHEAR_UPGRADE, "Upgrade Menu -> Shear Shop -> Buy one Shear Shop upgrade")

    @JvmStatic
    fun blockCompletedStepPurchase(player: Player?, requiredAction: String?): Boolean =
        blockMenuPurchase(player, Step.COMPLETE, requiredAction.orEmpty())

    @JvmStatic
    fun blockAbilityUse(player: Player?): Boolean =
        blockMenuPurchase(player, Step.USE_ABILITY, "Activate any quest ability")

    @JvmStatic
    fun progressLine(player: Player?): String =
        "Spawn ${spawnCount(player)}/$spawnTarget | Shear ${shearCount(player)}/$shearTarget" +
            " | Merge ${mergeCount(player)}/$mergeTarget | Menus ${sectionCount(player)}/${effectiveMenuSectionTarget()}"

    @JvmStatic
    fun canSpendUpgradePoints(player: Player?, spendPoints: BigInteger?): Boolean {
        if (!inProgress(player) || !inTutorialWorld(player) || player == null || spendPoints == null) return true
        val step = currentStep(player)
        if (step != Step.OPEN_PRESTIGE && step != Step.PRESTIGE_ONCE) return true
        val prestigeCost = SheepMergeManager.tutorialPrestigeCost(player)
        if (SheepMergeManager.tutorialPlayerPoints(player).subtract(spendPoints) >= prestigeCost) return true
        player.sendMessage(SheepMergeManager.warning("Tutorial: keep at least ${SheepMergeManager.formatPoints(prestigeCost)} Coins for your prestige step."))
        player.sendMessage(SheepMergeManager.hint("Merge/shear a bit more before buying this upgrade."))
        return false
    }

    @JvmStatic
    fun resetProgress(playerId: UUID?) {
        if (playerId == null) return
        SheepSnapshotState.removeTutorial(playerId)
        SheepTutorialState.resetPlayer(playerId)
    }

    private fun markSection(player: Player?, section: SheepTutorialState.Section, message: String) {
        if (!inProgress(player) || !inTutorialWorld(player) || player == null) return
        val playerId = player.uniqueId
        if (SheepTutorialState.isSectionComplete(playerId, section)) return
        SheepTutorialState.setSectionComplete(playerId, section)
        player.sendMessage(SheepMergeManager.action(message))
        sendStatusFeed(player)
        maybeGrantPrestigePrepReward(player)
        checkCompletion(player)
    }

    private fun record(
        player: Player?,
        getCount: (UUID) -> Int,
        setCount: (UUID, Int) -> Unit,
        checkShearReward: Boolean,
    ) {
        if (!inProgress(player) || !inTutorialWorld(player) || player == null) return
        setCount(player.uniqueId, getCount(player.uniqueId) + 1)
        sendStatusFeed(player)
        if (checkShearReward) maybeGrantShearTaskReward(player)
        maybeGrantPrestigePrepReward(player)
        checkCompletion(player)
    }

    private fun maybeGrantShearTaskReward(player: Player) {
        val playerId = player.uniqueId
        if (SheepTutorialState.isShearTaskRewardGranted(playerId) || spawnCount(player) < spawnTarget || shearCount(player) < shearTarget) return
        SheepTutorialState.setShearTaskRewardGranted(playerId)
        player.sendMessage(SheepMergeManager.action("Tutorial milestone: spawn + shear goals complete."))
    }

    private fun maybeGrantPrestigePrepReward(player: Player) {
        val playerId = player.uniqueId
        if (SheepTutorialState.isPrestigePrepRewardGranted(playerId) ||
            SheepTutorialState.isSectionComplete(playerId, SheepTutorialState.Section.PRESTIGED_ONCE) ||
            shearCount(player) < shearTarget || spawnCount(player) < spawnTarget || mergeCount(player) < mergeTarget
        ) return
        val required = listOf(
            SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT,
            SheepTutorialState.Section.UPGRADE_OPENED,
            SheepTutorialState.Section.QUEST_OPENED,
            SheepTutorialState.Section.PRESTIGE_OPENED,
            SheepTutorialState.Section.ABILITY_USED,
            SheepTutorialState.Section.SHEAR_UPGRADED,
        )
        if (required.any { !SheepTutorialState.isSectionComplete(playerId, it) }) return
        SheepTutorialState.setPrestigePrepRewardGranted(playerId)
        player.sendMessage(SheepMergeManager.action("Tutorial milestone: prestige prep complete."))
    }

    private fun currentStep(player: Player?): Step {
        if (player == null) return Step.COMPLETE
        if (spawnCount(player) < spawnTarget) return Step.SPAWN
        if (shearCount(player) < shearTarget) return Step.SHEAR
        if (mergeCount(player) < mergeTarget) return Step.MERGE
        val playerId = player.uniqueId
        if (!complete(playerId, SheepTutorialState.Section.UPGRADE_OPENED)) return Step.OPEN_UPGRADES
        if (!complete(playerId, SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT)) return Step.BUY_REGULAR_UPGRADE
        if (!complete(playerId, SheepTutorialState.Section.QUEST_OPENED)) return Step.OPEN_QUESTS
        if (!complete(playerId, SheepTutorialState.Section.ABILITY_USED)) return Step.USE_ABILITY
        if (!complete(playerId, SheepTutorialState.Section.SHEAR_UPGRADED)) return Step.BUY_SHEAR_UPGRADE
        if (!complete(playerId, SheepTutorialState.Section.PRESTIGE_OPENED)) return Step.OPEN_PRESTIGE
        if (!complete(playerId, SheepTutorialState.Section.PRESTIGED_ONCE)) return Step.PRESTIGE_ONCE
        return Step.COMPLETE
    }

    private fun nextStepLine(player: Player?): String {
        if (player == null) return "Enter your tutorial world"
        return when (val step = currentStep(player)) {
            Step.SPAWN -> "Spawn sheep (${spawnCount(player)}/$spawnTarget)"
            Step.SHEAR -> "Shear sheep (${shearCount(player)}/$shearTarget)"
            Step.MERGE -> "Merge same-tier sheep (SHIFT + RIGHT-CLICK) (${mergeCount(player)}/$mergeTarget)"
            else -> taskLabel(step)
        }
    }

    private fun taskLabel(step: Step): String = when (step) {
        Step.SPAWN -> "Spawn sheep"
        Step.SHEAR -> "Shear sheep"
        Step.MERGE -> "Merge same-tier sheep"
        Step.OPEN_UPGRADES -> "Hotbar Slot 9 -> Upgrade Menu"
        Step.BUY_REGULAR_UPGRADE -> "Hotbar Slot 9 -> Upgrade Menu -> Buy one regular upgrade"
        Step.OPEN_QUESTS -> "Upgrade Menu -> Quest Menu"
        Step.USE_ABILITY -> "Upgrade Menu -> Quest Menu -> Activate any quest ability"
        Step.BUY_SHEAR_UPGRADE -> "Upgrade Menu -> Shear Shop -> Buy one Shear Shop upgrade"
        Step.OPEN_PRESTIGE -> "Upgrade Menu -> Prestige Menu"
        Step.PRESTIGE_ONCE -> "Upgrade Menu -> Prestige Menu -> Prestige Reset"
        Step.COMPLETE -> "Tutorial complete"
    }

    private fun isActionAllowed(step: Step, action: Action): Boolean = when (action) {
        Action.SPAWN_SHEEP -> step == Step.SPAWN
        Action.SHEAR_SHEEP -> step == Step.SHEAR
        Action.MERGE_SHEEP -> step == Step.MERGE
        Action.OPEN_UPGRADE_COMMAND -> step in Step.OPEN_UPGRADES..Step.PRESTIGE_ONCE
        Action.OPEN_SHOP_COMMAND -> step == Step.BUY_SHEAR_UPGRADE
        Action.OPEN_PRESTIGE_COMMAND -> step == Step.OPEN_PRESTIGE || step == Step.PRESTIGE_ONCE
        Action.OTHER_COMMAND -> step == Step.COMPLETE
    }

    private fun blockMenuPurchase(player: Player?, requiredStep: Step, requiredAction: String): Boolean {
        if (!inProgress(player) || !inTutorialWorld(player) || player == null) return false
        if (currentStep(player) == requiredStep) return false
        notifyOffTask(player, requiredAction, currentStep(player))
        return false
    }

    private fun notifyOffTask(player: Player, attemptedAction: String?, step: Step) {
        val now = System.currentTimeMillis()
        val lastShownAt = SheepTutorialState.getLastFocusNotificationTimestamp(player.uniqueId)
        if (now - lastShownAt < focusNotificationCooldownMs) return
        SheepTutorialState.setLastFocusNotificationTimestamp(player.uniqueId, now)
        val currentTask = taskLabel(step)
        if (!attemptedAction.isNullOrBlank()) player.sendMessage(SheepMergeManager.warning("Not now: $attemptedAction"))
        player.sendMessage(SheepMergeManager.hint("Do this now: $currentTask"))
        SheepMergeManager.showOverlay(player, SheepMergeManager.warning("Tutorial step: $currentTask"))
        sendTitle(player, "&6Tutorial Focus", "&f$currentTask")
    }

    private fun tickTaskTitle(player: Player, now: Long) {
        val titleStep = nextStepLine(player)
        val previousStep = SheepTutorialState.getLastTaskTitleStep(player.uniqueId)
        val lastShownAt = SheepTutorialState.getLastTaskTitleTimestamp(player.uniqueId)
        if (titleStep == previousStep && now - lastShownAt < taskTitleRepeatMs) return
        sendTitle(player, "&eTutorial Step", "&f$titleStep")
        SheepTutorialState.setLastTaskTitleTimestamp(player.uniqueId, now)
        SheepTutorialState.setLastTaskTitleStep(player.uniqueId, titleStep)
    }

    private fun maybeSendMergePointsReminder(player: Player, now: Long) {
        val step = currentStep(player)
        val requiredPoints = when (step) {
            Step.BUY_REGULAR_UPGRADE -> SheepMergeManager.tutorialMinimumRegularUpgradeCost(player)
            Step.BUY_SHEAR_UPGRADE -> SheepMergeManager.tutorialMinimumShearUpgradeCost(player)
            Step.PRESTIGE_ONCE -> SheepMergeManager.tutorialPrestigeCost(player)
            else -> BigInteger.valueOf(-1L)
        }
        if (requiredPoints.signum() <= 0) return
        val currentPoints = SheepMergeManager.tutorialPlayerPoints(player)
        if (currentPoints >= requiredPoints) return
        val lastReminder = SheepTutorialState.getLastMergePointsReminderTimestamp(player.uniqueId)
        if (now - lastReminder < mergePointsReminderRepeatMs) return
        val missing = requiredPoints.subtract(currentPoints).max(BigInteger.ZERO)
        SheepTutorialState.setLastMergePointsReminderTimestamp(player.uniqueId, now)
        player.sendMessage(SheepMergeManager.warning("Need ${SheepMergeManager.formatPoints(requiredPoints)} Coins for: ${taskLabel(step)}"))
        player.sendMessage(SheepMergeManager.hint("You are short ${SheepMergeManager.formatPoints(missing)}. Merge sheep to gain Coins fast."))
    }

    private fun sendStatusFeed(player: Player) {
        val progressLine = progressLine(player)
        val stepLine = nextStepLine(player)
        val now = System.currentTimeMillis()
        val changed = progressLine != SheepTutorialState.getLastProgressFeedLine(player.uniqueId) ||
            stepLine != SheepTutorialState.getLastStepFeedLine(player.uniqueId)
        if (!changed && now - SheepTutorialState.getLastStatusFeedTimestamp(player.uniqueId) < statusFeedRepeatMs) return
        SheepTutorialState.setLastStatusFeedTimestamp(player.uniqueId, now)
        SheepTutorialState.setLastProgressFeedLine(player.uniqueId, progressLine)
        SheepTutorialState.setLastStepFeedLine(player.uniqueId, stepLine)
        player.sendMessage(SheepMergeManager.hint("Step: $stepLine"))
        player.sendMessage(SheepMergeManager.accent(progressLine))
    }

    private fun checkCompletion(player: Player) {
        if (SheepMergeManager.hasUnlockedFarm(player)) return
        if (shearCount(player) < shearTarget || spawnCount(player) < spawnTarget || mergeCount(player) < mergeTarget ||
            sectionCount(player) < effectiveMenuSectionTarget()
        ) return
        SheepTutorialState.setCompleted(player.uniqueId, true)
        SheepTutorialState.clearRuntimeState(player.uniqueId)
        SheepMergeManager.tutorialCompleteWorldTransition(player)
        SheepMergeManager.tutorialSaveData()
    }

    private fun sectionCount(player: Player?): Int {
        if (player == null) return 0
        val sections = listOf(
            SheepTutorialState.Section.UPGRADE_OPENED,
            SheepTutorialState.Section.QUEST_OPENED,
            SheepTutorialState.Section.PRESTIGE_OPENED,
            SheepTutorialState.Section.ABILITY_USED,
            SheepTutorialState.Section.SHEAR_UPGRADED,
            SheepTutorialState.Section.REGULAR_UPGRADES_BOUGHT,
            SheepTutorialState.Section.PRESTIGED_ONCE,
        )
        return sections.count { complete(player.uniqueId, it) }
    }

    private fun effectiveMenuSectionTarget(): Int = menuSectionTarget.coerceIn(1, 7)

    private fun complete(playerId: UUID, section: SheepTutorialState.Section): Boolean =
        SheepTutorialState.isSectionComplete(playerId, section)

    private fun inProgress(player: Player?): Boolean = player != null && !SheepMergeManager.hasUnlockedFarm(player)

    private fun inTutorialWorld(player: Player?): Boolean = player != null && SheepMergeManager.isTutorialWorld(player.world)

    private fun sendTitle(player: Player, title: String, subtitle: String) {
        val plain = ChatColor.stripColor(SheepMergeManager.tutorialColor(subtitle))
        val stayTicks = (35 + kotlin.math.ceil((plain?.trim()?.length ?: 0) * 1.5).toInt()).coerceIn(35, 120)
        player.sendTitle(SheepMergeManager.tutorialColor(title), SheepMergeManager.tutorialColor(subtitle), 0, stayTicks, 10)
    }
}