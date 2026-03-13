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
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public final class ComplexBlockFFM {


//    public static final ValueLayout.OfShort SHORT_LAYOUT = ValueLayout.JAVA_SHORT_UNALIGNED
//            .withOrder(ByteOrder.BIG_ENDIAN);

    public static final ValueLayout.OfShort SHORT_LAYOUT = ValueLayout.JAVA_SHORT
            .withOrder(ByteOrder.nativeOrder());

    /**
     * readable:
     * int cellOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private static int getCellNSWE(int geoX, int geoY, int blockDataOffset, MemorySegment data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return data.get(SHORT_LAYOUT, blockDataOffset + 2 * cellOffset) & 0x0F;
//        return data.getShort(blockDataOffset + 2 * cellOffset) & 0x0F;
    }


    private static int getCellHeight(int geoX, int geoY, int blockDataOffset, MemorySegment data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
//        int height = data.getShort(blockDataOffset + 2 * cellOffset) & 0xFFFFFFF0;
        int height = data.get(SHORT_LAYOUT, blockDataOffset + 2 * cellOffset) & 0xFFFFFFF0;
        return height >> 1;
    }


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, int blockDataOffset, MemorySegment data) {
        return (getCellNSWE(geoX, geoY, blockDataOffset, data) & nswe) == nswe;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, int blockDataOffset, MemorySegment data) {
        return getCellHeight(geoX, geoY, blockDataOffset, data);
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, int blockDataOffset, MemorySegment data) {
        int cellHeight = getCellHeight(geoX, geoY, blockDataOffset, data);
        return Math.min(cellHeight, worldZ);
    }

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, int blockDataOffset, MemorySegment data) {
        int cellHeight = getCellHeight(geoX, geoY, blockDataOffset, data);
        return Math.max(cellHeight, worldZ);
    }
}
