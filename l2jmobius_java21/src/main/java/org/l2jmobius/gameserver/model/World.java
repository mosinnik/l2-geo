/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.model;

/**
 * NOTE: removed all unused for compilation reasons
 */
public class World {

    public static volatile int MAX_CONNECTED_COUNT = 0;
    public static volatile int OFFLINE_TRADE_COUNT = 0;

    /**
     * Gracia border Flying objects not allowed to the east of it.
     */
    public static final int GRACIA_MAX_X = -166168;
    public static final int GRACIA_MAX_Z = 6105;
    public static final int GRACIA_MIN_Z = -895;

    /**
     * Bit shift, defines number of regions note, shifting by 15 will result in regions corresponding to map tiles shifting by 11 divides one tile to 16x16 regions.
     */
    public static final int SHIFT_BY = 11;

    public static final int TILE_SIZE = 32768;

    /**
     * Map dimensions.
     */
    public static final int TILE_X_MIN = 11;
    public static final int TILE_Y_MIN = 10;
    public static final int TILE_X_MAX = 28;
    public static final int TILE_Y_MAX = 26;
    public static final int TILE_ZERO_COORD_X = 20;
    public static final int TILE_ZERO_COORD_Y = 18;
    public static final int WORLD_X_MIN = (TILE_X_MIN - TILE_ZERO_COORD_X) * TILE_SIZE;
    public static final int WORLD_Y_MIN = (TILE_Y_MIN - TILE_ZERO_COORD_Y) * TILE_SIZE;

    public static final int WORLD_X_MAX = ((TILE_X_MAX - TILE_ZERO_COORD_X) + 1) * TILE_SIZE;
    public static final int WORLD_Y_MAX = ((TILE_Y_MAX - TILE_ZERO_COORD_Y) + 1) * TILE_SIZE;

    /**
     * Calculated offset used so top left region is 0,0
     */
    public static final int OFFSET_X = Math.abs(WORLD_X_MIN >> SHIFT_BY);
    public static final int OFFSET_Y = Math.abs(WORLD_Y_MIN >> SHIFT_BY);

    /**
     * Number of regions.
     */
    private static final int REGIONS_X = (WORLD_X_MAX >> SHIFT_BY) + OFFSET_X;
    private static final int REGIONS_Y = (WORLD_Y_MAX >> SHIFT_BY) + OFFSET_Y;


    /**
     * Constructor of World.
     */
    protected World() {

    }

    public static World getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final World INSTANCE = new World();
    }
}
