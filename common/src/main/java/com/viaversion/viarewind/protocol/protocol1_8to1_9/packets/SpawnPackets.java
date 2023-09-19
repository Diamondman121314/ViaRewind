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

package com.viaversion.viarewind.protocol.protocol1_8to1_9.packets;

import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.Protocol1_8To1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.entityreplacement.ShulkerBulletReplacement;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.entityreplacement.ShulkerReplacement;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.items.ReplacementRegistry1_8to1_9;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.metadata.MetadataRewriter;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.storage.EntityTracker;
import com.viaversion.viarewind.replacement.EntityReplacement;
import com.viaversion.viarewind.replacement.Replacement;
import com.viaversion.viarewind.utils.PacketUtil;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_10Types;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_8;
import com.viaversion.viaversion.api.type.types.version.Types1_9;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_8.ServerboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ServerboundPackets1_9;

import java.util.List;

public class SpawnPackets {

	public static void register(Protocol<ClientboundPackets1_9, ClientboundPackets1_8,
			ServerboundPackets1_9, ServerboundPackets1_8> protocol) {
		/*  OUTGOING  */

		//Spawn Object
		protocol.registerClientbound(ClientboundPackets1_9.SPAWN_ENTITY, new PacketHandlers() {
			@Override
			public void register() {
				map(Type.VAR_INT);
				map(Type.UUID, Type.NOTHING);
				map(Type.BYTE);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.INT);

				handler(packetWrapper -> {
					final int entityId = packetWrapper.get(Type.VAR_INT, 0);
					final int typeId = packetWrapper.get(Type.BYTE, 0);
					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					final Entity1_10Types.EntityType type = Entity1_10Types.getTypeFromId(typeId, true);

					//cancel AREA_EFFECT_CLOUD = 3, SPECTRAL_ARROW = 91, DRAGON_FIREBALL = 93
					if (typeId == 3 || typeId == 91 || typeId == 92 || typeId == 93) {
						packetWrapper.cancel();
						return;
					}

					if (type == null) {
						ViaRewind.getPlatform().getLogger().warning("[ViaRewind] Unhandled Spawn Object Type: " + typeId);
						packetWrapper.cancel();
						return;
					}

					int x = packetWrapper.get(Type.INT, 0);
					int y = packetWrapper.get(Type.INT, 1);
					int z = packetWrapper.get(Type.INT, 2);

					if (type.is(Entity1_10Types.EntityType.BOAT)) {
						byte yaw = packetWrapper.get(Type.BYTE, 1);
						yaw -= 64;
						packetWrapper.set(Type.BYTE, 1, yaw);
						y += 10;
						packetWrapper.set(Type.INT, 1, y);
					} else if (type.is(Entity1_10Types.EntityType.SHULKER_BULLET)) {
						packetWrapper.cancel();
						ShulkerBulletReplacement shulkerBulletReplacement = new ShulkerBulletReplacement(entityId, packetWrapper.user());
						shulkerBulletReplacement.setLocation(x / 32.0, y / 32.0, z / 32.0);
						tracker.addEntityReplacement(shulkerBulletReplacement);
						return;
					}

					int data = packetWrapper.get(Type.INT, 3);

					//Rewrite Object Data
					if (type.isOrHasParent(Entity1_10Types.EntityType.ARROW) && data != 0) {
						packetWrapper.set(Type.INT, 3, --data);
					}
					if (type.is(Entity1_10Types.EntityType.FALLING_BLOCK)) {
						int blockId = data & 0xFFF;
						int blockData = data >> 12 & 0xF;
						Replacement replace = ReplacementRegistry1_8to1_9.getReplacement(blockId, blockData);
						if (replace != null) {
							packetWrapper.set(Type.INT, 3, replace.getId() | replace.replaceData(data) << 12);
						}
					}

					if (data > 0) {
						packetWrapper.passthrough(Type.SHORT);
						packetWrapper.passthrough(Type.SHORT);
						packetWrapper.passthrough(Type.SHORT);
					} else {
						short vX = packetWrapper.read(Type.SHORT);
						short vY = packetWrapper.read(Type.SHORT);
						short vZ = packetWrapper.read(Type.SHORT);
						PacketWrapper velocityPacket = PacketWrapper.create(0x12, null, packetWrapper.user());
						velocityPacket.write(Type.VAR_INT, entityId);
						velocityPacket.write(Type.SHORT, vX);
						velocityPacket.write(Type.SHORT, vY);
						velocityPacket.write(Type.SHORT, vZ);
						PacketUtil.sendPacket(velocityPacket, Protocol1_8To1_9.class);
					}

					tracker.getClientEntityTypes().put(entityId, type);
					tracker.sendMetadataBuffer(entityId);
				});
			}
		});

		//Spawn Experience Orb
		protocol.registerClientbound(ClientboundPackets1_9.SPAWN_EXPERIENCE_ORB, new PacketHandlers() {
			@Override
			public void register() {
				map(Type.VAR_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.SHORT);
				handler(packetWrapper -> {
					int entityId = packetWrapper.get(Type.VAR_INT, 0);
					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					tracker.getClientEntityTypes().put(entityId, Entity1_10Types.EntityType.EXPERIENCE_ORB);
					tracker.sendMetadataBuffer(entityId);
				});
			}
		});

