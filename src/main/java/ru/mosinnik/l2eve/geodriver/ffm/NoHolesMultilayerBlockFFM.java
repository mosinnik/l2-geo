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

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static ru.mosinnik.l2eve.geodriver.ffm.Indexed32MultilayerBlockFFM.SHORT_HANDLE;

public class NoHolesMultilayerBlockFFM {

    private static final VarHandle BYTE_HANDLE = JAVA_BYTE.varHandle();

    private static final long LAYER_COUNT_OFFSET = 0;
    private static final long INNER_DATA_OFFSET = 1;

    public static final int LAYER_DATA_SIZE = IBlock.BLOCK_CELLS;

    //---------------------------------------------------------------


    private static int extractLayerHeight(short layer) {
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }

    private static int extractLayerNswe(short layer) {
        return (byte) (layer & 0x000F);
    }

    /**
     * Don't need to optimized with layersCount == 1 or == 2 - no perf boost
     */
    private static short getNearestLayer(int geoX, int geoY, int worldZ, MemorySegment data) {
        byte layersCount = (byte) BYTE_HANDLE.get(data, (long) (LAYER_COUNT_OFFSET));
//        byte layersCount = data.get(LAYER_COUNT_OFFSET);

        int startOffset = 2 * layersCount * (((geoX & 0x07) << 3) + (geoY & 0x07));

        int nearestDZ = 0;
        short nearestData = 0;
        for (int i = 0; i < layersCount; i++) {
            short layerData = (short) SHORT_HANDLE.get(data, (long) (INNER_DATA_OFFSET + startOffset + 2 * i));
//            short layerData = data.getShort(INNER_DATA_OFFSET + startOffset + 2 * i);
            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerData; // exact z
            }

            int layerDZ = Math.abs(layerZ - worldZ);
            if (i == 0 || (layerDZ < nearestDZ)) {
                nearestDZ = layerDZ;
                nearestData = layerData;
            } else {
                return nearestData;
            }
        }

        return nearestData;
    }

    private static int getNearestNSWE(int geoX, int geoY, int worldZ, MemorySegment data) {
        return extractLayerNswe(getNearestLayer(geoX, geoY, worldZ, data));
    }


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, MemorySegment data) {
        return (getNearestNSWE(geoX, geoY, worldZ, data) & nswe) == nswe;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        return extractLayerHeight(getNearestLayer(geoX, geoY, worldZ, data));
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        byte layersCount = (byte) BYTE_HANDLE.get(data, (long) (LAYER_COUNT_OFFSET));
        int startOffset = 2 * layersCount * (((geoX & 0x07) << 3) + (geoY & 0x07));

        long baseOffset = INNER_DATA_OFFSET + startOffset;
        for (int i = 0; i < layersCount; i++) {
            short layerData = (short) SHORT_HANDLE.get(data, (long) (baseOffset + 2 * i));
//            short layerData = data.getShort(baseOffset + 2 * i);
            int layerZ = extractLayerHeight(layerData);
            if (layerZ <= worldZ) {
                return layerZ;
            }
        }
        return worldZ;
    }

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        byte layersCount = (byte) BYTE_HANDLE.get(data, (long) (LAYER_COUNT_OFFSET));
        int startOffset = 2 * layersCount * (((geoX & 0x07) << 3) + (geoY & 0x07));

        long baseOffset = INNER_DATA_OFFSET + startOffset;
        int prevLayerZ = worldZ;
        for (int i = 0; i < layersCount; i++) {
            short layerData = (short) SHORT_HANDLE.get(data, (long) (baseOffset + 2 * i));
//            short layerData = data.getShort(baseOffset + 2 * i);
            int layerZ = extractLayerHeight(layerData);
            if (layerZ < worldZ) {
                return prevLayerZ;
            }
            prevLayerZ = layerZ;
        }
        return prevLayerZ;
    }
}
