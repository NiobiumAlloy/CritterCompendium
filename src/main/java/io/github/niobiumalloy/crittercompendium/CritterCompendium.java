package io.github.niobiumalloy.crittercompendium;

import io.github.niobiumalloy.crittercompendium.util.ClientTickHandler;
import io.github.niobiumalloy.crittercompendium.util.ConfigManager;
import io.github.niobiumalloy.crittercompendium.util.GameChatHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CritterCompendium implements ClientModInitializer {
    public static final String MOD_ID = "crittercompendium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigManager.load();

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickHandler::onTick);
        ClientCommandRegistrationCallback.EVENT.register(Commands::register);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                GameChatHandler.handleGameChat(message.getString());
            }
        });

        LOGGER.info("CritterCompendium Client initialized!");
    }
}