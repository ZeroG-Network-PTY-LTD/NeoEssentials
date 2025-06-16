Essentials System (Economy, Chat, Player Utils, Admin Utils)

Commands
Module	Command	Aliases	Description	Syntax
Essentials	editsign	sign, esign, eeditsign	Edits a sign in the world.	/<command> <set/clear/copy/paste> [line number] [text]
Essentials	repair	fix, efix, erepair	Repairs the durability of one or all items.	/<command> [hand|all]
Essentials	msgtoggle	emsgtoggle	Blocks receiving all private messages.	/<command> [player] [on|off]
Essentials	setjail	esetjail, createjail, ecreatejail	Creates a jail where you specified named [jailname].	/<command> <jailname>
Essentials	settpr	esettpr, settprandom, esettprandom	Set the random teleport location and parameters.	/<command> [center|minrange|maxrange] [value]
Essentials	help	ehelp	Views a list of available commands.	/<command> [search term] [page]
Essentials	clearinventoryconfirmtoggle	eclearinventoryconfirmtoggle, clearinventoryconfirmoff, eclearinventoryconfirmoff, clearconfirmoff, eclearconfirmoff, clearconfirmon, eclearconfirmon, clearconfirm, eclearconfirm	Toggles whether you are prompted to confirm inventory clears.	/<command>
Essentials	mute	emute, silence, esilence	Mute, unmute, or temporarily mute a player.	/<command> <player> [datediff] [reason]
Essentials	spawner	changems, echangems, espawner, mobspawner, emobspawner	Change the mob type of a spawner.	/<command> <mob> [delay]
Essentials	deljail	edeljail, remjail, eremjail, rmjail, ermjail	Removes a jail.	/<command> <jailname>
Essentials	powertool	epowertool, pt, ept	Assigns a command to the item in hand. Use 'c:' as the command to make a chat macro. Use 'a:' to append multiple commands. Use 'r:' to remove a single command. Use 'l:' to list all power tools Use 'd:' to remove all power tools	/<command> [l:|a:|r:|c:|d:][command] [arguments] ({player} can be replaced by name of a clicked player.)
Essentials	weather	rain, erain, sky, esky, storm, estorm, sun, esun, eweather	Sets the weather.	/<command> <storm/sun> [duration]
Essentials	gc	lag, elag, egc, mem, emem, memory, ememory, uptime, euptime, tps, etps, entities, eentities	Reports memory, uptime and tick info.	/<command>
Essentials	delhome	edelhome, remhome, eremhome, rmhome, ermhome	Removes a home.	/<command> [player:]<name>
Essentials	enderchest	echest, eechest, eenderchest, endersee, eendersee, ec, eec	Lets you see inside an enderchest.	/<command> [player]
Essentials	powertooltoggle	epowertooltoggle, ptt, eptt, pttoggle, epttoggle	Enables or disables all current powertools.	/<command>
Essentials	kickall	ekickall	Kicks all players off the server except the issuer.	/<command> [reason]
Essentials	speed	flyspeed, eflyspeed, fspeed, efspeed, espeed, walkspeed, ewalkspeed, wspeed, ewspeed	Change your walk or fly speed.	/<command> [type] <speed> [player]
Essentials	tpall	etpall	Teleport all online players to another player.	/<command> [player]
Essentials	suicide	esuicide	Causes you to perish.	/<command>
Essentials	whois	ewhois	Displays player information	/<command> <nickname|playername>
Essentials	essentials	eessentials, ess, eess, essversion	Reloads essentials and has some debug featues.	/<command> [debug|verbose|ver|version|cmd|commands|dump|reload|reset|cleanup|homes|uuidconvert|uuidtest|nya|nyan|moo]
Essentials	tptoggle	etptoggle	Blocks all forms of teleportation.	/<command> [player] [on|off]
Essentials	disposal	edisposal, trash, etrash	Opens a portable disposal menu.	/<command>
Essentials	top	etop	Teleport to the highest block at your current position.	/<command>
Essentials	gamemode	adventure, eadventure, adventuremode, eadventuremode, creative, eecreative, creativemode, ecreativemode, egamemode, gm, egm, gma, egma, gmc, egmc, gms, egms, gmt, egmt, survival, esurvival, survivalmode, esurvivalmode, gmsp, sp, egmsp, spec, spectator	Change player gamemode.	/<command> <survival|creative|adventure|spectator> [player]
Essentials	condense	econdense, compact, ecompact, blocks, eblocks, toblocks, etoblocks	Condenses items into a more compact blocks.	/<command> [itemname]
Essentials	tp	tele, etele, teleport, eteleport, etp, tp2p, etp2p	Teleport to a player.	/<command> <player> [otherplayer]
Essentials	tpoffline	otp, offlinetp, tpoff, tpoffline	Teleport to a player's last known logout location	/<command> <player>
Essentials	setworth	esetworth	Set the sell value of an item.	/<command> [itemname|id] <price>
Essentials	spawnmob	mob, emob, spawnentity, espawnentity, espawnmob	Spawns a mob.	/<command> <mob>[:data][,<mount>[:data]] [amount] [player]
Essentials	book	ebook	Allows reopening and editing of sealed books.	/<command> [title|author [name]]
Essentials	time	day, eday, night, enight, etime	Display/Change the world time. Defaults to current world.	/<command> [day|night|dawn|17:30|4pm|4000ticks] [worldname|all]
Essentials	tpaall	etpaall	Requests all players online to teleport to you.	/<command> <player>
Essentials	mail	email, eemail, memo, ememo	Manages inter-player, intra-server mail.	/<command> [read|clear|clear [number]|clear <player> [number]|send [to] [message]|sendtemp [to] [expire time] [message]|sendall [message]]
Essentials	hat	ehat, head, ehead	Get some cool new headgear.	/<command> [remove]
Essentials	worth	eprice, price, eworth	Calculates the worth of items in hand or as specified.	/<command> <<itemname>|<id>|hand|inventory|blocks> [-][amount]
Essentials	kick	ekick	Kicks a specified player with a reason.	/<command> <player> [reason]
Essentials	more	emore	Fills the item stack in hand to specified amount, or to maximum size if none is specified.	/<command> [amount]
Essentials	info	about, eabout, ifo, eifo, einfo, inform, einform, news, enews	Shows information set by the server owner.	/<command> [chapter] [page]
Essentials	tpahere	etpahere	Request that the specified player teleport to you.	/<command> <player>
Essentials	realname	erealname	Displays the username of a user based on nick.	/<command> <nickname>
Essentials	bigtree	ebigtree, largetree, elargetree	Spawn a big tree where you are looking.	/<command> <tree|redwood|jungle>
Essentials	rest	erest	Rests you or the given player.	/<command> [player]
Essentials	rules	erules	Views the server rules.	/<command> [chapter] [page]
Essentials	recipe	formula, eformula, method, emethod, erecipe, recipes, erecipes	Displays how to craft items.	/<command> <item> [number]
Essentials	sethome	esethome, createhome, ecreatehome	Set home to your current location.	/<command> [player:]<name>
Essentials	renamehome	erenamehome	Renames a home.	/<command> <[player:]name> <new name>
Essentials	pweather	playerweather, eplayerweather, epweather	Adjust a player's weather	/<command> [list|reset|storm|sun|clear] [player|*]
Essentials	setwarp	createwarp, ecreatewarp, esetwarp	Creates a new warp.	/<command> <warp>
Essentials	delwarp	edelwarp, remwarp, eremwarp, rmwarp, ermwarp	Deletes the specified warp.	/<command> <warp>
Essentials	warpinfo	ewarpinfo	Finds location information for a specified warp.	/<command> <warp>
Essentials	warp	ewarp, warps, ewarps	List all warps or warp to the specified location.	/<command> <pagenumber|warp> [player]
Essentials	bottom	ebottom	Teleport to the lowest block at your current position.	/<command>
Essentials	break	ebreak	Breaks the block you are looking at.	/<command>
Essentials	msg	w, m, t, pm, emsg, epm, tell, etell, whisper, ewhisper	Sends a private message to the specified player.	/<command> <to> <message>
Essentials	compass	ecompass, direction, edirection	Describes your current bearing.	/<command>
Essentials	world	eworld	Switch between worlds.	/<command> [world]
Essentials	me	action, eaction, describe, edescribe, eme	Describes an action in the context of the player.	/<command> <description>
Essentials	payconfirmtoggle	epayconfirmtoggle, payconfirmoff, epayconfirmoff, payconfirmon, epayconfirmon, payconfirm, epayconfirm	Toggles whether you are prompted to confirm payments.	/<command>
Essentials	ext	eext, extinguish, eextinguish	Extinguish players.	/<command> [player]
Essentials	broadcastworld	bcw, ebcw, bcastw, ebcastw, ebroadcastworld, shoutworld, eshoutworld	Broadcasts a message to a world.	/<command> <world> <msg>
Essentials	toggleshout	etoggleshout	Toggles whether you are talking in shout mode	/<command> [player] [on|off]
Essentials	togglejail	jail, ejail, tjail, etjail, etogglejail, unjail, eunjail	Jails/Unjails a player, TPs them to the jail specified.	/<command> <player> <jailname> [datediff]
Essentials	jails	ejails	List all jails.	/<command>
Essentials	list	elist, online, eonline, playerlist, eplayerlist, plist, eplist, who, ewho	List all online players.	/<command> [group]
Essentials	remove	eremove, butcher, ebutcher, killall, ekillall, mobkill, emobkill	Removes entities in your world.	/<command> <all|tamed|named|drops|arrows|boats| minecarts|xp|paintings|itemframes|endercrystals| monsters|animals|ambient|mobs|[mobType]> [radius|world]
Essentials	lightning	elightning, shock, eshock, smite, esmite, strike, estrike, thor, ethor	The power of Thor. Strikes at the cursor or a player.	/<command> [player] [power]
Essentials	ice	eice, efreeze	Cools a player off.	/<command> [player]
Essentials	ignore	eignore, unignore, eunignore, delignore, edelignore, remignore, eremignore, rmignore, ermignore	Ignore or unignore other players.	/<command> <player>
Essentials	item	i, eitem, ei	Spawn an item.	/<command> <item|numeric> [amount [itemmeta...]]
Essentials	near	enear, nearby, enearby	Lists the players near by or around a player.	/<command> [playername] [radius]
Essentials	firework	efirework	Allows you to modify a stack of fireworks.	/<command> <<meta param>|power [amount]|clear|fire [amount]>
Essentials	invsee	einvsee	See and/or edit the inventory of other players.	/<command> <player> [armor]
Essentials	balancetop	ebalancetop, baltop, ebaltop	Lists players by top balances.	/<command> [page]
Essentials	ban	eban	Bans a player.	/<command> <player> [reason]
Essentials	kill	ekill	Kills specified player.	/<command> <player>
Essentials	balance	bal, ebal, ebalance, money, emoney	States the current balance of a player.	/<command> [player]
Essentials	enchant	eenchant, enchantment, eenchantment	Enchants the item the user is holding.	/<command> <enchantmentname> [level]
Essentials	banip	ebanip	Bans an IP address.	/<command> <address> [reason]
Essentials	socialspy	esocialspy	Toggles if you can see msg/mail commands in chat.	/<command> [player] [on|off]
Essentials	workbench	craft, ecraft, wb, ewb, wbench, ewbench, eworkbench	Opens up a workbench.	/<command>
Essentials	anvil	eanvil	Opens up an Anvil.	/<command>
Essentials	cartographytable	ecartographytable, carttable, ecarttable	Opens up a cartography table.	/<command>
Essentials	grindstone	egrindstone	Opens up a grindstone.	/<command>
Essentials	loom	eloom	Opens up a loom.	/<command>
Essentials	smithingtable	esmithingtable, smithtable, esmithtable	Opens up a smithing table.	/<command>
Essentials	stonecutter	estonecutter	Opens up a stonecutter.	/<command>
Essentials	motd	emotd	Views the Message Of The Day.	/<command> [chapter] [page]
Essentials	ptime	playertime, eplayertime, eptime	Adjust player's client time. Add @ prefix to fix.	/<command> [list|reset|day|night|dawn|17:30|4pm|4000ticks] [player|*]
Essentials	give	egive	Give a player an item.	/<command> <player> <item|numeric> [amount [itemmeta...]]
Essentials	tree	etree	Spawn a tree where you are looking.	/<command> <tree|birch|redwood|redmushroom| brownmushroom|jungle|junglebush|swamp>
Essentials	tpacancel	etpacancel	Cancel all outstanding teleport requests. Specify [player] to cancel requests with them.	/<command> [player]
Essentials	sudo	esudo	Make another user perform a command.	/<command> <player> <command [args]>
Essentials	back	eback, return, ereturn	Teleports you to your location prior to tp/spawn/warp.	/<command>
Essentials	jump	j, ej, ejump, jumpto, ejumpto	Jumps to the nearest block in the line of sight.	/<command>
Essentials	fly	efly	Take off, and soar!	/<command> [player] [on|off]
Essentials	potion	epotion, elixer, eelixer	Adds custom potion effects to a potion.	/<command> <clear|apply|effect:<effect> power:<power> duration:<duration>>
Essentials	seen	eseen	Shows the last logout time of a player.	/<command> <playername>
Essentials	home	ehome, homes, ehomes	Teleport to your home.	/<command> [player:]<name>
Essentials	vanish	v, ev, evanish	Hide yourself from other players.	/<command> [player] [on|off]
Essentials	tpdeny	etpdeny, tpno, etpno	Rejects teleport requests.	/<command> [player|*]
Essentials	tpohere	etpohere	Teleport here override for tptoggle.	/<command> <player>
Essentials	getpos	eposition	Get your current coordinates or those of a player.	/<command> [player]
Essentials	god	tgm	Enables your godly powers.	/<command> [player] [on|off]
Essentials	ping	echo, eecho, eping, pong, epong	Pong!	/<command>
Essentials	helpop	eamsg	Message online admins.	/<command> <message>
Essentials	skull	head, playerskull, eskull, ehead, eplayerskull	Get the player head item with your skin or a skin of a specified player, or change skin of the head you're holding.	/<command> [owner] [player]
Essentials	broadcast	ebcast, bc, bcast, ebc	Broadcasts a message to the entire server.	/<command> <msg>
Essentials	clearinventory	ci, eci, clean, eclean, clear, eclear, clearinvent, eclearinvent, eclearinventory	Clear all items in your inventory.	/<command> [player|*] [item[:<data>]|*|**] [amount]
Essentials	unlimited	eul	Allows the unlimited placing of items.	/<command> <list|item|clear> [player]
Essentials	antioch	eantioch,grenade,egrenade,tnt,etnt	A little surprise for operators.	/<command> [message]
Essentials	fireball	fireskull	Throw a fireball or other assorted projectiles.	/<command> [fireball|small|large|arrow|skull|egg|snowball|expbottle|dragon|splashpotion|lingeringpotion|trident] [speed]
Essentials	tpa	call,ecall,etpa,tpask,etpask	Request to teleport to the specified player.	/<command> <player>
Essentials	createkit	ck	Create a kit in game!	/<command> <kitname> <delay>
Essentials	kitreset	ekitreset, kitr, ekitr, resetkit, eresetkit	Resets the cooldown on the specified kit.	/<command> <kit> [player]
Essentials	depth	eheight	States current depth	/depth
Essentials	paytoggle	payon	Toggles whether you are accepting payments.	/<command> [player]
Essentials	itemdb	eitemdb	Searches for an item.	/<command> <item>
Essentials	itemlore	lore, elore, ilore, eilore, eitemlore	Edit the lore of an item.	/<command> <add/set/clear> [text/line] [text]
Essentials	itemname	iname, einame, eitemname, itemrename, irename, eitemrename, eirename	Renames the item your currently holding. Leave name empty to reset. You can add in color codes when you have permission to.	/<command> [name]
Essentials	afk	eafk, away, eaway	Marks you as away-from-keyboard.	/<command> [player/message...]
Essentials	backup	ebackup	Runs the backup if configured.	/<command>
Essentials	burn	eburn	Set a player on fire.	/<command> <player> <seconds>
Essentials	customtext		Allows you to create custom text commands.	/<alias> - Define in bukkit.yml
Essentials	eco	eeco,economy,eeconomy	Manages the server economy.	/<command> <give|take|set|reset> <player> <amount>
Essentials	exp	eexp, xp	Give, set or look at a players exp.	/<command> [show|set|give] [playername [amount]]
Essentials	feed	eat,eeat,efeed	Satisfy the hunger.	/<command> [player]
Essentials	heal	eheal	Heals you or the given player.	/<command> [player]
Essentials	kit	ekit,kits,ekits	Obtains the specified kit or views all available kits.	/<command> [kit] [player]
Essentials	kittycannon	ekittycannon	Throw an exploding kitten at your opponent.	/<command>
Essentials	beezooka	ebeezooka, beecannon, ebeecannon	Throw an exploding bee at your opponent.	/<command>
Essentials	nick	enick,nickname,enickname	Change your nickname or that of another player.	/<command> [player] <nickname|off>
Essentials	nuke	enuke	May death rain upon them.	/<command> [player]
Essentials	pay	epay	Pays another player from your balance.	/<command> <player> <amount>
Essentials	r	er,reply,ereply	Quickly reply to the last player to message you.	/<command> <message>
Essentials	rtoggle	ertoggle, replytoggle, ereplytoggle	Change whether the recipient of the reply is last recipient or last sender.	/<command> [player] [on|off]
Essentials	sell	esell	Sells the item currently in your hand.	/<command> <<itemname>|<id>|hand|inventory|blocks> [amount]
Essentials	showkit	kitpreview,preview,kitshow	Show contents of a kit.	/<command> <kitname>
Essentials	tempban	etempban	Temporary ban a user.	/<command> <playername> <datediff> [reason]
Essentials	tempbanip	etempbanip	Temporarily ban an IP Address.	/<command> <playername> <datediff> [reason]
Essentials	thunder	ethunder	Enable/disable thunder.	/<command> <true/false> [duration]
Essentials	tpaccept	etpaccept,tpyes,etpyes	Accepts teleport requests.	/<command> [player|*]
Essentials	tphere	s,etphere	Teleport a player to you.	/<command> <player>
Essentials	tpohere	etpohere	Teleport here override for tptoggle.	/<command> <player>
Essentials	tppos	etppos	Teleport to coordinates.	/<command> <x> <y> <z> [yaw] [pitch] [world]
Essentials	tpr	etpr, tprandom, etprandom	Teleport randomly.	/<command>
Essentials	unban	pardon,eunban,epardon	Unbans the specified player.	/<command> <player>
Essentials	unbanip	eunbanip,pardonip,epardonip	Unbans the specified IP address.	/<command> <address>
Essentials	tpauto	etpauto	Automatically accept teleportation requests.	/<command> [player]
Essentials	delkit		Delete a given kit	/<command> <kitname>
Essentials	playtime		Shows a player's time played in game	/<command> [player]
EssentialsDiscord	discordbroadcast	dbroadcast, dbc, dbcast, ediscordbroadcast, edbroadcast, edbc, edbcast	Broadcasts a message to the specified Discord channel.	/<command> <channel> <msg>
EssentialsDiscord	discord	ediscord	Sends the discord invite link to the player.	/<command>
EssentialsDiscordLink	link	elink, discordlink, ediscordlink	Generates a code to link your Minecraft account to Discord.	/<command>
EssentialsDiscordLink	unlink	eunlink, discordunlink, ediscordunlink	Unlinks your Minecraft account from any associated Discord account.	/<command>
EssentialsSpawn	setspawn	esetspawn	Set the spawnpoint to your current position.	/<command> <group>
EssentialsSpawn	spawn	espawn	Teleport to the spawnpoint.	/<command> [player]
EssentialsXMPP	setxmpp		Set your xmpp address.	/<command> <address>
EssentialsXMPP	xmpp		Send a message to a player.	/<command> <player> <message>
EssentialsXMPP	xmppspy		Toggle xmpp spy for all messages.	/<command> <player>

