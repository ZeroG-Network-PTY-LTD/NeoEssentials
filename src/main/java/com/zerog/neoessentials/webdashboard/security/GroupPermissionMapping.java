package com.zerog.neoessentials.webdashboard.security;

import java.util.List;

/**
 * One entry of {@code webDashboard.groupPermissions[]} — maps an in-game permission group to a
 * set of dashboard tab permissions, parsed by {@link com.zerog.neoessentials.config.ConfigManager#getGroupPermissionMappings()}.
 * Evaluated in array order by {@link PermissionRoleSyncTask}: first mapping whose {@link #group}
 * matches a player's primary group wins, same single-primary-group assumption the existing
 * {@code webDashboard.roleSync.adminGroup} check already makes.
 */
public final class GroupPermissionMapping {
    private final String group;
    private final List<TabGrant> tabs;

    public GroupPermissionMapping(String group, List<TabGrant> tabs) {
        this.group = group;
        this.tabs = tabs;
    }

    public String getGroup() { return group; }
    public List<TabGrant> getTabs() { return tabs; }

    /** One {@code {tab, access}} entry — {@code access} is {@code "read"} or {@code "write"}. */
    public static final class TabGrant {
        private final String tab;
        private final boolean write;

        public TabGrant(String tab, boolean write) {
            this.tab = tab;
            this.write = write;
        }

        public String getTab() { return tab; }
        public boolean isWrite() { return write; }

        /** @return this grant as a {@code "tab:<id>:<read|write>"} permission string, ready to
         *          hand to {@link User#addPermission(String)}. */
        public String toPermissionString() {
            return DashboardPermission.build(tab, write);
        }
    }
}
