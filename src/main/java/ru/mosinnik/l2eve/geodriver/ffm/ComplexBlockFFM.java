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


/**
 * MemorySegment data тут это слайс от общего объема, взятый в точке начала блока.
 */
public final class ComplexBlockFFM {

    public static final ValueLayout.OfShort SHORT_LAYOUT = ValueLayout.JAVA_SHORT
            .withOrder(ByteOrder.nativeOrder());

    /**
     * readable:
     * int cellOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private static int getCellNSWE(int geoX, int geoY, MemorySegment data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return data.get(SHORT_LAYOUT, 2L * cellOffset);
    }


    private static int getCellHeight(int geoX, int geoY, MemorySegment data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int height = data.get(SHORT_LAYOUT, 2L * cellOffset) & 0xFFFFFFF0;
        return height >> 1;
    }


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, MemorySegment data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return (data.get(SHORT_LAYOUT, 2L * cellOffset) & nswe) == nswe;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        return getCellHeight(geoX, geoY, data);
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        int cellHeight = getCellHeight(geoX, geoY, data);
        return Math.min(cellHeight, worldZ);
    }

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, MemorySegment data) {
        int cellHeight = getCellHeight(geoX, geoY, data);
        return Math.max(cellHeight, worldZ);
    }
}
