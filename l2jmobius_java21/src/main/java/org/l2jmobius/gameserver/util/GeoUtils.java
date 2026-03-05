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
package org.l2jmobius.gameserver.util;

import org.l2jmobius.gameserver.geoengine.geodata.Cell;

/**
 * NOTE: removed all unused for compilation reasons
 *
 * @author HorridoJoho, Mobius
 */
public class GeoUtils {

    /**
     * Computes the NSWE direction based on the difference between two points.
     *
     * @param lastX The x coordinate of the previous point.
     * @param lastY The y coordinate of the previous point.
     * @param x     The x coordinate of the current point.
     * @param y     The y coordinate of the current point.
     * @return The NSWE direction.
     */
    public static int computeNswe(int lastX, int lastY, int x, int y) {
        if (x > lastX) // east
        {
            if (y > lastY) {
                return Cell.NSWE_SOUTH_EAST;
            } else if (y < lastY) {
                return Cell.NSWE_NORTH_EAST;
            } else {
                return Cell.NSWE_EAST;
            }
        } else if (x < lastX) // west
        {
            if (y > lastY) {
                return Cell.NSWE_SOUTH_WEST;
            } else if (y < lastY) {
                return Cell.NSWE_NORTH_WEST;
            } else {
                return Cell.NSWE_WEST;
            }
        } else // unchanged x
        {
            if (y > lastY) {
                return Cell.NSWE_SOUTH;
            } else if (y < lastY) {
                return Cell.NSWE_NORTH;
            } else {
                throw new RuntimeException();
            }
        }
    }
}
