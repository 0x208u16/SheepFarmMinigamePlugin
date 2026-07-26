package dev.x208.sheepmerge

object SheepLeaderboardRefreshState {
    private val lock = Any()
    private var latestRequestVersion = 0L

    @JvmStatic
    fun nextRequestVersion(): Long = synchronized(lock) {
        ++latestRequestVersion
    }

    @JvmStatic
    fun isLatestRequest(requestVersion: Long): Boolean = synchronized(lock) {
        requestVersion == latestRequestVersion
    }
}