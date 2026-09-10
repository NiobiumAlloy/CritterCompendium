package io.github.niobiumalloy.crittercompendium.util;

import io.github.niobiumalloy.crittercompendium.Critter;

public class PartyCommandHandler {
    public static void handlePartyCommand(String playerMessage) {
        String lowerMessage = playerMessage.toLowerCase().trim();

        if (lowerMessage.startsWith("!m ") || lowerMessage.startsWith("!missing ")) {
            String[] parts = lowerMessage.split("\\s+");
            if (parts.length >= 2) {
                String zoneArg = parts[1];
                Critter.missingCritterPartyCommand(zoneArg);
            }
        }
        else if (lowerMessage.equals("!m") || lowerMessage.equals("!missing")) {
            Critter.missingCritterPartyCommand("");
        }
    }
}