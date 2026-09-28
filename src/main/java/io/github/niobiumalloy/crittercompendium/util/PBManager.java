package io.github.niobiumalloy.crittercompendium.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PBManager {
    public static class PBData {
        public Map<String, Long> globalPBs = new HashMap<>();
        public Map<String, Map<String, Long>> playerPBs = new HashMap<>();
    }

    private static PBData data = new PBData();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class PBResult {
        public boolean newGlobal;
        public long previousGlobal;
        public boolean newIndividual;
        public long previousIndividual;
    }

    private static File getPBFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config/crittercompendium-pbs.json");
    }

    public static void load() {
        File file = getPBFile();
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            PBData loaded = GSON.fromJson(reader, PBData.class);
            if (loaded != null) data = loaded;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        File file = getPBFile();
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PBResult recordPB(String zone, long timeSeconds, Set<String> players, String localPlayer) {
        if (data == null) load();
        PBResult result = new PBResult();

        Long currentGlobal = data.globalPBs.get(zone);
        if (currentGlobal == null || timeSeconds < currentGlobal) {
            result.newGlobal = true;
            result.previousGlobal = currentGlobal == null ? timeSeconds : currentGlobal;
            data.globalPBs.put(zone, timeSeconds);
        } else {
            result.newGlobal = false;
            result.previousGlobal = currentGlobal;
        }

        // Ensure the local player is always included in the PB log even if they didn't get a catch
        players.add(localPlayer);

        // Check Individual PBs for all active members in the run
        for (String player : players) {
            String pName = player.toLowerCase();
            Map<String, Long> pData = data.playerPBs.computeIfAbsent(pName, k -> new HashMap<>());
            Long currentIndiv = pData.get(zone);
            boolean isNew = currentIndiv == null || timeSeconds < currentIndiv;

            if (isNew) {
                pData.put(zone, timeSeconds);
            }

            // Only return the individual PB result for the local client player to display in chat
            if (pName.equals(localPlayer.toLowerCase())) {
                result.newIndividual = isNew;
                result.previousIndividual = currentIndiv == null ? timeSeconds : currentIndiv;
            }
        }

        save();
        return result;
    }

    public static String formatTime(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    public static void printGlobalPBs() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        client.player.sendSystemMessage(Component.literal("§6--- Global Personal Bests ---"));
        if (data.globalPBs.isEmpty()) {
            client.player.sendSystemMessage(Component.literal("§7No global PBs recorded yet."));
            return;
        }
        for (Map.Entry<String, Long> entry : data.globalPBs.entrySet()) {
            client.player.sendSystemMessage(Component.literal("§e" + entry.getKey() + ": §f" + formatTime(entry.getValue())));
        }
    }

    public static void printPlayerPBs(String playerName) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String searchName = playerName.toLowerCase();
        client.player.sendSystemMessage(Component.literal("§6--- PBs for " + playerName + " ---"));
        Map<String, Long> pData = data.playerPBs.get(searchName);
        if (pData == null || pData.isEmpty()) {
            client.player.sendSystemMessage(Component.literal("§7No PBs recorded for this player."));
            return;
        }
        for (Map.Entry<String, Long> entry : pData.entrySet()) {
            client.player.sendSystemMessage(Component.literal("§e" + entry.getKey() + ": §f" + formatTime(entry.getValue())));
        }
    }
}