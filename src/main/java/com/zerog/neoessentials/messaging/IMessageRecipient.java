package com.zerog.neoessentials.messaging;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Interface for any entity that can receive messages (player, console, etc.)
 */
public interface IMessageRecipient {
	/**
	 * Send a message to the recipient
	 */
	void sendMessage(Component message);

	/**
	 * Get the recipient's name (for formatting)
	 */
	String getName();

	/**
	 * Get the underlying player, if applicable
	 */
	ServerPlayer getPlayer();

	/**
	 * Get the reply recipient (for /reply)
	 */
	IMessageRecipient getReplyRecipient();

	/**
	 * Set the reply recipient
	 */
	void setReplyRecipient(IMessageRecipient recipient);

	/**
	 * Is the recipient AFK?
	 */
	boolean isAfk();

	/**
	 * Is the sender ignored by this recipient?
	 */
	boolean isIgnoring(ServerPlayer sender);

	/**
	 * Is the recipient online?
	 */
	boolean isOnline();
}
