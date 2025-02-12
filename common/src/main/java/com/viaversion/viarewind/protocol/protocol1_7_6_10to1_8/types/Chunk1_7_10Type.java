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

import com.viaversion.viaversion.api.minecraft.Environment;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.type.PartialType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.util.zip.DeflaterOutputStream;

public class Chunk1_7_10Type extends PartialType<Chunk, ClientWorld> {

    public Chunk1_7_10Type(ClientWorld param) {
        super(param, Chunk.class);
    }

    @Override
    public Chunk read(ByteBuf byteBuf, ClientWorld clientWorld) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(ByteBuf output, ClientWorld clientWorld, Chunk chunk) throws Exception {
        output.writeInt(chunk.getX());
        output.writeInt(chunk.getZ());
        output.writeBoolean(chunk.isFullChunk());
        output.writeShort(chunk.getBitmask());
        output.writeShort(0);

        ByteBuf dataToCompress = output.alloc().buffer();
        try {
            for (int i = 0; i < chunk.getSections().length; i++) {
                if ((chunk.getBitmask() & 1 << i) == 0) continue;
                ChunkSection section = chunk.getSections()[i];
				DataPalette palette = section.palette(PaletteType.BLOCKS);
				for (int j = 0; j < 4096; j++) {
					int block = palette.idAt(j);
                    dataToCompress.writeByte(block >> 4);
                }
            }

            for (int i = 0; i < chunk.getSections().length; i++) {
                if ((chunk.getBitmask() & 1 << i) == 0) continue;
                ChunkSection section = chunk.getSections()[i];
				DataPalette palette = section.palette(PaletteType.BLOCKS);
				for (int j = 0; j < 4096; j += 2) {
					int data0 = palette.idAt(j) & 0xF;
                    int data1 = palette.idAt(j + 1) & 0xF;

                    dataToCompress.writeByte((data1 << 4) | data0);
                }
            }

            for (int i = 0; i < chunk.getSections().length; i++) {
                if ((chunk.getBitmask() & 1 << i) == 0) continue;
                chunk.getSections()[i].getLight().writeBlockLight(dataToCompress);
            }

            boolean skyLight = clientWorld != null && clientWorld.getEnvironment() == Environment.NORMAL;
            if (skyLight) {
                for (int i = 0; i < chunk.getSections().length; i++) {
                    if ((chunk.getBitmask() & 1 << i) == 0) continue;
                    chunk.getSections()[i].getLight().writeSkyLight(dataToCompress);
                }
            }

            if (chunk.isFullChunk() && chunk.isBiomeData()) {
                for (int biome : chunk.getBiomeData()) {
                    dataToCompress.writeByte(biome);
                }
            }

            ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
            // todo config for compression level
            try (DeflaterOutputStream compressorStream = new DeflaterOutputStream(compressedStream)) {
                compressorStream.write(Type.REMAINING_BYTES.read(dataToCompress));
            }

            output.writeInt(compressedStream.size());
            output.writeBytes(compressedStream.toByteArray());
        } finally {
            dataToCompress.release();
        }
    }
}
