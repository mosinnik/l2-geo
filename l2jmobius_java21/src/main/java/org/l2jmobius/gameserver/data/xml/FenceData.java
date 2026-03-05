/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.xml;

import org.l2jmobius.gameserver.model.instancezone.Instance;

/**
 * NOTE: removed all unused for compilation reasons
 *
 * @author HorridoJoho
 */
public class FenceData {

    protected FenceData() {
    }


    /**
     * Checks if there is a fence with geodata enabled between two sets of coordinates within a specified instance.
     *
     * @param x        the x-coordinate of the starting point.
     * @param y        the y-coordinate of the starting point.
     * @param z        the z-coordinate of the starting point.
     * @param tx       the x-coordinate of the ending point.
     * @param ty       the y-coordinate of the ending point.
     * @param tz       the z-coordinate of the ending point.
     * @param instance the instance in which to check for fences.
     * @return {@code true} if there is a fence between the coordinates, {@code false} otherwise.
     */
    public boolean checkIfFenceBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance) {
        return false;
    }


    public static FenceData getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final FenceData INSTANCE = new FenceData();
    }
}
