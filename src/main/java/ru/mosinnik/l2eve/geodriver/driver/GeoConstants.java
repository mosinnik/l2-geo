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

package ru.mosinnik.l2eve.geodriver.driver;


import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.abstraction.IRegion;

public interface GeoConstants {
    // world dimensions: 1048576 * 1048576 = 1_099_511_627_776
    int WORLD_MIN_X = -655360;
    int WORLD_MAX_X = 393215;
    int WORLD_MIN_Y = -589824;
    int WORLD_MAX_Y = 458751;

    /**
     * Regions in the world on the x axis
     */
    int GEO_REGIONS_X = 32;

    /**
     * Blocks in the world on the x axis
     */
    int GEO_BLOCKS_X = GEO_REGIONS_X * IRegion.REGION_BLOCKS_X;

    /**
     * Cells in the world on the x axis
     */
    int GEO_CELLS_X = GEO_BLOCKS_X * IBlock.BLOCK_CELLS_X;

    /**
     * Regions in the world on the y axis
     */
    int GEO_REGIONS_Y = 32;

    /**
     * Blocks in the world on the y axis
     */
    int GEO_BLOCKS_Y = GEO_REGIONS_Y * IRegion.REGION_BLOCKS_Y;

    /**
     * Cells in the world in the y axis
     */
    int GEO_CELLS_Y = GEO_BLOCKS_Y * IBlock.BLOCK_CELLS_Y;

    /**
     * Region in the world
     */
    int GEO_REGIONS = GEO_REGIONS_X * GEO_REGIONS_Y;

    /**
     * Blocks in the world
     */
    int GEO_BLOCKS = GEO_REGIONS * IRegion.REGION_BLOCKS;
}
