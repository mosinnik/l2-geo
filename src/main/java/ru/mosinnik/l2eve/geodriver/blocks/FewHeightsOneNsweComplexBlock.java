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

package ru.mosinnik.l2eve.geodriver.blocks;


import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.util.BlockStat;

import java.util.HashMap;
import java.util.Map;

/**
 * FewHeightsComplexBlock, но для случая когда используется одинаковый nswe во всех ячейках
 * Таких блоков 1193065
 * На всем объеме геодаты экономия ~38Мб
 */
public final class FewHeightsOneNsweComplexBlock implements IBlock {
    private final byte[] data = new byte[IBlock.BLOCK_CELLS / 2];
    private final short[] heights;
    private final byte nswe;

    public FewHeightsOneNsweComplexBlock(short[] tmpData, BlockStat blockStat) {
        if (blockStat.heights.size() > 16) {
            throw new RuntimeException("More than 16 heights for FewHeightsComplexBlock");
        }
        if (blockStat.nswes.size() != 1) {
            throw new RuntimeException("More than one NSWE for FewHeightsOneNsweComplexBlock");
        }

        heights = new short[blockStat.heights.size()];
        Map<Integer, Integer> heightToIndex = new HashMap<>();
        int i = 0;
        for (Integer height : blockStat.heights) {
            heights[i] = (short) (int) height;
            heightToIndex.put(height, i);
            i++;
        }
        nswe = (byte) (int) blockStat.nswes.stream().findFirst().orElseThrow();

        for (int cellOffset = 0; cellOffset < tmpData.length; cellOffset++) {
            short height = (short) (tmpData[cellOffset] & 0x0FFF0);
            int heightI = height >> 1;
            int heightIndex = heightToIndex.get(heightI);
            if ((cellOffset & 0x01) == 0) {
                data[cellOffset / 2] |= (byte) (heightIndex & 0x0F);
            } else {
                data[cellOffset / 2] |= (byte) ((heightIndex & 0x0F) << 4);
            }
        }
    }

    public byte[] getData() {
        return data;
    }

    public short[] getHeights() {
        return heights;
    }

    public byte getNswe() {
        return nswe;
    }

    private int getCellHeight(int geoX, int geoY) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int heightIndex;
        if ((cellOffset & 0x01) == 0) {
            heightIndex = data[cellOffset / 2] & 0x0F;
        } else {
            heightIndex = (data[cellOffset / 2] >> 4) & 0x0F;
        }
        return heights[heightIndex];
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return (this.nswe & nswe) == nswe;
    }

    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        return getCellHeight(geoX, geoY);
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        int cellHeight = getCellHeight(geoX, geoY);
        return Math.min(cellHeight, worldZ);
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        int cellHeight = getCellHeight(geoX, geoY);
        return Math.max(cellHeight, worldZ);
    }
}
