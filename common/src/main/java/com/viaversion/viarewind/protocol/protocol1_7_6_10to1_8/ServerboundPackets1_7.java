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

package com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8;

import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;

public enum ServerboundPackets1_7 implements ServerboundPacketType {
	KEEP_ALIVE, // 0x00
	CHAT_MESSAGE, // 0x01
	INTERACT_ENTITY, // 0x02
	PLAYER_MOVEMENT, // 0x03
	PLAYER_POSITION, // 0x04
	PLAYER_ROTATION, // 0x05
	PLAYER_POSITION_AND_ROTATION, // 0x06
	PLAYER_DIGGING, // 0x07
	PLAYER_BLOCK_PLACEMENT, // 0x08
	HELD_ITEM_CHANGE, // 0x09
	ANIMATION, // 0x0A
	ENTITY_ACTION, // 0x0B
	STEER_VEHICLE, // 0x0C
	CLOSE_WINDOW, // 0x0D
	CLICK_WINDOW, // 0x0E
	WINDOW_CONFIRMATION, // 0x0F
	CREATIVE_INVENTORY_ACTION, // 0x10
	CLICK_WINDOW_BUTTON, // 0x11
	UPDATE_SIGN, // 0x12
	PLAYER_ABILITIES, // 0x13
	TAB_COMPLETE, // 0x14
	CLIENT_SETTINGS, // 0x15
	CLIENT_STATUS, // 0x16
	PLUGIN_MESSAGE; // 0x17

	@Override
	public int getId() {
		return ordinal();
	}

	@Override
	public String getName() {
		return name();
	}
}
