package io.github.niobiumalloy.crittercompendium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.niobiumalloy.crittercompendium.util.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Critter {
    public boolean isSparkling;
    private String critterName;

    public static class CatchRecord {
        public String playerName;
        public String critterName;
        public boolean isSparkling;
        public String zone;
        public long timestamp;

        public CatchRecord(String playerName, String critterName, boolean isSparkling) {
            this.playerName = playerName;
            this.critterName = critterName;
            this.isSparkling = isSparkling;
            this.zone = getZoneForCritter(critterName);
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static final ArrayList<String> allCrittersCaught = new ArrayList<>();
    private static final Set<String> uniqueCrittersCaught = new HashSet<>();
    public static final List<CatchRecord> catchRecords = new ArrayList<>();

    private static boolean cavernDone = false;
    private static boolean forestDone = false;
    private static boolean hauntedDone = false;
    private static boolean icyDone = false;
    private static boolean allDone = false;

    private static String cavernCompleteTimeStr = "";
    private static String forestCompleteTimeStr = "";
    private static String hauntedCompleteTimeStr = "";
    private static String icyCompleteTimeStr = "";

    public static final Set<String> CAVERN_CRITTERS = Set.of("cavernfish", "chuckwalla", "driftling", "gemzie", "rockmite", "scrappy", "shyworm", "snoozle", "flitter");
    public static final Set<String> FOREST_BASE_CRITTERS = Set.of("bluebird", "fluffling", "foxtrot", "hideonfloor", "honeybug", "parakeet", "treefrog", "woodchucker");
    public static final Set<String> HAUNTED_CRITTERS = Set.of("areita", "bloodbat", "doomspiral", "duplico", "gazer", "gimmiegold", "hideonwall", "hideyho", "litterbug", "solsnatcher");
    public static final Set<String> ICY_CRITTERS = Set.of("billygoat", "mantis shrimp", "nozzlenose", "polaris", "shuddersquid", "strongarm", "tepid", "troodon", "wumpa");

    private static final Pattern CAPTURE_PATTERN = Pattern.compile("capture! you (?:caught|found) (?:a|the|an) (sparkling )?(.+?)and", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOT_SHARE_PATTERN = Pattern.compile("(?:from ([a-zA-Z0-9_]+)(?:'s)? |([a-zA-Z0-9_]+) )?(?:catching|finding) (?:a|the|an) (sparkling )?([^!]+)!", Pattern.CASE_INSENSITIVE);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class SafariMessages {
        public String cavernMsg = "/pc cavern uniques done";
        public String forestMsg = "/pc forest uniques done";
        public String hauntedMsg = "/pc haunted uniques done";
        public String icyMsg = "/pc icy uniques done";
        public String allUniquesMsg = "/pc all uniques done";
    }

    private static SafariMessages safariMessages = new SafariMessages();

    private static File getMessagesFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config/crittercompendium-safari-messages.json");
    }

    private static void loadSafariMessages() {
        File messagesFile = getMessagesFile();
        if (!messagesFile.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(messagesFile), StandardCharsets.UTF_8)) {
            SafariMessages loaded = GSON.fromJson(reader, SafariMessages.class);
            if (loaded != null) safariMessages = loaded;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveSafariMessages() {
        File messagesFile = getMessagesFile();
        try {
            messagesFile.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(messagesFile), StandardCharsets.UTF_8)) {
                GSON.toJson(safariMessages, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getZoneForCritter(String critterName) {
        if (CAVERN_CRITTERS.contains(critterName)) return "Cavern";
        if (FOREST_BASE_CRITTERS.contains(critterName) || critterName.equals("macaw")) return "Forest";
        if (HAUNTED_CRITTERS.contains(critterName)) return "Haunted";
        if (ICY_CRITTERS.contains(critterName)) return "Icy";
        return "Unknown";
    }

    public Critter(String cleanCaptureMessage) {
        loadSafariMessages();

        String parsedPlayer = "Unknown";
        Matcher captureMatcher = CAPTURE_PATTERN.matcher(cleanCaptureMessage);
        Matcher lootShareMatcher = LOOT_SHARE_PATTERN.matcher(cleanCaptureMessage);

        if (captureMatcher.find()) {
            isSparkling = captureMatcher.group(1) != null;
            critterName = captureMatcher.group(2).trim();
            if (critterName.endsWith(",")) critterName = critterName.substring(0, critterName.length() - 1);

            if (Minecraft.getInstance().player != null) {
                parsedPlayer = Minecraft.getInstance().player.getName().getString();
            } else {
                parsedPlayer = "You";
            }
        } else if (lootShareMatcher.find()) {
            parsedPlayer = lootShareMatcher.group(1);
            if (parsedPlayer == null) parsedPlayer = lootShareMatcher.group(2);
            if (parsedPlayer != null && parsedPlayer.equalsIgnoreCase("from")) {
                parsedPlayer = "Unknown";
            }
            if (parsedPlayer == null) parsedPlayer = "Unknown";

            isSparkling = lootShareMatcher.group(3) != null;
            critterName = lootShareMatcher.group(4).trim();
        } else {
            isSparkling = cleanCaptureMessage.toLowerCase().contains("sparkling");
            critterName = "Unknown";
        }

        critterName = critterName.toLowerCase();
        if (!critterName.equals("unknown")) {
            allCrittersCaught.add(critterName);
            uniqueCrittersCaught.add(critterName);
            catchRecords.add(new CatchRecord(parsedPlayer, critterName, isSparkling));
        }

        if (Config.INSTANCE.outputDebugToChat && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§d§lCaught " + critterName + (isSparkling ? " §e(Sparkling)" : "") + " §7by " + parsedPlayer));
        }

        checkZoneCompletions();
    }

    private static void checkZoneCompletions() {
        if (allDone) return;

        boolean justFinishedCavern = !cavernDone && uniqueCrittersCaught.containsAll(CAVERN_CRITTERS);
        boolean justFinishedForest = !forestDone && uniqueCrittersCaught.containsAll(FOREST_BASE_CRITTERS);
        boolean justFinishedHaunted = !hauntedDone && uniqueCrittersCaught.containsAll(HAUNTED_CRITTERS);
        boolean justFinishedIcy = !icyDone && uniqueCrittersCaught.containsAll(ICY_CRITTERS);

        if (justFinishedCavern) {
            cavernDone = true;
            cavernCompleteTimeStr = SafariZoneHandler.getRunDurationString();
        }
        if (justFinishedForest) {
            forestDone = true;
            forestCompleteTimeStr = SafariZoneHandler.getRunDurationString();
        }
        if (justFinishedHaunted) {
            hauntedDone = true;
            hauntedCompleteTimeStr = SafariZoneHandler.getRunDurationString();
        }
        if (justFinishedIcy) {
            icyDone = true;
            icyCompleteTimeStr = SafariZoneHandler.getRunDurationString();
        }

        if (cavernDone && forestDone && hauntedDone && icyDone) {
            allDone = true;
            if (Config.INSTANCE.announceZoneCompleted) {
                broadcastMessage(appendTimestamp(safariMessages.allUniquesMsg));
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.gui.setTitle(Component.literal("§dAll Uniques Caught!"));
                client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            return;
        }

        if (Config.INSTANCE.announceZoneCompleted) {
            if (justFinishedCavern) broadcastMessage(appendTimestamp(safariMessages.cavernMsg));
            if (justFinishedForest) broadcastMessage(appendTimestamp(safariMessages.forestMsg));
            if (justFinishedHaunted) broadcastMessage(appendTimestamp(safariMessages.hauntedMsg));
            if (justFinishedIcy) broadcastMessage(appendTimestamp(safariMessages.icyMsg));
        }
    }

    private static String appendTimestamp(String message) {
        if (Config.INSTANCE.includeTimestamps) {
            return message + " in " + SafariZoneHandler.getRunDurationString();
        }
        return message;
    }

    public static void broadcastMessage(String msg) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || msg == null || msg.isEmpty()) return;

        if (msg.startsWith("/")) {
            client.player.connection.sendCommand(msg.substring(1));
        } else {
            client.player.connection.sendChat(msg);
        }
    }

    public static void printRunBreakdown(boolean forceParty) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (!Config.INSTANCE.showRunBreakdown && !Config.INSTANCE.partyChatBreakdown && !forceParty) return;

        Map<String, Map<String, Integer>> playerZoneCounts = new HashMap<>();
        Map<String, Integer> playerTotalCounts = new HashMap<>();

        for (CatchRecord record : catchRecords) {
            playerZoneCounts.putIfAbsent(record.playerName, new HashMap<>());
            Map<String, Integer> zCounts = playerZoneCounts.get(record.playerName);
            zCounts.put(record.zone, zCounts.getOrDefault(record.zone, 0) + 1);

            playerTotalCounts.put(record.playerName, playerTotalCounts.getOrDefault(record.playerName, 0) + 1);
        }

        if (Config.INSTANCE.showRunBreakdown || forceParty) {
            List<String> messages = new ArrayList<>();
            messages.add("§6--- Safari Run Breakdown ---");
            messages.add("§eTotal Time: §f" + SafariZoneHandler.getRunDurationString());

            if (cavernDone) messages.add("§6Cavern: §f" + cavernCompleteTimeStr);
            if (forestDone) messages.add("§aForest: §f" + forestCompleteTimeStr);
            if (hauntedDone) messages.add("§5Haunted: §f" + hauntedCompleteTimeStr);
            if (icyDone) messages.add("§bIcy: §f" + icyCompleteTimeStr);

            if (!playerTotalCounts.isEmpty()) {
                messages.add("§ePlayers:");
                for (Map.Entry<String, Integer> entry : playerTotalCounts.entrySet()) {
                    String player = entry.getKey();
                    int total = entry.getValue();
                    Map<String, Integer> zCounts = playerZoneCounts.get(player);

                    List<String> zStrings = new ArrayList<>();
                    if (zCounts.containsKey("Cavern")) zStrings.add("§6C:" + zCounts.get("Cavern"));
                    if (zCounts.containsKey("Forest")) zStrings.add("§aF:" + zCounts.get("Forest"));
                    if (zCounts.containsKey("Haunted")) zStrings.add("§5H:" + zCounts.get("Haunted"));
                    if (zCounts.containsKey("Icy")) zStrings.add("§bI:" + zCounts.get("Icy"));

                    messages.add("§f" + player + " §7- §dTotal: " + total + " §7(" + String.join("§7, ", zStrings) + "§7)");
                }
            } else {
                messages.add("§7No critters caught.");
            }

            for (String msg : messages) {
                client.player.sendSystemMessage(Component.literal(msg));
            }
        }

        if (Config.INSTANCE.partyChatBreakdown || forceParty) {
            broadcastPartyBreakdown(playerTotalCounts, playerZoneCounts);
        }
    }

    private static void broadcastPartyBreakdown(Map<String, Integer> playerTotalCounts, Map<String, Map<String, Integer>> playerZoneCounts) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        List<String> messages = new ArrayList<>();

        List<String> zoneTimes = new ArrayList<>();
        if (cavernDone) zoneTimes.add("Cavern: " + cavernCompleteTimeStr);
        if (forestDone) zoneTimes.add("Forest: " + forestCompleteTimeStr);
        if (hauntedDone) zoneTimes.add("Haunted: " + hauntedCompleteTimeStr);
        if (icyDone) zoneTimes.add("Icy: " + icyCompleteTimeStr);

        if (!zoneTimes.isEmpty()) {
            messages.add("Safari Times -> " + String.join(" | ", zoneTimes));
        }

        List<String> playerStats = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : playerTotalCounts.entrySet()) {
            String player = entry.getKey();
            int total = entry.getValue();
            Map<String, Integer> zCounts = playerZoneCounts.get(player);

            List<String> zStrings = new ArrayList<>();
            if (zCounts.containsKey("Cavern")) zStrings.add("C:" + zCounts.get("Cavern"));
            if (zCounts.containsKey("Forest")) zStrings.add("F:" + zCounts.get("Forest"));
            if (zCounts.containsKey("Haunted")) zStrings.add("H:" + zCounts.get("Haunted"));
            if (zCounts.containsKey("Icy")) zStrings.add("I:" + zCounts.get("Icy"));

            playerStats.add(player + ": " + total + " (" + String.join(" ", zStrings) + ")");
        }

        if (!playerStats.isEmpty()) {
            StringBuilder currentMsg = new StringBuilder("Stats -> ");
            for (String stat : playerStats) {
                if (currentMsg.length() + stat.length() + 3 > 250) {
                    messages.add(currentMsg.toString());
                    currentMsg = new StringBuilder("Stats -> ");
                }
                if (currentMsg.length() > 9) currentMsg.append(" | ");
                currentMsg.append(stat);
            }
            if (currentMsg.length() > 9) messages.add(currentMsg.toString());
        }

        if (messages.isEmpty()) return;

        new Thread(() -> {
            for (int i = 0; i < messages.size(); i++) {
                String msg = messages.get(i);
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.connection.sendCommand("pc " + msg);
                    }
                });
                if (i < messages.size() - 1) {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                }
            }
        }).start();
    }

    public static void printMissingCritters(String zoneInput) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        List<Character> zonesToCheck = new ArrayList<>();

        if (zoneInput == null || zoneInput.trim().isEmpty()) {
            zonesToCheck.add('c'); // Cavern
            zonesToCheck.add('f'); // Forest
            zonesToCheck.add('h'); // Haunted
            zonesToCheck.add('i'); // Icy
        } else {
            zonesToCheck.add(zoneInput.toLowerCase().charAt(0));
        }

        List<String> combinedMessages = new ArrayList<>();

        for (char zoneChar : zonesToCheck) {
            Set<String> targetSet;
            String formattedZoneName;

            switch (zoneChar) {
                case 'c':
                    targetSet = CAVERN_CRITTERS;
                    formattedZoneName = "§6Cavern";
                    break;
                case 'f':
                    targetSet = FOREST_BASE_CRITTERS;
                    formattedZoneName = "§aForest";
                    break;
                case 'h':
                    targetSet = HAUNTED_CRITTERS;
                    formattedZoneName = "§5Haunted";
                    break;
                case 'i':
                    targetSet = ICY_CRITTERS;
                    formattedZoneName = "§bIcy";
                    break;
                default:
                    if (zoneInput != null && !zoneInput.trim().isEmpty()) {
                        client.player.sendSystemMessage(Component.literal("§cUnknown Safari Zone: " + zoneInput + ". Use Cavern, Forest, Haunted, or Icy."));
                    }
                    return;
            }

            List<String> missing = new ArrayList<>();

            for (String critter : targetSet) {
                if (!uniqueCrittersCaught.contains(critter)) {
                    missing.add(capitalizeWords(critter));
                }
            }

            if (zoneChar == 'f' && !uniqueCrittersCaught.contains("macaw")) {
                missing.add("Macaw (Opt.)");
            }

            if (!missing.isEmpty()) {
                String missingString = String.join("§7, §c", missing);
                combinedMessages.add(formattedZoneName + " §eMissing: §c" + missingString);
            }
        }

        if (combinedMessages.isEmpty()) {
            client.player.sendSystemMessage(Component.literal("§aYou have caught all critters in the selected zone(s)!"));
            return;
        }

        int maxCommandLength = 256;
        String separator = " §8| ";

        List<String> finalCommands = new ArrayList<>();
        StringBuilder currentMessage = new StringBuilder();

        for (String messageChunk : combinedMessages) {
            if (currentMessage.length() > 0 &&
                    currentMessage.length() + separator.length() + messageChunk.length() > maxCommandLength) {

                finalCommands.add(currentMessage.toString());
                currentMessage.setLength(0);
            }

            if (currentMessage.length() > 0) {
                currentMessage.append(separator);
            }
            currentMessage.append(messageChunk);
        }

        if (currentMessage.length() > 0) {
            finalCommands.add(currentMessage.toString());
        }

        for (String commandText : finalCommands) {
            client.player.sendSystemMessage(Component.literal(commandText));
        }
    }

    public static void missingCritterPartyCommand(String zoneInput) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        List<Character> zonesToCheck = new ArrayList<>();

        if (zoneInput == null || zoneInput.trim().isEmpty()) {
            zonesToCheck.add('c'); // Cavern
            zonesToCheck.add('f'); // Forest
            zonesToCheck.add('h'); // Haunted
            zonesToCheck.add('i'); // Icy
        } else {
            zonesToCheck.add(zoneInput.toLowerCase().charAt(0));
        }

        List<String> combinedMessages = new ArrayList<>();

        for (char zoneChar : zonesToCheck) {
            Set<String> targetSet;
            String plainZoneName;

            switch (zoneChar) {
                case 'c':
                    targetSet = CAVERN_CRITTERS;
                    plainZoneName = "Cavern";
                    break;
                case 'f':
                    targetSet = FOREST_BASE_CRITTERS;
                    plainZoneName = "Forest";
                    break;
                case 'h':
                    targetSet = HAUNTED_CRITTERS;
                    plainZoneName = "Haunted";
                    break;
                case 'i':
                    targetSet = ICY_CRITTERS;
                    plainZoneName = "Icy";
                    break;
                default:
                    if (zoneInput != null && !zoneInput.trim().isEmpty()) {
                        client.player.sendSystemMessage(Component.literal("§cUnknown Safari Zone: " + zoneInput + ". Use Cavern, Forest, Haunted, or Icy."));
                    }
                    return;
            }

            List<String> missing = new ArrayList<>();

            for (String critter : targetSet) {
                if (!uniqueCrittersCaught.contains(critter)) {
                    missing.add(capitalizeWords(critter));
                }
            }

            if (zoneChar == 'f' && !uniqueCrittersCaught.contains("macaw")) {
                missing.add("Macaw (Opt.)");
            }

            if (!missing.isEmpty()) {
                String missingString = String.join(", ", missing);
                combinedMessages.add(plainZoneName + " Missing: " + missingString);
            }
        }

        int maxCommandLength = 256;
        String prefix = "pc ";
        String separator = " | ";

        List<String> finalCommands = new ArrayList<>();
        StringBuilder currentMessage = new StringBuilder();

        for (String messageChunk : combinedMessages) {
            if (currentMessage.length() > 0 &&
                    prefix.length() + currentMessage.length() + separator.length() + messageChunk.length() > maxCommandLength) {

                finalCommands.add(currentMessage.toString());
                currentMessage.setLength(0);
            }

            if (currentMessage.length() > 0) {
                currentMessage.append(separator);
            }
            currentMessage.append(messageChunk);
        }

        if (currentMessage.length() > 0) {
            finalCommands.add(currentMessage.toString());
        }

        new Thread(() -> {
            for (int i = 0; i < finalCommands.size(); i++) {
                String commandText = finalCommands.get(i);

                client.execute(() -> {
                    if (client.player != null) {
                        client.player.connection.sendCommand(prefix + commandText);
                    }
                });

                if (i < finalCommands.size() - 1) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    private static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }
        return sb.toString().trim();
    }

    public static void setAllDone() {
        allDone = true;
    }

    public static void clear() {
        allCrittersCaught.clear();
        uniqueCrittersCaught.clear();
        catchRecords.clear();
        cavernDone = false;
        forestDone = false;
        hauntedDone = false;
        icyDone = false;
        allDone = false;
        cavernCompleteTimeStr = "";
        forestCompleteTimeStr = "";
        hauntedCompleteTimeStr = "";
        icyCompleteTimeStr = "";
        SafariZoneHandler.macawAnnounced = false;
    }

    public boolean isSparkling() { return isSparkling; }
    public static boolean isAllDone() { return allDone; }
    public static boolean uniquesCaught() { return cavernDone && forestDone && hauntedDone && icyDone; }
}