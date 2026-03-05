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

import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.IPositionable;

/**
 * NOTE: removed all unused for compilation reasons
 * <p>
 * Base class for all interactive objects.
 */
public abstract class WorldObject implements IPositionable {
    /**
     * Location
     */
    private final Location _location = new Location(0, 0, -10000);

    public WorldObject() {
    }


    /**
     * Verify if object is instance of Artefact.
     *
     * @return {@code true} if object is instance of Artefact, {@code false} otherwise.
     */
    public boolean isArtefact() {
        return false;
    }


    /**
     * Verify if object is instance of Door.
     *
     * @return {@code true} if object is instance of Door, {@code false} otherwise.
     */
    public boolean isDoor() {
        return false;
    }

    /**
     * Gets the X coordinate.
     *
     * @return the X coordinate
     */
    @Override
    public int getX() {
        return _location.getX();
    }

    /**
     * Gets the Y coordinate.
     *
     * @return the Y coordinate
     */
    @Override
    public int getY() {
        return _location.getY();
    }

    /**
     * Gets the Z coordinate.
     *
     * @return the Z coordinate
     */
    @Override
    public int getZ() {
        return _location.getZ();
    }


    /**
     * Get instance world where object is currently located.
     *
     * @return {@link Instance} if object is inside instance world, otherwise {@code null}
     */
    public Instance getInstanceWorld() {
        return null;
    }

}
