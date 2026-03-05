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
 * This class loads and hold info about doors.
 *
 * @author JIV, GodKratos, UnAfraid
 */
public class DoorData {
    protected DoorData() {
    }

    public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance, boolean doubleFaceCheck) {
        return false;
    }

    public static DoorData getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final DoorData INSTANCE = new DoorData();
    }
}
