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

package com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.entityreplacements;

import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.ClientboundPackets1_7;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.Protocol1_7_6_10To1_8;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.metadata.MetadataRewriter;
import com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.types.Types1_7_6_10;
import com.viaversion.viarewind.utils.PacketUtil;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_10Types;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;

import java.util.ArrayList;
import java.util.List;

public class EndermiteReplacement extends EntityReplacement1_7to1_8 {
	private final int entityId;
	private final List<Metadata> datawatcher = new ArrayList<>();
	private double locX, locY, locZ;
	private float yaw, pitch;
	private float headYaw;

	public EndermiteReplacement(int entityId, UserConnection user) {
		super(user);
		this.entityId = entityId;
		spawn();
	}

	@Override
	public void setLocation(double x, double y, double z) {
		this.locX = x;
		this.locY = y;
		this.locZ = z;
		updateLocation();
	}

	@Override
	public void relMove(double x, double y, double z) {
		this.locX += x;
		this.locY += y;
		this.locZ += z;
		updateLocation();
	}

	@Override
	public void setYawPitch(float yaw, float pitch) {
		if (this.yaw != yaw || this.pitch != pitch) {
			this.yaw = yaw;
			this.pitch = pitch;
			updateLocation();
		}
	}

	@Override
	public void setHeadYaw(float yaw) {
		if (this.headYaw != yaw) {
			this.headYaw = yaw;
			updateLocation();
		}
	}

	@Override
	public void updateMetadata(List<Metadata> metadataList) {
		for (Metadata metadata : metadataList) {
			datawatcher.removeIf(m -> m.id() == metadata.id());
			datawatcher.add(metadata);
		}
		updateMetadata();
	}

	public void updateLocation() {
		sendTeleportWithHead(entityId, locX, locY, locZ, yaw, pitch, headYaw);
	}

	public void updateMetadata() {
		PacketWrapper metadataPacket = PacketWrapper.create(ClientboundPackets1_7.ENTITY_METADATA, user);
		metadataPacket.write(Type.INT, entityId);

		List<Metadata> metadataList = new ArrayList<>();
		for (Metadata metadata : datawatcher) {
			metadataList.add(new Metadata(metadata.id(), metadata.metaType(), metadata.getValue()));
		}

		MetadataRewriter.transform(Entity1_10Types.EntityType.SQUID, metadataList);

		metadataPacket.write(Types1_7_6_10.METADATA_LIST, metadataList);

		PacketUtil.sendPacket(metadataPacket, Protocol1_7_6_10To1_8.class);
	}

	@Override
	public void spawn() {
		sendSpawn(entityId, 60, locX, locY, locZ);
	}

	@Override
	public void despawn() {
		PacketWrapper despawn = PacketWrapper.create(ClientboundPackets1_7.DESTROY_ENTITIES, null, user);
		despawn.write(Types1_7_6_10.INT_ARRAY, new int[]{entityId});

		PacketUtil.sendPacket(despawn, Protocol1_7_6_10To1_8.class, true, true);
	}

	@Override
	public int getEntityId() {
		return this.entityId;
	}
}
