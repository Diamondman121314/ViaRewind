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

package com.viaversion.viarewind.protocol.protocol1_8to1_9.storage;

import com.viaversion.viarewind.api.ViaRewindConfig;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viarewind.utils.PacketUtil;
import com.viaversion.viarewind.utils.Tickable;
import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.util.Pair;
import com.viaversion.viarewind.ViaRewind;

import java.util.ArrayList;
import java.util.UUID;

public class Cooldown extends StoredObject implements Tickable {

	private double attackSpeed = 4.0;
	private long lastHit = 0;
	private final ViaRewindConfig.CooldownIndicator cooldownIndicator;
	private UUID bossUUID;
	private boolean lastSend;

	public Cooldown(final UserConnection user) {
		super(user);

		ViaRewindConfig.CooldownIndicator indicator;
		try {
			indicator = ViaRewind.getConfig().getCooldownIndicator();
		} catch (IllegalArgumentException e) {
			ViaRewind.getPlatform().getLogger().warning("Invalid cooldown-indicator setting");
			indicator = ViaRewindConfig.CooldownIndicator.DISABLED;
		}

		this.cooldownIndicator = indicator;
	}

	@Override
	public void tick() {
		if (!hasCooldown()) {
			if (lastSend) {
				hide();
				lastSend = false;
			}
			return;
		}

		BlockPlaceDestroyTracker tracker = getUser().get(BlockPlaceDestroyTracker.class);
		if (tracker.isMining()) {
			lastHit = 0;
			if (lastSend) {
				hide();
				lastSend = false;
			}
			return;
		}

		showCooldown();
		lastSend = true;
	}

	private void showCooldown() {
		if (cooldownIndicator == ViaRewindConfig.CooldownIndicator.TITLE) {
			sendTitle("", getTitle(), 0, 2, 5);
		} else if (cooldownIndicator == ViaRewindConfig.CooldownIndicator.ACTION_BAR) {
			sendActionBar(getTitle());
		} else if (cooldownIndicator == ViaRewindConfig.CooldownIndicator.BOSS_BAR) {
			sendBossBar((float) getCooldown());
		}
	}

	private void hide() {
		if (cooldownIndicator == ViaRewindConfig.CooldownIndicator.ACTION_BAR) {
			sendActionBar("§r");
		} else if (cooldownIndicator == ViaRewindConfig.CooldownIndicator.TITLE) {
			hideTitle();
		} else if (cooldownIndicator == ViaRewindConfig.CooldownIndicator.BOSS_BAR) {
			hideBossBar();
		}
	}

