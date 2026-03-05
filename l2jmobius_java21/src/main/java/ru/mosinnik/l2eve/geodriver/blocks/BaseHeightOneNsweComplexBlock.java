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

/**
 * Комплекс блоки, в которых одинаковый NSWE, можно выделить NSWE, тогда можно высвободить
 * 4 бита под хранение высоты. Таким образом можно в 1 байт упаковать высоты дельтой max-min = 2040.
 */
public final class BaseHeightOneNsweComplexBlock implements IBlock {
    private final byte[] data = new byte[IBlock.BLOCK_CELLS];
    private final short baseHeight;
    private final byte nswe;

    public BaseHeightOneNsweComplexBlock(short[] tmpData, BlockStat blockStat) {
        if (blockStat.nswes.size() != 1) {
            throw new RuntimeException("More than one NSWE for BaseHeightOneNsweComplexBlock");
        }

        int baseHeightTmp = blockStat.heights.stream()
            .min(Integer::compareTo)
            .orElseThrow();
        baseHeight = (short) baseHeightTmp;
        nswe = (byte) (int) blockStat.nswes.stream().findFirst().orElseThrow();


        for (int cellOffset = 0; cellOffset < tmpData.length; cellOffset++) {
            short height = (short) (tmpData[cellOffset] & 0x0FFF0);
            int heightI = height >> 1;
            // всегда положительно
            int heightBased = (heightI - baseHeightTmp) >> 3;
            if (heightBased < 0 || 255 < heightBased) {
                throw new RuntimeException("Invalid height after rebase, base: " + baseHeightTmp + " heightI: " + heightI + " heightBased:" + heightBased);
            }
            data[cellOffset] = (byte) heightBased;
        }
    }

    public byte[] getData() {
        return data;
    }

    public short getBaseHeight() {
        return baseHeight;
    }

    public byte getNswe() {
        return nswe;
    }

    private int getCellHeight(int geoX, int geoY) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int height = (data[cellOffset] & 0x0000_00FF) << 3;
        return height + baseHeight;
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
