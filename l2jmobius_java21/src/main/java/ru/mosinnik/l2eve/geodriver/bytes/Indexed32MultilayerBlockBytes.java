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
import ru.mosinnik.l2eve.geodriver.blocks.Indexed32MultilayerBlock;

import java.nio.ByteBuffer;

public class Indexed32MultilayerBlockBytes {

    public static final int INDEX_SIZE = 2 * IBlock.BLOCK_CELLS;

    private static final int INDEX_OFFSET = 0;
    private static final int INNER_DATA_OFFSET = INDEX_SIZE;

    public static int calcBytesCount(Indexed32MultilayerBlock block) {
        return INDEX_SIZE + 2 * block.getData().length;
    }

    public static void appendBytes(Indexed32MultilayerBlock block, ByteBuffer data) {
        for (short index : block.getIndex()) {
            data.putShort(index);
        }
        for (short layerData : block.getData()) {
            data.putShort(layerData);
        }
    }

    public static byte[] toBytes(Indexed32MultilayerBlock block) {
        ByteBuffer buffer = ByteBuffer.allocate(calcBytesCount(block));
        appendBytes(block, buffer);
        return buffer.array();
    }

    public static int getSize(int blockDataOffset, ByteBuffer data) {
        int totalLayersCount = 0;
        for (int i = 0; i < IBlock.BLOCK_CELLS; i++) {
            short cellDataOffset = data.getShort(blockDataOffset + INDEX_OFFSET + i);
            int nLayers = (cellDataOffset >> 11) & 0x01F;
            totalLayersCount += nLayers;
        }

        return INNER_DATA_OFFSET + 2 * totalLayersCount;
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


    private static int extractLayerNswe(short layer) {
        return (byte) (layer & 0x000F);
    }

    private static int extractLayerHeight(short layer) {
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }

    private static short extractLayerData(int dataOffset, int blockDataOffset, ByteBuffer data) {
        return data.getShort(blockDataOffset + INNER_DATA_OFFSET + dataOffset);
    }


    private static short getNearestLayer(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        int cellDataOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
        int startOffset = (cellDataOffset & 0x01FF) << 1;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        if (nLayers == 1) {
            return data.getShort(blockDataOffset + INNER_DATA_OFFSET + startOffset);
        }
        if (nLayers == 2) {
            short layerDataH = data.getShort(blockDataOffset + INNER_DATA_OFFSET + startOffset);
            short layer1 = layerDataH;
            layer1 = (short) (layer1 & 0x0fff0);
            int layerZH = layer1 >> 1;
            if (layerZH <= worldZ) {
                return layerDataH;
            }

            short layerDataL = data.getShort(blockDataOffset + INNER_DATA_OFFSET + startOffset + 2);
            short layer = layerDataL;
            layer = (short) (layer & 0x0fff0);
            int layerZL = layer >> 1;
            if (layerZL >= worldZ) {
                return layerDataL;
            }

            int layerDZH = layerZH - worldZ;
            int layerDZL = worldZ - layerZL;
            if (layerDZH <= layerDZL) {
                return layerDataH;
            } else {
                return layerDataL;
            }
        }
        int endOffset = startOffset + 2 * nLayers;

        // 1 layer at least was required on loading so this is set at least once on the loop below
        int nearestDZ = 0;
        short nearestData = 0;
        // offset - is bytes offset, so we need +=2 to iterate over shorts
        for (int offset = startOffset; offset < endOffset; offset += 2) {
            short layerData = extractLayerData(offset, blockDataOffset, data);
            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerData; // exact z
            }

            int layerDZ = Math.abs(layerZ - worldZ);
            if ((offset == startOffset) || (layerDZ < nearestDZ)) {
                nearestDZ = layerDZ;
                nearestData = layerData;
            } else {
                return nearestData;
            }
        }
        return nearestData;
    }

    private static int getNearestNSWE(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        short nearestLayer = getNearestLayer(geoX, geoY, worldZ, blockDataOffset, data);
        return extractLayerNswe(nearestLayer);
    }


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, int blockDataOffset, ByteBuffer data) {
        return (getNearestNSWE(geoX, geoY, worldZ, blockDataOffset, data) & nswe) == nswe;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        short layer = getNearestLayer(geoX, geoY, worldZ, blockDataOffset, data);
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        int cellDataOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
        int startOffset = (cellDataOffset & 0x01FF) << 1;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        int endOffset = startOffset + 2 * nLayers;
        for (int offset = startOffset; offset < endOffset; offset += 2) {
            short layerData = data.getShort(blockDataOffset + INNER_DATA_OFFSET + offset);
            layerData = (short) (layerData & 0x0fff0);
            int layerZ = layerData >> 1;
            if (layerZ <= worldZ) {
                return layerZ;
            }
        }
        return worldZ;
    }

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        int cellDataOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
        int startOffset = (cellDataOffset & 0x01FF) << 1;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        int prevLayerZ = worldZ;
        int endOffset = startOffset + 2 * nLayers;
        // offset - is bytes offset, so we need +=2 to iterate over shorts
        for (int offset = startOffset; offset < endOffset; offset += 2) {
            short layerData = data.getShort(blockDataOffset + INNER_DATA_OFFSET + offset);
            layerData = (short) (layerData & 0x0fff0);
            int layerZ = layerData >> 1;
            if (layerZ < worldZ) {
                return prevLayerZ;
            }
            prevLayerZ = layerZ;
        }
        return prevLayerZ;
    }
}
