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


import java.lang.foreign.MemorySegment;
import java.lang.invoke.VarHandle;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class MultilayerBlockFFM {

    //---------------------------------------------------------------

    public static final VarHandle INT_HANDLE = JAVA_INT.varHandle();
    public static final VarHandle BYTE_HANDLE = JAVA_BYTE.varHandle();

    /**
     * readable:
     * int cellLocalOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private static int getCellDataOffset(int geoX, int geoY, int blockDataOffset, MemorySegment data) {
        int cellLocalOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int cellDataOffset = 0;

        // move index to cell, we need to parse on each request, OR we parse on creation and save indexes
        for (int i = 0; i < cellLocalOffset; i++) {
//            cellDataOffset += 1 + (data.getAtIndex(JAVA_BYTE, blockDataOffset + cellDataOffset) * 2);
            cellDataOffset += 1 + ((byte) BYTE_HANDLE.get(data, (long)(blockDataOffset + cellDataOffset)) * 2);
        }
        // now the index points to the cell we need
        return cellDataOffset;
    }

    private static short extractLayerData(int dataOffset, int blockDataOffset, MemorySegment data) {
        return (short) (((byte) BYTE_HANDLE.get(data, (long)(blockDataOffset + dataOffset)) & 0xFF) |
                ((byte) BYTE_HANDLE.get(data, (long)(blockDataOffset + dataOffset + 1)) << 8));
    }

//    private static short extractLayerData(int dataOffset, int blockDataOffset, MemorySegment data) {
//        return (short) ((data.getAtIndex(JAVA_BYTE, blockDataOffset + dataOffset) & 0xFF) |
//                (data.getAtIndex(JAVA_BYTE, blockDataOffset + dataOffset + 1) << 8));
//    }

    private static int extractLayerHeight(short layer) {
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }


    private static short getNearestLayer(int geoX, int geoY, int worldZ, int blockDataOffset, MemorySegment data) {

        // локальный оффсет
        int startOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
//        byte nLayers = data.getAtIndex(JAVA_BYTE, blockDataOffset + startOffset);
        byte nLayers = (byte) BYTE_HANDLE.get(data, (long)(blockDataOffset + startOffset));
        int endOffset = startOffset + 1 + (nLayers * 2);

//        System.out.println("------- FFM getNearestLayer");
//        System.out.println("startOffset = " + startOffset);
//        System.out.println("nLayers = " + nLayers);
//        System.out.println("endOffset = " + endOffset);

        // 1 layer at least was required on loading so this is set at least once on the loop below
        int nearestDZ = 0;
        short nearestData = 0;
        for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
            short layerData = extractLayerData(offset, blockDataOffset, data);
            int layerZ = extractLayerHeight(layerData);
//            System.out.println("layerData = " + layerData);
//            System.out.println("layerZ = " + layerZ);
            if (layerZ == worldZ) {
//                System.out.println(" return in loop: " + layerData);
                return layerData; // exact z
            }

            int layerDZ = Math.abs(layerZ - worldZ);
//            System.out.println("layerDZ = " + layerDZ);
            if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ)) {
                nearestDZ = layerDZ;
                nearestData = layerData;
            }
        }
//        System.out.println(" return after loop: " + nearestData);
        return nearestData;
    }


    private static int extractLayerNswe(short layer) {
        return (byte) (layer & 0x000F);
    }

    private static int getNearestNSWE(int geoX, int geoY, int worldZ, int blockDataOffset, MemorySegment data) {
        return extractLayerNswe(getNearestLayer(geoX, geoY, worldZ, blockDataOffset, data));
    }


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, int blockDataOffset, MemorySegment data) {
        return (getNearestNSWE(geoX, geoY, worldZ, blockDataOffset, data) & nswe) == nswe;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, int blockDataOffset, MemorySegment data) {
        return extractLayerHeight(getNearestLayer(geoX, geoY, worldZ, blockDataOffset, data));
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, int blockDataOffset, MemorySegment data) {
        int startOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
        byte nLayers = data.getAtIndex(JAVA_BYTE, blockDataOffset + startOffset);
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

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, int blockDataOffset, MemorySegment data) {
        int startOffset = getCellDataOffset(geoX, geoY, blockDataOffset, data);
        byte nLayers = data.getAtIndex(JAVA_BYTE, blockDataOffset + startOffset);
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
