package io.github.niobiumalloy.crittercompendium;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.niobiumalloy.crittercompendium.util.Config;
import io.github.niobiumalloy.crittercompendium.util.ConfigManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

public class Commands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext registryAccess) {
        Config config = Config.INSTANCE;

        dispatcher.register(ClientCommands.literal("chelp")
                .executes(context -> {
                    FabricClientCommandSource src = context.getSource();
                    src.sendFeedback(Component.literal("§6--- Critter Compendium Help ---"));
                    src.sendFeedback(Component.literal("§e/ctoggle §7- Enables/Disables the mod (except chelp and ctoggle)"));
                    src.sendFeedback(Component.literal("§e/c m [zone] §7- Client side missing critters output"));
                    src.sendFeedback(Component.literal("§e/c anounceZoneCompleted §7- Toggles completion announcements"));
                    src.sendFeedback(Component.literal("§e/c includeTimestamps §7- Toggles timestamps appended to completion"));
                    src.sendFeedback(Component.literal("§e/c announceMacaw §7- Toggles Macaw PC announcement on spawn"));
                    src.sendFeedback(Component.literal("§e/c announceHotspot §7- Toggles Hotspot PC announcement"));
                    src.sendFeedback(Component.literal("§e/c enablePartyCommand §7- Toggles the !m and !missing party commands"));
                    src.sendFeedback(Component.literal("§e/c outputDebugToChat §7- Toggles debug messaging"));
                    src.sendFeedback(Component.literal("§6--- Party Command ---"));
                    src.sendFeedback(Component.literal("§e!m [zone] or !missing [zone] §7- Broadcasts missing critters for a specified zone or all"));

                    return 1;
                })
        );

        dispatcher.register(ClientCommands.literal("ctoggle")
                .executes(context -> {
                    config.isModEnabled = !config.isModEnabled;
                    FabricClientCommandSource src = context.getSource();
                    src.sendFeedback(Component.literal("§6Mod enabled set to §f" + config.isModEnabled));
                    ConfigManager.save();
                    return 1;
                })
        );

        dispatcher.register(ClientCommands.literal("clink")
                .executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player == null) return 1;
                    client.player.connection.sendCommand("/pc github . com/NiobiumAlloy/CritterCompendium");
                    return 1;
                })
        );

        dispatcher.register(ClientCommands.literal("c")
                .then(ClientCommands.literal("anounceZoneCompleted")
                        .executes(context -> {
                            config.announceZoneCompleted = !config.announceZoneCompleted;
                            FabricClientCommandSource src = context.getSource();
                            src.sendFeedback(Component.literal("§6anounceZoneCompleted set to §f" + config.announceZoneCompleted));
                            ConfigManager.save();
                            return 1;
                        })
                )
                .then(ClientCommands.literal("includeTimestamps")
                        .executes(context -> {
                            config.includeTimestamps = !config.includeTimestamps;
                            FabricClientCommandSource src = context.getSource();
                            src.sendFeedback(Component.literal("§6includeTimestamps set to §f" + config.includeTimestamps));
                            ConfigManager.save();
                            return 1;
                        })
                )
                .then(ClientCommands.literal("announceMacaw")
                        .executes(context -> {
                            config.announceMacaw = !config.announceMacaw;
                            FabricClientCommandSource src = context.getSource();
                            src.sendFeedback(Component.literal("§6announceMacaw set to §f" + config.announceMacaw));
                            ConfigManager.save();
                            return 1;
                        })
                )
                .then(ClientCommands.literal("announceHotspot")
                        .executes(context -> {
                            config.announceHotspot = !config.announceHotspot;
                            FabricClientCommandSource src = context.getSource();
                            src.sendFeedback(Component.literal("§6announceHotspot set to §f" + config.announceHotspot));
                            ConfigManager.save();
                            return 1;
                        })
                )
                .then(ClientCommands.literal("enablePartyCommand")
                        .executes(context -> {
                            config.enablePartyCommand = !config.enablePartyCommand;
                            FabricClientCommandSource src = context.getSource();
                            src.sendFeedback(Component.literal("§6enablePartyCommand set to §f" + config.enablePartyCommand));
                            ConfigManager.save();
                            return 1;
                        })
                )
                .then(ClientCommands.literal("outputDebugToChat")
                        .executes(context -> {
                            config.outputDebugToChat = !config.outputDebugToChat;
                            FabricClientCommandSource src = context.getSource();
                            src.sendFeedback(Component.literal("§6outputDebugToChat set to §f" + config.outputDebugToChat));
                            ConfigManager.save();
                            return 1;
                        })
                )
                .then(ClientCommands.literal("m")
                        .executes(context -> {
                            Critter.printMissingCritters("");
                            return 1;
                        })
                        .then(ClientCommands.argument("zone", StringArgumentType.word())
                                .executes(context -> {
                                    String zoneArg = StringArgumentType.getString(context, "zone");
                                    Critter.printMissingCritters(zoneArg);
                                    return 1;
                                })
                        )
                )
        );
    }
}