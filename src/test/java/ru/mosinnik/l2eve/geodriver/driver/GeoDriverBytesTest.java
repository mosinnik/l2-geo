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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static ru.mosinnik.l2eve.geodriver.GeoDriverTestConstants.*;

public class GeoDriverBytesTest {

    @Ignore("Print memory layout and write bins")
    @Test
    public void loadAll() throws IOException {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setOneHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
        geoConfig.setFewHeightsComplexBlockEnabled(true);
        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
        geoConfig.setNoHolesMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayer32BlockEnabled(true);

        GeoDriverBytes driver = new GeoDriverBytes(geoConfig);

        String geodataDir = GEODATA_DIR;
        driver.loadL2J(Path.of(geodataDir));

        String binGeoDataDir = GEODATA_BIN_DIR;
        driver.writeToFiles(Path.of(binGeoDataDir));

        GraphLayout graphLayout = GraphLayout.parseInstance(driver);
        System.out.println("footprint = " + graphLayout.toFootprint());
        System.out.println("totalCount = " + graphLayout.totalCount());
        System.out.println("totalSize = " + graphLayout.totalSize());
        System.out.println("-------------------------");

        driver.printStats();
    }

    @Ignore("Generate binary from .l2j")
    @Test
    public void genBinsPerfAll() throws IOException {
        GeoConfig geoConfig = GeoConfig.maxPerfBytes();
//        GeoConfig geoConfig = GeoConfig.lowMemory();

//        GeoConfig geoConfig = new GeoConfig();
//        geoConfig.setOneHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
//        geoConfig.setFewHeightsComplexBlockEnabled(true);
//        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
//        geoConfig.setNoHolesMultilayerBlockEnabled(true);
//        geoConfig.setIndexedMultilayerBlockEnabled(true);
//        geoConfig.setIndexedMultilayer32BlockEnabled(true);

        GeoDriverBytes driver = new GeoDriverBytes(geoConfig);

        driver.loadL2J(Path.of(GEODATA_DIR));

        Files.createDirectories(Path.of(GEODATA_BIN_DIR));
        driver.writeToFiles(Path.of(GEODATA_BIN_DIR));

    }

    @Ignore("Print memory layout and need already generated bins")
    @Test
    public void loadAllBin() throws IOException {
        GeoConfig geoConfig = new GeoConfig();
//        geoConfig.setOneHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
//        geoConfig.setFewHeightsComplexBlockEnabled(true);
//        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
//        geoConfig.setNoHolesMultilayerBlockEnabled(true);
//        geoConfig.setIndexedMultilayerBlockEnabled(true);
//        geoConfig.setIndexedMultilayer32BlockEnabled(true);

        GeoDriverBytes driver = new GeoDriverBytes(geoConfig);

        Instant t1 = Instant.now();
        driver.loadBin(Path.of(GEODATA_BIN_DIR));
        Instant t2 = Instant.now();
        System.out.println("Geo loadBin for " + t1.until(t2, ChronoUnit.MILLIS) + "ms");

        GraphLayout graphLayout = GraphLayout.parseInstance(driver);
        System.out.println("footprint = " + graphLayout.toFootprint());
        System.out.println("totalCount = " + graphLayout.totalCount());
        System.out.println("totalSize = " + graphLayout.totalSize());
        System.out.println("-------------------------");

        driver.printStats();
    }

    @Ignore("Heavy")
    @Test
    public void shouldHaveSameToOldDriver() throws IOException {
        int regionX = 12;
        int regionY = 24;
        String tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
        File resource = new File(GeoDriverBytesTest.class.getClassLoader().getResource(tstRegion).getFile());

        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setOneHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
        geoConfig.setFewHeightsComplexBlockEnabled(true);
        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
        geoConfig.setNoHolesMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayer32BlockEnabled(true);

        GeoDriver oldDriver = new GeoDriver(new GeoConfig());
        oldDriver.loadRegion(resource.toPath(), regionX, regionY);

        GeoDriverBytes driver = new GeoDriverBytes(geoConfig);
        driver.loadFromL2J(List.of(resource.toPath()));

        int cornerMinX = regionX * 32768 + GeoConstants.WORLD_MIN_X;
        int cornerMinY = regionY * 32768 + GeoConstants.WORLD_MIN_Y;
        int cornerMaxX = cornerMinX + 32768 - 1;
        int cornerMaxY = cornerMinY + 32768 - 1;

        System.out.println("cornerMinX = " + cornerMinX);
        System.out.println("cornerMinY = " + cornerMinY);
        System.out.println("cornerMaxX = " + cornerMaxX);
        System.out.println("cornerMaxY = " + cornerMaxY);
        System.out.println("--------------------------");

        Instant t1 = Instant.now();
        compareDriversHeavy(cornerMinX, cornerMaxX, driver, cornerMinY, cornerMaxY, oldDriver);
        Instant t2 = Instant.now();
        System.out.println("Comparison time: " + t1.until(t2, ChronoUnit.MILLIS) / 1000.0 + " seconds");
    }


