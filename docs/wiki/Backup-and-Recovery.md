# Backup and Recovery

This guide covers how to properly back up your NeoForge server with NeoEssentials and recover from potential issues.

## Why Backups Matter

Regular backups are crucial for:

- Recovering from server crashes
- Mitigating corruption issues
- Resolving player disputes
- Testing configuration changes safely
- Migrating to new server hardware

## Automatic Backups with NeoEssentials

NeoEssentials includes a built-in backup system that can be configured in `config/neoessentials/config.toml`:

```toml
[backup]
enabled = true
interval = 360  # Minutes between backups (6 hours)
maxBackups = 10  # Maximum number of backups to keep
compressionLevel = 7  # ZIP compression level (1-9)
backupPath = "backups"  # Relative to server directory
excludeFiles = ["cache", "logs", "crash-reports"]
includePluginData = true
backupOnShutdown = true
notifyAdmins = true
```

## Manual Backup Commands

NeoEssentials provides commands for managing backups:

- `/neoessentials:backup create [name]` - Create a backup with optional name
- `/neoessentials:backup list` - List available backups
- `/neoessentials:backup restore <name>` - Restore a specific backup
- `/neoessentials:backup delete <name>` - Delete a specific backup
- `/neoessentials:backup info <name>` - Show backup details

## What Gets Backed Up

By default, NeoEssentials backs up:

- World data
- NeoEssentials configuration and data
- Player data
- Server configuration files
- Mod configurations

## External Backup Solutions

For more robust backup strategies:

### Windows Scripts

Create a batch file (e.g., `backup.bat`):

```batch
@echo off
echo Starting backup at %time% on %date%
set BACKUP_NAME=server_backup_%date:~10,4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%
set BACKUP_PATH=D:\Server_Backups\%BACKUP_NAME%

:: Stop server if running
echo Stopping server...
taskkill /f /im java.exe

:: Wait a moment
timeout /t 5

:: Create backup directory
mkdir %BACKUP_PATH%

:: Copy server files
echo Copying files...
xcopy /E /I /H /Y "C:\MinecraftServer\*" "%BACKUP_PATH%"

:: Start server
echo Starting server...
start "" "C:\MinecraftServer\start.bat"

echo Backup completed successfully!
```

### Linux Scripts

Create a bash script (e.g., `backup.sh`):

```bash
#!/bin/bash

# Configuration
SERVER_DIR="/home/minecraft/server"
BACKUP_DIR="/mnt/backups/minecraft"
DATE=$(date +"%Y-%m-%d_%H-%M")
BACKUP_FILE="minecraft_backup_$DATE.tar.gz"

# Announce backup starting
screen -S minecraft -X stuff "say Server backup starting in 10 seconds. Expect brief lag.^M"
sleep 10

# Save world and disable saving
screen -S minecraft -X stuff "save-all^M"
screen -S minecraft -X stuff "save-off^M"
sleep 5

# Create backup
echo "Creating backup: $BACKUP_FILE"
tar -czf "$BACKUP_DIR/$BACKUP_FILE" -C "$SERVER_DIR" .

# Enable saving
screen -S minecraft -X stuff "save-on^M"

# Announce completion
screen -S minecraft -X stuff "say Backup complete!^M"

# Delete old backups (keep last 7 days)
find "$BACKUP_DIR" -name "minecraft_backup_*.tar.gz" -type f -mtime +7 -delete

echo "Backup completed successfully"
```

Make the script executable with `chmod +x backup.sh`.

## Scheduling Backups

### Windows Task Scheduler

1. Open Task Scheduler
2. Click "Create Basic Task"
3. Name it "Minecraft Server Backup"
4. Set trigger to Daily or Weekly
5. Action: Start a program
6. Program/script: path to your batch file
7. Finish

### Linux Cron Jobs

Add to crontab (use `crontab -e`):

```
# Backup every 6 hours
0 */6 * * * /home/minecraft/backup.sh >> /var/log/minecraft-backups.log 2>&1
```

## Recovery Procedures

### Using NeoEssentials Built-in Recovery

1. Stop your server
2. Run `/neoessentials:backup restore <name>` before shutdown, or
3. Start server with the recovery parameter: `java -jar forge-server.jar --restore-backup=<name>`

### Manual Recovery

1. Stop your server
2. Rename your current server directory (e.g., `server_old`)
3. Create a new directory with the original name
4. Extract/copy backup files to the new directory
5. Verify file permissions
6. Start the server

### Recovering Specific Data

#### Player Data Recovery

1. Stop server
2. Navigate to `world/playerdata/`
3. Replace corrupted UUID.dat file with backup version
4. Restart server

#### World Region Recovery

For corrupted chunks:

1. Use a tool like MCEdit or WorldEdit to delete corrupted chunks
2. Replace region files from backup: `world/region/r.x.z.mca`
3. Restart server

## Testing Backups

Regularly test your backup and recovery procedures:

1. Create a backup
2. Set up a test server environment
3. Restore the backup to the test server
4. Verify that everything works as expected

## Best Practices

1. **Redundancy**: Store backups in multiple locations
2. **Rotation**: Implement a backup rotation strategy
3. **Offsite**: Keep copies offsite (cloud storage)
4. **Documentation**: Document your backup and recovery procedures
5. **Regular Testing**: Test your backups monthly

## Automated Cloud Backup Solutions

### Google Drive

Use rclone to sync backups:

```bash
# Install rclone
curl https://rclone.org/install.sh | sudo bash

# Configure rclone
rclone config

# Add to backup script
rclone copy /path/to/backups remote:minecraft-backups
```

### Amazon S3

Use the AWS CLI:

```bash
# Install AWS CLI
pip install awscli

# Configure AWS
aws configure

# Add to backup script
aws s3 sync /path/to/backups s3://bucket-name/minecraft-backups
```

## Troubleshooting

### Common Backup Issues

- **Incomplete Backups**: Ensure server is saving correctly (`save-all` before backup)
- **Corrupted Backups**: Verify compression tools are working correctly
- **Failed Automated Backups**: Check log files for errors
- **Insufficient Space**: Monitor disk space and clean up old backups

### Recovery Issues

- **Missing Files**: Verify backup completeness before recovery
- **Permission Problems**: Check file ownership and permissions
- **Incompatible Versions**: Ensure backup and server versions match
- **Database Inconsistencies**: Use database recovery tools if needed

## Additional Resources

- [NeoForge Forums](https://forums.neoforged.net/)
- [GitHub Repository](https://github.com/ZeroG-Network/NeoEssentials)
- [Discord Support](https://discord.gg/dUGAQF2Mga)
