package com.zerog.neoessentials.economy;

import java.io.FileReader;
import java.math.BigDecimal;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class EconomyConfig {
    public BigDecimal startingBalance = new BigDecimal("100.0");
    public String currencySymbol = "$";
    public BigDecimal maxBalance = new BigDecimal("100000.0");
    public boolean cleanupInactiveAccounts = true;
    public double taxPercentage = 1.5;
    public BigDecimal maxTransferAmount = new BigDecimal("10000.0");
    public boolean paytoggleDefault = true;
    public boolean allowNegativeBalances = false;
    public int inactiveAccountCleanupDays = 30; // Number of days before an inactive account is cleaned up

    public static EconomyConfig load(File configFile) {
        // If the file does not exist, copy the default from resources
        if (!configFile.exists()) {
            try {
                File parent = configFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                try (InputStream in = EconomyConfig.class.getClassLoader().getResourceAsStream("assets/neoessentials/economy.json")) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(configFile)) {
                            byte[] buf = new byte[4096];
                            int len;
                            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        EconomyConfig config = new EconomyConfig();
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject econ = root.getAsJsonObject("economySettings");
            if (econ != null) {
                if (econ.has("startingBalance")) config.startingBalance = econ.get("startingBalance").getAsBigDecimal();
                if (econ.has("currencySymbol")) config.currencySymbol = econ.get("currencySymbol").getAsString();
                if (econ.has("maxBalance")) config.maxBalance = econ.get("maxBalance").getAsBigDecimal();
                if (econ.has("cleanupInactiveAccounts")) config.cleanupInactiveAccounts = econ.get("cleanupInactiveAccounts").getAsBoolean();
                if (econ.has("taxPercentage")) config.taxPercentage = econ.get("taxPercentage").getAsDouble();
                if (econ.has("maxTransferAmount")) config.maxTransferAmount = econ.get("maxTransferAmount").getAsBigDecimal();
                if (econ.has("paytoggleDefault")) config.paytoggleDefault = econ.get("paytoggleDefault").getAsBoolean();
                if (econ.has("allowNegativeBalances")) config.allowNegativeBalances = econ.get("allowNegativeBalances").getAsBoolean();
                if (econ.has("inactiveAccountCleanupDays")) config.inactiveAccountCleanupDays = econ.get("inactiveAccountCleanupDays").getAsInt();
            }
        } catch (Exception e) {
            // Log error and use defaults
            e.printStackTrace();
        }
        return config;
    }
}