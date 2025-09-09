# Complete Command Registration Analysis

## Available Commands in Codebase

### Essential Commands (48 commands)
1. AFKCommand ✅ REGISTERED
2. AnvilCommand ❌ NOT REGISTERED  
3. BackCommand ✅ REGISTERED
4. BalanceCommand ✅ REGISTERED (via EconomyCommands)
5. BanCommand ❌ NOT REGISTERED
6. ConfigCommand ✅ REGISTERED
7. CreateShopCommand ❌ NOT REGISTERED
8. EconomyCommand ✅ REGISTERED
9. EnderChestCommand ✅ REGISTERED
10. FeedCommand ✅ REGISTERED
11. FlyCommand ✅ REGISTERED
12. GameModeCommand ✅ REGISTERED
13. GiveCommand ✅ REGISTERED
14. GodCommand ✅ REGISTERED
15. HealCommand ✅ REGISTERED
16. HelpCommand ✅ REGISTERED
17. HomeCommand ❌ NOT REGISTERED (uses HomeCommands instead)
18. InfoCommand ✅ REGISTERED
19. InvSeeCommand ✅ REGISTERED
20. ItemCommand ✅ REGISTERED
21. KickCommand ❌ NOT REGISTERED
22. KitCommand ✅ REGISTERED
23. ListCommand ✅ REGISTERED
24. MailCommand ✅ REGISTERED
25. MessageCommand ✅ REGISTERED
26. MotdCommand ✅ REGISTERED
27. MuteCommand ❌ NOT REGISTERED
28. NickCommand ✅ REGISTERED
29. PayCommand ❌ NOT REGISTERED
30. RepairCommand ✅ REGISTERED
31. ReplyCommand ✅ REGISTERED
32. RulesCommand ✅ REGISTERED
33. SeenCommand ✅ REGISTERED
34. SmithingCommand ❌ NOT REGISTERED
35. SocialSpyCommand ✅ REGISTERED
36. SpawnCommand ❌ NOT REGISTERED (uses SpawnCommands instead)
37. SpawnerCommand ✅ REGISTERED
38. SpeedCommand ✅ REGISTERED
39. StonecutterCommand ❌ NOT REGISTERED
40. TeleportCommand ✅ REGISTERED
41. TempBanCommand ✅ REGISTERED
42. TimeCommand ✅ REGISTERED
43. TpaCommand ✅ REGISTERED
44. VanishCommand ✅ REGISTERED
45. WarpCommand ❌ NOT REGISTERED (uses WarpCommands instead)
46. WeatherCommand ✅ REGISTERED
47. WhoisCommand ✅ REGISTERED
48. WorkbenchCommand ❌ NOT REGISTERED

### Admin Commands (6 commands)
1. CleanupCommand ❌ NOT REGISTERED
2. ErrorCommand ❌ NOT REGISTERED
3. KitAdminCommand ✅ REGISTERED
4. NeoEssentialsCommand ✅ REGISTERED
5. PerformanceCommand ❌ NOT REGISTERED
6. StatusCommand ✅ REGISTERED
7. TabListDebugCommand ✅ REGISTERED

## Missing Registrations (13 Essential + 3 Admin = 16 commands)

### Essential Commands Missing:
- AnvilCommand
- BanCommand  
- CreateShopCommand
- KickCommand
- MuteCommand
- PayCommand
- SmithingCommand
- StonecutterCommand
- WorkbenchCommand

### Admin Commands Missing:
- CleanupCommand
- ErrorCommand
- PerformanceCommand

## Commands Registered via Group Classes:
- HomeCommand → HomeCommands.register()
- WarpCommand → WarpCommands.register()  
- SpawnCommand → SpawnCommands.register()
- BalanceCommand → EconomyCommands.register()

## Problem Summary:
Your CommandRegistry.java is missing 16 individual command registrations that exist in your codebase!
