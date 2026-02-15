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
 * Есть блоки, в которых у всех ячеек одна высота, но разные флаги прохождения nswe.
 * Таких блоков 28349 и в них можно хранить одну высоты и выделить массив под хранение nswe
 * На всем объеме геодаты экономия ~1.6Мб
 */
public final class OneHeightComplexBlock implements IBlock {
    private final byte[] nswes;
    private final short height;


    public OneHeightComplexBlock(short[] tmpData, BlockStat blockStat) {
        if (blockStat.heights.size() > 1) {
            throw new RuntimeException("More than 1 diff heights");
        }
        int height = blockStat.heights.stream().findFirst().orElseThrow();
        this.height = (short) height;
        nswes = new byte[tmpData.length];
        for (int i = 0; i < tmpData.length; i++) {
            nswes[i] = (byte) (tmpData[i] & 0x0F);
        }
    }

    public byte[] getNswes() {
        return nswes;
    }

    public short getHeight() {
        return height;
    }

    private byte getCellNSWE(int geoX, int geoY) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return (byte) (nswes[cellOffset] & 0x0F);
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return (getCellNSWE(geoX, geoY) & nswe) == nswe;
    }

    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        return height;
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        return Math.min(height, worldZ);
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        return Math.max(height, worldZ);
    }
}
