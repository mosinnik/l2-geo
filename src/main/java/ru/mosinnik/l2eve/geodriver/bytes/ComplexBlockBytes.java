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
import ru.mosinnik.l2eve.geodriver.blocks.ComplexBlock;

import java.nio.ByteBuffer;

public final class ComplexBlockBytes {


    public static final int SIZE = 2 * IBlock.BLOCK_CELLS;

    public static int calcBytesCount(ComplexBlock block) {
        return SIZE;
    }

    public static void appendBytes(ComplexBlock block, ByteBuffer data) {
        for (short height : block.getData()) {
            int position = data.position();
            data.putShort(height);
            short s2 = data.getShort(position);
            if (s2 != height) {
                throw new RuntimeException("Wrong block height");
            }
        }
    }

    public static byte[] toBytes(ComplexBlock block) {
        ByteBuffer buffer = ByteBuffer.allocate(calcBytesCount(block));
        appendBytes(block, buffer);
        return buffer.array();
    }

    public static int getSize(int blockDataOffset, ByteBuffer data) {
        return SIZE;
    }

    //---------------------------------------------------------------


    /**
     * readable:
     * int cellOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private static int getCellNSWE(int geoX, int geoY, int blockDataOffset, ByteBuffer data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return data.getShort(blockDataOffset + 2 * cellOffset) & 0x0F;
    }


    private static int getCellHeight(int geoX, int geoY, int blockDataOffset, ByteBuffer data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int height = data.getShort(blockDataOffset + 2 * cellOffset) & 0xFFFFFFF0;
        return height >> 1;
    }


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, int blockDataOffset, ByteBuffer data) {
        return (getCellNSWE(geoX, geoY, blockDataOffset, data) & nswe) == nswe;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        return getCellHeight(geoX, geoY, blockDataOffset, data);
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        int cellHeight = getCellHeight(geoX, geoY, blockDataOffset, data);
        return Math.min(cellHeight, worldZ);
    }

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        int cellHeight = getCellHeight(geoX, geoY, blockDataOffset, data);
        return Math.max(cellHeight, worldZ);
    }
}
