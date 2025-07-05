package com.zerog.neoessentials.economy.storage;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the results of storage data validation
 */
public class StorageValidationReport {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final long validationTime;
    
    public StorageValidationReport(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
        this.validationTime = System.currentTimeMillis();
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }
    
    public long getValidationTime() {
        return validationTime;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StorageValidationReport{valid=").append(valid);
        if (hasErrors()) {
            sb.append(", errors=").append(errors.size());
        }
        if (hasWarnings()) {
            sb.append(", warnings=").append(warnings.size());
        }
        sb.append("}");
        return sb.toString();
    }
}
