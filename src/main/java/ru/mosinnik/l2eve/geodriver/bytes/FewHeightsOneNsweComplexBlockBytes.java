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
import ru.mosinnik.l2eve.geodriver.blocks.FewHeightsOneNsweComplexBlock;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;


public final class FewHeightsOneNsweComplexBlockBytes {

    public static final int INNER_DATA_SIZE = IBlock.BLOCK_CELLS / 2;

    private static final int NSWE_OFFSET = 0;
    private static final int INNER_DATA_OFFSET = 1;
    private static final int HEIGHTS_OFFSET = INNER_DATA_OFFSET + INNER_DATA_SIZE;

    public static int calcBytesCount(FewHeightsOneNsweComplexBlock block) {
        return 1 + INNER_DATA_SIZE + 2 * block.getHeights().length;
    }

    public static void appendBytes(FewHeightsOneNsweComplexBlock block, ByteBuffer data) {
        if (block.getData().length != INNER_DATA_SIZE) {
            throw new IllegalArgumentException("block length must be " + INNER_DATA_SIZE);
        }
        data.put(block.getNswe());
        data.put(block.getData());
        for (short height : block.getHeights()) {
            data.putShort(height);
        }
    }

    public static byte[] toBytes(FewHeightsOneNsweComplexBlock block) {
        ByteBuffer buffer = ByteBuffer.allocate(calcBytesCount(block));
        appendBytes(block, buffer);
        return buffer.array();
    }

    public static int getSize(int blockDataOffset, ByteBuffer data) {
        Set<Integer> heights = new HashSet<>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                heights.add(getCellHeight(x, y, blockDataOffset, data));
            }
        }
        return 1 + INNER_DATA_SIZE + 2 * heights.size();
    }

    //---------------------------------------------------------------


    private static int getCellHeight(int geoX, int geoY, int blockDataOffset, ByteBuffer data) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int heightIndex;
        if ((cellOffset & 0x01) == 0) {
            heightIndex = data.get(blockDataOffset + INNER_DATA_OFFSET + cellOffset / 2) & 0x0F;
        } else {
            heightIndex = (data.get(blockDataOffset + INNER_DATA_OFFSET + cellOffset / 2) >> 4) & 0x0F;
        }

        return data.getShort(blockDataOffset + HEIGHTS_OFFSET + 2 * heightIndex);
    }

    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe, int blockDataOffset, ByteBuffer data) {
        return (data.get(blockDataOffset + NSWE_OFFSET) & nswe) == nswe;
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