permissions
Module	Command	Permission	Description
Essentials	afk	essentials.afk	Allow access to the /afk command.
Essentials	afk	essentials.afk.message	Allow access to settings an afk message.
Essentials	afk	essentials.afk.others	Allows you to AFK other players.
Essentials	afk	essentials.afk.auto	Players with this permission will be set to afk after a period of inaction as defined in the config file.
Essentials	afk	essentials.afk.kickexempt	Exempts the user from being auto kicked for AFK
Essentials	antioch	essentials.antioch	Allow access to the /antioch command.
Essentials	back	essentials.back	Allow access to the /back command.
Essentials	back	essentials.back.ondeath	Give this permission to allow players to use /back to go to their death spot
Essentials	back	essentials.back.onteleport	Players with this permission will have back location stored during any teleportation
Essentials	backup	essentials.backup	Allow access to the /backup command.
Essentials	balance	essentials.balance	Allow access to the /balance command.
Essentials	balance	essentials.balance.others	Allows you to see the balance of other players
Essentials	balancetop	essentials.balancetop	Allow access to the /balancetop command.
Essentials	balancetop	essentials.balancetop.force	Allow a forced refresh of balancetop with "/balancetop force"
Essentials	ban	essentials.ban	Allow access to the /ban command.
Essentials	ban	essentials.ban.exempt	Prevents a specified group or player from being banned
Essentials	ban	essentials.ban.offline	Allows banning of players who are offline. This may allow you to ban exempt players.
Essentials	banip	essentials.banip	Allow access to the /banip command.
Essentials	unban	essentials.unban	Allow access to the /unban command.
Essentials	unbanip	essentials.unbanip	Allow access to the /unbanip command.
Essentials	bigtree	essentials.bigtree	Allow access to the /bigtree command.
Essentials	book	essentials.book	Allow access to the /book command.
Essentials	book	essentials.book.author	Allows the user to change the author of a book.
Essentials	book	essentials.book.others	Allow editing a book belonging to another player.
Essentials	book	essentials.book.title	Allows the user to change the title of a written book
Essentials	bottom	essentials.bottom	Allows access to the /bottom command.
Essentials	break	essentials.break	Allows access to the /break command.
Essentials	break	essentials.break.bedrock	Allows the breaking of bedrock.
Essentials	broadcast	essentials.broadcast	Allow access to the /broadcast command.
Essentials	broadcastworld	essentials.broadcastworld	Allow access to the /broadcastworld command.
Essentials	burn	essentials.burn	Allow access to the /burn command.
Essentials		essentials.chat.ignoreexempt	Someone with this permission will not be ignored, even if they are on another persons ignore list.
Essentials	me	essentials.chat.spy	A permission designed for admins, to intercept all local messages (ignoring the chat-radius).
Essentials		essentials.chat.spy.exempt	Allow to be exempt from chat spy
Essentials	clearinventory	essentials.clearinventory	Allow access to the /clearinventory command.
Essentials	clearinventory	essentials.clearinventory.all	Allows a player to clear all players inventories.
Essentials	clearinventory	essentials.clearinventory.multiple	Allow clearing inventory of multiple people at once
Essentials	clearinventory	essentials.clearinventory.others	Allows you to clear other player's inventory
Essentials	clearinventoryconfirmtoggle	essentials.clearinventoryconfirmtoggle	Allow access to the /clearinventoryconfirmtoggle command
Essentials		essentials.commandcooldowns.bypass	Allow to bypass all command cooldowns
Essentials		essentials.commandcooldowns.bypass.<commandname>	Allow to bypass a specific command cooldown
Essentials	compass	essentials.compass	Allow access to the /compass command.
Essentials	condense	essentials.condense	Allow access to the /condense command
Essentials	createkit	essentials.createkit	Allow access to the /createkit command
Essentials	delkit	essentials.delkit	Allow access to the /delkit command
Essentials	kitreset	essentials.kitreset	Allow access to the /kitreset command
Essentials	kitreset	essentials.kitreset.others	Allow to reset other another player's kit cooldown using /kitreset.
Essentials	customtext	essentials.customtext	Allow access to the /customtext command and all aliases.
Essentials	delwarp	essentials.delwarp	Allow access to the /delwarp command.
Essentials	depth	essentials.depth	Allow access to the /depth command.
Essentials	disposal	essentials.disposal	Allow access to the /disposal command
Essentials	eco	essentials.eco	Allow access to the /eco command.
Essentials	eco	essentials.eco.loan	Allows the player to have a negative balance.
Essentials	enchant	essentials.enchant	Allows access to the /enchant command.
Essentials	give	essentials.enchantments.allowunsafe	If enabled in the config, this permission allows unsafe enchantments.
Essentials	enderchest	essentials.enderchest	Allow access to the /enderchest command.
Essentials		essentials.enderchest.modify	Allows you to modify the contents of another players enderchest.
Essentials	enderchest	essentials.enderchest.others	Allows you to see the contents of another players enderchest.
Essentials	essentials	essentials.essentials	Allow access to the /essentials command.
Essentials	essentials	essentials.updatecheck	Notify players with this permission on join when there is an update available.
Essentials	exp	essentials.exp	Allow access to the /exp command.
Essentials	exp	essentials.exp.give	Allows players to give themselves exp.
Essentials	exp	essentials.exp.give.others	Allows players to give others exp (if they also have essentials.exp.give).
Essentials	exp	essentials.exp.others	Allows players to see another players exp.
Essentials	exp	essentials.exp.set	Allows players to set their own exp.
Essentials	exp	essentials.exp.set.others	Allows players to change others exp (if they also have essentials.exp.set).
Essentials	ext	essentials.ext	Allow access to the /ext command.
Essentials	ext	essentials.ext.others	Allow to specify other player in /ext.
Essentials	feed	essentials.feed	Allows access to the /feed command.
Essentials	feed	essentials.feed.cooldown.bypass	Bypass the feed cooldown. Since build 1452, essentials.commandcooldowns.bypass.feed can be used instead.
Essentials	feed	essentials.feed.others	Allows a player to feed another player.
Essentials	fireball	essentials.fireball	Allow access to the /fireball command. Note: you also need specific permissions for each projectile.
Essentials	fireball	essentials.fireball.fireball	Allow firing regular fireballs.
Essentials	fireball	essentials.fireball.small	Allow firing small fireballs.
Essentials	fireball	essentials.fireball.large	Allow firing large fireballs.
Essentials	fireball	essentials.fireball.arrow	Allow firing arrows.
Essentials	fireball	essentials.fireball.skull	Allow firing wither skulls.
Essentials	fireball	essentials.fireball.egg	Allow firing eggs.
Essentials	fireball	essentials.fireball.snowball	Allow firing frozen fireballs.
Essentials	fireball	essentials.fireball.expbottle	Allow firing experience bottles.
Essentials	fireball	essentials.fireball.dragon	Allow firing Ender Dragon fireballs.
Essentials	fireball	essentials.fireball.splashpotion	Allow firing splash potions.
Essentials	fireball	essentials.fireball.lingeringpotion	Allow firing lingering potions.
Essentials	fireball	essentials.fireball.trident	Allow firing tridents.
Essentials	firework	essentials.firework	Allow access to the /firework command.
Essentials	firework	essentials.firework.fire	Allows a user to 'spawn' a copy of the firework held in their hands.
Essentials	firework	essentials.firework.multiple	Allows to spawn multiple firework effects at once
Essentials	fly	essentials.fly	Allow access to the /fly command.
Essentials		essentials.fly.safelogin	Players with this permission will automatically switch to fly mode if they login whilst floating in the air.
Essentials	gamemode	essentials.gamemode	Allow access to the /gamemode command.
Essentials	gamemode	essentials.gamemode.all	Allow access to all the gamemodes in the /gamemode command
Essentials	gamemode	essentials.gamemode.others	Allows you to change the gamemode of other players.
Essentials	gc	essentials.gc	Allow access to the /gc command.
Essentials	getpos	essentials.getpos	Allow access to the /getpos command.
Essentials	getpos	essentials.getpos.others	Allow user to get the position of another player.
Essentials	give	essentials.give	Allow access to the /give command.
Essentials	give	essentials.give.item-all	If permission-based-item-spawn: Spawn all items
Essentials	give	essentials.give.item-[itemname]	If permission-based-item-spawn: Spawn [itemname]
Essentials	give	essentials.give.item-[itemid]	If permission-based-item-spawn: Spawn [itemname]
Essentials	god	essentials.god	Allow access to the /god command.
Essentials	god	essentials.god.pvp	Allows you to attack other players while in god mode.
Essentials	hat	essentials.hat	Allow access to the /hat command.
Essentials	heal	essentials.heal	Allow access to the /heal command.
Essentials	heal	essentials.heal.cooldown.bypass	Bypass the heal cooldown. Since build 1452, essentials.commandcooldowns.bypass.heal can be used instead.
Essentials	heal	essentials.heal.others	Allows healing another player
Essentials	help	essentials.help	Allow access to the /help command.
Essentials	helpop	essentials.helpop	Allow access to the /helpop command.
Essentials	helpop	essentials.helpop.receive	Allows you to see Helpop messages
Essentials	home	essentials.home.compass	Point the player's compass at their first home. compass-towards-home-perm needs to be enabled in the configuration.
Essentials	home	essentials.home.bed	Allow access to the vanilla bed home.
Essentials	home	essentials.home.others	Allows you to teleport to homes of other players
Essentials	ice	essentials.ice	Allow access to the /ice command.
Essentials	ice	essentials.ice.others	Allow to use the /ice command on other players.
Essentials	ignore	essentials.ignore	Allow access to the /ignore command.
Essentials	info	essentials.info	Allow access to the /info command.
Essentials	invsee	essentials.invsee	Allow access to the /invsee command.
Essentials	invsee	essentials.invsee.equip	Allow access to see someones equiped armor slots in "/invsee <name> [armor]"
Essentials	invsee	essentials.invsee.modify	Players can modify the other players inventory (remove/add items).
Essentials	invsee	essentials.invsee.preventmodify	Prevents other players from modifying the players inventory.
Essentials	item	essentials.item	Allow access to the /item command.
Essentials	itemlore	essentials.itemlore	Allow access to the /itemlore command.
Essentials	itemdb	essentials.itemdb	Allows access to the /itemdb command.
Essentials	itemname	essentials.itemname	Allows access to the /itemname command.
Essentials	itemname	essentials.itemname.<effect>	Gives access to use <effect> in the /itemname command. <effect> examples: bold, reset, gold, dark_red, *
Essentials	itemname	essentials.itemname.prevent-type.<material_name>	Prevents the material from being used with the /itemname command
Essentials	more	essentials.itemspawn.exempt	Allows spawning of items on the spawn blacklist.
Essentials	togglejail	essentials.jail.exempt	Prevents a specified group or player from being jailed
Essentials	jails	essentials.jails	Allow access to the /jails command.
Essentials	deljail	essentials.deljail	Allow access to the /deljail command.
Essentials	jail	essentials.jail.allow-break	Allow breaking blocks whilst jailed. (Might also want to add essentials.jail.allow-block-damage)
Essentials	jail	essentials.jail.allow-place	Allow placing blocks whilst jailed
Essentials	jail	essentials.jail.allow-block-damage	Allow damaging blocks whilst jailed. (Might also want to add essentials.jail.allow-break)
Essentials	jail	essentials.jail.allow-interact	Allow interacting whilst jailed
Essentials		essentials.joinfullserver	Player can join when the server is full.
Essentials	jump	essentials.jump	Allow access to the /jump command.
Essentials	jump	essentials.jump.lock	Allow access to the "/jump lock" command
Essentials	back	essentials.keepxp	Allows the user to keep their exp on death, instead of dropping it.
Essentials	kick	essentials.kick	Allow access to the /kick command.
Essentials	kick	essentials.kick.exempt	Prevents the player from being kicked.
Essentials	kick	essentials.kick.notify	User sees a notification when a user is kicked.
Essentials	kickall	essentials.kickall	Allow access to the /kickall command.
Essentials	kickall	essentials.kickall.exempt	Be exempt from the /kickall command.
Essentials	kill	essentials.kill	Allow access to the /kill command.
Essentials	kill	essentials.kill.exempt	Prevents the player from being killed.
Essentials	kill	essentials.kill.force	Force player death, even if event is cancelled.
Essentials	kit	essentials.kit	Allow access to the /kit command.
Essentials	kit	essentials.kit.exemptdelay	Exempts you from the kit delay feature, this affects signs as well as command.
Essentials	kit	essentials.kit.others	Allows spawning of kits on other players.
Essentials	kittycannon	essentials.kittycannon	Allow access to the /kittycannon command
Essentials	beezooka	essentials.beezooka	Allow access to the /beezooka command
Essentials	lightning	essentials.lightning	Allow access to the /lightning command.
Essentials	lightning	essentials.lightning.others	Allows use of /lightning [playername]
Essentials	list	essentials.list	Allow access to the /list command.
Essentials	list	essentials.list.hidden	Show hidden users
Essentials	mail	essentials.mail	Allow access to the /mail command.
Essentials	mail	essentials.mail.send	Send mail
Essentials	mail	essentials.mail.sendall	Allows sending mail to all players at the same time
Essentials	mail	essentials.mail.sendtemp	Allows to send self-destructing mail.
Essentials	mail	essentials.mail.clear.others	Allow to clear mail of another player.
Essentials	mail	essentials.mail.clearall	Allow to clear all mail for all players.
Essentials	me	essentials.me	Allow access to the /me command.
Essentials	more	essentials.more	Allows access to the /more command.
Essentials	motd	essentials.motd	User sees MOTD on connect, and can use the command.
Essentials	msg	essentials.msg	Allow access to the /msg command.
Essentials	msg	essentials.msg.color	Allows using color codes in /msg command.
Essentials	msg	essentials.msg.rgb	Allows using rgb colors in /msg command.
Essentials	msg	essentials.msg.magic	Allows to use the matrix/magic color in the /msg command.
Essentials	msg	essentials.msg.url	Allows using urls in the /msg command.
Essentials	msg	essentials.msg.format	Allows to use formatting in the /msg command.
Essentials	msg	essentials.msg.multiple	This allows you to message multiple users in a single command.
Essentials	msgtoggle	essentials.msgtoggle	Allows access to the /msgtoggle command
Essentials	mute	essentials.mute	Allow access to the /mute command.
Essentials	mute	essentials.mute.exempt	Prevents a specified group or player from being muted
Essentials	mute	essentials.mute.unlimited	Allow overriding the max-mute-time set in the config
Essentials	mute	essentials.mute.offline	Allows muting of players who are offline. This may allow you to mute exempt players.
Essentials	near	essentials.near	Allow access to the /near command.
Essentials	near	essentials.near.maxexempt	Allows bypassing the radius limit.
Essentials	near	essentials.near.others	Allows using the near command on another player.
Essentials	near	essentials.near.exclude	Player gets excluded from /near lookup.
Essentials	nick	essentials.nick	Allow access to the /nick command.
Essentials	nick	essentials.nick.<color>	Allows you to color your /nick in <color> using color codes.
Essentials	nick	essentials.nick.blacklist.bypass	Allow access to bypass the /nick blacklist.
Essentials	nick	essentials.nick.changecolors	Allow to **only** change your nickname's colors (And not the name itself)
Essentials	nick	essentials.nick.changecolors.bypass	Allows to bypass checking if only nickname colors were changed, thus also allowing to change letters.
Essentials	nick	essentials.nick.rgb	Gives permission to use hex colors in the /nick command using the &#RRGGBB format.
Essentials	nick	essentials.nick.others	Gives you permission to give other players a nick name
Essentials		essentials.nocommandcost.all	Removes the command cost of all commands.
Essentials	nuke	essentials.nuke	Allow access to the /nuke command.
Essentials	give	essentials.oversizedstacks	Allows spawning of oversized stacks.
Essentials	pay	essentials.pay	Allow access to the /pay command.
Essentials	pay	essentials.pay.multiple	This allows you to pay multiple users in a single command.
Essentials	pay	essentials.pay.offline	This allows you to pay offline players.
Essentials	payconfirmtoggle	essentials.payconfirmtoggle	Allow access to the /payconfirmtoggle command.
Essentials	paytoggle	essentials.paytoggle	Allow access to the /paytoggle command.
Essentials	ping	essentials.ping	Allow access to the /ping command.
Essentials	potion	essentials.potions.[potionName]	Allow access to the "/potion [potionName]" command
Essentials	potion	essentials.potion.apply	Allow access to the "/potion apply" command
Essentials	powertool	essentials.powertool	Allow access to the /powertool command.
Essentials	powertool	essentials.powertool.append	Allows adding multiple commands to a single powertool.
Essentials	powertooltoggle	essentials.powertooltoggle	Allow access to the /powertooltoggle command.
Essentials	ptime	essentials.ptime	Allow access to the /ptime command.
Essentials	ptime	essentials.ptime.others	Allows you to change the time of another online player.
Essentials		essentials.pvpdelay.exempt	Exempts players from the 'pvp delay' option in the config file.
Essentials	pweather	essentials.pweather	Allow access to the /pweather command
Essentials	pweather	essentials.pweather.others	Allow to change the pweather of someone else
Essentials	r	essentials.msg	Allow access to the /r command
Essentials	rtoggle	essentials.rtoggle	Allows access to toggle replying to last message recipient
Essentials	realname	essentials.realname	Allow access to the /realname command.
Essentials	recipe	essentials.recipe	Allow access to the /recipe command.
Essentials	remove	essentials.remove	Allow access to the /remove command.
Essentials	repair	essentials.repair	Allow access to the /repair command.
Essentials	repair	essentials.repair.all	Unlock the /repair all ability
Essentials	repair	essentials.repair.armor	Repair all also repairs equipped armor.
Essentials	repair	essentials.repair.enchanted	Repairing of enchanted items.
Essentials	rest	essentials.rest	Allow access to the /rest command.
Essentials	rest	essentials.rest.others	Allow to specify other player in /rest.
Essentials	rules	essentials.rules	Allow access to the /rules command.
Essentials	seen	essentials.seen	Allow access to the /seen command.
Essentials	seen	essentials.seen.banreason	Allows the user to see why a user is banned.
Essentials	seen	essentials.seen.ip	Allow to see someone's ip-address in /seen
Essentials	seen	essentials.seen.uuid	Allow to see someone's uuid in /seen
Essentials	seen	essentials.seen.ipsearch	Allows the user to search IPs for username matches.
Essentials	seen	essentials.seen.location	Allow to see someone's location in /seen
Essentials	alts	essentials.seen.alts	Allows the user to see accounts with the same IP in /seen
Essentials	whitelist	essentials.seen.whitelist	Allows the user to see if the player is currently whitelisted in /seen and /whois
Essentials	sell	essentials.sell	Allow access to the /sell command.
Essentials	sell	essentials.sell.bulk	Allow bulk selling of items
Essentials	sell	essentials.sell.hand	Allow selling of items from your hand
Essentials	home	essentials.home	Allows access to the /home command
Essentials	sethome	essentials.sethome	Allow access to the /sethome command.
Essentials	renamehome	essentials.renamehome	Allow access to the /renamehome command.
Essentials	renamehome	essentials.renamehome.others	Allow to rename the home of another player.
Essentials		essentials.sethome.bed	Allows the player to right click a bed during daytime to update their 'bed' home.
Essentials		essentials.sethome.multiple	Allows player to have multiple homes, or create named homes. Required for 2+ homes.
Essentials	sethome	essentials.sethome.multiple.[set name]	Raise the multiple home limit to a setting defined in the config file.
Essentials	home	essentials.sethome.multiple.unlimited	Removes the cap on the number of homes people are allowed (if they have multiple homes).
Essentials	sethome	essentials.sethome.others	Allows you to change another users home location.
Essentials	delhome	essentials.delhome	Allow access to the /delhome command.
Essentials	delhome	essentials.delhome.others	Allows you to delete the homes of other players.
Essentials	setjail	essentials.setjail	Allow access to the /setjail command.
Essentials	jail	essentials.jail.notify	Players with this permission will be notified when someone is jailed.
Essentials	setwarp	essentials.setwarp	Allow access to the /setwarp command.
Essentials	setworth	essentials.setworth	Allow access to the /setworth command.
Essentials	showkit	essentials.showkit	Allow access to the /showkit command.
Essentials		essentials.signs.enchant.allowunsafe	Allows using [enchant] signs to perform unsafe enchantments
Essentials		essentials.signs.protection.override	Used to override any protections. Typically given to mods & admins
Essentials		essentials.signs.trade.override	Used to override the creator only protection on trade signs. Typically given to mods & admins
Essentials		essentials.signs.trade.override.collect	Allow collecting of protected trade signs
Essentials		essentials.silentjoin	Allow to join silently
Essentials		essentials.silentjoin.vanish	Allow to join silently, and get put in vanish mode
Essentials		essentials.silentquit	Suppress leave/quit messages for users with this permission.
Essentials	skull	essentials.skull	Allow access to the /skull command.
Essentials	skull	essentials.skull.modify	Allow changing the owner of an existing player skull.
Essentials	skull	essentials.skull.others	Allow creating skulls belonging to other players.
Essentials	skull	essentials.skull.spawn	Allows the spawning of a skull, default if the player isn't holding a skull.
Essentials		essentials.sleepingignored	User isn't required to be sleeping, for time to be reset.
Essentials	socialspy	essentials.socialspy	Allow access to the /socialspy command.
Essentials	spawner	essentials.spawner	Allow access to the /spawner command.
Essentials	spawner	essentials.spawner.delay	Allow access to adjust delay in /spawner command.
Essentials	spawnmob	essentials.spawnmob	Allow access to the /spawnmob command.
Essentials	spawnmob	essentials.spawnmob.stack	Allows the spawning of stacked mobs.
Essentials	speed	essentials.speed	Allow access to the /speed command.
Essentials	speed	essentials.speed.bypass	Allows you to bypass the speed limits set in the config.yml
Essentials	speed	essentials.speed.fly	This permission will allow a player to change fly speed only.
Essentials	speed	essentials.speed.others	Allows you to modify the speed of other players.
Essentials	speed	essentials.speed.walk	This permission will allow a player to change walk speed only.
Essentials	sudo	essentials.sudo	Allow access to the /sudo command.
Essentials	sudo	essentials.sudo.exempt	Prevents the holder from being sudo'ed by another user.
Essentials	sudo	essentials.sudo.multiple	Allow access to the /sudo command for multiple people.
Essentials	suicide	essentials.suicide	Allow access to the /suicide command.
Essentials		essentials.teleport.cooldown.bypass.back	Allow to bypass the /back command cooldown. Since build 1452, essentials.commandcooldowns.bypass.back can be used instead.
Essentials		essentials.teleport.cooldown.bypass.tpa	Allow to bypass the /tpa command cooldown. Since build 1452, essentials.commandcooldowns.bypass.tpa can be used instead.
Essentials		essentials.teleport.timer.bypass	Bypass the teleport delay
Essentials		essentials.teleport.timer.move	Allows players to move while waiting for teleport.
Essentials	tempban	essentials.tempban	Allow access to the /tempban command.
Essentials	tempban	essentials.tempban.exempt	Prevents a specified group or player from being tempbanned
Essentials	tempban	essentials.tempban.offline	Allows temp-banning of players who are offline. This may allow you to ban exempt players.
Essentials	tempban	essentials.tempban.unlimited	Allows bypassing config file max ban time limit.
Essentials	tempbanip	essentials.tempbanip	Allow access to the /tempbanip command.
Essentials	thunder	essentials.thunder	Allow access to the /thunder command.
Essentials	time	essentials.time	Allow access to the /time command.
Essentials	time	essentials.time.set	User is allowed to change the time of the world.
Essentials	time	essentials.time.world.all	Allow changing time of all worlds
Essentials	togglejail	essentials.togglejail	Allow access to the /togglejail command.
Essentials	togglejail	essentials.togglejail.offline	Allows jailing of players who are offline. This may allow you to jail exempt players.
Essentials	top	essentials.top	Allow access to the /top command.
Essentials	tp	essentials.tp	Allow access to the /tp command.
Essentials	tpauto	essentials.tpauto	Allow access to the /tpauto command.
Essentials	tpo	essentials.tp.others	Allow players to teleport another player.
Essentials	tp	essentials.tp.position	Allow to teleport to an exact location
Essentials	tpoffline	essentials.tpoffline	Allow teleporting to a player's logout location using /tpoffline
Essentials	tpa	essentials.tpa	Allow access to the /tpa command.
Essentials	tpaall	essentials.tpaall	Allow access to the /tpaall command.
Essentials	tpacancel	essentials.tpacancel	Allow access to the /tpacancel command.
Essentials	tpaccept	essentials.tpaccept	Allow access to the /tpaccept command.
Essentials	tpahere	essentials.tpahere	Allow access to the /tpahere command.
Essentials	tpall	essentials.tpall	Allow access to the /tpall command.
Essentials	tpdeny	essentials.tpdeny	Allow access to the /tpdeny command.
Essentials	tphere	essentials.tphere	Allow access to the /tphere command.
Essentials	tpo	essentials.tpo	Allow access to the /tpo command.
Essentials	tpohere	essentials.tpohere	Allow access to the /tpohere command.
Essentials	tppos	essentials.tppos	Allow access to the /tppos command.
Essentials	tptoggle	essentials.tptoggle	Allow access to the /tptoggle command.
Essentials	tpr	essentials.tpr	Allow access to the /tpr command.
Essentials	settpr	essentials.settpr	Allow access to the /settpr command.
Essentials	tree	essentials.tree	Allow access to the /tree command.
Essentials	unlimited	essentials.unlimited	Allow access to the /unlimited command.
Essentials	unlimited	essentials.unlimited.item-all	Allows the player to recieve an unlimited stack of any item.
Essentials	unlimited	essentials.unlimited.item-bucket	Allows the player to recieve an unlimited use of empty/lava/water buckets.
Essentials	unlimited	essentials.unlimited.others	Allows the player to gift/remove another player with an unlimited stack.
Essentials	vanish	essentials.vanish	Allow access to the /vanish command.
Essentials	vanish	essentials.vanish.pvp	Allows players who are vanished to attack other players.
Essentials	vanish	essentials.vanish.see	Allows you to see a vanished player.
Essentials	essentials	essentials.version	Allow access to the "/essentials version" command
Essentials	warp	essentials.warp	Allow access to the /warp command.
Essentials	warp	essentials.warp.list	Specifies whether you can view warp list with /warp.
Essentials	warp	essentials.warp.otherplayers	Allows warping another player.
Essentials	warp	essentials.warp.others	Allows warping another player.
Essentials	warpinfo	essentials.warpinfo	Allow access to the /warpinfo command.
Essentials	weather	essentials.weather	Allow access to the /weather command.
Essentials	whois	essentials.whois	Allow access to the /whois command.
Essentials	workbench	essentials.workbench	Allow access to the /workbench command.
Essentials	anvil	essentials.anvil	Allow access to the /anvil command.
Essentials	cartographytable	essentials.cartographytable	Allow access to the /cartographytable command.
Essentials	grindstone	essentials.grindstone	Allow access to the /grindstone command.
Essentials	loom	essentials.loom	Allow access to the /loom command.
Essentials	smithingtable	essentials.smithingtable	Allow access to the /smithingtable command.
Essentials	stonecutter	essentials.stonecutter	Allow access to the /stonecutter command.
Essentials	world	essentials.world	Allow access to the /world command.
Essentials	worth	essentials.worth	Allow access to the /worth command.
Essentials	fly	essentials.fly.others	Allow executing the /fly command for someone else.
Essentials	god	essentials.god.others	Allow executing the /god command for someone else.
Essentials	msgtoggle	essentials.msgtoggle.others	Allow executing the /msgtoggle command for someone else.
Essentials	socialspy	essentials.socialspy.others	Allow executing the /socialspy command for someone else.
Essentials	tptoggle	essentials.tptoggle.others	Allow executing the /tptoggle command for someone else.
Essentials	vanish	essentials.vanish.others	Allow executing the /vanish command for someone else.
Essentials		essentials.seen.extra	Allow to see someone's ip-address and location in /seen
Essentials		essentials.nocommandcost.[command]	Removes the command cost of a given command.
Essentials		essentials.signs.break.balance	Controls who can break balance signs.
Essentials		essentials.signs.break.buy	Controls who can break buy signs.
Essentials		essentials.signs.break.disposal	Controls who can break disposal signs.
Essentials		essentials.signs.break.enchant	Controls who can break enchant signs.
Essentials		essentials.signs.break.free	Controls who can break free signs.
Essentials		essentials.signs.break.gamemode	Controls who can break gamemode signs.
Essentials		essentials.signs.break.heal	Controls who can break heal signs.
Essentials		essentials.signs.break.info	Controls who can break info signs.
Essentials		essentials.signs.break.kit	Controls who can break kit signs.
Essentials		essentials.signs.break.mail	Controls who can break a mail sign.
Essentials		essentials.signs.break.protection	Controls who can break a protection sign. Typically given to everybody.
Essentials		essentials.signs.break.repair	Controls who can break repair signs.
Essentials		essentials.signs.break.sell	Controls who can break sell signs.
Essentials		essentials.signs.break.spawnmob	Controls who can break spawnmob signs.
Essentials		essentials.signs.break.time	Controls who can break time signs.
Essentials		essentials.signs.break.trade	Controls who can break trade signs.
Essentials		essentials.signs.break.warp	Controls who can break warp signs.
Essentials		essentials.signs.break.weather	Controls who can break weather signs.
Essentials		essentials.signs.color	This allows you to color your signs using color codes.
Essentials		essentials.signs.rgb	This allows you to color your signs using hex color formatting, &#RRGGBB.
Essentials		essentials.signs.create.balance	Controls who can create balance signs.
Essentials		essentials.signs.create.buy	Controls who can create buy signs.
Essentials		essentials.signs.create.disposal	Controls who can create disposal signs.
Essentials		essentials.signs.create.enchant	Controls who can create enchant signs.
Essentials		essentials.signs.create.free	Controls who can create free signs.
Essentials		essentials.signs.create.gamemode	Controls who can create gamemode signs.
Essentials		essentials.signs.create.heal	Controls who can create heal signs.
Essentials		essentials.signs.create.info	Controls who can create info signs.
Essentials		essentials.signs.create.kit	Controls who can create kit signs.
Essentials		essentials.signs.create.mail	Controls who can create a mail sign.
Essentials		essentials.signs.create.protection	Controls who can create a protection sign.
Essentials		essentials.signs.create.repair	Controls who can create repair signs.
Essentials		essentials.signs.create.sell	Controls who can create sell signs.
Essentials		essentials.signs.create.spawnmob	Controls who can create spawnmob signs.
Essentials		essentials.signs.create.time	Controls who can create time signs.
Essentials		essentials.signs.create.trade	Controls who can create trade signs.
Essentials		essentials.signs.create.warp	Controls who can create warp signs.
Essentials		essentials.signs.create.weather	Controls who can create weather signs.
Essentials		essentials.signs.format	This allows you to format your signs using format codes.
Essentials		essentials.signs.magic	This allows you to use the matrix/magic color in your signs.
Essentials		essentials.signs.use.balance	Controls who can use balance signs.
Essentials		essentials.signs.use.buy	Controls who can use buy signs.
Essentials		essentials.signs.use.disposal	Controls who can use disposal signs.
Essentials		essentials.signs.use.enchant	Controls who can use enchant signs.
Essentials		essentials.signs.use.free	Controls who can use free signs.
Essentials		essentials.signs.use.gamemode	Controls who can use gamemode signs.
Essentials		essentials.signs.use.heal	Controls who can use heal signs.
Essentials		essentials.signs.use.info	Controls who can use info signs.
Essentials		essentials.signs.use.kit	Controls who can use kit signs.
Essentials		essentials.signs.use.mail	Controls who can use a mail sign.
Essentials		essentials.signs.use.protection	Controls who can use a protection sign. Typically given to everybody.
Essentials		essentials.signs.use.repair	Controls who can use repair signs.
Essentials		essentials.signs.use.sell	Controls who can use sell signs.
Essentials		essentials.signs.use.spawnmob	Controls who can use spawnmob signs.
Essentials		essentials.signs.use.time	Controls who can use time signs.
Essentials		essentials.signs.use.trade	Controls who can use trade signs.
Essentials		essentials.signs.use.warp	Controls who can use warp signs.
Essentials		essentials.signs.use.weather	Controls who can use weather signs.
Essentials	editsign	essentials.editsign	Allows access to the editsign command
Essentials	editsign	essentials.editsign.unlimited	Allows access to exceed the 15-character-per-line limit
Essentials	editsign	essentials.editsign.rgb	Allows access to hex colors in the /editsign command using the &#RRGGBB format
Essentials	editsign	essentials.editsign.color	This allows you to use any color code within the /editsign command, NOT including hex colors (&#RRGGBB).
Essentials	editsign	essentials.editsign.<color>	Allows you to /editsign in <color> using color codes.
Essentials	editsign	essentials.editsign.waxed.exempt	Allows you to use /editsign on waxed signs.
Essentials	give	essentials.itemspawn.meta-chapter-[chapter]	Allow access to the Allows spawning of specific books only, from book.txt./give command.
Essentials	help	essentials.help.[pluginname]	Manually adds the commands from a plugin to this group's /help. Name is all lowercase.
Essentials	help	essentials.help.[pluginname].[command]	Manually adds a command from a plugin to this group's /help. Name is all lowercase.
Essentials	togglejail	essentials.jail.allow.<command>	Allows essentials commands to be used while jailed (requires regular perm).
Essentials	kit	essentials.kits.*	Allows access to all created kits
Essentials	kit	essentials.kits.[kitname]	Give access to a single kit, where [kitname] is the name of an existing kit.
Essentials	spawner	essentials.spawner.*	Allows setting a spawner to any mob type
Essentials	spawner	essentials.spawner.[mob]	Allows setting a spawner to a specific mob type
Essentials	spawner	essentials.spawner.delay	Allows setting the delay of a spawner.
Essentials	spawner	essentials.spawnerconvert.*	Allows placing a spawner of any type
Essentials	spawner	essentials.spawnerconvert.[mob]	Allows placing a spawner of a specific mob type
Essentials	spawnmob	essentials.spawnmob.*	Allow the spawning of all mobs
Essentials	spawnmob	essentials.spawnmob.[mob]	Allow access to spawn a specific mob
Essentials	unlimited	essentials.unlimited.item-[itemid]	Allows the player to recieve an unlimited stack of [itemid]
Essentials	unlimited	essentials.unlimited.item-[itemname]	Allows the player to recieve an unlimited stack of [itemname]
Essentials	setwarp	essentials.warp.overwrite.*	Allows overwriting of all existing warps.
Essentials	setwarp	essentials.warp.overwrite.[warpname]	Allows overwriting of existing warps.
Essentials	warp	essentials.warps.*	Allows access to all warps.
Essentials	warp	essentials.warps.[warpname]	If you have per-warp-permission set to true in the config.yml then you can limit what warps players can use. This also controls what players would see with /warp and /warpinfo.
Essentials	back	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	home	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	tp	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	tpa	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	tpaall	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	tpahere	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	tpall	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	tphere	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	tpo	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	tpohere	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	world	essentials.worlds.<worldname>	Allows players access to a specific world.
Essentials	item	essentials.itemspawn.meta-chapter-[chapter]	Allow access to the Allows spawning of specific books only, from book.txt./give command.
Essentials	ban	essentials.ban.notify	Players with this permission will receive a notification when a ban is set.
Essentials	banip	essentials.banip.notify	Players with this permission will receive a notification when an IP ban is set.
Essentials	tempban	essentials.ban.notify	Players with this permission will receive a notification when a ban is set.
Essentials	unban	essentials.ban.notify	Players with this permission will receive a notification when a ban is removed.
Essentials	unbanip	essentials.banip.notify	Players with this permission will receive a notification when an IP ban is removed.
Essentials	enchant	essentials.enchantments.[enchantmentname]	Allows access to each enchantment type.
Essentials	give	essentials.enchantments.[enchantmentname]	Allows access to each enchantment type.
Essentials	item	essentials.enchantments.[enchantmentname]	Allows access to each enchantment type.
Essentials	enchant	essentials.enchantments.allowunsafe	If enabled in the config, this permission allows unsafe enchantments.
Essentials	give	essentials.enchantments.allowunsafe	If enabled in the config, this permission allows unsafe enchantments.
Essentials	item	essentials.enchantments.allowunsafe	If enabled in the config, this permission allows unsafe enchantments.
Essentials	give	essentials.itemspawn.item-<itemname>	If permission-based-item-spawn: Spawn <itemname>
Essentials	give	essentials.itemspawn.item-[itemid]	If permission-based-item-spawn: Spawn [itemid]
Essentials	item	essentials.itemspawn.item-[itemid]	If permission-based-item-spawn: Spawn [itemid]
Essentials	item	essentials.itemspawn.item-[itemname]	If permission-based-item-spawn: Spawn [itemname]
Essentials	give	essentials.itemspawn.item-all	If permission-based-item-spawn: Spawn all items
Essentials	item	essentials.itemspawn.item-all	If permission-based-item-spawn: Spawn all items.
Essentials	give	essentials.itemspawn.meta-author	Allow 'author' meta to be used in item spawning.
Essentials	item	essentials.itemspawn.meta-author	Allow 'author' meta to be used in item spawning.
Essentials	give	essentials.itemspawn.meta-book	Allows spawning of books with pre-filled content from books.txt
Essentials	item	essentials.itemspawn.meta-book	Allows spawning of books with pre-filled content from books.txt
Essentials	give	essentials.itemspawn.meta-firework	Allow specific meta for fireworks.
Essentials	item	essentials.itemspawn.meta-firework	Allow specific meta for fireworks.
Essentials	give	essentials.itemspawn.meta-head	Allows spawning of mob heads.
Essentials	item	essentials.itemspawn.meta-head	Allows spawning of mob heads.
Essentials	give	essentials.itemspawn.meta-lore	Allow 'lore' meta to be used in item spawning.
Essentials	item	essentials.itemspawn.meta-lore	Allow 'lore' meta to be used in item spawning.
Essentials	give	essentials.itemspawn.meta-title	Allow 'title' meta to be used in item spawning.
Essentials	item	essentials.itemspawn.meta-title	Allow 'title' meta to be used in item spawning.
Essentials	give	essentials.oversizedstacks	Allows spawning of stacks above normal max stack size
Essentials	item	essentials.oversizedstacks	Allows spawning of stacks above normal max stack size
Essentials	more	essentials.oversizedstacks	Allows spawning of oversized stacks.
Essentials	vanish	essentials.vanish.effect	Allow adding potion effects when someone goes into vanish
Essentials	vanish	essentials.vanish.interact	Allow interacting with vanished players
Essentials	back	essentials.back.into.<worldname>	Allows access to /back when the destination location is within the specified world
Essentials	whois	essentials.whois.ip	Allow seeing someones ip address in the /whois command.
Essentials	gamemode	essentials.gamemode.creative	Allow access to the /gamemode creative command. Also requires essentials.gamemode
Essentials	gamemode	essentials.gamemode.survival	Allow access to the /gamemode survival command. Also requires essentials.gamemode
Essentials	gamemode	essentials.gamemode.adventure	Allow access to the /gamemode adventure command. Also requires essentials.gamemode
Essentials	gamemode	essentials.gamemode.spectator	Allow access to the /gamemode spectator command. Also requires essentials.gamemode
Essentials	nick	essentials.nick.allowunsafe	If a player has this, they can set their username to any value. Use with caution, as this has the potential to break userdata files.
Essentials	nick	essentials.nick.hideprefix	Players with this permission will not have the nickname prefix applied to them
Essentials	msg	essentials.msgtoggle.bypass	This permission allows you to send private messages to users that have disabled them with /msgtoggle.
Essentials	tpauto	essentials.tpauto.others	Allow using /tpauto for other players
Essentials	nick	essentials.nick.color	Allows to use colors in the /nick command
Essentials	nick	essentials.nick.magic	Allows to use magic effect in the /nick command
Essentials	nick	essentials.nick.format	Allows to use formatting in the /nick command
Essentials	back	essentials.keepinv	Allows the user to keep their inventory on death, instead of dropping it.
Essentials	back	essentials.back.others	Allows the user to execute the /back command for other players
Essentials	mute	essentials.mute.notify	Get notified when an muted player tries to talk
Essentials	balancetop	essentials.balancetop.exclude	Players with this permission are excluded from the balancetop
Essentials	hat	essentials.hat.prevent-type.<item-name>	Give this permission to users to prevent them using this type of item in /hat
Essentials	hat	essentials.hat.ignore-binding	Disable blocking the removing of hats with curse of binding
Essentials	playtime	essentials.playtime	Shows a player's time played in game
Essentials	playtime	essentials.playtime.others	Shows a player's time played in game
EssentialsAntiBuild		essentials.build	Allows people to build when using permission systems which don't support build toggles.
EssentialsAntiBuild		essentials.build.<action>.<material>	Allows executing <action> for <material>, action being place, break, interact, craft, drop or pickup.
EssentialsAntiBuild		essentials.protect.alerts	Allows a player to recieve protect alerts.
EssentialsAntiBuild		essentials.protect.alerts.notrigger	Users with this permission do not trigger protect alerts.
EssentialsAntiBuild		essentials.protect.exemptbreak	Allows a player to ignore the break blacklist.
EssentialsAntiBuild		essentials.protect.exemptplacement	Allows a player to ignore the block placement blacklist.
EssentialsAntiBuild		essentials.protect.exemptusage	Allows a player to ignore the usage blacklist.
EssentialsChat		essentials.chat.color	This allows you to color your chat messages in any color using color codes, NOT including hex colors (&#RRGGBB).
EssentialsChat		essentials.chat.<color>	This allows you to color your chat messages in <color> using color codes.
EssentialsChat		essentials.chat.format	This allows you to format your chat messages using format codes.
EssentialsChat		essentials.chat.magic	This allows you to use the matrix/magic color in your chat messages.
EssentialsChat		essentials.chat.rgb	This allows you to use hex color formatting in your chat messages. Format is &#RRGGBB.
EssentialsChat		essentials.chat.question	This allows you to send global questions, used if a chat-radius is configured.
EssentialsChat		essentials.chat.shout	This allows you to send global messages, used if a chat-raidus is configured.
EssentialsChat		essentials.toggleshout	Toggles whether you are talking in shout mode.
EssentialsChat		essentials.toggleshout.others	Toggles shout mode for other players.
EssentialsChat		essentials.chat.spy	A permission designed for admins, to intercept all local messages (ignoring the chat-radius).
EssentialsChat		essentials.chat.url	This allows you to use urls in your chat messages.
EssentialsChat		essentials.chat.local	Allow to send messages to local chat. Given to all players by default.
EssentialsChat		essentials.chat.receive.local	Permission allowing to recieve local chat. Given to all players by default.
EssentialsChat		essentials.chat.receive.shout	Permission allowing to recieve shouts. Given to all players by default.
EssentialsChat		essentials.chat.receive.question	Permission allowing to recieve questions. Given to all players by default.
EssentialsDiscord	discord	essentials.discord	Allow access to the /discord command.
EssentialsDiscord		essentials.discord.receive.<group>	Allows user to receive chat messages from the specified discord channel
EssentialsDiscord		essentials.discord.markdown	Allows user to bypass the discord markdown filter
EssentialsDiscord		essentials.discord.ping	Allows user to ping everyone/role/here from mc in discord (bypass ping filters)
EssentialsDiscord	discordbroadcast	essentials.discordbroadcast	Allow access to the /discordbroadcast command.
EssentialsDiscord	discordbroadcast	essentials.discordbroadcast.markdown	Allow to use markdown in the /discordbroadcast command.
EssentialsDiscord	discordbroadcast	essentials.discordbroadcast.ping	Allow to ping discord roles, @here or @everyone in the /discordbroadcast command.
EssentialsDiscord	discordbroadcast	essentials.discordbroadcast.<channel_name>	Allow to send broadcasts in the specific discord channel.
EssentialsDiscordLink	link	essentials.link	Allow access to the /link command.
EssentialsDiscordLink	unlink	essentials.unlink	Allow access to the /unlink command.
EssentialsGeoIP		essentials.geoip.hide	Allows you to hide your country and city from people who have permission essentials.geoip.show
EssentialsGeoIP	whois	essentials.geoip.show	Shows the GeoIP location of a player, if the GeoIP module is installed.
EssentialsProtect		essentials.protect.entitytarget.bypass	Allows a player to be targeted by mobs.
EssentialsProtect		essentials.protect.pvp	When prevent PVP is set to true, this allows players to still PVP. Both attacker and defender need to have this permission.
EssentialsProtect		essentials.protect.damage.disable	Admin override to prevent admins from dying
EssentialsProtect		essentials.protect.damage.contact	Player will receive contact damage
EssentialsProtect		essentials.protect.damage.creeper	Player will receive creeper damage
EssentialsProtect		essentials.protect.damage.drowning	Player will receive drowning damage
EssentialsProtect		essentials.protect.damage.fall	Player will receive fall damage
EssentialsProtect		essentials.protect.damage.fire	Player will receive fire damage
EssentialsProtect		essentials.protect.damage.fireball	Player will receive fireball damage
EssentialsProtect		essentials.protect.damage.lava	Player will receive lava damage
EssentialsProtect		essentials.protect.damage.lightning	Player will receive lightning damage
EssentialsProtect		essentials.protect.damage.projectiles	Player will receive projectile damage
EssentialsProtect		essentials.protect.damage.suffocation	Player will receive suffocation damage
EssentialsProtect		essentials.protect.damage.tnt	Player will receive tnt damage
EssentialsSpawn	setspawn	essentials.setspawn	Allow access to the /setspawn command.
EssentialsSpawn	spawn	essentials.spawn	Allow access to the /spawn command.
EssentialsSpawn		essentials.spawn-on-join.exempt	Allow to be exempt from forced teleporting to spawn on join
EssentialsSpawn	spawn	essentials.spawn.others	Allows you to teleport other people to spawn
EssentialsXMPP	setxmpp	essentials.setxmpp	Allow access to the /setxmpp command.
EssentialsXMPP	xmpp	essentials.xmpp	Allow access to the /xmpp command.
EssentialsXMPP	xmppspy	essentials.xmppspy	Allow access to the /xmppspy command.


Tablist Plugin/system
About
This page lists all the commands and permissions that are available with TAB. Commands have required permissions associated with them.

BungeeCord / Velocity users:

If you have TAB installed on the proxy, use /btab instead of /tab. Permissions are the same.
Permission nodes are checked on the server where the plugin is installed (by default). This means that with TAB on BungeeCord, permission nodes are checked on BungeeCord, and therefore you'll need a permission plugin there as well. If you wish to take permission groups and checks from backend servers instead, set use-bukkit-permissions-manager: true in config.
Content
Configuration
/tab <player/group/playeruuid> <name> <property> [value] [options]
/tab <player/group/playeruuid> <name> remove
/tab reload
/tab debug [player]
/tab group <group>
/tab groups
Bossbar
/tab bossbar send <name> [player]
/tab bossbar [on/off/toggle] [player] [options]
/tab bossbar announce <name> <length>
Scoreboard
/tab scoreboard show <name> [player]
/tab scoreboard [on/off/toggle] [player] [options]
/tab scoreboard announce <name> <length>
Nametags / Teams
/tab nametag <show/hide/toggle> [player] [viewer] [options]
/tab nametag <showview/hideview/toggleview> [viewer] [options]
/tab setcollision <true/false>
MySQL
/tab mysql upload
/tab mysql download
Other
/tab cpu
/tab parse <player> <placeholder>
Additional permissions
Configuration commands
/tab <player/group/playeruuid> <name> <property> [value] [options]
Permission: tab.change.<property>
Description: Changes a property of a group/player to the given value. No value argument will result in that property being deleted.
Properties
For tablist: tabprefix, customtabname, and tabsuffix.
For nametag: tagprefix and tagsuffix.
Options
-s <server> - Applying value only for a specific server
-w <world> - Applying value only for a specific world
Names for each command type
player: Supports both player name and raw uuid. Player doesn't need to be online.
playeruuid: Requires name of an online player. Equal to using player with player's uuid.
group: Using group names from permission plugin. _DEFAULT_ is used for default settings for all groups.
/tab <player/group/playeruuid> <name> remove
Permission: tab.remove
Description: Removes all direct data from the given player/group.
Notes
The playeruuid argument requires the name of an online player to remove data from.
/tab reload
Permission: tab.reload
Description: Unloads the plugin, loads configuration files including changes and loads the plugin.
/tab debug [player]
Permission: tab.debug
Description: Shows the server version, plugin version, permission group choice logic, and sorting type. If player argument is filled, shows info about that player:
On BungeeCord, shows whether player is connected to the backend server with Bridge plugin or not.
Sorting value & explanation, useful to see what went wrong if players are not sorted correctly.
Primary group set using How to assign players into groups
List of all configured properties, their values and source.
/tab group <group>
Permission: tab.groupinfo
Description: Shows all settings applied to the specified group, both global and per-world / per-server. Handy when plugin is connected to MySQL, where you have no other way to effectively check group settings.
/tab groups
Permission: tab.grouplist
Description: Shows a list of all groups that have anything assigned to them, either globally or per-world / per-server. Handy when plugin is connected to MySQL, where you have no other way to effectively check your groups.
Scoreboard
/tab scoreboard show <name> [player]
Permission: tab.scoreboard.show for showing to yourself, tab.scoreboard.show.other for showing to others.
Description: Shows the scoreboard with the given name, either to yourself if no player was given, or to the given player.
/tab scoreboard [on/off/toggle] [player] [options]
Permission: tab.scoreboard.toggle for toggling for yourself, tab.scoreboard.toggle.other for toggling for others.
Description: Shows / hides / toggles scoreboard of specified player. If no player was given, command affects the sender.
Options:
-s for silent toggling (no chat message for affected player)
/tab scoreboard announce <name> <duration>
Permission: tab.announce.scoreboard
Description: Shows the scoreboard with the given name to every player on the server for the given duration, in seconds.
Bossbar
/tab bossbar show <name> [player]
Permission: tab.bossbar.show for showing to yourself, tab.bossbar.show.other for showing to others.
Description: Shows the bossbar with the given name, either to yourself if no player was given, or to the given player.
/tab bossbar [on/off/toggle] [player] [options]
Permission: tab.bossbar.toggle for toggling for yourself, tab.bossbar.toggle.other for toggling for others.
Description: Shows / hides / toggles bossbar of specified player. If no player was given, command affects the sender.
Options:
-s for silent toggling (no chat message for affected player)
/tab bossbar announce <name> <duration>
Permission: tab.announce.bar
Description: Shows the bossbar with the given name to every player on the server for the given duration, in seconds.
Nametags / Teams
/tab nametag <show/hide/toggle> [player] [viewer] [options]
Permission: tab.nametag.visibility (tab.nametag.visibility.other for toggling for other players)
Description: Shows / hides / toggles nametag of a specified player. If viewer is specified, view is only affected for the viewer.
Options:
-s for silent toggling (no chat message for affected player)
/tab nametag <showview/hideview/toggleview> [viewer] [options]
Permission: tab.nametag.view (tab.nametag.view.other for toggling for other players)
Description: Shows / hides / toggles nametag VIEW a specified player.
Options:
-s for silent toggling (no chat message for affected player)
/tab setcollision <player> <true|false>
Permission: tab.setcollision
Description: Forces collision rule for specified player, overriding configuration.
MySQL
/tab mysql upload
Permission: tab.mysql.upload
Description: Uploads all data from users.yml and groups.yml to MySQL. MySQL must be enabled and connected for this command to work.
/tab mysql download
Permission: tab.mysql.download
Description: Downloads all data to users.yml and groups.yml from MySQL. MySQL must be enabled and connected for this command to work.
Other
/tab cpu
Permission: tab.cpu
Description: Shows approximate CPU usage of the plugin from the last 10 seconds. There are parts that are impossible to be measured, so this shows slightly less than the real value is. The content is self-explanatory. All major ways to decrease CPU usage can be found at Optimizing the plugin.
/tab parse <player> <text>
Permission: tab.parse
Description: Replaces all placeholders (both the plugin's internal ones, and those from PlaceholderAPI if it is installed) in the given text. It can be used to verify if a placeholder works as expected.
Additional permissions
tab.admin - Allows the player to execute all commands.
tab.bypass - If the per-world player list is enabled as well as this permission, it allows the player to see everyone on the server, regardless of what the per-world player list settings allow. For example, if your per-world player list is set up to isolate worlds A and B, players with this permission will see all players from both A and B on their player list, whereas players that don't have the permission will only see either players from world A or players from world B, depending on what world they are in.
tab.staff - Allows the player to be counted in the %staffonline% placeholder.
tab.spectatorbypass - If enabling bypass permission in Spectator fix, this is the permission.
tab.tabcomplete - Allows the player to auto-complete the /tab command.
tab.seevanished - Allows the player to see other vanished players on the Global Playerlist and Layout.

Discord System
Commands
Command	Permission	Description
/discord
/discordsrv	discordsrv.discord (true)	Shows whatever is defined for DiscordCommandFormat in the messages.yml file
The following tables are arguments to the above command.

Player Commands
Command	Permission	Description
help
?	discordsrv.help	Shows a list of all the options you have permissions for
linked	discordsrv.linked	Shows if your Minecraft account is linked with a discord account.
link	discordsrv.link	Sends you instructions to link your Minecraft account with your Discord account.
unlink
clearlinked	discordsrv.unlink	Unlink your Discord account from your Minecraft account.
Staff Commands
Command	Arguments	Permission	Description
broadcast
bcast	<#ChannelID/#ChannelName> <Message>	discordsrv.bcast	Shows a list of all the options you have permissions for
linked	<Name/UUID/DiscordID/DiscordTag>	discordsrv.linked.others	Shows if your Minecraft account is linked with a discord account.
link	<Name/UUID> <DiscordID/DiscordTag>	discordsrv.link.others	Sends you instructions to link your Minecraft account with your Discord account.
unlink
clearlinked	<Name/UUID/DiscordID/DiscordTag>	discordsrv.unlink.others	Unlink your Discord account from your Minecraft account.
resync		discordsrv.resync	Triggers group synchronization (requires synchronization.yml)
reload		discordsrv.reload	Reloads the plugin. (Some changes require a server restart.)
language
lang	<Language> [-confirm]	discordsrv.language	Changes the language of the plugin.
debugger	<start [categories...]/stop/upload>	discordsrv.debug	A toggleable timings-like command to dump debug information to https://bin.scarsz.me. Use the sub command upload to return a debug link (Debug Categories)
Deprecated Commands
Command	Permission	Description	Notice
subscribe	discordsrv.subscribe (true)	Enables receiving messages from Discord for yourself. (removed since v15.3)	The subscription system was removed in version 15.3 due to low usage and unrecoverable problems with use of chat channel supporting plugins.
unsubscribe	discordsrv.unsubscribe (true)	Disables receiving messages from Discord for yourself. (removed since v15.3)	The subscription system was removed in version 15.3 due to low usage and unrecoverable problems with use of chat channel supporting plugins.
toggle	discordsrv.toggle (true)	Toggles receiving messages from Discord for yourself. (removed since v15.3)	The subscription system was removed in version 15.3 due to low usage and unrecoverable problems with use of chat channel supporting plugins.

permissions
Permissions
A permissions plugin (preferably LuckPerms) is highly recommended to use these permissions effectively. If this is your first time setting up permissions and you're planning on using LuckPerms, read this section of LuckPerms' wiki.

Parent Permissions
These are super permissions that grant a bunch of standard permissions for your use case. Generally, these two parent permissions are the ones you should use.

Parent Permission	Default	Description	Child Permissions
discordsrv.player	true	parent permission of player-related function of DiscordSRV	discordsrv.chat
discordsrv.help
discordsrv.link
discordsrv.linked
discordsrv.discord
discordsrv.nicknamesync
discordsrv.admin	OP	parent permission of admin-related functions of DiscordSRV	discordsrv.player
discordsrv.bcast
discordsrv.reload
discordsrv.resync
discordsrv.debug
discordsrv.link.others
discordsrv.linked.others
discordsrv.unlink
discordsrv.unlink.others
discordsrv.groupsyncwithcommands
discordsrv.updatenotification
discordsrv.language
Permissions
These are individual permissions that can be given or taken away to fine tune what players are able to do.

Permission	Default	Description
discordsrv.discord	true	allows access to the /discord//discordsrv command
discordsrv.chat	true	whether or not the user is able to have their chat forwarded to Discord
discordsrv.silentjoin	false	whether or not to have join messages silenced for players with this permission
discordsrv.silentquit	false	whether or not to have quit messages silenced for players with this permission
discordsrv.help	true	whether or not the player is able to run DiscordSRV's help command
discordsrv.updatenotification	OP	whether or not the player should be told if there's an update to DiscordSRV upon joining
discordsrv.bcast	OP	whether or not the player is able to run DiscordSRV's broadcast command
discordsrv.reload	OP	whether or not the player is able to reload DiscordSRV's configuration
discordsrv.resync	OP	whether or not the player is able to manually resynchronize all groups & roles
discordsrv.debug	OP	whether or not the player is able to run /discordsrv debugger and subcommands
discordsrv.link	true	whether or not the player is able to link their Minecraft account to their Discord account
discordsrv.link.others	OP	whether or not the player is able to link other people's Minecraft accounts to Discord accounts
discordsrv.unlink	OP	whether or not the player is able to unlink their Minecraft account from their Discord account
discordsrv.unlink.others	OP	whether or not the player is able to unlink other people's Minecraft accounts from their Discord accounts
discordsrv.linked	true	whether or not the player is able to check what Discord account their Minecraft account is linked to
discordsrv.linked.others	OP	whether or not the player is able to check what Discord account other Minecraft accounts are linked to
discordsrv.groupsyncwithcommands	OP	whether or not the player can run a permission plugin command to force group sync to occur
discordsrv.resync	OP	whether or not the player can run /discord resync to force a resync of all groups/roles
discordsrv.nicknamesync	true	whether or not the player should have their nickname synced with Discord, if doing so is enabled in synchronization.yml
discordsrv.sync.<group>	true	Groups that should be added to the player if their discord account is linked.

Each group must be added to the GroupRoleSynchronizationGroupsAndRolesToSync option in synchronization.yml first
discordsrv.sync.deny.<group>	true	Groups that should be removed from the player if their discord account is linked.

Permissions need to be enabled through the GroupRoleSynchronizationEnableDenyPermission option in synchronization.yml
discordsrv.language	OP	whether or not the player can change the language of the plugin
Deprecated Permissions
Permission	Default	Description	Notice
discordsrv.subscribe	true	whether or not the player is able to subscribe to Discord messages being sent to them	The subscription system was removed in version 15.3 due to low usage and unrecoverable problems with use of chat channel supporting plugins.
discordsrv.unsubscribe	true	whether or not the player is able to unsubscribe from Discord messages being sent to them	
discordsrv.toggle	true	whether or not the player is able to toggle their subscription status to Discord messages	