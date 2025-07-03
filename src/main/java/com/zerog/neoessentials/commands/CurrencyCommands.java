package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/**
 * Commands for managing multiple currencies in NeoEssentials
 */
public class CurrencyCommands {
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("currency")
                .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.currency"))
                .executes(context -> showCurrencyHelp(context.getSource()))
                
                // /currency list - List all available currencies
                .then(Commands.literal("list")
                    .executes(context -> listCurrencies(context.getSource())))
                
                // /currency info <currency> - Show currency information
                .then(Commands.literal("info")
                    .then(Commands.argument("currency", StringArgumentType.string())
                        .suggests(TabCompletionUtil.CURRENCY_SUGGESTIONS)
                        .executes(context -> showCurrencyInfo(context.getSource(),
                            StringArgumentType.getString(context, "currency")))))
                
                // /currency rates - Show exchange rates
                .then(Commands.literal("rates")
                    .executes(context -> showExchangeRates(context.getSource())))
                
                // /currency convert <amount> <from> <to> - Convert between currencies
                .then(Commands.literal("convert")
                    .then(Commands.argument("amount", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.01))
                        .then(Commands.argument("from_currency", StringArgumentType.string())
                            .suggests(TabCompletionUtil.CURRENCY_SUGGESTIONS)
                            .then(Commands.argument("to_currency", StringArgumentType.string())
                                .suggests(TabCompletionUtil.CURRENCY_SUGGESTIONS)
                                .executes(context -> convertCurrency(context.getSource(),
                                    com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "amount"),
                                    StringArgumentType.getString(context, "from_currency"),
                                    StringArgumentType.getString(context, "to_currency")))))))
                
                // Admin commands
                .then(Commands.literal("admin")
                    .requires(source -> CommandManager.hasPermission(source, "neoessentials.command.currency.admin"))
                    
                    // /currency admin create <id> <name> <symbol> [type]
                    .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.string())
                            .then(Commands.argument("name", StringArgumentType.string())
                                .then(Commands.argument("symbol", StringArgumentType.string())
                                    .executes(context -> createCurrency(context.getSource(),
                                        StringArgumentType.getString(context, "id"),
                                        StringArgumentType.getString(context, "name"),
                                        StringArgumentType.getString(context, "symbol"),
                                        "STANDARD"))
                                    .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("STANDARD");
                                            builder.suggest("RESOURCE_BACKED");
                                            builder.suggest("CRYPTO");
                                            builder.suggest("FIAT");
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> createCurrency(context.getSource(),
                                            StringArgumentType.getString(context, "id"),
                                            StringArgumentType.getString(context, "name"),
                                            StringArgumentType.getString(context, "symbol"),
                                            StringArgumentType.getString(context, "type"))))))))
                    
                    // /currency admin setrate <currency> <rate>
                    .then(Commands.literal("setrate")
                        .then(Commands.argument("currency", StringArgumentType.string())
                            .suggests(TabCompletionUtil.CURRENCY_SUGGESTIONS)
                            .then(Commands.argument("rate", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> setExchangeRate(context.getSource(),
                                    StringArgumentType.getString(context, "currency"),
                                    com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(context, "rate"))))))
                    
                    // /currency admin setdefault <currency>
                    .then(Commands.literal("setdefault")
                        .then(Commands.argument("currency", StringArgumentType.string())
                            .suggests(TabCompletionUtil.CURRENCY_SUGGESTIONS)
                            .executes(context -> setDefaultCurrency(context.getSource(),
                                StringArgumentType.getString(context, "currency"))))))
        );
    }
    
    private int showCurrencyHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== Currency Commands ===");
            MessageUtil.sendMessage(player, "§e/currency list §7- List all currencies");
            MessageUtil.sendMessage(player, "§e/currency info <currency> §7- Show currency details");
            MessageUtil.sendMessage(player, "§e/currency rates §7- Show exchange rates");
            MessageUtil.sendMessage(player, "§e/currency convert <amount> <from> <to> §7- Convert currencies");
            
            if (CommandManager.hasPermission(source, "neoessentials.command.currency.admin")) {
                MessageUtil.sendMessage(player, "");
                MessageUtil.sendMessage(player, "§6=== Admin Commands ===");
                MessageUtil.sendMessage(player, "§e/currency admin create <id> <name> <symbol> [type] §7- Create currency");
                MessageUtil.sendMessage(player, "§e/currency admin setrate <currency> <rate> §7- Set exchange rate");
                MessageUtil.sendMessage(player, "§e/currency admin setdefault <currency> §7- Set default currency");
            }
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use currency commands"));
            return 0;
        }
    }
    
    private int listCurrencies(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            CurrencyManager currencyManager = CurrencyManager.getInstance();
            List<Currency> currencies = new ArrayList<>(currencyManager.getAllCurrencies());
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            MessageUtil.sendMessage(player, "§6=== Available Currencies ===");
            
            if (currencies.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No currencies configured");
                return 1;
            }
            
            for (Currency currency : currencies) {
                String defaultMarker = currency.equals(defaultCurrency) ? " §a[DEFAULT]" : "";
                MessageUtil.sendMessage(player, String.format("§e%s §7(%s) - %s%s", 
                    currency.getDisplayName(), currency.getSymbol(), currency.getId(), defaultMarker));
                MessageUtil.sendMessage(player, String.format("  §7Type: §f%s §7| Physical: §f%s", 
                    currency.getType(), currency.isPhysical() ? "Yes" : "No"));
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use currency commands"));
            return 0;
        }
    }
    
    private int showCurrencyInfo(CommandSourceStack source, String currencyId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            CurrencyManager currencyManager = CurrencyManager.getInstance();
            Currency currency = currencyManager.getCurrency(currencyId);
            
            if (currency == null) {
                MessageUtil.sendMessage(player, "§cCurrency not found: " + currencyId);
                return 0;
            }
            
            MessageUtil.sendMessage(player, "§6=== Currency Information ===");
            MessageUtil.sendMessage(player, "§7ID: §e" + currency.getId());
            MessageUtil.sendMessage(player, "§7Name: §e" + currency.getDisplayName());
            MessageUtil.sendMessage(player, "§7Symbol: §e" + currency.getSymbol());
            MessageUtil.sendMessage(player, "§7Type: §e" + currency.getType());
            MessageUtil.sendMessage(player, "§7Physical: §e" + (currency.isPhysical() ? "Yes" : "No"));
            MessageUtil.sendMessage(player, "§7Exchange Rate: §e" + currency.getExchangeRate());
            
            // Show exchange rate if not default currency
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            if (!currency.equals(defaultCurrency)) {
                double rate = currencyManager.getExchangeRate(currency.getId());
                MessageUtil.sendMessage(player, String.format("§7Exchange Rate: §e%.4f %s per %s", 
                    rate, defaultCurrency.getSymbol(), currency.getSymbol()));
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use currency commands"));
            return 0;
        }
    }
    
    private int showExchangeRates(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            CurrencyManager currencyManager = CurrencyManager.getInstance();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            List<Currency> currencies = new ArrayList<>(currencyManager.getAllCurrencies());
            
            MessageUtil.sendMessage(player, "§6=== Exchange Rates ===");
            MessageUtil.sendMessage(player, "§7Base Currency: §e" + defaultCurrency.getDisplayName() + " (" + defaultCurrency.getSymbol() + ")");
            MessageUtil.sendMessage(player, "");
            
            for (Currency currency : currencies) {
                if (!currency.equals(defaultCurrency) && currency.isTradeable()) {
                    double rate = currencyManager.getExchangeRate(currency.getId());
                    MessageUtil.sendMessage(player, String.format("§e1 %s §7= §e%.4f %s", 
                        currency.getSymbol(), rate, defaultCurrency.getSymbol()));
                }
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use currency commands"));
            return 0;
        }
    }
    
    private int convertCurrency(CommandSourceStack source, double amount, String fromCurrencyId, String toCurrencyId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            CurrencyManager currencyManager = CurrencyManager.getInstance();
            
            Currency fromCurrency = currencyManager.getCurrency(fromCurrencyId);
            Currency toCurrency = currencyManager.getCurrency(toCurrencyId);
            
            if (fromCurrency == null) {
                MessageUtil.sendMessage(player, "§cFrom currency not found: " + fromCurrencyId);
                return 0;
            }
            
            if (toCurrency == null) {
                MessageUtil.sendMessage(player, "§cTo currency not found: " + toCurrencyId);
                return 0;
            }
            
            if (!fromCurrency.isTradeable() || !toCurrency.isTradeable()) {
                MessageUtil.sendMessage(player, "§cOne or both currencies are not tradeable");
                return 0;
            }
            
            double convertedAmount = currencyManager.convertCurrency(amount, fromCurrency, toCurrency);
            
            MessageUtil.sendMessage(player, "§6=== Currency Conversion ===");
            MessageUtil.sendMessage(player, String.format("§e%.2f %s §7= §e%.4f %s", 
                amount, fromCurrency.getSymbol(), convertedAmount, toCurrency.getSymbol()));
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use currency commands"));
            return 0;
        }
    }
    
    private int createCurrency(CommandSourceStack source, String id, String name, String symbol, String type) {
        try {
            CurrencyManager currencyManager = CurrencyManager.getInstance();
            
            // Check if currency already exists
            if (currencyManager.getCurrency(id) != null) {
                source.sendFailure(Component.literal("§cCurrency already exists: " + id));
                return 0;
            }
            
            Currency.CurrencyType currencyType;
            try {
                currencyType = Currency.CurrencyType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("§cInvalid currency type: " + type));
                return 0;
            }
            
            Currency newCurrency = new Currency(id, name, symbol, name + "s", true, true, 1.0, currencyType);
            currencyManager.registerCurrency(newCurrency);
            
            source.sendSuccess(() -> Component.literal("§aCurrency created successfully: " + name + " (" + symbol + ")"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError creating currency: " + e.getMessage()));
            return 0;
        }
    }
    
    private int setExchangeRate(CommandSourceStack source, String currencyId, double rate) {
        try {
            CurrencyManager currencyManager = CurrencyManager.getInstance();
            Currency currency = currencyManager.getCurrency(currencyId);
            
            if (currency == null) {
                source.sendFailure(Component.literal("§cCurrency not found: " + currencyId));
                return 0;
            }
            
            if (rate <= 0) {
                source.sendFailure(Component.literal("§cExchange rate must be positive"));
                return 0;
            }
            
            currencyManager.setExchangeRate(currencyId, rate);
            source.sendSuccess(() -> Component.literal(String.format("§aExchange rate set: 1 %s = %.4f %s", 
                currency.getSymbol(), rate, currencyManager.getDefaultCurrency().getSymbol())), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError setting exchange rate: " + e.getMessage()));
            return 0;
        }
    }
    
    private int setDefaultCurrency(CommandSourceStack source, String currencyId) {
        try {
            CurrencyManager currencyManager = CurrencyManager.getInstance();
            Currency currency = currencyManager.getCurrency(currencyId);
            
            if (currency == null) {
                source.sendFailure(Component.literal("§cCurrency not found: " + currencyId));
                return 0;
            }
            
            currencyManager.setDefaultCurrency(currency);
            source.sendSuccess(() -> Component.literal("§aDefault currency set to: " + currency.getDisplayName()), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError setting default currency: " + e.getMessage()));
            return 0;
        }
    }
}
