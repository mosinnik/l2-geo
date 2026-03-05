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

import org.l2jmobius.gameserver.model.interfaces.IPositionable;

/**
 * NOTE: removed all unused for compilation reasons
 * <p>
 * Represents a 3D coordinate (x, y, z) with an optional heading.
 */
public class Location implements IPositionable {
    protected volatile int _x;
    protected volatile int _y;
    protected volatile int _z;
    protected volatile int _heading;

    /**
     * Constructs a Location at a specified x, y and z coordinate, with a default heading of 0.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public Location(int x, int y, int z) {
        _x = x;
        _y = y;
        _z = z;
        _heading = 0;
    }


    /**
     * Retrieves the x coordinate.
     *
     * @return the x coordinate
     */
    @Override
    public int getX() {
        return _x;
    }

    /**
     * Retrieves the y coordinate.
     *
     * @return the y coordinate
     */
    @Override
    public int getY() {
        return _y;
    }

    /**
     * Retrieves the z coordinate.
     *
     * @return the z coordinate
     */
    @Override
    public int getZ() {
        return _z;
    }


}
