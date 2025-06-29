# Commands Reference

Complete command reference for NeoEssentials v1.0.1.89+ including new tablist and animation management commands.

## 📖 Command Format

```
/command <required> [optional] - Description
```

- **Bold text** - Required parameters that must be provided
- *Italic text* - Optional parameters  
- `code text` - Exact syntax to type

## 🔑 Permission Structure

All permissions follow the pattern:
```
neoessentials.command.<command_name>
neoessentials.admin.<admin_command>
neoessentials.tablist.<tablist_command>
```

## 🎯 Core Commands

### Basic Information
| Command | Description | Permission |
|---------|-------------|------------|
| `/neoessentials` | Shows mod information and version | `neoessentials.command.base` |
| `/neoessentials help` | Complete help system | `neoessentials.command.help` |
| `/neoessentials version` | Version and build information | `neoessentials.command.version` |
| `/neoessentials status` | System status and performance | `neoessentials.command.status` |

### Configuration Management  
| Command | Description | Permission |
|---------|-------------|------------|
| `/neoessentials reload` | Reload all configurations | `neoessentials.admin.reload` |
| `/neoessentials reload <system>` | Reload specific system | `neoessentials.admin.reload` |
| `/neoessentials debug` | Toggle debug mode | `neoessentials.admin.debug` |

## 🎨 Tablist & Animation Commands (NEW)

### Tablist Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/tablist` | Show current tablist info | `neoessentials.command.tablist` |
| `/tablist reload` | Reload tablist configuration | `neoessentials.tablist.reload` |
| `/tablist toggle` | Toggle tablist display | `neoessentials.tablist.toggle` |
| `/tablist debug` | Show tablist debug information | `neoessentials.tablist.debug` |
| `/tablist test **<animation>**` | Test specific animation | `neoessentials.tablist.test` |

### Animation System  
| Command | Description | Permission |
|---------|-------------|------------|
| `/tablist animations` | List all available animations | `neoessentials.tablist.animations` |
| `/tablist performance` | Show update system performance | `neoessentials.tablist.performance` |
| `/animations reload` | Reload animation definitions | `neoessentials.animations.reload` |

## 👤 Player Commands

### Basic Player Actions
| Command | Description | Permission |
|---------|-------------|------------|
| `/afk` | Toggle AFK status | `neoessentials.command.afk` |
| `/back` | Return to previous location | `neoessentials.command.back` |
| `/balance` or `/bal` | Check economy balance | `neoessentials.command.balance` |
| `/fly` | Toggle flight mode | `neoessentials.command.fly` |
| `/gamemode` *[mode]* | Change game mode | `neoessentials.command.gamemode` |
| `/nickname` **<name>** | Set display nickname | `neoessentials.command.nickname` |
| `/ping` | Show server latency | `neoessentials.command.ping` |
| `/spawn` | Teleport to spawn point | `neoessentials.command.spawn` |

### Communication
| Command | Description | Permission |
|---------|-------------|------------|
| `/msg` **<player>** **<message>** | Send private message | `neoessentials.command.msg` |
| `/reply` **<message>** | Reply to last message | `neoessentials.command.reply` |
| `/mail` *[action]* | Manage offline mail | `neoessentials.command.mail` |

### Economy & Trading
| Command | Description | Permission |
|---------|-------------|------------|
| `/balance` or `/bal` | Check your balance | `neoessentials.command.balance` |
| `/balancetop` or `/baltop` | View richest players | `neoessentials.command.balancetop` |
| `/pay` **<player>** **<amount>** | Transfer money | `neoessentials.command.pay` |
| `/eco` *[action]* | Economy management commands | `neoessentials.command.eco` |

### Banking System
| Command | Description | Permission |
|---------|-------------|------------|
| `/bank` | Show bank account information | `neoessentials.command.bank` |
| `/bank create` **<type>** | Create a new bank account | `neoessentials.command.bank.create` |
| `/bank list` | List all your bank accounts | `neoessentials.command.bank.list` |
| `/bank deposit` **<amount>** *[account]* | Deposit money to account | `neoessentials.command.bank.deposit` |
| `/bank withdraw` **<amount>** *[account]* | Withdraw money from account | `neoessentials.command.bank.withdraw` |
| `/bank transfer` **<from>** **<to>** **<amount>** | Transfer between accounts | `neoessentials.command.bank.transfer` |

### Shop System
| Command | Description | Permission |
|---------|-------------|------------|
| `/shop` | Show shop help and status | `neoessentials.command.shop` |
| `/shop create` **<name>** **<category>** **<ownership>** | Create a new shop | `neoessentials.command.shop.create` |
| `/shop list` *[category]* | List available shops | `neoessentials.command.shop.list` |
| `/shop buy` **<shop>** **<item>** **<quantity>** | Buy items from shop | `neoessentials.command.shop.buy` |
| `/shop sell` **<shop>** **<item>** **<quantity>** | Sell items to shop | `neoessentials.command.shop.sell` |
| `/shop manage` **<shop>** | Manage your shop | `neoessentials.command.shop.manage` |

