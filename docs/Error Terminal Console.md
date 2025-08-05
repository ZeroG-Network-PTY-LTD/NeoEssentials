The Following errors occure on the server

[11:19:40] [User Authenticator #1/INFO] [minecraft/ServerLoginPacketListenerImpl]: UUID of player MrWhiteFlamesYT is af533596-510c-4d44-9864-a0427c0df2ab
[11:19:43] [Server thread/INFO] [minecraft/PlayerList]: MrWhiteFlamesYT[/[::1]:39842] logged in with entity id 26 at (40.866870913975006, 63.0, 82.34911961360818)
[11:19:44] [Server thread/INFO] [minecraft/MinecraftServer]: MrWhiteFlamesYT joined the game
[11:19:44] [Server thread/INFO] [co.ze.ne.ev.NeoEssentialsEventHandler/]: Player MrWhiteFlamesYT joined the server
[11:19:44] [ForkJoinPool.commonPool-worker-3/INFO] [NeoEssentials-Notifications/]: [2025-08-05 11:19:44] [INFO] Player Joined: MrWhiteFlamesYT joined the server (Player: MrWhiteFlamesYT)
[11:19:44] [Server thread/ERROR] [co.ze.ne.fe.TablistScoreboardManager/]: Failed to update tablist for player: MrWhiteFlamesYT
java.lang.NullPointerException: Cannot invoke "net.minecraft.server.MinecraftServer.getPlayerCount()" because "this.server" is null
        at TRANSFORMER/neoessentials@1.0.2/com.zerog.neoessentials.features.TablistScoreboardManager.updatePlayerTablist(TablistScoreboardManager.java:129) ~[neoessentials-1.0.2.197.jar%23120!/:1.0.2] {re:classloading}
        at TRANSFORMER/neoessentials@1.0.2/com.zerog.neoessentials.features.TablistScoreboardManager.onPlayerJoin(TablistScoreboardManager.java:61) ~[neoessentials-1.0.2.197.jar%23120!/:1.0.2] {re:classloading}
        at MC-BOOTSTRAP/net.neoforged.bus/net.neoforged.bus.EventBus.post(EventBus.java:350) ~[bus-8.0.2.jar%2362!/:?] {}
        at MC-BOOTSTRAP/net.neoforged.bus/net.neoforged.bus.EventBus.post(EventBus.java:315) ~[bus-8.0.2.jar%2362!/:?] {}
        at TRANSFORMER/neoforge@21.1.179/net.neoforged.neoforge.event.EventHooks.firePlayerLoggedIn(EventHooks.java:884) ~[neoforge-21.1.179-universal.jar%23119!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.players.PlayerList.placeNewPlayer(PlayerList.java:270) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.network.ServerConfigurationPacketListenerImpl.handleConfigurationFinished(ServerConfigurationPacketListenerImpl.java:188) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket.handle(ServerboundFinishConfigurationPacket.java:22) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket.handle(ServerboundFinishConfigurationPacket.java:8) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.network.protocol.PacketUtils.lambda$ensureRunningOnSameThread$0(PacketUtils.java:27) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.TickTask.run(TickTask.java:18) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.util.thread.BlockableEventLoop.doRunTask(BlockableEventLoop.java:148) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.util.thread.ReentrantBlockableEventLoop.doRunTask(ReentrantBlockableEventLoop.java:23) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.doRunTask(MinecraftServer.java:872) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.doRunTask(MinecraftServer.java:170) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.util.thread.BlockableEventLoop.pollTask(BlockableEventLoop.java:122) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.pollTaskInternal(MinecraftServer.java:855) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.pollTask(MinecraftServer.java:849) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.util.thread.BlockableEventLoop.managedBlock(BlockableEventLoop.java:132) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.managedBlock(MinecraftServer.java:821) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.waitUntilNextTick(MinecraftServer.java:826) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:712) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.lambda$spin$2(MinecraftServer.java:267) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at java.base/java.lang.Thread.run(Thread.java:1583) [?:?] {}
[11:19:44] [Server thread/ERROR] [co.ze.ne.fe.TablistScoreboardManager/]: Failed to update scoreboard for player: MrWhiteFlamesYT
java.lang.NullPointerException: Cannot invoke "net.minecraft.server.MinecraftServer.getScoreboard()" because "this.server" is null
        at TRANSFORMER/neoessentials@1.0.2/com.zerog.neoessentials.features.TablistScoreboardManager.updatePlayerScoreboard(TablistScoreboardManager.java:155) ~[neoessentials-1.0.2.197.jar%23120!/:1.0.2] {re:classloading}
        at TRANSFORMER/neoessentials@1.0.2/com.zerog.neoessentials.features.TablistScoreboardManager.onPlayerJoin(TablistScoreboardManager.java:62) ~[neoessentials-1.0.2.197.jar%23120!/:1.0.2] {re:classloading}
        at MC-BOOTSTRAP/net.neoforged.bus/net.neoforged.bus.EventBus.post(EventBus.java:350) ~[bus-8.0.2.jar%2362!/:?] {}
        at MC-BOOTSTRAP/net.neoforged.bus/net.neoforged.bus.EventBus.post(EventBus.java:315) ~[bus-8.0.2.jar%2362!/:?] {}
        at TRANSFORMER/neoforge@21.1.179/net.neoforged.neoforge.event.EventHooks.firePlayerLoggedIn(EventHooks.java:884) ~[neoforge-21.1.179-universal.jar%23119!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.players.PlayerList.placeNewPlayer(PlayerList.java:270) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.network.ServerConfigurationPacketListenerImpl.handleConfigurationFinished(ServerConfigurationPacketListenerImpl.java:188) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket.handle(ServerboundFinishConfigurationPacket.java:22) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket.handle(ServerboundFinishConfigurationPacket.java:8) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.network.protocol.PacketUtils.lambda$ensureRunningOnSameThread$0(PacketUtils.java:27) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.TickTask.run(TickTask.java:18) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.util.thread.BlockableEventLoop.doRunTask(BlockableEventLoop.java:148) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.util.thread.ReentrantBlockableEventLoop.doRunTask(ReentrantBlockableEventLoop.java:23) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.doRunTask(MinecraftServer.java:872) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.doRunTask(MinecraftServer.java:170) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.util.thread.BlockableEventLoop.pollTask(BlockableEventLoop.java:122) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.pollTaskInternal(MinecraftServer.java:855) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.pollTask(MinecraftServer.java:849) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.util.thread.BlockableEventLoop.managedBlock(BlockableEventLoop.java:132) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.managedBlock(MinecraftServer.java:821) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.waitUntilNextTick(MinecraftServer.java:826) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:712) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.MinecraftServer.lambda$spin$2(MinecraftServer.java:267) ~[server-1.21.1-20240808.144430-srg.jar%23118!/:?] {re:classloading,pl:accesstransformer:B}
        at java.base/java.lang.Thread.run(Thread.java:1583) [?:?] {}
[11:19:45] [Server thread/WARN] [minecraft/MinecraftServer]: Can't keep up! Is the server overloaded? Running 2847ms or 56 ticks behind
[11:20:24] [Server thread/INFO] [co.ze.ne.ma.EconomyManager/]: Deposited $500.00 to player af533596-510c-4d44-9864-a0427c0df2ab (reason: Admin give by MrWhiteFlamesYT). New balance: $500.00
[11:22:18] [Server thread/INFO] [minecraft/ServerGamePacketListenerImpl]: MrWhiteFlamesYT lost connection: Disconnected
[11:22:18] [Server thread/INFO] [minecraft/MinecraftServer]: MrWhiteFlamesYT left the game
[11:22:18] [Server thread/INFO] [co.ze.ne.ev.NeoEssentialsEventHandler/]: Player MrWhiteFlamesYT left the server
[11:22:18] [ForkJoinPool.commonPool-worker-7/INFO] [NeoEssentials-Notifications/]: [2025-08-05 11:22:18] [INFO] Player Left: MrWhiteFlamesYT left the server (Player: MrWhiteFlamesYT)