	private void hideBossBar() {
		if (bossUUID == null) return;
		PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_9.BOSSBAR, null, getUser());
		wrapper.write(Type.UUID, bossUUID);
		wrapper.write(Type.VAR_INT, 1);
		PacketUtil.sendPacket(wrapper, Protocol1_8To1_9.class, false, true);
		bossUUID = null;
	}

	private void sendBossBar(float cooldown) {
		PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_9.BOSSBAR, getUser());
		if (bossUUID == null) {
			bossUUID = UUID.randomUUID();
			wrapper.write(Type.UUID, bossUUID);
			wrapper.write(Type.VAR_INT, 0);
			wrapper.write(Type.COMPONENT, new JsonPrimitive(" "));
			wrapper.write(Type.FLOAT, cooldown);
			wrapper.write(Type.VAR_INT, 0);
			wrapper.write(Type.VAR_INT, 0);
			wrapper.write(Type.UNSIGNED_BYTE, (short) 0);
		} else {
			wrapper.write(Type.UUID, bossUUID);
			wrapper.write(Type.VAR_INT, 2);
			wrapper.write(Type.FLOAT, cooldown);
		}
		PacketUtil.sendPacket(wrapper, Protocol1_8To1_9.class, false, true);
	}

	private void hideTitle() {
		PacketWrapper hide = PacketWrapper.create(ClientboundPackets1_8.TITLE, null, getUser());
		hide.write(Type.VAR_INT, 3);
		PacketUtil.sendPacket(hide, Protocol1_8To1_9.class);
	}

	private void sendTitle(String title, String subTitle, int fadeIn, int stay, int fadeOut) {
		PacketWrapper timePacket = PacketWrapper.create(ClientboundPackets1_8.TITLE, null, getUser());
		timePacket.write(Type.VAR_INT, 2);
		timePacket.write(Type.INT, fadeIn);
		timePacket.write(Type.INT, stay);
		timePacket.write(Type.INT, fadeOut);
		PacketWrapper titlePacket = PacketWrapper.create(ClientboundPackets1_8.TITLE, getUser());
		titlePacket.write(Type.VAR_INT, 0);
		titlePacket.write(Type.COMPONENT, new JsonPrimitive(title));
		PacketWrapper subtitlePacket = PacketWrapper.create(ClientboundPackets1_8.TITLE, getUser());
		subtitlePacket.write(Type.VAR_INT, 1);
		subtitlePacket.write(Type.COMPONENT, new JsonPrimitive(subTitle));

		PacketUtil.sendPacket(titlePacket, Protocol1_8To1_9.class);
		PacketUtil.sendPacket(subtitlePacket, Protocol1_8To1_9.class);
		PacketUtil.sendPacket(timePacket, Protocol1_8To1_9.class);
	}

	private void sendActionBar(String bar) {
		PacketWrapper actionBarPacket = PacketWrapper.create(ClientboundPackets1_8.CHAT_MESSAGE, getUser());
		actionBarPacket.write(Type.COMPONENT, new JsonPrimitive(bar));
		actionBarPacket.write(Type.BYTE, (byte) 2);

		PacketUtil.sendPacket(actionBarPacket, Protocol1_8To1_9.class);
	}

	public boolean hasCooldown() {
		long time = System.currentTimeMillis() - lastHit;
		double cooldown = restrain(((double) time) * attackSpeed / 1000d, 0, 1.5);
		return cooldown > 0.1 && cooldown < 1.1;
	}

	public double getCooldown() {
		long time = System.currentTimeMillis() - lastHit;
		return restrain(((double) time) * attackSpeed / 1000d, 0, 1);
	}

	private double restrain(double x, double a, double b) {
		if (x < a) return a;
		return Math.min(x, b);
	}

	private static final int max = 10;

	private String getTitle() {
		String symbol = cooldownIndicator == ViaRewindConfig.CooldownIndicator.ACTION_BAR ? "■" : "˙";

		double cooldown = getCooldown();
		int green = (int) Math.floor(((double) max) * cooldown);
		int grey = max - green;
		StringBuilder builder = new StringBuilder("§8");
		while (green-- > 0) builder.append(symbol);
		builder.append("§7");
		while (grey-- > 0) builder.append(symbol);
		return builder.toString();
	}

	public double getAttackSpeed() {
		return attackSpeed;
	}

	public void setAttackSpeed(double attackSpeed) {
		this.attackSpeed = attackSpeed;
	}

	public void setAttackSpeed(double base, ArrayList<Pair<Byte, Double>> modifiers) {
		attackSpeed = base;
		for (int j = 0; j < modifiers.size(); j++) {
			if (modifiers.get(j).key() == 0) {
				attackSpeed += modifiers.get(j).value();
				modifiers.remove(j--);
			}
		}
		for (int j = 0; j < modifiers.size(); j++) {
			if (modifiers.get(j).key() == 1) {
				attackSpeed += base * modifiers.get(j).value();
				modifiers.remove(j--);
			}
		}
		for (int j = 0; j < modifiers.size(); j++) {
			if (modifiers.get(j).key() == 2) {
				attackSpeed *= (1.0 + modifiers.get(j).value());
				modifiers.remove(j--);
			}
		}
	}

	public void hit() {
		lastHit = System.currentTimeMillis();
	}

	public void setLastHit(long lastHit) {
		this.lastHit = lastHit;
	}
}
