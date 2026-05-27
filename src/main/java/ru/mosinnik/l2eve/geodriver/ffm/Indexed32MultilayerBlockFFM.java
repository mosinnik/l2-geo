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

package ru.mosinnik.l2eve.geodriver.ffm;


import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

import static java.lang.foreign.ValueLayout.JAVA_SHORT_UNALIGNED;

public class Indexed32MultilayerBlockFFM {

    public static final VarHandle SHORT_HANDLE = JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN).varHandle();


    public static final int INDEX_SIZE = 2 * IBlock.BLOCK_CELLS;

    private static final int INDEX_OFFSET = 0;
    private static final int INNER_DATA_OFFSET = INDEX_SIZE;

    //---------------------------------------------------------------

    /**
     * readable:
     * int cellLocalOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private static int getCellDataOffset(int geoX, int geoY, MemorySegment data) {
        int cellLocalOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return (short) SHORT_HANDLE.get(data, (long) (INDEX_OFFSET + 2 * cellLocalOffset));
//        return data.getShort( INDEX_OFFSET + 2 * cellLocalOffset);
    }


    private static int extractLayerNswe(short layer) {
        return (byte) (layer & 0x000F);
    }

    private static int extractLayerHeight(short layer) {
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }

    private static short extractLayerData(int dataOffset, MemorySegment data) {
        return (short) SHORT_HANDLE.get(data, (long) (INNER_DATA_OFFSET + dataOffset));
//        return data.getShort( INNER_DATA_OFFSET + dataOffset);
    }


    private static short getNearestLayer(int geoX, int geoY, int worldZ, MemorySegment data) {
        int cellDataOffset = getCellDataOffset(geoX, geoY, data);
        int startOffset = (cellDataOffset & 0x07FF) << 1;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        if (nLayers == 1) {
            return (short) SHORT_HANDLE.get(data, (long) (INNER_DATA_OFFSET + startOffset));
//            return data.getShort( INNER_DATA_OFFSET + startOffset);
        }
        if (nLayers == 2) {
            short layerDataH = (short) SHORT_HANDLE.get(data, (long) (INNER_DATA_OFFSET + startOffset));
//            short layerDataH = data.getShort( INNER_DATA_OFFSET + startOffset);
            short layer1 = layerDataH;
            layer1 = (short) (layer1 & 0x0fff0);
            int layerZH = layer1 >> 1;
            if (layerZH <= worldZ) {
                return layerDataH;
            }

            short layerDataL = (short) SHORT_HANDLE.get(data, (long) (INNER_DATA_OFFSET + startOffset + 2));
//            short layerDataL = data.getShort( INNER_DATA_OFFSET + startOffset + 2);
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
            short layerData = extractLayerData(offset, data);
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

    private static int getNearestNSWE(int geoX, int geoY, int worldZ, MemorySegment data) {
        short nearestLayer = getNearestLayer(geoX, geoY, worldZ, data);
        return extractLayerNswe(nearestLayer);
    }


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, MemorySegment data) {
        return (getNearestNSWE(geoX, geoY, worldZ, data) & nswe) == nswe;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        short layer = getNearestLayer(geoX, geoY, worldZ, data);
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        int cellDataOffset = getCellDataOffset(geoX, geoY, data);
        int startOffset = (cellDataOffset & 0x07FF) << 1;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        int endOffset = startOffset + 2 * nLayers;
        for (int offset = startOffset; offset < endOffset; offset += 2) {
            short layerData = (short) SHORT_HANDLE.get(data, (long) (INNER_DATA_OFFSET + offset));
//            short layerData = data.getShort( INNER_DATA_OFFSET + offset);
            layerData = (short) (layerData & 0x0fff0);
            int layerZ = layerData >> 1;
            if (layerZ <= worldZ) {
                return layerZ;
            }
        }
        return worldZ;
    }

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        int cellDataOffset = getCellDataOffset(geoX, geoY, data);
        int startOffset = (cellDataOffset & 0x07FF) << 1;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        int prevLayerZ = worldZ;
        int endOffset = startOffset + 2 * nLayers;
        // offset - is bytes offset, so we need +=2 to iterate over shorts
        for (int offset = startOffset; offset < endOffset; offset += 2) {
            short layerData = (short) SHORT_HANDLE.get(data, (long) (INNER_DATA_OFFSET + offset));
//            short layerData = data.getShort( INNER_DATA_OFFSET + offset);
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
