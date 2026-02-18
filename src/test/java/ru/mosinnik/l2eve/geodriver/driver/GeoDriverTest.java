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
import ru.mosinnik.l2eve.geodriver.Cell;
import ru.mosinnik.l2eve.geodriver.regions.BlockManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static ru.mosinnik.l2eve.geodriver.GeoDriverTestConstants.*;

public class GeoDriverTest {

    @Ignore("Print memory layout")
    @Test
    public void loadAlmostEmptyRegion() throws IOException {
        GeoDriver driver = new GeoDriver();
        String tstRegion = TST_BLOCK_RESOURCE_ALMOST_EMPTY;
        File resource = new File(GeoDriverTest.class.getClassLoader().getResource(tstRegion).getFile());
        driver.loadRegion(resource.toPath(), 1, 1);

        GraphLayout graphLayout = GraphLayout.parseInstance(driver);

        System.out.println("footprint = " + graphLayout.toFootprint());
        System.out.println("totalCount = " + graphLayout.totalCount());
        System.out.println("totalSize = " + graphLayout.totalSize());
    }

    @Ignore("Print memory layout")
    @Test
    public void loadMostComplexRegion() throws IOException {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setOneHeightComplexBlockEnabled(true);
        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
        geoConfig.setFewHeightsComplexBlockEnabled(true);

        GeoDriver driver = new GeoDriver(geoConfig);
        String tstRegion = TST_BLOCK_RESOURCE_MOST_COMPLEX;
        File resource = new File(GeoDriverTest.class.getClassLoader().getResource(tstRegion).getFile());
        driver.loadRegion(resource.toPath(), 1, 1);

        GraphLayout graphLayout = GraphLayout.parseInstance(driver);

        System.out.println("footprint = " + graphLayout.toFootprint());
        System.out.println("totalCount = " + graphLayout.totalCount());
        System.out.println("totalSize = " + graphLayout.totalSize());
    }

    @Ignore("Print memory layout")
    @Test
    public void loadBiggestRegion() throws IOException {
        GeoDriver driver = new GeoDriver();
        String tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
        File resource = new File(GeoDriverTest.class.getClassLoader().getResource(tstRegion).getFile());
        driver.loadRegion(resource.toPath(), 1, 1);

        GraphLayout graphLayout = GraphLayout.parseInstance(driver);

        System.out.println("footprint = " + graphLayout.toFootprint());
        System.out.println("totalCount = " + graphLayout.totalCount());
        System.out.println("totalSize = " + graphLayout.totalSize());
    }

