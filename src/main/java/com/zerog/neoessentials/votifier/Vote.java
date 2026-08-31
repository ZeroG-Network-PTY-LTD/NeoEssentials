package com.zerog.neoessentials.votifier;

/** One validated vote received by the {@link VotifierServer}. */
public record Vote(String serviceName, String username, String address, String timestamp) {}
