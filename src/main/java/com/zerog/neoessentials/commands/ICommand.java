package com.zerog.neoessentials.commands;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public interface ICommand {
    void execute(ServerPlayer player, String[] args);
    List<String> tabComplete(ServerPlayer player, String[] args);
    List<String> getAliases();
}
