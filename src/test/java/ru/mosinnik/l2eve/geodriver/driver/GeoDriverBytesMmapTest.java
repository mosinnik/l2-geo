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

import org.junit.Ignore;
import org.junit.Test;
import ru.mosinnik.l2eve.geodriver.util.Cmp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static ru.mosinnik.l2eve.geodriver.GeoDriverTestConstants.*;

public class GeoDriverBytesMmapTest {

    @Ignore("Heavy")
    @Test
    public void shouldHaveSameToOldDriverFromBinary() throws IOException {
        int regionX = 12;
        int regionY = 24;
        String tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
        File resource = new File(GeoDriverBytesMmapTest.class.getClassLoader().getResource(tstRegion).getFile());

        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setOneHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
        geoConfig.setFewHeightsComplexBlockEnabled(true);
        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
        geoConfig.setNoHolesMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayerBlockEnabled(true);
        geoConfig.setIndexed32MultilayerBlockEnabled(true);

        GeoDriver oldDriver = new GeoDriver();
        oldDriver.loadRegion(resource.toPath(), regionX, regionY);

        Path binGeoData = Path.of(GEODATA_BIN_DIR);

        GeoDriverBytes tmpDriver = new GeoDriverBytes(geoConfig);
        tmpDriver.loadFromL2J(List.of(resource.toPath()));
        Files.createDirectories(binGeoData);
        tmpDriver.writeToFiles(binGeoData);

        Instant t1 = Instant.now();
        GeoDriverBytesMmap driver = new GeoDriverBytesMmap();
        driver.loadBin(binGeoData);
        Instant t2 = Instant.now();
        System.out.println("Geo loadBin for " + t1.until(t2, ChronoUnit.MILLIS) + "ms");

        int cornerMinX = regionX * 32768 + GeoConstants.WORLD_MIN_X;
        int cornerMinY = regionY * 32768 + GeoConstants.WORLD_MIN_Y;
        int cornerMaxX = cornerMinX + 32768 - 1;
        int cornerMaxY = cornerMinY + 32768 - 1;

        System.out.println("cornerMinX = " + cornerMinX);
        System.out.println("cornerMinY = " + cornerMinY);
        System.out.println("cornerMaxX = " + cornerMaxX);
        System.out.println("cornerMaxY = " + cornerMaxY);
        System.out.println("--------------------------");

        Cmp.compareDrivers(driver, oldDriver, cornerMinX, cornerMaxX, cornerMinY, cornerMaxY);
    }

}
