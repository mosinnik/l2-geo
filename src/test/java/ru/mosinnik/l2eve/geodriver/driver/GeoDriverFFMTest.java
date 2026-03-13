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
import org.openjdk.jol.info.GraphLayout;
import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.blocks.ComplexBlock;
import ru.mosinnik.l2eve.geodriver.blocks.FlatBlock;
import ru.mosinnik.l2eve.geodriver.blocks.MultilayerBlock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static ru.mosinnik.l2eve.geodriver.GeoDriverTestConstants.GEODATA_DIR;
import static ru.mosinnik.l2eve.geodriver.GeoDriverTestConstants.TST_BLOCK_RESOURCE_BIGGEST;

public class GeoDriverFFMTest {

    @Ignore("Print memory layout and write bins")
    @Test
    public void loadAll() throws IOException {
        String geodataDir = GEODATA_DIR;

        GeoConfig geoConfig = new GeoConfig();
//        geoConfig.setOneHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
//        geoConfig.setFewHeightsComplexBlockEnabled(true);
//        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
//        geoConfig.setNoHolesMultilayerBlockEnabled(true);
//        geoConfig.setIndexedMultilayerBlockEnabled(true);
//        geoConfig.setIndexed32MultilayerBlockEnabled(true);



        GeoDriverFFM driver = new GeoDriverFFM(geoConfig, Path.of(geodataDir));

//        driver.loadL2J(Path.of(geodataDir));

//        String binGeoDataDir = GEODATA_BIN_DIR;
//        driver.writeToFiles(Path.of(binGeoDataDir));

        GraphLayout graphLayout = GraphLayout.parseInstance(driver);
        System.out.println("footprint = " + graphLayout.toFootprint());
        System.out.println("totalCount = " + graphLayout.totalCount());
        System.out.println("totalSize = " + graphLayout.totalSize());
        System.out.println("-------------------------");

//        driver.printStats();
    }

    @Ignore("Heavy")
    @Test
    public void shouldHaveSameToOldDriver() throws IOException {
        int regionX = 12;
        int regionY = 24;
        String tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
        File resource = new File(GeoDriverBytesTest.class.getClassLoader().getResource(tstRegion).getFile());

        GeoConfig geoConfig = new GeoConfig();
//        geoConfig.setOneHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
//        geoConfig.setFewHeightsComplexBlockEnabled(true);
//        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
//        geoConfig.setNoHolesMultilayerBlockEnabled(true);
//        geoConfig.setIndexedMultilayerBlockEnabled(true);
//        geoConfig.setIndexed32MultilayerBlockEnabled(true);

        GeoDriver oldDriver = new GeoDriver(new GeoConfig());
        oldDriver.loadRegion(resource.toPath(), regionX, regionY);

        GeoDriverFFM driver = new GeoDriverFFM(geoConfig, List.of(resource.toPath()));
//        driver.loadFromL2J(List.of(resource.toPath()));

        int cornerMinX = regionX * 32768 + GeoConstants.WORLD_MIN_X;
        int cornerMinY = regionY * 32768 + GeoConstants.WORLD_MIN_Y;
        int cornerMaxX = cornerMinX + 32768 - 1;
        int cornerMaxY = cornerMinY + 32768 - 1;

        System.out.println("cornerMinX = " + cornerMinX);
        System.out.println("cornerMinY = " + cornerMinY);
        System.out.println("cornerMaxX = " + cornerMaxX);
        System.out.println("cornerMaxY = " + cornerMaxY);
        System.out.println("--------------------------");

//        int x = 24576, y = 49152, z = -4304;
//        int nearestZOld = oldDriver.getNearestZ(x, y, z);
//        int nearestZFFM = driver.getNearestZ(x, y, z);
//        System.out.println("nearestZOld = " + nearestZOld);
//        System.out.println("nearestZFFM = " + nearestZFFM);


        Instant t1 = Instant.now();
        compareDriversHeavy(cornerMinX, cornerMaxX, driver, cornerMinY, cornerMaxY, oldDriver);
        Instant t2 = Instant.now();
        System.out.println("Comparison time: " + t1.until(t2, ChronoUnit.MILLIS) / 1000.0 + " seconds");
    }


    /**
     * Compare each coords in each block
     */
    public static void compareDriversHeavy(int cornerMinX, int cornerMaxX, GeoDriverFFM driver, int cornerMinY, int cornerMaxY, GeoDriver oldDriver) {
        int stepX = 16;
        int stepY = 16;
        int stepZ = 1;

        for (int worldX = cornerMinX; worldX < cornerMaxX; worldX += stepX) {
            int x = driver.getGeoX(worldX);
            System.out.println("start x = " + x + ", worldX = " + worldX + ", cornerMaxX " + cornerMaxX);
            for (int worldY = cornerMinY; worldY < cornerMaxY; worldY += stepY) {
                int y = driver.getGeoY(worldY);
//                System.out.println("   start y = " + y + ", worldY = " + worldY + ", cornerMaxY " + cornerMaxY);
                int minZ = -16000;
                int maxZ = 16000;
                IBlock block = oldDriver.getBlock(x, y);
                if (block instanceof MultilayerBlock mb) {
                    minZ = mb.getMinHeight() - 100;
                    maxZ = mb.getMaxHeight() + 100;
                } else if (block instanceof FlatBlock fb) {
                    minZ = fb.getHeight() - 100;
                    maxZ = fb.getHeight() + 100;
                } else if (block instanceof ComplexBlock cb) {
                    minZ = cb.getMinHeight() - 100;
                    maxZ = cb.getMaxHeight() + 100;
                }
                for (int z = minZ; z < maxZ; z += stepZ) {
//                    for (int l = 0; l < 16; l++) {
//                        boolean expected = oldDriver.checkNearestNSWE(x, y, z, (byte) l);
//                        boolean actual = driver.checkNearestNSWE(x, y, z, (byte) l);
//                        if (expected != actual) {
//                            System.out.println("block = " + block);
//                            throw new AssertionError("Nearest NSWE did not match: expected=" + expected + ", actual=" + actual);
//                        }
//                        assertEquals(
//                                expected,
//                                actual
//                        );
//                    }
                    assertEquals(
                            "Error at x = " + x + ", y = " + y + ", z = " + z,
                            oldDriver.getNearestZ(x, y, z),
                            driver.getNearestZ(x, y, z)
                    );
//                    assertEquals(
//                            "Error at x = " + x + ", y = " + y + ", z = " + z,
//                            oldDriver.getNextLowerZ(x, y, z),
//                            driver.getNextLowerZ(x, y, z)
//                    );
//                    assertEquals(
//                            "Error at x = " + x + ", y = " + y + ", z = " + z,
//                            oldDriver.getNextHigherZ(x, y, z),
//                            driver.getNextHigherZ(x, y, z)
//                    );
//                    assertEquals(
//                            "Error at x = " + x + ", y = " + y + ", z = " + z,
//                            oldDriver.hasGeoPos(x, y),
//                            driver.hasGeoPos(x, y)
//                    );
                }
            }
        }
    }
}
