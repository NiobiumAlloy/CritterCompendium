package io.github.niobiumalloy.crittercompendium.util;

import io.github.niobiumalloy.crittercompendium.SafariZoneHandler;
import net.minecraft.client.Minecraft;

public class ClientTickHandler {
    public static void onTick(Minecraft client) {
        if (client.player == null) return;

        if (client.player.tickCount % 20 == 0) {
            LocDetector.updateAreaFromTab();
        }

        SafariZoneHandler.onTick(client);
    }
}