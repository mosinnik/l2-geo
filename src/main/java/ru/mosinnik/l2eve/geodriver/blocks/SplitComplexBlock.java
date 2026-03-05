/*
 * Copyright (C) 2004-2013 L2J Server
 *
 * This file is part of L2J Server.
 *
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.mosinnik.l2eve.geodriver.blocks;


import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;

import java.nio.ByteBuffer;


public final class SplitComplexBlock implements IBlock {
    private final short[] heights;
    private final byte[] nswes;

    public SplitComplexBlock(ByteBuffer bb) {
        heights = new short[IBlock.BLOCK_CELLS];
        nswes = new byte[IBlock.BLOCK_CELLS];
        for (int cellOffset = 0; cellOffset < IBlock.BLOCK_CELLS; cellOffset++) {
            short layerData = bb.getShort();
            heights[cellOffset] = (short) ((layerData & 0xFFFFFFF0) >> 1);
            nswes[cellOffset] = (byte) (layerData & 0x0F);
        }
    }

    public SplitComplexBlock(short[] tmpData) {
        heights = new short[IBlock.BLOCK_CELLS];
        nswes = new byte[IBlock.BLOCK_CELLS];
        for (int cellOffset = 0; cellOffset < IBlock.BLOCK_CELLS; cellOffset++) {
            short layerData = tmpData[cellOffset];
            heights[cellOffset] = (short) ((layerData & 0xFFFFFFF0) >> 1);
            nswes[cellOffset] = (byte) (layerData & 0x0F);
        }
    }

    public short[] getHeights() {
        return heights;
    }

    public byte[] getNswes() {
        return nswes;
    }

    /**
     * readable:
     * int cellOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private int getCellNSWE(int geoX, int geoY) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return nswes[cellOffset];
    }

    private int getCellHeight(int geoX, int geoY) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return heights[cellOffset];
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        int cellNSWE = getCellNSWE(geoX, geoY);
        return (cellNSWE & nswe) == nswe;
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