### Auction System
| Command | Description | Permission |
|---------|-------------|------------|
| `/auction` | Show auction help | `neoessentials.command.auction` |
| `/auction create` **<type>** **<starting-price>** *[duration]* | Create new auction | `neoessentials.command.auction.create` |
| `/auction list` *[type]* | List active auctions | `neoessentials.command.auction.list` |
| `/auction bid` **<auction-id>** **<amount>** | Place bid on auction | `neoessentials.command.auction.bid` |
| `/auction buyout` **<auction-id>** | Buy auction immediately | `neoessentials.command.auction.buyout` |
| `/auction cancel` **<auction-id>** | Cancel your auction | `neoessentials.command.auction.cancel` |

### Loan System
| Command | Description | Permission |
|---------|-------------|------------|
| `/loan` | Show loan information | `neoessentials.command.loan` |
| `/loan apply` **<amount>** **<type>** *[term]* | Apply for a loan | `neoessentials.command.loan.apply` |
| `/loan list` | List your active loans | `neoessentials.command.loan.list` |
| `/loan pay` **<loan-id>** **<amount>** | Make loan payment | `neoessentials.command.loan.pay` |
| `/loan info` **<loan-id>** | Get loan information | `neoessentials.command.loan.info` |

## 🏠 Home & Warp Commands

### Home Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/home` *[name]* | Teleport to home | `neoessentials.command.home` |
| `/homes` | List all your homes | `neoessentials.command.homes` |
| `/sethome` *[name]* | Set a new home | `neoessentials.command.sethome` |
| `/delhome` **<name>** | Delete a home | `neoessentials.command.delhome` |

### Warp System
| Command | Description | Permission |
|---------|-------------|------------|
| `/warp` **<name>** | Teleport to warp | `neoessentials.command.warp` |
| `/warps` | List all available warps | `neoessentials.command.warps` |
| `/setwarp` **<name>** | Create new warp | `neoessentials.command.setwarp` |
| `/delwarp` **<name>** | Delete warp | `neoessentials.command.delwarp` |

### Teleportation Requests  
| Command | Description | Permission |
|---------|-------------|------------|
| `/tpa` **<player>** | Request teleport to player | `neoessentials.command.tpa` |
| `/tpahere` **<player>** | Request player teleport to you | `neoessentials.command.tpahere` |
| `/tpaccept` | Accept pending request | `neoessentials.command.tpaccept` |
| `/tpdeny` | Deny pending request | `neoessentials.command.tpdeny` |

## 🎁 Kit System

| Command | Description | Permission |
|---------|-------------|------------|
| `/kit` **<name>** | Claim a kit | `neoessentials.command.kit` |
| `/kits` | List available kits | `neoessentials.command.kits` |
| `/kitinfo` **<name>** | View kit details | `neoessentials.command.kitinfo` |

## ⚡ Admin Commands

### Server Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/broadcast` **<message>** | Server-wide announcement | `neoessentials.admin.broadcast` |
| `/maintenance` | Toggle maintenance mode | `neoessentials.admin.maintenance` |
| `/whitelist` *[action]* | Manage server whitelist | `neoessentials.admin.whitelist` |

### Player Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/ban` **<player>** *[reason]* | Ban player permanently | `neoessentials.admin.ban` |
| `/tempban` **<player>** **<time>** *[reason]* | Ban player temporarily | `neoessentials.admin.tempban` |
| `/kick` **<player>** *[reason]* | Kick player from server | `neoessentials.admin.kick` |
| `/mute` **<player>** *[time]* | Mute player chat | `neoessentials.admin.mute` |
| `/vanish` | Toggle invisibility | `neoessentials.admin.vanish` |

### Economy Management
| Command | Description | Permission |
|---------|-------------|------------|
| `/eco give` **<player>** **<amount>** | Give money to player | `neoessentials.admin.eco.give` |
| `/eco take` **<player>** **<amount>** | Take money from player | `neoessentials.admin.eco.take` |
| `/eco set` **<player>** **<amount>** | Set player balance | `neoessentials.admin.eco.set` |
| `/eco reset` **<player>** | Reset player balance | `neoessentials.admin.eco.reset` |

## Command Aliases

Many commands have shorter aliases for convenience:

| Command | Aliases |
|---------|---------|
| `/balance` | `/bal`, `/money` |
| `/broadcast` | `/bc` |
| `/clearinventory` | `/ci`, `/clean` |
| `/gamemode` | `/gm` |
| `/nickname` | `/nick` |
| `/teleport` | `/tp` |
| `/teleporthere` | `/tph`, `/tphere` |
| `/vanish` | `/v` |

## Command Customization

Commands can be customized in the `config/neoessentials/commands.toml` file:
- Enable/disable individual commands
- Change command aliases
- Modify command cooldowns
- Configure command costs (economy integration)

For advanced command management, see the [Custom Commands](Custom-Commands) guide.
