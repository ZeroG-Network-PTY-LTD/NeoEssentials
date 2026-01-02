# Fix all compilation errors script

$files = @{
    "FletchingCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\FletchingCommand.java"
    "HelpopCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\HelpopCommand.java"
    "MailCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\MailCommand.java"
    "NearCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\NearCommand.java"
    "NickCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\NickCommand.java"
    "PingCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\PingCommand.java"
    "SmithingCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\SmithingCommand.java"
    "StonecuttingCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\StonecuttingCommand.java"
    "SuicideCommand" = "D:\ADrive_minecraft\Minecraft Mod Development\NeoEssentials\src\main\java\com\zerog\neoessentials\util\commands\SuicideCommand.java"
}

foreach ($file in $files.Keys) {
    $path = $files[$file]
    Write-Host "Processing: $file"

    $content = Get-Content $path -Raw

    # Add CommandSourceHelper import if not present
    if ($content -notmatch "import com\.zerog\.neoessentials\.util\.CommandSourceHelper") {
        $content = $content -replace "(import com\.zerog\.neoessentials\.util\.MessageUtil;)", "import com.zerog.neoessentials.util.CommandSourceHelper;`n`$1"
        Set-Content $path $content -NoNewline
        Write-Host "  Added CommandSourceHelper import"
    }
}

Write-Host "`nDone!"

