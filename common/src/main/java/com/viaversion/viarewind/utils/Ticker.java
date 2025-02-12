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

package com.viaversion.viarewind.utils;

import com.viaversion.viaversion.api.Via;

public class Ticker {
	private static boolean init = false;

	public static void init() {
		if (init) return;
		synchronized (Ticker.class) {
			if (init) return;
			init = true;
		}
		Via.getPlatform().runRepeatingSync(() ->
				Via.getManager().getConnectionManager().getConnections().forEach(user ->
						user.getStoredObjects().values().stream()
								.filter(Tickable.class::isInstance)
								.map(Tickable.class::cast)
								.forEach(Tickable::tick)
				), 1L);
	}
}
