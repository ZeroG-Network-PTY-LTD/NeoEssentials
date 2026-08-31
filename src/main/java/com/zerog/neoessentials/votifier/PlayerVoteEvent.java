package com.zerog.neoessentials.votifier;

import net.neoforged.bus.api.Event;

/**
 * Fired on the NeoForge game event bus once {@link VotifierServer} has validated an incoming
 * vote (RSA-decrypted for V1, HMAC-verified for V2). Listeners (reward granting, vote party,
 * stats) subscribe normally — keeps the raw protocol handling decoupled from what happens
 * after a vote, same shape as {@code ShopTransactionEvent} elsewhere in this mod.
 */
public class PlayerVoteEvent extends Event {
    private final Vote vote;

    public PlayerVoteEvent(Vote vote) {
        this.vote = vote;
    }

    public Vote getVote() {
        return vote;
    }
}
