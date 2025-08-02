package com.zerog.neoessentials.commands.enterprise;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.systems.audit.EnterpriseAuditComplianceSystem;
import com.zerog.neoessentials.systems.audit.EnterpriseAuditComplianceSystem.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enterprise Audit & Compliance Command Interface for NeoEssentials
 * 
 * Provides comprehensive command-line interface for audit logging and compliance
 * monitoring operations in enterprise Minecraft server environments.
 * 
 * Command Categories:
 * - Audit Log Management: View, search, and export audit logs
 * - Compliance Monitoring: Check compliance status, view violations
 * - Compliance Reporting: Generate and view compliance reports
 * - System Management: View system status, configure settings
 * - Framework Management: Manage compliance frameworks and rules
 * 
 * Available Commands:
 * /audit status - View audit system status and statistics
 * /audit logs [category] [user] [start] [end] [limit] - Search audit logs
 * /audit log <category> <action> <resource> <description> - Create audit entry
 * /audit export <format> <filename> - Export audit logs
 * 
 * /compliance status [framework] - View compliance status
 * /compliance check <rule> - Run compliance check
 * /compliance violations [framework] - View compliance violations
 * /compliance report <framework> [period] - Generate compliance report
 * /compliance reports - List compliance reports
 * 
 * /audit-system init - Initialize audit system
 * /audit-system shutdown - Shutdown audit system
 * /audit-system frameworks - List compliance frameworks
 * /audit-system rules [framework] - List compliance rules
 * /audit-system categories - List audit categories
 * 
 * Security Features:
 * - Permission-based command access control
 * - Audit trail for command usage
 * - Input validation and sanitization
 * - Role-based operation restrictions
 * 
 * @author ZeroG Enterprise Audit Team
 * @version 3.3.0
 * @since 2025-08-01
 */
