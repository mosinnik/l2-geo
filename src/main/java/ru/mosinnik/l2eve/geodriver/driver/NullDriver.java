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

import lombok.extern.slf4j.Slf4j;
import ru.mosinnik.l2eve.geodriver.abstraction.IGeoDriver;

import java.nio.file.Path;


@Slf4j
public final class NullDriver implements IGeoDriver {

    public NullDriver() {
        log.info("Using Null GeoDriver.");
    }

    @Override
    public boolean hasGeoPos(int geoX, int geoY) {
        return false;
    }

    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        return worldZ;
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        return worldZ;
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        return worldZ;
    }


    @Override
    public void loadRegion(Path filePath, int regionX, int regionY) {
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return true;
    }
}
