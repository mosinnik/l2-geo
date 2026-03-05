/*
 * Copyright (c) 2026 mosinnik
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ru.mosinnik.l2eve.geodriver.bytes;


import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.blocks.IndexedMultilayerBlock;

import java.nio.ByteBuffer;

public class IndexedMultilayerBlockBytes {

    public static final int INDEX_SIZE = 2 * IBlock.BLOCK_CELLS;

    private static final int INDEX_OFFSET = 0;
    private static final int INNER_DATA_OFFSET = INDEX_SIZE;

    public static int calcBytesCount(IndexedMultilayerBlock block) {
        return INDEX_SIZE + block.getData().length;
    }

    public static void appendBytes(IndexedMultilayerBlock block, ByteBuffer data) {
        for (short index : block.getIndex()) {
            data.putShort(index);
        }
        data.put(block.getData());
    }

    public static byte[] toBytes(IndexedMultilayerBlock block) {
        ByteBuffer buffer = ByteBuffer.allocate(calcBytesCount(block));
        appendBytes(block, buffer);
        return buffer.array();
    }

    public static int getSize(int blockDataOffset, ByteBuffer data) {
        int cellDataOffset = 0;
        for (int i = 0; i < 64; i++) {
            cellDataOffset += 1 + (data.get(blockDataOffset + INNER_DATA_OFFSET + cellDataOffset) * 2);
        }
        return INNER_DATA_OFFSET + cellDataOffset;
    }

    //---------------------------------------------------------------

    /**
     * readable:
     * int cellLocalOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private static int getCellDataOffset(int geoX, int geoY, int blockDataOffset, ByteBuffer data) {
        int cellLocalOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return data.getShort(blockDataOffset + INDEX_OFFSET + 2 * cellLocalOffset);
    }

    private static short extractLayerData(int dataOffset, int blockDataOffset, ByteBuffer data) {
        return (short) ((data.get(blockDataOffset + INNER_DATA_OFFSET + dataOffset) & 0xFF) |
                (data.get(blockDataOffset + INNER_DATA_OFFSET + dataOffset + 1) << 8));
    }

    private static int extractLayerHeight(short layer) {
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }


    private static short getNearestLayer(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        int startOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
        byte nLayers = data.get(blockDataOffset + INNER_DATA_OFFSET + startOffset);
        int endOffset = startOffset + 1 + (nLayers * 2);

        // 1 layer at least was required on loading so this is set at least once on the loop below
        int nearestDZ = 0;
        short nearestData = 0;
        for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
            short layerData = extractLayerData(offset, blockDataOffset, data);
            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerData; // exact z
            }

            int layerDZ = Math.abs(layerZ - worldZ);
            if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ)) {
                nearestDZ = layerDZ;
                nearestData = layerData;
            }
        }
        return nearestData;
    }


    private static int extractLayerNswe(short layer) {
        return (byte) (layer & 0x000F);
    }

    private static int getNearestNSWE(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        short nearestLayer = getNearestLayer(geoX, geoY, worldZ, blockDataOffset, data);
        return extractLayerNswe(nearestLayer);
    }


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, int blockDataOffset, ByteBuffer data) {
        return (getNearestNSWE(geoX, geoY, worldZ, blockDataOffset, data) & nswe) == nswe;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        return extractLayerHeight(getNearestLayer(geoX, geoY, worldZ, blockDataOffset, data));
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        int startOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
        byte nLayers = data.get(blockDataOffset + INNER_DATA_OFFSET + startOffset);
        int endOffset = startOffset + 1 + (nLayers * 2);

        int lowerZ = Integer.MIN_VALUE;
        for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
            short layerData = extractLayerData(offset, blockDataOffset, data);

            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerZ;                // exact z
            }

            if ((layerZ < worldZ) && (layerZ > lowerZ)) {
                lowerZ = layerZ;
            }
        }
        return lowerZ == Integer.MIN_VALUE ? worldZ : lowerZ;
    }

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        int startOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
        byte nLayers = data.get(blockDataOffset + INNER_DATA_OFFSET + startOffset);
        int endOffset = startOffset + 1 + (nLayers * 2);

        int higherZ = Integer.MAX_VALUE;
        for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
            short layerData = extractLayerData(offset, blockDataOffset, data);

            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerZ;                // exact z
            }

            if ((layerZ > worldZ) && (layerZ < higherZ)) {
                higherZ = layerZ;
            }
        }
        return higherZ == Integer.MAX_VALUE ? worldZ : higherZ;
    }
}