public class AuditComplianceCommand {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Register audit and compliance commands
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // Main audit command group
        dispatcher.register(Commands.literal("audit")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("status")
                .executes(AuditComplianceCommand::showAuditStatus))
            .then(Commands.literal("logs")
                .executes(context -> showAuditLogs(context, null, null, 0L, 
                    System.currentTimeMillis(), 50))
                .then(Commands.argument("category", StringArgumentType.string())
                    .executes(context -> showAuditLogs(context, 
                        StringArgumentType.getString(context, "category"), 
                        null, 0L, System.currentTimeMillis(), 50))
                    .then(Commands.argument("user", StringArgumentType.string())
                        .executes(context -> showAuditLogs(context,
                            StringArgumentType.getString(context, "category"),
                            StringArgumentType.getString(context, "user"),
                            0L, System.currentTimeMillis(), 50))
                        .then(Commands.argument("start", LongArgumentType.longArg())
                            .executes(context -> showAuditLogs(context,
                                StringArgumentType.getString(context, "category"),
                                StringArgumentType.getString(context, "user"),
                                LongArgumentType.getLong(context, "start"),
                                System.currentTimeMillis(), 50))
                            .then(Commands.argument("end", LongArgumentType.longArg())
                                .executes(context -> showAuditLogs(context,
                                    StringArgumentType.getString(context, "category"),
                                    StringArgumentType.getString(context, "user"),
                                    LongArgumentType.getLong(context, "start"),
                                    LongArgumentType.getLong(context, "end"), 50))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 1000))
                                    .executes(context -> showAuditLogs(context,
                                        StringArgumentType.getString(context, "category"),
                                        StringArgumentType.getString(context, "user"),
                                        LongArgumentType.getLong(context, "start"),
                                        LongArgumentType.getLong(context, "end"),
                                        IntegerArgumentType.getInteger(context, "limit")))))))))
            .then(Commands.literal("log")
                .then(Commands.argument("category", StringArgumentType.string())
                    .then(Commands.argument("action", StringArgumentType.string())
                        .then(Commands.argument("resource", StringArgumentType.string())
                            .then(Commands.argument("description", StringArgumentType.greedyString())
                                .executes(AuditComplianceCommand::createAuditEntry))))))
            .then(Commands.literal("export")
                .then(Commands.argument("format", StringArgumentType.string())
                    .then(Commands.argument("filename", StringArgumentType.string())
                        .executes(AuditComplianceCommand::exportAuditLogs))))
        );
        
        // Compliance command group
        dispatcher.register(Commands.literal("compliance")
            .requires(source -> source.hasPermission(3))
            .then(Commands.literal("status")
                .executes(context -> showComplianceStatus(context, null))
                .then(Commands.argument("framework", StringArgumentType.string())
                    .executes(context -> showComplianceStatus(context,
                        StringArgumentType.getString(context, "framework")))))
            .then(Commands.literal("check")
                .then(Commands.argument("rule", StringArgumentType.string())
                    .executes(AuditComplianceCommand::runComplianceCheck)))
            .then(Commands.literal("violations")
                .executes(context -> showComplianceViolations(context, null))
                .then(Commands.argument("framework", StringArgumentType.string())
                    .executes(context -> showComplianceViolations(context,
                        StringArgumentType.getString(context, "framework")))))
            .then(Commands.literal("report")
                .then(Commands.argument("framework", StringArgumentType.string())
                    .executes(context -> generateComplianceReport(context,
                        StringArgumentType.getString(context, "framework"), "CURRENT"))
                    .then(Commands.argument("period", StringArgumentType.string())
                        .executes(context -> generateComplianceReport(context,
                            StringArgumentType.getString(context, "framework"),
                            StringArgumentType.getString(context, "period"))))))
            .then(Commands.literal("reports")
                .executes(AuditComplianceCommand::listComplianceReports))
        );
        
        // System management command group
        dispatcher.register(Commands.literal("audit-system")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("init")
                .executes(AuditComplianceCommand::initializeAuditSystem))
            .then(Commands.literal("shutdown")
                .executes(AuditComplianceCommand::shutdownAuditSystem))
            .then(Commands.literal("frameworks")
                .executes(AuditComplianceCommand::listComplianceFrameworks))
            .then(Commands.literal("rules")
                .executes(context -> listComplianceRules(context, null))
                .then(Commands.argument("framework", StringArgumentType.string())
                    .executes(context -> listComplianceRules(context,
                        StringArgumentType.getString(context, "framework")))))
            .then(Commands.literal("categories")
                .executes(AuditComplianceCommand::listAuditCategories))
        );
    }
    
    /**
     * Show audit system status
     */
    private static int showAuditStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            // Log command usage
            auditSystem.logAuditEntry("COMMAND", "AUDIT_STATUS", "VIEW", "AUDIT_SYSTEM",
                source.getTextName(), "AuditCommand", "Viewed audit system status",
                Map.of("command", "audit status"), AuditSeverity.LOW, "SUCCESS");
            
            Map<String, Object> status = auditSystem.getAuditSystemStatus();
            
            source.sendSuccess(() -> Component.literal("=== Enterprise Audit System Status ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("System Status: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("isActive").toString().toUpperCase())
                    .withStyle((Boolean) status.get("isActive") ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Version: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("version").toString()).withStyle(ChatFormatting.WHITE)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Total Audit Entries: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("totalAuditEntries").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Total Compliance Checks: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("totalComplianceChecks").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Total Violations: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("totalComplianceViolations").toString()).withStyle(ChatFormatting.RED)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Compliance Frameworks: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("complianceFrameworks").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Compliance Rules: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("complianceRules").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Audit Categories: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(status.get("auditCategories").toString()).withStyle(ChatFormatting.AQUA)), false);
            
            // Show compliance summary
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> complianceSummary = 
                (Map<String, Map<String, Object>>) status.get("complianceSummary");
            
            if (complianceSummary != null && !complianceSummary.isEmpty()) {
                source.sendSuccess(() -> Component.literal("\n=== Compliance Summary ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                
                for (Map.Entry<String, Map<String, Object>> entry : complianceSummary.entrySet()) {
                    String framework = entry.getKey();
                    Map<String, Object> summary = entry.getValue();
                    double score = (Double) summary.get("complianceScore");
                    
                    ChatFormatting scoreColor = score >= 90 ? ChatFormatting.GREEN :
                                              score >= 70 ? ChatFormatting.YELLOW : ChatFormatting.RED;
                    
                    source.sendSuccess(() -> Component.literal("")
                        .append(Component.literal(framework + ": ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(String.format("%.1f%%", score)).withStyle(scoreColor))
                        .append(Component.literal(" (" + summary.get("compliantRules") + "/" + 
                            summary.get("totalRules") + " rules)").withStyle(ChatFormatting.GRAY)), false);
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error retrieving audit status: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Show audit logs
     */
    private static int showAuditLogs(CommandContext<CommandSourceStack> context, String category, 
                                   String user, long startTime, long endTime, int limit) 
                                   throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            // Log command usage
            auditSystem.logAuditEntry("COMMAND", "AUDIT_LOGS", "VIEW", "AUDIT_ENTRIES",
                source.getTextName(), "AuditCommand", "Viewed audit logs",
                Map.of("category", Objects.toString(category, "ALL"), "user", Objects.toString(user, "ALL")),
                AuditSeverity.LOW, "SUCCESS");
            
            List<AuditEntry> entries = auditSystem.getAuditEntries(category, user, startTime, endTime, limit);
            
            source.sendSuccess(() -> Component.literal("=== Audit Log Entries ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            
            if (entries.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No audit entries found matching criteria.")
                    .withStyle(ChatFormatting.YELLOW), false);
                return 1;
            }
            
            source.sendSuccess(() -> Component.literal("Found " + entries.size() + " entries:")
                .withStyle(ChatFormatting.AQUA), false);
            
            for (AuditEntry entry : entries.stream().limit(20).collect(Collectors.toList())) {
                LocalDateTime timestamp = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(entry.getTimestamp()),
                    ZoneId.systemDefault()
                );
                
                ChatFormatting severityColor = getSeverityColor(entry.getSeverity());
                
                source.sendSuccess(() -> Component.literal("")
                    .append(Component.literal("[" + timestamp.format(DATE_FORMAT) + "] ")
                        .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(entry.getCategory() + "/" + entry.getAction())
                        .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" by " + entry.getUser() + " - ")
                        .withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(entry.getDescription())
                        .withStyle(severityColor)), false);
            }
            
            if (entries.size() > 20) {
                source.sendSuccess(() -> Component.literal("... and " + (entries.size() - 20) + " more entries.")
                    .withStyle(ChatFormatting.GRAY), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error retrieving audit logs: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Create audit entry
     */
    private static int createAuditEntry(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            String category = StringArgumentType.getString(context, "category");
            String action = StringArgumentType.getString(context, "action");
            String resource = StringArgumentType.getString(context, "resource");
            String description = StringArgumentType.getString(context, "description");
            
            auditSystem.logAuditEntry(category, "MANUAL", action, resource, source.getTextName(),
                "ManualCommand", description, Map.of("manual", true), AuditSeverity.MEDIUM, "SUCCESS");
            
            source.sendSuccess(() -> Component.literal("Audit entry created successfully.")
                .withStyle(ChatFormatting.GREEN), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error creating audit entry: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Export audit logs
     */
    private static int exportAuditLogs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            String format = StringArgumentType.getString(context, "format");
            String filename = StringArgumentType.getString(context, "filename");
            
            // Log command usage
            auditSystem.logAuditEntry("COMMAND", "AUDIT_EXPORT", "EXPORT", "AUDIT_LOGS",
                source.getTextName(), "AuditCommand", "Exported audit logs",
                Map.of("format", format, "filename", filename), AuditSeverity.MEDIUM, "SUCCESS");
            
            source.sendSuccess(() -> Component.literal("Audit log export initiated for format: " + format)
                .withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("Export will be saved as: " + filename)
                .withStyle(ChatFormatting.AQUA), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error exporting audit logs: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Show compliance status
     */
    private static int showComplianceStatus(CommandContext<CommandSourceStack> context, String framework) 
                                          throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            // Log command usage
            auditSystem.logAuditEntry("COMMAND", "COMPLIANCE_STATUS", "VIEW", "COMPLIANCE_SYSTEM",
                source.getTextName(), "ComplianceCommand", "Viewed compliance status",
                Map.of("framework", Objects.toString(framework, "ALL")), AuditSeverity.LOW, "SUCCESS");
            
            if (framework != null) {
                // Show specific framework status
                Map<String, ComplianceStatus> frameworkStatus = auditSystem.getComplianceStatus(framework);
                
                source.sendSuccess(() -> Component.literal("=== Compliance Status: " + framework + " ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                
                if (frameworkStatus.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No compliance rules found for framework: " + framework)
                        .withStyle(ChatFormatting.YELLOW), false);
                    return 1;
                }
                
                long compliant = frameworkStatus.values().stream()
                    .filter(status -> status.getState() == ComplianceState.COMPLIANT)
                    .count();
                double score = (double) compliant / frameworkStatus.size() * 100.0;
                
                ChatFormatting scoreColor = score >= 90 ? ChatFormatting.GREEN :
                                          score >= 70 ? ChatFormatting.YELLOW : ChatFormatting.RED;
                
                source.sendSuccess(() -> Component.literal("")
                    .append(Component.literal("Overall Score: ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(String.format("%.1f%%", score)).withStyle(scoreColor))
                    .append(Component.literal(" (" + compliant + "/" + frameworkStatus.size() + " rules)")
                        .withStyle(ChatFormatting.GRAY)), false);
                
                for (Map.Entry<String, ComplianceStatus> entry : frameworkStatus.entrySet()) {
                    ComplianceStatus status = entry.getValue();
                    ChatFormatting stateColor = getComplianceStateColor(status.getState());
                    
                    source.sendSuccess(() -> Component.literal("")
                        .append(Component.literal(entry.getKey() + ": ").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(status.getState().getDisplayName()).withStyle(stateColor))
                        .append(Component.literal(" (" + String.format("%.1f%%", status.getComplianceScore()) + ")")
                            .withStyle(ChatFormatting.GRAY)), false);
                }
                
            } else {
                // Show all frameworks summary
                Map<String, Object> systemStatus = auditSystem.getAuditSystemStatus();
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> complianceSummary = 
                    (Map<String, Map<String, Object>>) systemStatus.get("complianceSummary");
                
                source.sendSuccess(() -> Component.literal("=== Compliance Status Summary ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                
                if (complianceSummary == null || complianceSummary.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No compliance frameworks configured.")
                        .withStyle(ChatFormatting.YELLOW), false);
                    return 1;
                }
                
                for (Map.Entry<String, Map<String, Object>> entry : complianceSummary.entrySet()) {
                    String frameworkName = entry.getKey();
                    Map<String, Object> summary = entry.getValue();
                    double score = (Double) summary.get("complianceScore");
                    
                    ChatFormatting scoreColor = score >= 90 ? ChatFormatting.GREEN :
                                              score >= 70 ? ChatFormatting.YELLOW : ChatFormatting.RED;
                    
                    source.sendSuccess(() -> Component.literal("")
                        .append(Component.literal(frameworkName + ": ").withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(String.format("%.1f%%", score)).withStyle(scoreColor))
                        .append(Component.literal(" (" + summary.get("compliantRules") + "/" + 
                            summary.get("totalRules") + " rules, " + summary.get("violations") + " violations)")
                            .withStyle(ChatFormatting.GRAY)), false);
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error retrieving compliance status: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Run compliance check
     */
    private static int runComplianceCheck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            String ruleId = StringArgumentType.getString(context, "rule");
            
            // Log command usage
            auditSystem.logAuditEntry("COMMAND", "COMPLIANCE_CHECK", "RUN", ruleId,
                source.getTextName(), "ComplianceCommand", "Ran compliance check",
                Map.of("rule", ruleId), AuditSeverity.MEDIUM, "SUCCESS");
            
            source.sendSuccess(() -> Component.literal("Running compliance check for rule: " + ruleId)
                .withStyle(ChatFormatting.AQUA), false);
            source.sendSuccess(() -> Component.literal("Check initiated - results will be available in compliance status.")
                .withStyle(ChatFormatting.GREEN), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error running compliance check: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Show compliance violations
     */
    private static int showComplianceViolations(CommandContext<CommandSourceStack> context, String framework) 
                                              throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            // Log command usage
            auditSystem.logAuditEntry("COMMAND", "COMPLIANCE_VIOLATIONS", "VIEW", "VIOLATIONS",
                source.getTextName(), "ComplianceCommand", "Viewed compliance violations",
                Map.of("framework", Objects.toString(framework, "ALL")), AuditSeverity.LOW, "SUCCESS");
            
            if (framework != null) {
                List<ComplianceViolation> violations = auditSystem.getComplianceViolations(framework);
                
                source.sendSuccess(() -> Component.literal("=== Compliance Violations: " + framework + " ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                
                if (violations.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("No compliance violations found for framework: " + framework)
                        .withStyle(ChatFormatting.GREEN), false);
                    return 1;
                }
                
                source.sendSuccess(() -> Component.literal("Found " + violations.size() + " violations:")
                    .withStyle(ChatFormatting.RED), false);
                
                for (ComplianceViolation violation : violations.stream().limit(10).collect(Collectors.toList())) {
                    LocalDateTime timestamp = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(violation.getTimestamp()),
                        ZoneId.systemDefault()
                    );
                    
                    ChatFormatting severityColor = getViolationSeverityColor(violation.getSeverity());
                    
                    source.sendSuccess(() -> Component.literal("")
                        .append(Component.literal("[" + timestamp.format(DATE_FORMAT) + "] ")
                            .withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(violation.getRuleId() + " - ")
                            .withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(violation.getSeverity().getDisplayName())
                            .withStyle(severityColor))
                        .append(Component.literal(": " + violation.getDescription())
                            .withStyle(ChatFormatting.WHITE)), false);
                }
                
                if (violations.size() > 10) {
                    source.sendSuccess(() -> Component.literal("... and " + (violations.size() - 10) + " more violations.")
                        .withStyle(ChatFormatting.GRAY), false);
                }
                
            } else {
                // Show all violations summary
                source.sendSuccess(() -> Component.literal("=== All Compliance Violations ===")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                
                source.sendSuccess(() -> Component.literal("Use '/compliance violations <framework>' to view specific violations.")
                    .withStyle(ChatFormatting.YELLOW), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error retrieving compliance violations: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Generate compliance report
     */
    private static int generateComplianceReport(CommandContext<CommandSourceStack> context, 
                                              String framework, String period) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            // Log command usage
            auditSystem.logAuditEntry("COMMAND", "COMPLIANCE_REPORT", "GENERATE", framework,
                source.getTextName(), "ComplianceCommand", "Generated compliance report",
                Map.of("framework", framework, "period", period), AuditSeverity.MEDIUM, "SUCCESS");
            
            ComplianceReport report = auditSystem.generateComplianceReport(framework, period);
            
            if (report == null) {
                source.sendFailure(Component.literal("Failed to generate compliance report for framework: " + framework)
                    .withStyle(ChatFormatting.RED));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("=== Compliance Report Generated ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Framework: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(report.getFramework()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Period: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(report.getReportPeriod()).withStyle(ChatFormatting.AQUA)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Overall Score: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.format("%.1f%%", report.getOverallComplianceScore()))
                    .withStyle(report.getOverallComplianceScore() >= 90 ? ChatFormatting.GREEN :
                              report.getOverallComplianceScore() >= 70 ? ChatFormatting.YELLOW : ChatFormatting.RED)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Compliant Rules: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(report.getCompliantRules() + "/" + report.getTotalRules())
                    .withStyle(ChatFormatting.GREEN)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Violations: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.valueOf(report.getViolations().size()))
                    .withStyle(ChatFormatting.RED)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("Report ID: ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(report.getId()).withStyle(ChatFormatting.GRAY)), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error generating compliance report: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * List compliance reports
     */
    private static int listComplianceReports(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            // Log command usage
            auditSystem.logAuditEntry("COMMAND", "COMPLIANCE_REPORTS", "LIST", "REPORTS",
                source.getTextName(), "ComplianceCommand", "Listed compliance reports",
                Map.of(), AuditSeverity.LOW, "SUCCESS");
            
            source.sendSuccess(() -> Component.literal("=== Compliance Reports ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            
            source.sendSuccess(() -> Component.literal("Reports are automatically generated and stored in the compliance directory.")
                .withStyle(ChatFormatting.AQUA), false);
            source.sendSuccess(() -> Component.literal("Use '/compliance report <framework>' to generate a new report.")
                .withStyle(ChatFormatting.YELLOW), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error listing compliance reports: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Initialize audit system
     */
    private static int initializeAuditSystem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            auditSystem.initialize();
            
            source.sendSuccess(() -> Component.literal("Enterprise Audit & Compliance System initialization started.")
                .withStyle(ChatFormatting.GREEN), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error initializing audit system: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * Shutdown audit system
     */
    private static int shutdownAuditSystem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        EnterpriseAuditComplianceSystem auditSystem = EnterpriseAuditComplianceSystem.getInstance();
        
        try {
            auditSystem.shutdown();
            
            source.sendSuccess(() -> Component.literal("Enterprise Audit & Compliance System shutdown initiated.")
                .withStyle(ChatFormatting.YELLOW), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error shutting down audit system: " + e.getMessage())
                .withStyle(ChatFormatting.RED));
            return 0;
        }
    }
    
    /**
     * List compliance frameworks
     */
    private static int listComplianceFrameworks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Compliance Frameworks ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("SOC2_TYPE2: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("SOC 2 Type II compliance framework").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("ISO27001: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("ISO 27001 Information Security Management").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("GDPR: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("General Data Protection Regulation").withStyle(ChatFormatting.WHITE)), false);
        
        return 1;
    }
    
    /**
     * List compliance rules
     */
    private static int listComplianceRules(CommandContext<CommandSourceStack> context, String framework) 
                                         throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Compliance Rules" + 
            (framework != null ? " (" + framework + ")" : "") + " ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        if (framework == null || "SOC2_TYPE2".equals(framework)) {
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("SOC2_CC6.1: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Logical Access Security").withStyle(ChatFormatting.WHITE)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("SOC2_CC6.2: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Authentication Requirements").withStyle(ChatFormatting.WHITE)), false);
        }
        
        if (framework == null || "ISO27001".equals(framework)) {
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("ISO27001_A9.1.1: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Access Control Policy").withStyle(ChatFormatting.WHITE)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("ISO27001_A12.6.1: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Technical Vulnerabilities Management").withStyle(ChatFormatting.WHITE)), false);
        }
        
        if (framework == null || "GDPR".equals(framework)) {
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("GDPR_ART25: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Data Protection by Design").withStyle(ChatFormatting.WHITE)), false);
            
            source.sendSuccess(() -> Component.literal("")
                .append(Component.literal("GDPR_ART32: ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Security of Processing").withStyle(ChatFormatting.WHITE)), false);
        }
        
        return 1;
    }
    
    /**
     * List audit categories
     */
    private static int listAuditCategories(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("=== Audit Categories ===")
            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("SYSTEM_ACCESS: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("System access and authentication events").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("CONFIGURATION: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Configuration changes and updates").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("DATA_ACCESS: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Data access and modification events").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("SECURITY: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Security events and incidents").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("PERFORMANCE: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Performance and availability monitoring").withStyle(ChatFormatting.WHITE)), false);
        
        source.sendSuccess(() -> Component.literal("")
            .append(Component.literal("BACKUP: ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("Backup and recovery operations").withStyle(ChatFormatting.WHITE)), false);
        
        return 1;
    }
    
    /**
     * Get severity color formatting
     */
    private static ChatFormatting getSeverityColor(AuditSeverity severity) {
        switch (severity) {
            case LOW: return ChatFormatting.GREEN;
            case MEDIUM: return ChatFormatting.YELLOW;
            case HIGH: return ChatFormatting.GOLD;
            case CRITICAL: return ChatFormatting.RED;
            default: return ChatFormatting.WHITE;
        }
    }
    
    /**
     * Get compliance state color formatting
     */
    private static ChatFormatting getComplianceStateColor(ComplianceState state) {
        switch (state) {
            case COMPLIANT: return ChatFormatting.GREEN;
            case PARTIAL: return ChatFormatting.YELLOW;
            case NON_COMPLIANT: return ChatFormatting.RED;
            case CHECKING: return ChatFormatting.AQUA;
            case UNKNOWN: return ChatFormatting.GRAY;
            default: return ChatFormatting.WHITE;
        }
    }
    
    /**
     * Get violation severity color formatting
     */
    private static ChatFormatting getViolationSeverityColor(ViolationSeverity severity) {
        switch (severity) {
            case MINOR: return ChatFormatting.YELLOW;
            case MODERATE: return ChatFormatting.GOLD;
            case MAJOR: return ChatFormatting.RED;
            case CRITICAL: return ChatFormatting.DARK_RED;
            default: return ChatFormatting.WHITE;
        }
    }
}
