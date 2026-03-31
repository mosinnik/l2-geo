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

package ru.mosinnik.l2eve.geodriver.jmh;

import lombok.SneakyThrows;
import org.openjdk.jmh.runner.RunnerException;
import ru.mosinnik.l2eve.geodriver.driver.GeoConstants;
import ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants;
import ru.mosinnik.l2eve.geodriver.util.RegionCoords;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static ru.mosinnik.l2eve.geodriver.jmh.GeoDriverBenchParams.*;
import static ru.mosinnik.l2eve.geodriver.jmh.GeoDriverTestConstants.DIR_WITH_POINTS;

public class PointsPreparer {

    public static final String POINTS_SUFFIX = "_points.txt";

    private static final int cornerMinWorldX;
    private static final int cornerMinWorldY;
    private static final int cornerMaxWorldX;
    private static final int cornerMaxWorldY;

    static {
        File resource = new File(GeoDriverBenchParams.class.getClassLoader().getResource(TST_REGION).getFile());
        RegionCoords coords = RegionCoords.extract(resource.toPath());
        cornerMinWorldX = coords.regionX() * 32768 + GeoConstants.WORLD_MIN_X;
        cornerMinWorldY = coords.regionY() * 32768 + GeoConstants.WORLD_MIN_Y;
        cornerMaxWorldX = cornerMinWorldX + 32768 - 1;
        cornerMaxWorldY = cornerMinWorldY + 32768 - 1;
    }


    public static void main(String[] args) throws RunnerException {
        generateRandomPoints();
//        generatePoints();

//        GeoDriverBenchParams.MyState state = new GeoDriverBenchParams.MyState();
//        state.setup();

//        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
//        GeoDriverBenchParams params = new GeoDriverBenchParams();
//        params.checkNearestNSWEBytes(blackhole, state);

    }


    // generate files with points
    static void generatePoints() {
        for (GeoDriverBytesConstants.E type : GeoDriverBytesConstants.E.values()) {
            System.out.println("Generating points for " + type.name());

            if (type == GeoDriverBytesConstants.E.NO_DATA_BLOCK
                    || type == GeoDriverBytesConstants.E.MULTILAYER_BLOCK
                    || type == GeoDriverBytesConstants.E.FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK
                    || type == GeoDriverBytesConstants.E.INDEXED_MULTILAYER_BLOCK
            ) {
                continue;
            }

//            if (type != GeoDriverBytesConstants.E.ONE_HEIGHT_COMPLEX_BLOCK
//            ) {
//                continue;
//            }

            GeoDriverBenchParams.MyState state = new GeoDriverBenchParams.MyState();

            state.loadFromFile = false;
            state.saveToFile = true;
            state.blockTypeStr = type.toString();

            state.setup();

            // NOTE: need to clear mmaped file before next iteration
            state.driverBytesMmap = null;
            System.gc();
        }
    }

    // generate files with points
    static void generateRandomPoints() {

        GeoDriverBenchParams.MyState state = new GeoDriverBenchParams.MyState();

        state.loadFromFile = false;
        state.saveToFile = true;

        state.setup();

        // NOTE: need to clear mmaped file before next iteration
        state.driverBytesMmap = null;
        System.gc();
    }


    @SneakyThrows
    public static void loadPoints(GeoDriverBenchParams.MyState myState) {

        final List<Point> points = new ArrayList<>();

        if (myState.loadFromFile) {
            String pointsFileName = getPointsFileName(myState);
            Path dir = Path.of(DIR_WITH_POINTS);
            List<String> pointsStrs = Files.readAllLines(dir.resolve(pointsFileName));

            pointsStrs.forEach(pointStr -> {
                String[] split = pointStr.split(";");
                points.add(new GeoDriverBenchParams.Point(
                        Integer.parseInt(split[0]),
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2]),
                        Integer.parseInt(split[3]),
                        Byte.parseByte(split[4]),
                        Byte.parseByte(split[5])
                ));
            });
        } else {
            Random r = new Random(1);
            int filteredBlockType;
            if (isBlockTypeSet(myState)) {
                filteredBlockType = GeoDriverBytesConstants.blockNameToType(myState.blockType.name());
                ensureBlocksExists(myState, filteredBlockType);
            } else {
                filteredBlockType = -1;
            }
            while (points.size() < CHECK_POINT_COUNT) {
                int worldX = r.nextInt(cornerMaxWorldX - cornerMinWorldX) + cornerMinWorldX;
                int worldY = r.nextInt(cornerMaxWorldY - cornerMinWorldY) + cornerMinWorldY;
                int geoX = myState.driver.getGeoX(worldX);
                int geoY = myState.driver.getGeoY(worldY);

                int blockType = myState.driver.getBlockType(geoX, geoY);
                if (filteredBlockType == -1 || blockType == filteredBlockType) {
                    points.add(new GeoDriverBenchParams.Point(
                                    worldX,
                                    worldY,
                                    geoX,
                                    geoY,
                                    (byte) (1 << r.nextInt(4)),
                                    (byte) blockType
                            )
                    );
                }
            }
        }

        if (myState.saveToFile) {
            String pointsString = points.stream()
                    .map(p -> p.x() + ";" + p.y() + ";" + p.geoX() + ";" + p.geoY() + ";" + p.nswe() + ";" + p.type())
                    .collect(Collectors.joining("\n"));

            Path dir = Path.of(DIR_WITH_POINTS);
            Files.createDirectories(dir);

            String pointsFileName = getPointsFileName(myState);
            Files.writeString(dir.resolve(pointsFileName), pointsString);
            System.out.println("Points saved at " + pointsFileName);
        }

        myState.checkPoints = Collections.unmodifiableList(points);
        printPointsStat(myState);

    }

    private static void ensureBlocksExists(MyState myState, int filteredBlockType) {
        boolean flag = false;
        for (int worldX = cornerMinWorldX; worldX < cornerMaxWorldX; worldX++) {
            for (int worldY = cornerMinWorldY; worldY < cornerMaxWorldY; worldY++) {
                int geoX = myState.driver.getGeoX(worldX);
                int geoY = myState.driver.getGeoY(worldY);
                if (myState.driver.getBlockType(geoX, geoY) == filteredBlockType) {
                    flag = true;
                    worldY = cornerMaxWorldY;
                    worldX = cornerMaxWorldX;
                }
            }
        }
        if (!flag) {
            throw new RuntimeException("Not found any blocks in region for type: " + myState.blockType.name());
        }
    }

    private static String getPointsFileName(MyState myState) {
        String filePrefix;
        if (isBlockTypeSet(myState)) {
            filePrefix = myState.blockType.name();
        } else {
            filePrefix = "RANDOM";
        }
        return filePrefix + POINTS_SUFFIX;
    }

    private static void printPointsStat(MyState myState) {
        System.out.println("----- points counts per block type for " + myState.blockTypeStr + ": ");
        myState.checkPoints.stream()
                .collect(Collectors.groupingBy(
                        Point::type,
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach((e) -> {
                    System.out.println(e.getKey() + " -> " + e.getValue());
                });
        System.out.println("----------------------------------------------");
    }

    private static boolean isBlockTypeSet(MyState myState) {
        return myState.blockType != null;
    }
}