		//Spawn Global Entity
		protocol.registerClientbound(ClientboundPackets1_9.SPAWN_GLOBAL_ENTITY, new PacketHandlers() {
			@Override
			public void register() {
				map(Type.VAR_INT);
				map(Type.BYTE);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				handler(packetWrapper -> {
					int entityId = packetWrapper.get(Type.VAR_INT, 0);
					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					tracker.getClientEntityTypes().put(entityId, Entity1_10Types.EntityType.LIGHTNING);
					tracker.sendMetadataBuffer(entityId);
				});
			}
		});

		//Spawn Mob
		protocol.registerClientbound(ClientboundPackets1_9.SPAWN_MOB, new PacketHandlers() {
			@Override
			public void register() {
				map(Type.VAR_INT);
				map(Type.UUID, Type.NOTHING);
				map(Type.UNSIGNED_BYTE);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.BYTE);
				map(Type.SHORT);
				map(Type.SHORT);
				map(Type.SHORT);
				map(Types1_9.METADATA_LIST, Types1_8.METADATA_LIST);
				handler(packetWrapper -> {
					int entityId = packetWrapper.get(Type.VAR_INT, 0);
					int typeId = packetWrapper.get(Type.UNSIGNED_BYTE, 0);
					int x = packetWrapper.get(Type.INT, 0);
					int y = packetWrapper.get(Type.INT, 1);
					int z = packetWrapper.get(Type.INT, 2);
					byte pitch = packetWrapper.get(Type.BYTE, 1);
					byte yaw = packetWrapper.get(Type.BYTE, 0);
					byte headYaw = packetWrapper.get(Type.BYTE, 2);

					if (typeId == 69) {
						packetWrapper.cancel();
						EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
						ShulkerReplacement shulkerReplacement = new ShulkerReplacement(entityId, packetWrapper.user());
						shulkerReplacement.setLocation(x / 32.0, y / 32.0, z / 32.0);
						shulkerReplacement.setYawPitch(yaw * 360f / 256, pitch * 360f / 256);
						shulkerReplacement.setHeadYaw(headYaw * 360f / 256);
						tracker.addEntityReplacement(shulkerReplacement);
					} else if (typeId == -1 || typeId == 255) {
						packetWrapper.cancel();
					}
				});
				handler(packetWrapper -> {
					int entityId = packetWrapper.get(Type.VAR_INT, 0);
					int typeId = packetWrapper.get(Type.UNSIGNED_BYTE, 0);
					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					tracker.getClientEntityTypes().put(entityId, Entity1_10Types.getTypeFromId(typeId, false));
					tracker.sendMetadataBuffer(entityId);
				});
				handler(wrapper -> {
					List<Metadata> metadataList = wrapper.get(Types1_8.METADATA_LIST, 0);
					int entityId = wrapper.get(Type.VAR_INT, 0);
					EntityTracker tracker = wrapper.user().get(EntityTracker.class);
					EntityReplacement replacement;
					if ((replacement = tracker.getEntityReplacement(entityId)) != null) {
						replacement.updateMetadata(metadataList);
					} else if (tracker.getClientEntityTypes().containsKey(entityId)) {
						MetadataRewriter.transform(tracker.getClientEntityTypes().get(entityId), metadataList);
					} else {
						wrapper.cancel();
					}
				});
			}
		});

		//Spawn Painting
		protocol.registerClientbound(ClientboundPackets1_9.SPAWN_PAINTING, new PacketHandlers() {
			@Override
			public void register() {
				map(Type.VAR_INT);
				map(Type.UUID, Type.NOTHING);
				map(Type.STRING);
				map(Type.POSITION);
				map(Type.BYTE, Type.UNSIGNED_BYTE);
				handler(packetWrapper -> {
					int entityId = packetWrapper.get(Type.VAR_INT, 0);
					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					tracker.getClientEntityTypes().put(entityId, Entity1_10Types.EntityType.PAINTING);
					tracker.sendMetadataBuffer(entityId);
				});
			}
		});

		//Spawn Player
		protocol.registerClientbound(ClientboundPackets1_9.SPAWN_PLAYER, new PacketHandlers() {
			@Override
			public void register() {
				map(Type.VAR_INT);
				map(Type.UUID);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.DOUBLE, Protocol1_8To1_9.TO_OLD_INT);
				map(Type.BYTE);
				map(Type.BYTE);
				handler(packetWrapper -> packetWrapper.write(Type.SHORT, (short) 0));
				map(Types1_9.METADATA_LIST, Types1_8.METADATA_LIST);
				this.handler(wrapper -> {
					List<Metadata> metadataList = wrapper.get(Types1_8.METADATA_LIST, 0);
					MetadataRewriter.transform(Entity1_10Types.EntityType.PLAYER, metadataList);
				});
				handler(packetWrapper -> {
					int entityId = packetWrapper.get(Type.VAR_INT, 0);
					EntityTracker tracker = packetWrapper.user().get(EntityTracker.class);
					tracker.getClientEntityTypes().put(entityId, Entity1_10Types.EntityType.PLAYER);
					tracker.sendMetadataBuffer(entityId);
				});
			}
		});
	}
}
