package com.zerog.neoessentials.messaging;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Simple implementation of IMessageRecipient for player-based messaging
 */
public class SimpleMessageRecipient implements IMessageRecipient {
	private final ServerPlayer player;
	private IMessageRecipient replyRecipient;

	public SimpleMessageRecipient(ServerPlayer player) {
		this.player = player;
	}

	@Override
	public void sendMessage(Component message) {
		player.sendSystemMessage(message);
	}

	@Override
	public String getName() {
		return player.getName().getString();
	}

	@Override
	public ServerPlayer getPlayer() {
		return player;
	}

	@Override
	public IMessageRecipient getReplyRecipient() {
		return replyRecipient;
	}

	@Override
	public void setReplyRecipient(IMessageRecipient recipient) {
		this.replyRecipient = recipient;
	}

	@Override
	public boolean isAfk() {
		// TODO: Integrate with AFK system
		return false;
	}

	@Override
	public boolean isIgnoring(ServerPlayer sender) {
		// TODO: Integrate with ignore list
		return false;
	}

	@Override
	public boolean isOnline() {
		return !player.isRemoved();
	}
}