    @Ignore("Heavy")
    @Test
    public void shouldHaveSameToOldDriverFromBinary() throws IOException {
        int regionX = 12;
        int regionY = 24;
        String tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
        File resource = new File(GeoDriverBytesTest.class.getClassLoader().getResource(tstRegion).getFile());

        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setOneHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
        geoConfig.setFewHeightsComplexBlockEnabled(true);
        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
        geoConfig.setNoHolesMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayer32BlockEnabled(true);

        GeoDriver oldDriver = new GeoDriver(geoConfig);
        oldDriver.loadRegion(resource.toPath(), regionX, regionY);

        GeoDriverBytes tmpDriver = new GeoDriverBytes(geoConfig);
        tmpDriver.loadFromL2J(List.of(resource.toPath()));
        Path binGeoData = Path.of(GEODATA_BIN_DIR);
        Files.createDirectories(binGeoData);
        tmpDriver.writeToFiles(binGeoData);

        Instant t1 = Instant.now();
        GeoDriverBytes driver = new GeoDriverBytes(geoConfig);
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

        compareDrivers(cornerMinX, cornerMaxX, driver, cornerMinY, cornerMaxY, oldDriver);
    }


    private static void compareDrivers(int cornerMinX, int cornerMaxX, GeoDriverBytes driver, int cornerMinY, int cornerMaxY, GeoDriver oldDriver) {
        int stepX = 64;
        int stepY = 64;
        int stepZ = 100;

        for (int worldX = cornerMinX; worldX < cornerMaxX; worldX += stepX) {
            int x = driver.getGeoX(worldX);
            for (int worldY = cornerMinY; worldY < cornerMaxY; worldY += stepY) {
                int y = driver.getGeoY(worldY);
                for (int z = -16000; z < 16000; z += stepZ) {
                    for (int l = 0; l < 16; l++) {
                        boolean expected = oldDriver.checkNearestNSWE(x, y, z, (byte) l);
                        boolean actual = driver.checkNearestNSWE(x, y, z, (byte) l);
                        if (expected != actual) {
                            throw new AssertionError("Nearest NSWE did not match");
                        }
                        assertEquals(
                                expected,
                                actual
                        );
                    }
                    assertEquals(
                            oldDriver.getNearestZ(x, y, z),
                            driver.getNearestZ(x, y, z)
                    );
                    assertEquals(
                            oldDriver.getNextLowerZ(x, y, z),
                            driver.getNextLowerZ(x, y, z)
                    );
                    assertEquals(
                            oldDriver.getNextHigherZ(x, y, z),
                            driver.getNextHigherZ(x, y, z)
                    );
                    assertEquals(
                            oldDriver.hasGeoPos(x, y),
                            driver.hasGeoPos(x, y)
                    );
                }
            }
        }
    }

    /**
     * Compare each coords in each block
     */
    private static void compareDriversHeavy(int cornerMinX, int cornerMaxX, GeoDriverBytes driver, int cornerMinY, int cornerMaxY, GeoDriver oldDriver) {
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
                    for (int l = 0; l < 16; l++) {
                        boolean expected = oldDriver.checkNearestNSWE(x, y, z, (byte) l);
                        boolean actual = driver.checkNearestNSWE(x, y, z, (byte) l);
                        if (expected != actual) {
                            throw new AssertionError("Nearest NSWE did not match");
                        }
                        assertEquals(
                                expected,
                                actual
                        );
                    }
                    assertEquals(
                            oldDriver.getNearestZ(x, y, z),
                            driver.getNearestZ(x, y, z)
                    );
                    assertEquals(
                            oldDriver.getNextLowerZ(x, y, z),
                            driver.getNextLowerZ(x, y, z)
                    );
                    assertEquals(
                            oldDriver.getNextHigherZ(x, y, z),
                            driver.getNextHigherZ(x, y, z)
                    );
                    assertEquals(
                            oldDriver.hasGeoPos(x, y),
                            driver.hasGeoPos(x, y)
                    );
                }
            }
        }
    }

}
