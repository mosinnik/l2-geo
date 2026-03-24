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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;
import ru.mosinnik.l2eve.geodriver.driver.GeoConstants;
import ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static ru.mosinnik.l2eve.geodriver.jmh.GeoDriverBenchParams.*;
import static ru.mosinnik.l2eve.geodriver.jmh.GeoDriverTestConstants.DIR_WITH_POINTS;

public class PointsPreparer {

    public static void main(String[] args) throws RunnerException {
        generateRandomPoints();
        generatePoints();

        GeoDriverBenchParams.MyState state = new GeoDriverBenchParams.MyState();
        state.setup();

        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        GeoDriverBenchParams params = new GeoDriverBenchParams();
        params.checkNearestNSWEBytes(blackhole, state);

    }


    // generate files with points
    static void generatePoints() {
        for (GeoDriverBytesConstants.E type : GeoDriverBytesConstants.E.values()) {
            if (type == GeoDriverBytesConstants.E.NO_DATA_BLOCK
                    || type == GeoDriverBytesConstants.E.MULTILAYER_BLOCK
                    || type == GeoDriverBytesConstants.E.FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK
                    || type == GeoDriverBytesConstants.E.INDEXED_MULTILAYER_BLOCK
            ) {
                continue;
            }

            GeoDriverBenchParams.MyState state = new GeoDriverBenchParams.MyState();

            state.loadFromFile = false;
            state.saveToFile = true;
            state.blockType = type;

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
        int cornerMinWorldX = REGION_X * 32768 + GeoConstants.WORLD_MIN_X;
        int cornerMinWorldY = REGION_Y * 32768 + GeoConstants.WORLD_MIN_Y;
        int cornerMaxWorldX = cornerMinWorldX + 32768 - 1;
        int cornerMaxWorldY = cornerMinWorldY + 32768 - 1;

        myState.checkPoints.clear();

        if (myState.loadFromFile) {
            String filePrefix;
            if (myState.blockType != null) {
                filePrefix = myState.blockType.name();
            } else {
                filePrefix = "RANDOM";
            }
            Path dir = Path.of(DIR_WITH_POINTS);
            List<String> pointsStrs = Files.readAllLines(dir.resolve(filePrefix + "_points.txt"));

            pointsStrs.forEach(pointStr -> {
                String[] split = pointStr.split(";");
                myState.checkPoints.add(new GeoDriverBenchParams.Point(
                        Integer.parseInt(split[0]),
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2]),
                        Integer.parseInt(split[3]),
                        Byte.parseByte(split[4]),
                        (byte) 0
                ));
            });
        } else {
            Random r = new Random(1);
            int filteredBlockType;
            if (myState.blockType == null) {
                filteredBlockType = -1;
            } else {
                filteredBlockType = GeoDriverBytesConstants.blockNameToType(myState.blockType.name());
            }
            while (myState.checkPoints.size() < CHECK_POINT_COUNT) {
                int worldX = r.nextInt(cornerMaxWorldX - cornerMinWorldX) + cornerMinWorldX;
                int worldY = r.nextInt(cornerMaxWorldY - cornerMinWorldY) + cornerMinWorldY;
                int geoX = myState.driver.getGeoX(worldX);
                int geoY = myState.driver.getGeoY(worldY);

                int blockType = myState.driver.getBlockType(geoX, geoY);
                if (filteredBlockType == -1 || blockType == filteredBlockType) {
                    myState.checkPoints.add(new GeoDriverBenchParams.Point(
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

            System.out.println("----- generated points counts per block type:");
            myState.checkPoints.stream()
                    .collect(Collectors.groupingBy(
                            GeoDriverBenchParams.Point::type,
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

        if (myState.saveToFile) {
            String pointsString = myState.checkPoints.stream()
                    .map(p -> p.x() + ";" + p.y() + ";" + p.geoX() + ";" + p.geoY() + ";" + p.nswe() + ";" + p.type())
                    .collect(Collectors.joining("\n"));

            Path dir = Path.of(DIR_WITH_POINTS);
            Files.createDirectories(dir);

            String filePrefix;
            if (myState.blockType != null) {
                filePrefix = myState.blockType.name();
            } else {
                filePrefix = "RANDOM";
            }
            Files.writeString(dir.resolve(filePrefix + "_points.txt"), pointsString);
            System.out.println("Points saved at " + filePrefix + "_points.txt");
        }
    }
}
