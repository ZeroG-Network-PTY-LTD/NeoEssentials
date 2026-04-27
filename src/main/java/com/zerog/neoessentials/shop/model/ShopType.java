package com.zerog.neoessentials.shop.model;

/** Classifies how a shop was created and what rules govern it. */
public enum ShopType {
    /** Admin-created sign shop — unlimited stock, money is voided. */
    SIGN_ADMIN,
    /** Player-owned sign shop — deducts from chest inventory, money flows to owner. */
    SIGN_PLAYER,
    /** Entity (NPC) shop — managed via /npcshop command, opens a virtual GUI. */
    NPC
}

