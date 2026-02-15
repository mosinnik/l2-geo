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
 * Блоки в которых используется до 16 разных высот можно упаковать компактнее.
 * Для этого создан отдельный массив-маппинг для используемых высот, а в самом
 * массиве данных высота меняется на индекс высоты в массиве-маппинге.
 * Таким образом на одну ячейку приходится 4 бита для enws и 4 бита под индекс - 1 байт вместо двух у обычного
 * комплексного блока.
 * На всем объеме геодаты экономия ~32Мб
 */
public final class FewHeightsComplexBlock implements IBlock {
    private final byte[] data = new byte[IBlock.BLOCK_CELLS];
    private final short[] heights;

    public FewHeightsComplexBlock(short[] tmpData, BlockStat blockStat) {
        if (blockStat.heights.size() > 16) {
            throw new RuntimeException("More than 16 heights for FewHeightsComplexBlock");
        }

        heights = new short[blockStat.heights.size()];
        Map<Integer, Integer> heightToIndex = new HashMap<>();
        int i = 0;
        for (Integer height : blockStat.heights) {
            heights[i] = (short) (int) height;
            heightToIndex.put(height, i);
            i++;
        }

        for (int cellOffset = 0; cellOffset < tmpData.length; cellOffset++) {
            short height = (short) (tmpData[cellOffset] & 0x0FFF0);
            int heightI = height >> 1;
            int heightIndex = heightToIndex.get(heightI);
            data[cellOffset] = (byte) (((heightIndex & 0x0F) << 4) | (tmpData[cellOffset] & 0x0F));
        }
    }

    public byte[] getData() {
        return data;
    }

    public short[] getHeights() {
        return heights;
    }

    private byte getCellNSWE(int geoX, int geoY) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return (byte) (data[cellOffset] & 0x0F);
    }

    private int getCellHeight(int geoX, int geoY) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int heightIndex = (data[cellOffset] >> 4) & 0x0F;
        return heights[heightIndex];
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return (getCellNSWE(geoX, geoY) & nswe) == nswe;
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
