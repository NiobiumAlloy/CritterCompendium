package io.github.niobiumalloy.crittercompendium;

import io.github.niobiumalloy.crittercompendium.util.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.niobiumalloy.crittercompendium.util.LocDetector.isAt;

public class SafariZoneHandler {

    private static final Pattern HOTSPOT_PATTERN = Pattern.compile("HOTSPOT! Your Hunting Hotspot is the (.+?) Biome!", Pattern.CASE_INSENSITIVE);

    private static boolean isRunActive = false;
    private static long runStartTime = 0L;
    private static int endRunDelay = -1;

    public static boolean macawAnnounced = false;

    public static void onTick(Minecraft client) {
        if (endRunDelay > 0) {
            endRunDelay--;
            if (endRunDelay == 0) {
                endRun();
                if (isAt("safari")) {
                    startRun();
                }

                endRunDelay = -1;
            }
        }

        if (!isAt("safari") || client.player == null) return;

        // entity scan specifically for tracking Macaw
        // DOES NOT highlight or track local coordinates.
        if (Config.INSTANCE.announceMacaw && !macawAnnounced) {
            double searchRadius = 16.0;
            for (Entity entity : client.level.getEntitiesOfClass(Entity.class, client.player.getBoundingBox().inflate(searchRadius))) {
                if (entity.getDisplayName().getString().toLowerCase().contains("macaw")) {
                    macawAnnounced = true;
                    Critter.broadcastMessage("/pc Macaw Spawned!");
                    break;
                }
            }
        }
    }

    public static void startRun() {
        if (isRunActive) return;
        isRunActive = true;
        runStartTime = System.currentTimeMillis();
        Critter.clear();
    }

    public static void endRun() {
        if (!isRunActive) return;
        isRunActive = false;
    }

    public static void triggerGracefulEnd() {
        if (isRunActive && endRunDelay <= 0) {
            Critter.printRunBreakdown(false);
            Critter.clear();
            endRunDelay = 20;
        }
    }

    public static String getRunDurationString() {
        if (!isRunActive || runStartTime == 0) return "00:00";
        long elapsedMillis = System.currentTimeMillis() - runStartTime;
        long totalSeconds = elapsedMillis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public static boolean isRunActive() { return isRunActive; }
    public static long getRunStartTime() { return runStartTime; }

    public static void safariChatHandler(String cleanMessage) {
        if (cleanMessage == null || cleanMessage.isEmpty()) return;

        if (cleanMessage.contains("SAFARI REWARD SUMMARY")) {
            triggerGracefulEnd();
        }

        if (Config.INSTANCE.announceHotspot) {
            announceHotspots(cleanMessage);
        }

        boolean isCapture = cleanMessage.startsWith("CAPTURE! ");
        boolean isLootShare = cleanMessage.startsWith("LOOT SHARE! ");

        if ((isCapture || isLootShare) && isRunActive) {
            new Critter(cleanMessage);
        }

        if (Config.INSTANCE.hideyhoAudioQueues) {
            hideyhoChatHandler(cleanMessage);
        }
    }

    private static void hideyhoChatHandler(String cleanMessage) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        if (cleanMessage.equals("Select an option: [Sure] [No thanks...]")) {
            localPlayer.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        if (cleanMessage.equals("[MOB] Hideyho: No peeking!")) {
            localPlayer.playSound(SoundEvents.PLAYER_TELEPORT, 1.0f, 1.0f);
        }
    }

    private static void announceHotspots(String cleanMessage) {
        Matcher matcher = HOTSPOT_PATTERN.matcher(cleanMessage);
        if (matcher.find()) {
            String biome = matcher.group(1);
            LocalPlayer player = Minecraft.getInstance().player;

            if (player != null && player.connection != null) {
                Critter.broadcastMessage("/pc Hotspot is: " + biome);
            }
        }
    }
}