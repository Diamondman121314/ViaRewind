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

package com.viaversion.viarewind.protocol.protocol1_7_6_10to1_8.types;

import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.NBTIO;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressedNBTType extends Type<CompoundTag> {
	public CompressedNBTType() {
		super(CompoundTag.class);
	}

	@Override
	public CompoundTag read(ByteBuf buffer) throws IOException {
		short length = buffer.readShort();
		if (length <= 0) {
			return null;
		}

		ByteBuf compressed = buffer.readSlice(length);

		try (GZIPInputStream gzipStream = new GZIPInputStream(new ByteBufInputStream(compressed))) {
			return NBTIO.readTag(gzipStream);
		}
	}

	@Override
	public void write(ByteBuf buffer, CompoundTag nbt) throws Exception {
		if (nbt == null) {
			buffer.writeShort(-1);
			return;
		}

		ByteBuf compressedBuf = buffer.alloc().buffer();
		try {
			try (GZIPOutputStream gzipStream = new GZIPOutputStream(new ByteBufOutputStream(compressedBuf))) {
				NBTIO.writeTag(gzipStream, nbt);
			}

			buffer.writeShort(compressedBuf.readableBytes());
			buffer.writeBytes(compressedBuf);
		} finally {
			compressedBuf.release();
		}
	}
}
