/*
 * This file is part of ViaRewind - https://github.com/ViaVersion/ViaRewind
 * Copyright (C) 2016-2023 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.viarewind;

import com.google.inject.Inject;
import com.viaversion.viaversion.sponge.util.LoggerWrapper;
import com.viaversion.viarewind.api.ViaRewindPlatform;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin("viarewind")
public class SpongePlugin implements ViaRewindPlatform {
    private Logger logger;
    @SuppressWarnings("SpongeInjection")
    @Inject
    private org.apache.logging.log4j.Logger loggerSlf4j;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;
    private ViaRewindConfig conf;

    @Listener(order = Order.LATE)
    public void loadPlugin(ConstructPluginEvent e) {
        // Setup Logger
        this.logger = new LoggerWrapper(loggerSlf4j);
        // Init!
        conf = new ViaRewindConfig(configDir.resolve("config.yml").toFile());
        conf.reloadConfig();
        this.init(conf);
    }

    @Listener
    public void reload(RefreshGameEvent e) {
        conf.reloadConfig();
    }

	@Override
    public Logger getLogger() {
        return this.logger;
    }
}
