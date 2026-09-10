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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Critter {
    public boolean isSparkling;
    private String critterName;

    private static final ArrayList<String> allCrittersCaught = new ArrayList<>();
    private static final Set<String> uniqueCrittersCaught = new HashSet<>();

    private static boolean cavernDone = false;
    private static boolean forestDone = false;
    private static boolean hauntedDone = false;
    private static boolean icyDone = false;
    private static boolean allDone = false;

    public static final Set<String> CAVERN_CRITTERS = Set.of("cavernfish", "chuckwalla", "driftling", "gemzie", "rockmite", "scrappy", "shyworm", "snoozle", "flitter");
    public static final Set<String> FOREST_BASE_CRITTERS = Set.of("bluebird", "fluffling", "foxtrot", "hideonfloor", "honeybug", "parakeet", "treefrog", "woodchucker");
    public static final Set<String> HAUNTED_CRITTERS = Set.of("areita", "bloodbat", "doomspiral", "duplico", "gazer", "gimmiegold", "hideonwall", "hideyho", "litterbug", "solsnatcher");
    public static final Set<String> ICY_CRITTERS = Set.of("billygoat", "mantis shrimp", "nozzlenose", "polaris", "shuddersquid", "strongarm", "tepid", "troodon", "wumpa");

    private static final Pattern CAPTURE_PATTERN = Pattern.compile("capture! you (?:caught|found) (?:a|the|an) (sparkling )?(.+?)and", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOOT_SHARE_PATTERN = Pattern.compile("(?:catching|finding) (?:a|the|an) (sparkling )?([^!]+)!", Pattern.CASE_INSENSITIVE);

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

    public Critter(String cleanCaptureMessage) {
        loadSafariMessages();

        Matcher captureMatcher = CAPTURE_PATTERN.matcher(cleanCaptureMessage);
        Matcher lootShareMatcher = LOOT_SHARE_PATTERN.matcher(cleanCaptureMessage);

        if (captureMatcher.find()) {
            isSparkling = captureMatcher.group(1) != null;
            critterName = captureMatcher.group(2).trim();
            if (critterName.endsWith(",")) critterName = critterName.substring(0, critterName.length() - 1);
        } else if (lootShareMatcher.find()) {
            isSparkling = lootShareMatcher.group(1) != null;
            critterName = lootShareMatcher.group(2).trim();
        } else {
            isSparkling = cleanCaptureMessage.toLowerCase().contains("sparkling");
            critterName = "Unknown";
        }

        critterName = critterName.toLowerCase();
        if (!critterName.equals("unknown")) {
            allCrittersCaught.add(critterName);
            uniqueCrittersCaught.add(critterName);
        }

        if (Config.INSTANCE.outputDebugToChat && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal("§d§lCaught " + critterName + (isSparkling ? " §e(Sparkling)" : "")));
        }

        checkZoneCompletions();
    }

    private static void checkZoneCompletions() {
        if (allDone) return;

        boolean justFinishedCavern = !cavernDone && uniqueCrittersCaught.containsAll(CAVERN_CRITTERS);
        boolean justFinishedForest = !forestDone && uniqueCrittersCaught.containsAll(FOREST_BASE_CRITTERS);
        boolean justFinishedHaunted = !hauntedDone && uniqueCrittersCaught.containsAll(HAUNTED_CRITTERS);
        boolean justFinishedIcy = !icyDone && uniqueCrittersCaught.containsAll(ICY_CRITTERS);

        if (justFinishedCavern) cavernDone = true;
        if (justFinishedForest) forestDone = true;
        if (justFinishedHaunted) hauntedDone = true;
        if (justFinishedIcy) icyDone = true;

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
        cavernDone = false;
        forestDone = false;
        hauntedDone = false;
        icyDone = false;
        allDone = false;
        SafariZoneHandler.macawAnnounced = false;
    }

    public boolean isSparkling() { return isSparkling; }
    public static boolean isAllDone() { return allDone; }
    public static boolean uniquesCaught() { return cavernDone && forestDone && hauntedDone && icyDone; }
}