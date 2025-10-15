package com.zerog.neoessentials.resourcepacks;

import java.time.Instant;
import java.util.*;

/**
 * Data model for a server resource pack.
 * Stores metadata, file information, and player assignments.
 */
public class ResourcePack {
    private String id;
    private String name;
    private String description;
    private String fileName;
    private String fileHash; // SHA-1 hash
    private long fileSize;
    private String url; // External URL or local path
    private boolean isExternal; // True if hosted externally, false if local
    private PackMetadata metadata; // Parsed from pack.mcmeta
    private byte[] iconData; // Preview icon (pack.png)
    private Instant uploadedAt;
    private String uploadedBy;
    private boolean isActive; // Currently active on server
    private EnforcementMode enforcementMode;
    private Set<String> assignedPlayers; // UUIDs
    private Set<String> assignedGroups; // Group names
    
    public enum EnforcementMode {
        OPTIONAL,    // Player can decline
        REQUIRED,    // Player must accept to join
        FORCED       // Automatically applied, no prompt
    }
    
    public static class PackMetadata {
        private int packFormat;
        private String description;
        private Map<String, Object> additionalData;
        
        public PackMetadata() {
            this.additionalData = new HashMap<>();
        }
        
        public int getPackFormat() {
            return packFormat;
        }
        
        public void setPackFormat(int packFormat) {
            this.packFormat = packFormat;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Map<String, Object> getAdditionalData() {
            return additionalData;
        }
        
        public void setAdditionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
        }
    }
    
    public ResourcePack() {
        this.id = UUID.randomUUID().toString();
        this.uploadedAt = Instant.now();
        this.assignedPlayers = new HashSet<>();
        this.assignedGroups = new HashSet<>();
        this.enforcementMode = EnforcementMode.OPTIONAL;
        this.isActive = false;
        this.isExternal = false;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileHash() {
        return fileHash;
    }
    
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public boolean isExternal() {
        return isExternal;
    }
    
    public void setExternal(boolean external) {
        isExternal = external;
    }
    
    public PackMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(PackMetadata metadata) {
        this.metadata = metadata;
    }
    
    public byte[] getIconData() {
        return iconData;
    }
    
    public void setIconData(byte[] iconData) {
        this.iconData = iconData;
    }
    
    public Instant getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public String getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public EnforcementMode getEnforcementMode() {
        return enforcementMode;
    }
    
    public void setEnforcementMode(EnforcementMode enforcementMode) {
        this.enforcementMode = enforcementMode;
    }
    
    public Set<String> getAssignedPlayers() {
        return assignedPlayers;
    }
    
    public void setAssignedPlayers(Set<String> assignedPlayers) {
        this.assignedPlayers = assignedPlayers;
    }
    
    public Set<String> getAssignedGroups() {
        return assignedGroups;
    }
    
    public void setAssignedGroups(Set<String> assignedGroups) {
        this.assignedGroups = assignedGroups;
    }
    
    public void addAssignedPlayer(String playerUuid) {
        this.assignedPlayers.add(playerUuid);
    }
    
    public void removeAssignedPlayer(String playerUuid) {
        this.assignedPlayers.remove(playerUuid);
    }
    
    public void addAssignedGroup(String groupName) {
        this.assignedGroups.add(groupName);
    }
    
    public void removeAssignedGroup(String groupName) {
        this.assignedGroups.remove(groupName);
    }
    
    public boolean isAssignedToPlayer(String playerUuid) {
        return assignedPlayers.contains(playerUuid);
    }
}
