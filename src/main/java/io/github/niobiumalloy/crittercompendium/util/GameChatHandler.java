package io.github.niobiumalloy.crittercompendium.util;

import io.github.niobiumalloy.crittercompendium.SafariZoneHandler;

public class GameChatHandler {
    public static void handleGameChat(String rawText) {
        if (!Config.INSTANCE.isModEnabled) return;

        String cleanMessage = cleanMessage(rawText);
        Config config = Config.INSTANCE;

        if (cleanMessage.startsWith("Party > ") && config.enablePartyCommand) {
            String playerMessage = cleanMessage.substring(cleanMessage.indexOf(":")+2);
            if (playerMessage.startsWith("!")) PartyCommandHandler.handlePartyCommand(playerMessage);
        }

        SafariZoneHandler.safariChatHandler(cleanMessage);
    }

    private static String cleanMessage(String rawText) {
        return rawText.replaceAll("§.", "").trim();
    }
}