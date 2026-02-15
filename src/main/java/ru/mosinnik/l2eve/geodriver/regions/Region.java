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

package ru.mosinnik.l2eve.geodriver.regions;


import ru.mosinnik.l2eve.geodriver.driver.GeoConfig;
import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.abstraction.IRegion;

import java.nio.ByteBuffer;


public final class Region implements IRegion {
    private final IBlock[] blocks = new IBlock[IRegion.REGION_BLOCKS];

    public Region(ByteBuffer bb, GeoConfig geoConfig) {
        BlockManager blockManager = new BlockManager(geoConfig);
        for (int blockOffset = 0; blockOffset < IRegion.REGION_BLOCKS; blockOffset++) {
            int blockType = bb.get();
            IBlock block = switch (blockType) {
                case IBlock.TYPE_FLAT -> blockManager.flatBlock(bb);
                case IBlock.TYPE_COMPLEX -> blockManager.complexBlock(bb);
                case IBlock.TYPE_MULTILAYER -> blockManager.multilayerBlock(bb);
                default -> throw new RuntimeException("Invalid block type " + blockType + "!");
            };
            blocks[blockOffset] = block;
        }
    }

    public IBlock getBlock(int blockOffset) {
        return blocks[blockOffset];
    }

    /**
     * readable variant:
     * int blockOffset = (((geoX / IBlock.BLOCK_CELLS_X) % IRegion.REGION_BLOCKS_X) * IRegion.REGION_BLOCKS_Y)
     * + ((geoY / IBlock.BLOCK_CELLS_Y) % IRegion.REGION_BLOCKS_Y);
     */
    @Override
    public IBlock getBlock(int geoX, int geoY) {
        int blockOffset = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);

        return blocks[blockOffset];
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return getBlock(geoX, geoY).checkNearestNSWE(geoX, geoY, worldZ, nswe);
    }

    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        return getBlock(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        return getBlock(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        return getBlock(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
    }

    @Override
    public boolean hasGeo() {
        return true;
    }
}
