/*
 *     MCEF (Minecraft Chromium Embedded Framework)
 *     Copyright (C) 2023 CinemaMod Group
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */

package com.cinemamod.mcef;

import com.cinemamod.mcef.example.ExampleScreen;
import com.cinemamod.mcef.example.MCEFExampleMod;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(NeoForgeMCEFMod.MODID)
public class NeoForgeMCEFMod {
    public static final String MODID = "mcef";
    private static final Logger LOGGER = LogUtils.getLogger();

    public NeoForgeMCEFMod(IEventBus modEventBus) {
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::serverSetup);
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::registerClientCommands);
    }

    private void registerClientCommands(final RegisterClientCommandsEvent event) {
        // /mcefdemo opens the demo browser directly -- useful for testing since it doesn't
        // depend on the F10 key mapping actually being routed to the game (that registration
        // succeeds per the game's own log, but the key press itself doesn't seem to reach
        // KeyMapping#isDown(); left as a known issue in the upstream demo code for now).
        if (!FMLEnvironment.isProduction()) {
            event.getDispatcher().register(
                    LiteralArgumentBuilder.<CommandSourceStack>literal("mcefdemo")
                            .executes(ctx -> {
                                Minecraft.getInstance().gui.setScreen(new ExampleScreen(
                                        Component.literal("Example Screen")
                                ));
                                return 1;
                            })
            );
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        if (!FMLEnvironment.isProduction()) {
            new MCEFExampleMod();
        }
    }

    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        if (!FMLEnvironment.isProduction()) {
            event.register(MCEFExampleMod.KEY_MAPPING);
        }
    }

    private void serverSetup(final FMLDedicatedServerSetupEvent event) {
        // MCEF server-side does nothing
    }
}
