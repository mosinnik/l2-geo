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
import ru.mosinnik.l2eve.geodriver.blocks.BaseHeightComplexBlock;

import java.nio.ByteBuffer;


public final class BaseHeightComplexBlockBytes {

    public static final int SIZE = 2 + IBlock.BLOCK_CELLS;

    private static final int INNER_DATA_OFFSET = 2;

    public static int calcBytesCount(BaseHeightComplexBlock block) {
        return SIZE;
    }

    public static void appendBytes(BaseHeightComplexBlock block, ByteBuffer data) {
        if (block.getData().length != IBlock.BLOCK_CELLS) {
            throw new IllegalArgumentException("block length must be " + IBlock.BLOCK_CELLS);
        }
        data.putShort(block.getBaseHeight());
        for (byte nswe : block.getData()) {
            data.put(nswe);
        }
    }

    public static byte[] toBytes(BaseHeightComplexBlock block) {
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
    private static byte getCellNSWE(int geoX, int geoY, int blockDataOffset, ByteBuffer data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return (byte) (data.get(blockDataOffset + INNER_DATA_OFFSET + cellOffset) & 0x0F);
    }

    private static int getCellHeight(int geoX, int geoY, int blockDataOffset, ByteBuffer data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int height = (data.get(blockDataOffset + INNER_DATA_OFFSET + cellOffset) & 0x0000_00F0) >> 1;
        return height + data.getShort(blockDataOffset);
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
