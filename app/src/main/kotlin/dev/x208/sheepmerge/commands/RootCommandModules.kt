package dev.x208.sheepmerge.commands

class HelpCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("help", executor, tabCompleter)

class DashHelpCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("-help", executor, tabCompleter)

class UpgradeCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("upgrade", executor, tabCompleter)

class PrestigeCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("prestige", executor, tabCompleter)

class ShopCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("shop", executor, tabCompleter)

class TopCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("top", executor, tabCompleter)

class VisitCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("visit", executor, tabCompleter)

class KickCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("kick", executor, tabCompleter)

class StatusCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("status", executor, tabCompleter)

class StormCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("storm", executor, tabCompleter)

class SummonCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("summon", executor, tabCompleter)

class ComboFrenzyCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("combofrenzy", executor, tabCompleter)

class ReloadCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("reload", executor, tabCompleter)

class LeaderboardCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("leaderboard", executor, tabCompleter)

class ResetDataCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("resetdata", executor, tabCompleter)

class StatsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("stats", executor, tabCompleter)

class CheckpointsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("checkpoints", executor, tabCompleter)

class CheckQuestPointsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("checkquestpoints", executor, tabCompleter)

class CheckPrestigeCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("checkprestige", executor, tabCompleter)

class GivePointsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("givepoints", executor, tabCompleter)

class GiveAutomationPointsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("giveautomationpoints", executor, tabCompleter)

class GiveSacrificePointsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("givesacrificepoints", executor, tabCompleter)

class SetPointsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("setpoints", executor, tabCompleter)

class GiveQuestPointsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("givequestpoints", executor, tabCompleter)

class SetQuestPointsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("setquestpoints", executor, tabCompleter)

class SetPrestigeCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("setprestige", executor, tabCompleter)

class CompleteAchievementCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("completeachievement", executor, tabCompleter)

class CompleteAllAchievementsCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("completeallachievements", executor, tabCompleter)

class BackupCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("backup", executor, tabCompleter)

class WorldCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("world", executor, tabCompleter)

class ScoreboardCommandModule(executor: RootCommandExecutor, tabCompleter: RootTabCompleter) :
    BaseRootCommandModule("scoreboard", executor, tabCompleter)
