package io.github.niobiumalloy.crittercompendium.util;

public class Config {
    public static Config INSTANCE = new Config();

    public boolean isModEnabled = true;
    public boolean announceZoneCompleted = true;
    public boolean includeTimestamps = false;
    public boolean announceMacaw = false;
    public boolean announceHotspot = false;
    public boolean enablePartyCommand = true;
    public boolean outputDebugToChat = false;
}