    @Ignore("Print memory layout")
    @Test
    public void loadAll() throws IOException {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setBlockStatSavingEnabled(true);

//        geoConfig.setOneHeightComplexBlockEnabled(true);
//        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
//        geoConfig.setFewHeightsComplexBlockEnabled(true);
//        geoConfig.setBaseHeightComplexBlockEnabled(true);
//        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
//        geoConfig.setNoHolesMultilayerBlockEnabled(true);
//        geoConfig.setIndexedMultilayerBlockEnabled(true);
//        geoConfig.setIndexedMultilayer32BlockEnabled(true);

        GeoDriver driver = new GeoDriver(geoConfig);

        String geodataDir = GEODATA_DIR;
        try (Stream<Path> pathStream = Files.list(Path.of(geodataDir))) {
            List<Path> paths = pathStream
                    .filter(path -> path.getFileName().toString().endsWith(".l2j"))
                    .toList();
            for (Path path : paths) {
                String fileName = path.getFileName().toString();
                String[] split = fileName.split("[_.]");
                int regionX = Integer.parseInt(split[0]);
                int regionY = Integer.parseInt(split[1]);
                try {
                    driver.loadRegion(path, regionX, regionY);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        GraphLayout graphLayout = GraphLayout.parseInstance(driver);

        System.out.println("footprint = " + graphLayout.toFootprint());
        System.out.println("totalCount = " + graphLayout.totalCount());
        System.out.println("totalSize = " + graphLayout.totalSize());

        System.out.println("-------------------------");

        printBlocksLayouts();

    }

    private static void printBlocksLayouts() {
        GraphLayout allFlatBlocksLayout = GraphLayout.parseInstance(BlockManager.allFlatBlocks);
        System.out.println("--- allFlatBlocks");
        System.out.println(allFlatBlocksLayout.toFootprint());

        GraphLayout allOneHeightComplexBlocksLayout = GraphLayout.parseInstance(BlockManager.allOneHeightComplexBlocks);
        System.out.println("--- allOneHeightComplexBlocks");
        System.out.println(allOneHeightComplexBlocksLayout.toFootprint());

        GraphLayout allFewHeightComplexBlocksLayout = GraphLayout.parseInstance(BlockManager.allFewHeightComplexBlocks);
        System.out.println("--- allFewHeightComplexBlocks");
        System.out.println(allFewHeightComplexBlocksLayout.toFootprint());

        GraphLayout allFewHeightOneNsweComplexBlocksLayout = GraphLayout.parseInstance(BlockManager.allFewHeightOneNsweComplexBlocks);
        System.out.println("--- allFewHeightOneNsweComplexBlocks");
        System.out.println(allFewHeightOneNsweComplexBlocksLayout.toFootprint());

        GraphLayout allBaseHeightComplexBlocksLayout = GraphLayout.parseInstance(BlockManager.allBaseHeightComplexBlocks);
        System.out.println("--- allBaseHeightComplexBlocks");
        System.out.println(allBaseHeightComplexBlocksLayout.toFootprint());

        GraphLayout allBaseHeightOneNsweComplexBlocksLayout = GraphLayout.parseInstance(BlockManager.allBaseHeightOneNsweComplexBlocks);
        System.out.println("--- allBaseHeightOneNsweComplexBlocks");
        System.out.println(allBaseHeightOneNsweComplexBlocksLayout.toFootprint());

        GraphLayout allComplexBlocksLayout = GraphLayout.parseInstance(BlockManager.allComplexBlocks);
        System.out.println("--- allComplexBlocks");
        System.out.println(allComplexBlocksLayout.toFootprint());

        GraphLayout allMultilayerBlocksLayout = GraphLayout.parseInstance(BlockManager.allMultilayerBlocks);
        System.out.println("--- allMultilayerBlocks");
        System.out.println(allMultilayerBlocksLayout.toFootprint());

        GraphLayout allNoHolesMultilayerBlocksLayout = GraphLayout.parseInstance(BlockManager.allNoHolesMultilayerBlocks);
        System.out.println("--- allNoHolesMultilayerBlocks");
        System.out.println(allNoHolesMultilayerBlocksLayout.toFootprint());

        GraphLayout allIndexedMultilayerBlocksLayout = GraphLayout.parseInstance(BlockManager.allIndexedMultilayerBlocks);
        System.out.println("--- allIndexedMultilayerBlocks");
        System.out.println(allIndexedMultilayerBlocksLayout.toFootprint());

        GraphLayout allIndexed32MultilayerBlocksLayout = GraphLayout.parseInstance(BlockManager.allIndexed32MultilayerBlocks);
        System.out.println("--- allIndexed32MultilayerBlocks");
        System.out.println(allIndexed32MultilayerBlocksLayout.toFootprint());
    }


    @Test
    public void testPos() throws IOException {
        GeoDriver driver = new GeoDriver();
        String tstRegion = TST_BLOCK_RESOURCE_ALMOST_EMPTY;
        File resource = new File(GeoDriverTest.class.getClassLoader().getResource(tstRegion).getFile());
        driver.loadRegion(resource.toPath(), 25, 22);

        //mid of 25_22
        int geoX = driver.getGeoX(180224);
        System.out.println("geoX = " + geoX);
        int geoY = driver.getGeoY(147456);
        System.out.println("geoY = " + geoY);

        System.out.println(driver.hasGeoPos(geoX, geoY));
        System.out.println(driver.getNearestZ(geoX, geoY, -3000));
        System.out.println(driver.checkNearestNSWE(geoX, geoY, -3000, Cell.NSWE_NORTH));

        System.out.println("World.WORLD_X_MIN = " + GeoConstants.WORLD_MIN_X);
        System.out.println("World.WORLD_X_MAX = " + GeoConstants.WORLD_MAX_X);

        System.out.println("World.WORLD_Y_MIN = " + GeoConstants.WORLD_MIN_Y);
        System.out.println("World.WORLD_Y_MAX = " + GeoConstants.WORLD_MAX_Y);

        int cornerMinX = 25 * 32768 + GeoConstants.WORLD_MIN_X;
        int cornerMinY = 22 * 32768 + GeoConstants.WORLD_MIN_Y;
        int cornerMaxX = cornerMinX + 32768 - 1;
        int cornerMaxY = cornerMinY + 32768 - 1;

        System.out.println("cornerMinX = " + cornerMinX);
        System.out.println("cornerMinY = " + cornerMinY);
        System.out.println("cornerMaxX = " + cornerMaxX);
        System.out.println("cornerMaxY = " + cornerMaxY);


    }
}