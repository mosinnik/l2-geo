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
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.mosinnik.l2eve.geodriver.driver.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static ru.mosinnik.l2eve.geodriver.GeoDriverTestConstants.*;


@Threads(1)
@Fork(1)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5, time = 30)
@Timeout(time = 10)
public class GeoDriverBenchParams {
    private static final int checkPointCount = 10000;

    //    private static final String tstRegion = TST_BLOCK_RESOURCE_ALMOST_EMPTY;
//    private static final int regionX = 25;
//    private static final int regionY = 22;
//    private static final String tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
//    private static final int regionX = 12;
//    private static final int regionY = 24;
    private static final String tstRegion = TST_BLOCK_RESOURCE_MOST_COMPLEX;
    private static final int regionX = 23;
    private static final int regionY = 12;


    public static void main(String[] args) throws RunnerException {
        generatePoints();

        new Runner(new OptionsBuilder()
                .include(GeoDriverBenchParams.class.getSimpleName())
//                // uncomment if need jfrs
//                .jvmArgsAppend(
//                        "-XX:+UnlockDiagnosticVMOptions",
//                        "-XX:+DebugNonSafepoints",
//                        "-XX:+FlightRecorder",
//                        "-XX:StartFlightRecording:filename=jfrs/,debugNonSafePoints=true,jdk.ExecutionSample#period=1 ms"
//                )
                .build()
        ).run();
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

            MyState state = new MyState();

            state.loadFromFile = false;
            state.saveToFile = true;
            state.blockType = type;

            state.setup();

            // NOTE: need to clear mmaped file before next iteration
            state.driverBytesMmap = null;
            System.gc();
        }
    }


    @State(Scope.Benchmark)
    public static class MyState {

        GeoDriver driverOld;
        GeoDriver driver;
        GeoDriverBytes driverBytes;
        GeoDriverBytesMmap driverBytesMmap;
        List<Point> checkPoints = new ArrayList<>();

        @Param({
                "FLAT_BLOCK",
                "COMPLEX_BLOCK",
//            "MULTILAYER_BLOCK",
                "ONE_HEIGHT_COMPLEX_BLOCK",
                "BASE_HEIGHT_COMPLEX_BLOCK",
                "BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK",
                "FEW_HEIGHTS_COMPLEX_BLOCK",
//            "FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK",
                "NO_HOLES_MULTILAYER_BLOCK",
//            "INDEXED_MULTILAYER_BLOCK",
                "INDEXED_MULTILAYER_32_BLOCK",
        })
        GeoDriverBytesConstants.E blockType;
        boolean loadFromFile = true;
        boolean saveToFile = false;

        @SneakyThrows
        @Setup(Level.Trial)
        public void setup() {
            GeoConfig geoConfigOld = new GeoConfig();
            geoConfigOld.setReuseFlatBlockEnabled(false);

            driverOld = new GeoDriver(geoConfigOld);

            GeoConfig geoConfig = new GeoConfig();
            geoConfig.setOneHeightComplexBlockEnabled(true);
            geoConfig.setBaseHeightComplexBlockEnabled(true);
            geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
            geoConfig.setFewHeightsComplexBlockEnabled(true);
            geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
            geoConfig.setNoHolesMultilayerBlockEnabled(true);
            geoConfig.setIndexedMultilayerBlockEnabled(true);
            geoConfig.setIndexedMultilayer32BlockEnabled(true);

            driver = new GeoDriver(geoConfig);
            File resource = new File(GeoDriverBenchParams.class.getClassLoader().getResource(tstRegion).getFile());
            try {
                driverOld.loadRegion(resource.toPath(), regionX, regionY);
                driver.loadRegion(resource.toPath(), regionX, regionY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            driverBytes = new GeoDriverBytes(geoConfig);
            driverBytes.loadFromL2J(List.of(resource.toPath()));

            Path binGeoData = Path.of(GEODATA_BIN_DIR);
            Files.createDirectories(binGeoData);
            driverBytes.writeToFiles(binGeoData);

            driverBytesMmap = new GeoDriverBytesMmap();
            driverBytesMmap.loadBin(binGeoData);

            generateCheckPoints();
        }

        @SneakyThrows
        public void generateCheckPoints() {
            int cornerMinWorldX = regionX * 32768 + GeoConstants.WORLD_MIN_X;
            int cornerMinWorldY = regionY * 32768 + GeoConstants.WORLD_MIN_Y;
            int cornerMaxWorldX = cornerMinWorldX + 32768 - 1;
            int cornerMaxWorldY = cornerMinWorldY + 32768 - 1;

            checkPoints.clear();

            if (loadFromFile && blockType != null) {
                Path dir = Path.of(DIR_WITH_POINTS);
                List<String> pointsStrs = Files.readAllLines(dir.resolve(blockType.name() + "_points.txt"));
                pointsStrs.forEach(pointStr -> {
                    String[] split = pointStr.split(";");
                    checkPoints.add(new Point(
                            Integer.parseInt(split[0]),
                            Integer.parseInt(split[1]),
                            Integer.parseInt(split[2]),
                            Integer.parseInt(split[3]),
                            Byte.parseByte(split[4])
                    ));
                });
            } else {
                Random r = new Random(1);
                int filteredBlockType;
                if (blockType == null) {
                    filteredBlockType = -1;
                } else {
                    filteredBlockType = GeoDriverBytesConstants.blockNameToType(blockType.name());
                }
                while (checkPoints.size() < checkPointCount) {
                    int worldX = r.nextInt(cornerMaxWorldX - cornerMinWorldX) + cornerMinWorldX;
                    int worldY = r.nextInt(cornerMaxWorldY - cornerMinWorldY) + cornerMinWorldY;
                    int geoX = driver.getGeoX(worldX);
                    int geoY = driver.getGeoY(worldY);

                    if (filteredBlockType == -1 || driver.getBlockType(geoX, geoY) == filteredBlockType) {
                        checkPoints.add(new Point(
                                        worldX,
                                        worldY,
                                        geoX,
                                        geoY,
                                        (byte) (1 << r.nextInt(4))
                                )
                        );
                    }
                }
            }

            if (saveToFile && blockType != null) {
                String pointsString = checkPoints.stream()
                        .map(p -> p.x() + ";" + p.y() + ";" + p.geoX() + ";" + p.geoY() + ";" + p.nswe())
                        .collect(Collectors.joining("\n"));

                Path dir = Path.of(DIR_WITH_POINTS);
                Files.createDirectories(dir);
                Files.writeString(dir.resolve(blockType.name() + "_points.txt"), pointsString);
                System.out.println("Points saved at " + blockType.name() + "_points.txt");
            }
        }
    }

    record Point(int x, int y, int geoX, int geoY, byte nswe) {
    }

    //----  geo old driver


//    @Benchmark
//    public void hasGeoPos_old(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driverOld;
//        for (Point checkPoint : state.checkPoints) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }

    @Benchmark
    public void getNearestZ_old(Blackhole blackhole, MyState state) {
        GeoDriver driver = state.driverOld;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

    @Benchmark
    public void getNextLowerZ_old(Blackhole blackhole, MyState state) {
        GeoDriver driver = state.driverOld;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

    @Benchmark
    public void getNextHigherZ_old(Blackhole blackhole, MyState state) {
        GeoDriver driver = state.driverOld;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

//
//    @Benchmark
//    public void checkNearestNSWE_old(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPoints) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }

    //----  geo with config

//    @Benchmark
//    public void hasGeoPos(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPoints) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }

    @Benchmark
    public void getNearestZ(Blackhole blackhole, MyState state) {
        GeoDriver driver = state.driver;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

    @Benchmark
    public void getNextLowerZ(Blackhole blackhole, MyState state) {
        GeoDriver driver = state.driver;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

    @Benchmark
    public void getNextHigherZ(Blackhole blackhole, MyState state) {
        GeoDriver driver = state.driver;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

//
//    @Benchmark
//    public void checkNearestNSWE(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPoints) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }

    //----  geo bytes

//    @Benchmark
//    public void hasGeoPosBytes(Blackhole blackhole, MyState state) {
//        GeoDriverBytes driver = state.driverBytes;
//        for (Point checkPoint : state.checkPoints) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }

    @Benchmark
    public void getNearestZBytes(Blackhole blackhole, MyState state) {
        GeoDriverBytes driver = state.driverBytes;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

    @Benchmark
    public void getNextLowerZBytes(Blackhole blackhole, MyState state) {
        GeoDriverBytes driver = state.driverBytes;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

    @Benchmark
    public void getNextHigherZBytes(Blackhole blackhole, MyState state) {
        GeoDriverBytes driver = state.driverBytes;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }
//
//    @Benchmark
//    public void checkNearestNSWEBytes(Blackhole blackhole, MyState state) {
//        GeoDriverBytes driver = state.driverBytes;
//        for (Point checkPoint : state.checkPoints) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }

    //----  geo bytes mmap

//    @Benchmark
//    public void hasGeoPosBytesMmap(Blackhole blackhole, MyState state) {
//        GeoDriverBytesMmap driver = state.driverBytesMmap;
//        for (Point checkPoint : state.checkPoints) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }

    @Benchmark
    public void getNearestZBytesMmap(Blackhole blackhole, MyState state) {
        GeoDriverBytesMmap driver = state.driverBytesMmap;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

    @Benchmark
    public void getNextLowerZBytesMmap(Blackhole blackhole, MyState state) {
        GeoDriverBytesMmap driver = state.driverBytesMmap;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }

    @Benchmark
    public void getNextHigherZBytesMmap(Blackhole blackhole, MyState state) {
        GeoDriverBytesMmap driver = state.driverBytesMmap;
        for (Point checkPoint : state.checkPoints) {
            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
        }
    }
//
//    @Benchmark
//    public void checkNearestNSWEBytesMmap(Blackhole blackhole, MyState state) {
//        GeoDriverBytesMmap driver = state.driverBytesMmap;
//        for (Point checkPoint : state.checkPoints) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }

}
