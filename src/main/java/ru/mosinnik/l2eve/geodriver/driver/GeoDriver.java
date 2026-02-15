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
import ru.mosinnik.l2eve.geodriver.abstraction.IGeoDriver;
import ru.mosinnik.l2eve.geodriver.abstraction.IRegion;
import ru.mosinnik.l2eve.geodriver.regions.NullRegion;
import ru.mosinnik.l2eve.geodriver.regions.Region;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;

import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.GEO_REGIONS;
import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.GEO_REGIONS_Y;


public final class GeoDriver implements IGeoDriver {

    /**
     * The regions array
     */
    private final IRegion[] regions = new IRegion[GEO_REGIONS];

    private final GeoConfig config;

    public GeoDriver() {
        config = new GeoConfig();
        Arrays.fill(regions, NullRegion.INSTANCE);
    }

    public GeoDriver(GeoConfig config) {
        this.config = config;
        Arrays.fill(regions, NullRegion.INSTANCE);
    }

    /**
     * readable variant:
     * int regionOffset = ((geoX / IRegion.REGION_CELLS_X) * GEO_REGIONS_Y) + (geoY / IRegion.REGION_CELLS_Y);
     */
    private IRegion getRegion(int geoX, int geoY) {
        int regionOffset = ((geoX >> 11) << 5) + (geoY >> 11);
        return regions[regionOffset];
    }

    @Override
    public void loadRegion(Path filePath, int regionX, int regionY) throws IOException {
        final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            regions[regionOffset] = new Region(
                raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length()).order(ByteOrder.LITTLE_ENDIAN),
                config
            );
        }
    }


    @Override
    public boolean hasGeoPos(int geoX, int geoY) {
        return getRegion(geoX, geoY).hasGeo();
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return getRegion(geoX, geoY).checkNearestNSWE(geoX, geoY, worldZ, nswe);
    }

    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        return getRegion(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        return getRegion(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        return getRegion(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
    }

    public int getBlockType(int geoX, int geoY) {
        IBlock block = getRegion(geoX, geoY).getBlock(geoX, geoY);
        return GeoDriverBytes.getType(block);
    }

    public IBlock getBlock(int geoX, int geoY) {
        return getRegion(geoX, geoY).getBlock(geoX, geoY);
    }
}
