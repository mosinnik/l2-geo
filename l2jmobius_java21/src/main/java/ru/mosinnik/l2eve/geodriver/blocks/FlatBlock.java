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

/**
 * @author FBIagent
 */
public class FlatBlock implements IBlock {
    private final short height;

    public FlatBlock(ByteBuffer bb) {
        height = bb.getShort();
    }

    public FlatBlock(short height) {
        this.height = height;
    }

    public short getHeight() {
        return height;
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return true;
    }

    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        return height;
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        return height <= worldZ ? height : worldZ;
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        return height >= worldZ ? height : worldZ;
    }
}
