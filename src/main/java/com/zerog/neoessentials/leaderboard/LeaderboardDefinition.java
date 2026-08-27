package com.zerog.neoessentials.leaderboard;

/**
 * @param id                     board id, used in commands/placeholders (e.g. "money", "kills")
 * @param displayName            human-readable name shown in headers
 * @param exemptPermissionSuffix full exemption permission node (e.g. "neoessentials.economy.baltop.exempt")
 * @param higherIsBetter         true for "top" boards (kills, money); false would rank ascending
 */
public record LeaderboardDefinition(String id, String displayName, String exemptPermissionSuffix, boolean higherIsBetter) {}
