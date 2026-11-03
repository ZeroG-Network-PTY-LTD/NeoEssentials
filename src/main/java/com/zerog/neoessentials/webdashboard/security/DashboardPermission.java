package com.zerog.neoessentials.webdashboard.security;

/**
 * Builds and checks the {@code "tab:<tabId>:<read|write>"} strings stored in a CUSTOM-role
 * {@link User}'s existing {@code permissions} set (see {@link User#hasPermission(String)}'s
 * CUSTOM branch) — centralizes the syntax so {@link AuthenticationFilter}, the
 * {@code /api/auth/my-permissions} handler, and anything else that needs to grant/check one of
 * these strings doesn't each hand-roll the format.
 *
 * <p>{@code write} access implies {@code read} on the same tab (matches the existing GET-vs-
 * mutating-method convention {@link AuthenticationFilter}'s VIEWER branch already uses) — a
 * permission string ending in {@code :write} satisfies a check for either access level on that
 * tab; one ending in {@code :read} only satisfies a read check.
 */
public final class DashboardPermission {
    private DashboardPermission() {}

    public static String build(String tabId, boolean write) {
        return "tab:" + tabId + ":" + (write ? "write" : "read");
    }

    /**
     * @return true if {@code granted} (one of a user's stored permission strings) satisfies a
     *         request for {@code tabId} at the given access level — write access on a tab
     *         satisfies both a read and a write check for that same tab; read access only
     *         satisfies a read check.
     */
    public static boolean satisfies(String granted, String tabId, boolean requireWrite) {
        if (granted == null) return false;
        String[] parts = granted.split(":", 3);
        if (parts.length != 3 || !parts[0].equals("tab") || !parts[1].equals(tabId)) return false;
        return parts[2].equals("write") || (!requireWrite && parts[2].equals("read"));
    }
}
