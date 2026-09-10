package io.github.niobiumalloy.crittercompendium.util;

import io.github.niobiumalloy.crittercompendium.SafariZoneHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Collection;

public class LocDetector {
    private static String currentArea = "unknown";
    private static String lastArea = "unknown";

    public static void updateAreaFromTab() {
        var connection = Minecraft.getInstance().getConnection();

        if (connection == null) {
            currentArea = "unknown";
            return;
        }

        Collection<PlayerInfo> playerInfos = connection.getOnlinePlayers();
        for (PlayerInfo info : playerInfos) {
            Component displayName = info.getTabListDisplayName();
            if (displayName == null) continue;

            String text = displayName.getString();

            if (text.contains("Area: ")) {
                handleName(text, "Area: ");
                return;
            }
        }
        currentArea = "unknown";
    }

    private static void handleName(String text, String header) {
        try {
            currentArea = text.split(header)[1].trim().toLowerCase();
            if (!currentArea.equals(lastArea)) {
                onAreaChange(lastArea, currentArea);
                lastArea = currentArea;
            }
        } catch (Exception e) {
            currentArea = "unknown";
        }
    }

    public static boolean isAt(String areaName) {
        return currentArea.contains(areaName.toLowerCase());
    }

    public static void onAreaChange(String lastArea, String currentArea){
        if (Config.INSTANCE.outputDebugToChat && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(
                    Component.literal("§6[SBTools Debug] §eLocation Changed! §b" + lastArea + " --> " + currentArea)
            );
        }

        if (currentArea.contains("safari")){
            SafariZoneHandler.startRun();
        }

        if (lastArea.contains("safari") && !currentArea.contains("safari")) {
            SafariZoneHandler.triggerGracefulEnd();
        }
    }
}