package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.util.LanguageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.ArrayList;

/**
 * Handles notifications for the loan system including overdue payments,
 * payment reminders, and loan status changes.
 */
public class LoanNotificationManager {
    private static LoanNotificationManager instance;
    
    // Store pending notifications for offline players
    private final Map<UUID, List<PendingNotification>> pendingNotifications = new ConcurrentHashMap<>();
    
    private LoanNotificationManager() {
    }
    
    public static LoanNotificationManager getInstance() {
        if (instance == null) {
            instance = new LoanNotificationManager();
        }
        return instance;
    }
    
    /**
     * Send notification to player about overdue loan
     */
    public void notifyLoanOverdue(Loan loan) {
        UUID playerId = loan.getBorrowerId();
        ServerPlayer player = getOnlinePlayer(playerId);
        
        if (player != null) {
            // Player is online - send immediate notification
            LanguageUtil.sendMessage(player, "§c⚠ LOAN OVERDUE NOTICE ⚠");
            LanguageUtil.sendMessage(player, "§7Your loan payment is overdue!");
            LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
            LanguageUtil.sendMessage(player, "§7Amount Due: §c" + loan.getCurrency().format(loan.getMonthlyPayment()));
            LanguageUtil.sendMessage(player, "§7Current Balance: §c" + loan.getCurrency().format(loan.getCurrentBalance()));
            LanguageUtil.sendMessage(player, "§7Use §a/loan pay <amount> §7to make a payment");
            LanguageUtil.sendMessage(player, "§c※ Late fees may apply for extended overdue periods");
        } else {
            // Player is offline - store notification for when player logs in
            storePendingNotification(playerId, PendingNotificationType.LOAN_OVERDUE, loan);
            System.out.println("[NeoEssentials] Loan overdue notification queued for player " + 
                playerId.toString().substring(0, 8) + " (offline)");
        }
    }
    
    /**
     * Send notification about late fee applied
     */
    public void notifyLateFeeApplied(Loan loan, double lateFee) {
        UUID playerId = loan.getBorrowerId();
        ServerPlayer player = getOnlinePlayer(playerId);
        
        if (player != null) {
            LanguageUtil.sendMessage(player, "§c💰 LATE FEE APPLIED");
            LanguageUtil.sendMessage(player, "§7A late fee of §c" + loan.getCurrency().format(lateFee) + 
                "§7 has been added to your loan");
            LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
            LanguageUtil.sendMessage(player, "§7New Balance: §c" + loan.getCurrency().format(loan.getCurrentBalance()));
            LanguageUtil.sendMessage(player, "§7Make a payment soon to avoid further penalties");
        } else {
            // Player is offline - store notification
            storePendingNotification(playerId, PendingNotificationType.LATE_FEE_APPLIED, loan, lateFee, null);
        }
    }
    
    /**
     * Send notification when loan moves to default status
     */
    public void notifyLoanDefault(Loan loan) {
        UUID playerId = loan.getBorrowerId();
        ServerPlayer player = getOnlinePlayer(playerId);
        
        if (player != null) {
            LanguageUtil.sendMessage(player, "§4⚠ LOAN DEFAULT NOTICE ⚠");
            LanguageUtil.sendMessage(player, "§cYour loan has been moved to DEFAULT status");
            LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
            LanguageUtil.sendMessage(player, "§7Outstanding Balance: §c" + loan.getCurrency().format(loan.getCurrentBalance()));
            LanguageUtil.sendMessage(player, "§c※ This will negatively impact your credit score");
            LanguageUtil.sendMessage(player, "§c※ Contact an administrator immediately");
        } else {
            // Player is offline - store notification
            storePendingNotification(playerId, PendingNotificationType.LOAN_DEFAULT, loan);
        }
    }
    
    /**
     * Send notification when loan is approved
     */
    public void notifyLoanApproved(Loan loan) {
        UUID playerId = loan.getBorrowerId();
        ServerPlayer player = getOnlinePlayer(playerId);
        
        if (player != null) {
            LanguageUtil.sendMessage(player, "§a✓ LOAN APPROVED!");
            LanguageUtil.sendMessage(player, "§7Your loan application has been approved");
            LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
            LanguageUtil.sendMessage(player, "§7Amount: §a" + loan.getCurrency().format(loan.getPrincipalAmount()));
            LanguageUtil.sendMessage(player, "§7Monthly Payment: §e" + loan.getCurrency().format(loan.getMonthlyPayment()));
            LanguageUtil.sendMessage(player, "§7Funds have been deposited to your primary account");
            LanguageUtil.sendMessage(player, "§a§lFirst payment due in 30 days!");
        } else {
            // Player is offline - store notification
            storePendingNotification(playerId, PendingNotificationType.LOAN_APPROVED, loan);
        }
    }
    
    /**
     * Send notification when loan is denied
     */
    public void notifyLoanDenied(Loan loan, String reason) {
        UUID playerId = loan.getBorrowerId();
        ServerPlayer player = getOnlinePlayer(playerId);
        
        if (player != null) {
            LanguageUtil.sendMessage(player, "§c✗ LOAN APPLICATION DENIED");
            LanguageUtil.sendMessage(player, "§7Your loan application has been denied");
            LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
            LanguageUtil.sendMessage(player, "§7Reason: §c" + reason);
            LanguageUtil.sendMessage(player, "§7You may apply for a different loan type or amount");
        } else {
            // Player is offline - store notification
            storePendingNotification(playerId, PendingNotificationType.LOAN_DENIED, loan, null, reason);
        }
    }
    
    /**
     * Send reminder about upcoming payment (called daily)
     */
    public void sendPaymentReminders() {
        try {
            BankManager bankManager = EconomyManager.getInstance().getBankManager();
            List<Loan> activeLoans = bankManager.getAllActiveLoans().stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.CURRENT)
                .toList();
            
            long currentTime = System.currentTimeMillis();
            long threeDaysInMs = 3L * 24L * 60L * 60L * 1000L;
            
            for (Loan loan : activeLoans) {
                long timeUntilDue = loan.getNextPaymentDueTime() - currentTime;
                
                // Send reminder if payment is due within 3 days
                if (timeUntilDue > 0 && timeUntilDue <= threeDaysInMs) {
                    sendPaymentReminder(loan, timeUntilDue);
                }
            }
        } catch (Exception e) {
            System.err.println("[NeoEssentials] Error sending payment reminders: " + e.getMessage());
        }
    }
    
    private void sendPaymentReminder(Loan loan, long timeUntilDue) {
        UUID playerId = loan.getBorrowerId();
        ServerPlayer player = getOnlinePlayer(playerId);
        
        if (player != null) {
            long daysUntilDue = timeUntilDue / (24L * 60L * 60L * 1000L);
            String timeText = daysUntilDue == 0 ? "today" : 
                             daysUntilDue == 1 ? "tomorrow" : 
                             "in " + daysUntilDue + " days";
            
            LanguageUtil.sendMessage(player, "§e💰 PAYMENT REMINDER");
            LanguageUtil.sendMessage(player, "§7Your loan payment is due §e" + timeText);
            LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
            LanguageUtil.sendMessage(player, "§7Payment Amount: §a" + loan.getCurrency().format(loan.getMonthlyPayment()));
            LanguageUtil.sendMessage(player, "§7Use §a/loan pay " + String.format("%.2f", loan.getMonthlyPayment()) + "§7 to pay");
        }
    }
    
    /**
     * Get online player by UUID
     */
    private ServerPlayer getOnlinePlayer(UUID playerId) {
        try {
            // Try to get the server instance through the economy manager
            // This is a simplified approach - in a real implementation you'd want
            // to store a reference to the server during initialization
            return null; // Placeholder - notifications will be console-only for now
        } catch (Exception e) {
            // Server not available or player not found
        }
        return null;
    }
    
    /**
     * Send welcome message when player joins with active loans
     */
    public void onPlayerJoin(ServerPlayer player) {
        try {
            UUID playerId = player.getUUID();
            
            // First, send any pending notifications
            sendPendingNotifications(player);
            
            BankManager bankManager = EconomyManager.getInstance().getBankManager();
            List<Loan> playerLoans = bankManager.getPlayerLoans(playerId);
            
            long overdueLoans = playerLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.LATE || loan.getStatus() == Loan.LoanStatus.DEFAULT)
                .count();
            
            long currentLoans = playerLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.CURRENT)
                .count();
            
            if (overdueLoans > 0) {
                LanguageUtil.sendMessage(player, "§c⚠ You have " + overdueLoans + " overdue loan(s)!");
                LanguageUtil.sendMessage(player, "§7Use §e/loan list §7to view your loans");
            } else if (currentLoans > 0) {
                LanguageUtil.sendMessage(player, "§a💰 Welcome back! You have " + currentLoans + " active loan(s)");
                LanguageUtil.sendMessage(player, "§7Use §e/loan list §7to view payment schedules");
            }
        } catch (Exception e) {
            // Silently fail - not critical
        }
    }
    
    /**
     * Store a pending notification for offline player
     */
    private void storePendingNotification(UUID playerId, PendingNotificationType type, Loan loan) {
        storePendingNotification(playerId, type, loan, null, null);
    }
    
    private void storePendingNotification(UUID playerId, PendingNotificationType type, Loan loan, Double amount, String reason) {
        PendingNotification notification = new PendingNotification(type, loan, amount, reason);
        pendingNotifications.computeIfAbsent(playerId, k -> new ArrayList<>()).add(notification);
    }
    
    /**
     * Send all pending notifications to a player who just joined
     */
    private void sendPendingNotifications(ServerPlayer player) {
        UUID playerId = player.getUUID();
        List<PendingNotification> notifications = pendingNotifications.remove(playerId);
        
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        
        // Send a summary first if there are many notifications
        if (notifications.size() > 3) {
            LanguageUtil.sendMessage(player, "§e⚠ You have " + notifications.size() + " loan notifications:");
        }
        
        for (PendingNotification notification : notifications) {
            sendNotificationToPlayer(player, notification);
        }
        
        if (notifications.size() > 1) {
            LanguageUtil.sendMessage(player, "§7Use §e/loan list §7to view all your loans");
        }
    }
    
    /**
     * Send a specific notification to a player
     */
    private void sendNotificationToPlayer(ServerPlayer player, PendingNotification notification) {
        Loan loan = notification.getLoan();
        
        switch (notification.getType()) {
            case LOAN_OVERDUE:
                LanguageUtil.sendMessage(player, "§c⚠ LOAN OVERDUE NOTICE ⚠");
                LanguageUtil.sendMessage(player, "§7Your loan payment was overdue while you were offline!");
                LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
                LanguageUtil.sendMessage(player, "§7Amount Due: §c" + loan.getCurrency().format(loan.getMonthlyPayment()));
                LanguageUtil.sendMessage(player, "§7Current Balance: §c" + loan.getCurrency().format(loan.getCurrentBalance()));
                break;
                
            case LATE_FEE_APPLIED:
                LanguageUtil.sendMessage(player, "§c💰 LATE FEE APPLIED (while offline)");
                LanguageUtil.sendMessage(player, "§7A late fee of §c" + loan.getCurrency().format(notification.getAmount()) + 
                    "§7 was added to your loan");
                LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
                break;
                
            case LOAN_DEFAULT:
                LanguageUtil.sendMessage(player, "§4⚠ LOAN DEFAULT NOTICE ⚠");
                LanguageUtil.sendMessage(player, "§cYour loan was moved to DEFAULT status while you were offline");
                LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
                LanguageUtil.sendMessage(player, "§c※ This negatively impacts your credit score");
                break;
                
            case LOAN_APPROVED:
                LanguageUtil.sendMessage(player, "§a✓ LOAN APPROVED! (while offline)");
                LanguageUtil.sendMessage(player, "§7Your loan application was approved");
                LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
                LanguageUtil.sendMessage(player, "§7Amount: §a" + loan.getCurrency().format(loan.getPrincipalAmount()));
                break;
                
            case LOAN_DENIED:
                LanguageUtil.sendMessage(player, "§c✗ LOAN APPLICATION DENIED (while offline)");
                LanguageUtil.sendMessage(player, "§7Your loan application was denied");
                LanguageUtil.sendMessage(player, "§7Loan ID: §e" + loan.getLoanId().toString().substring(0, 8));
                if (notification.getReason() != null) {
                    LanguageUtil.sendMessage(player, "§7Reason: §c" + notification.getReason());
                }
                break;
        }
    }
    
    /**
     * Clear old pending notifications (called periodically)
     */
    public void clearOldNotifications() {
        long cutoffTime = System.currentTimeMillis() - (7L * 24L * 60L * 60L * 1000L); // 7 days
        
        pendingNotifications.entrySet().removeIf(entry -> {
            List<PendingNotification> notifications = entry.getValue();
            notifications.removeIf(notification -> notification.getTimestamp() < cutoffTime);
            return notifications.isEmpty();
        });
    }
    
    /**
     * Enum for different types of pending notifications
     */
    private enum PendingNotificationType {
        LOAN_OVERDUE,
        LATE_FEE_APPLIED,
        LOAN_DEFAULT,
        LOAN_APPROVED,
        LOAN_DENIED
    }
    
    /**
     * Class to store pending notifications for offline players
     */
    private static class PendingNotification {
        private final PendingNotificationType type;
        private final Loan loan;
        private final Double amount; // For late fees
        private final String reason; // For denials
        private final long timestamp;
        
        public PendingNotification(PendingNotificationType type, Loan loan, Double amount, String reason) {
            this.type = type;
            this.loan = loan;
            this.amount = amount;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
        
        public PendingNotificationType getType() { return type; }
        public Loan getLoan() { return loan; }
        public Double getAmount() { return amount; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
    }
}
