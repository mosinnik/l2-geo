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
import org.openjdk.jmh.profile.LinuxPerfNormProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import ru.mosinnik.l2eve.geodriver.driver.*;
import ru.mosinnik.l2eve.geodriver.gen.GeoDriverBytesGen;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ru.mosinnik.l2eve.geodriver.jmh.GeoDriverTestConstants.GEODATA_BIN_DIR;
import static ru.mosinnik.l2eve.geodriver.jmh.GeoDriverTestConstants.TST_BLOCK_RESOURCE_MOST_COMPLEX;
import static ru.mosinnik.l2eve.geodriver.jmh.PointsPreparer.loadPoints;


@Threads(4)
@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 60)
@Timeout(time = 100)
//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class GeoDriverBenchParams {
    public static final int CHECK_POINT_COUNT = 10000;

    //    public static final String TST_REGION tstRegion = TST_BLOCK_RESOURCE_ALMOST_EMPTY;
//    public static final String TST_REGION tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
    public static final String TST_REGION = TST_BLOCK_RESOURCE_MOST_COMPLEX;

    public static void main(String[] args) throws Exception {

        String hostName = InetAddress.getLocalHost().getHostName();

        boolean needJfr = false;
        boolean setC1CompileFlags = false;


        List<String> argsAppend = new ArrayList<>();
        if (needJfr) {
            // uncomment if need jfrs
            argsAppend.add("-XX:+UnlockDiagnosticVMOptions");
            argsAppend.add("-XX:+DebugNonSafepoints");
            argsAppend.add("-XX:+FlightRecorder");
            argsAppend.add("-XX:StartFlightRecording:filename=jfrs/,debugNonSafePoints=true,jdk.ExecutionSample#period=1 ms");
        }

        if (setC1CompileFlags) {
//            argsAppend.add("-XX:+UnlockDiagnosticVMOptions");
            argsAppend.add("-XX:CompileCommandFile=hotspot_compiler");
//            argsAppend.add("-XX:+PrintCompilation");
//            argsAppend.add("-XX:+PrintInlining");
//            argsAppend.add("-XX:C1InlineStackLimit=50");
//            argsAppend.add("-XX:C1MaxInlineLevel=50");
//            argsAppend.add("-XX:C1MaxInlineSize=5000");
            argsAppend.add("-XX:TieredStopAtLevel=1");
//            argsAppend.add("-Xint");
//            argsAppend.add("-XX:+PrintFlagsFinal");
        }

        ChainedOptionsBuilder builder = new OptionsBuilder()
                .include(GeoDriverBenchParams.class.getSimpleName())
                .jvmArgsAppend(argsAppend.toArray(new String[0]))
//                .addProfiler(WinPerfAsmProfiler.class, "tooBigThreshold=2500")
//                .addProfiler(Prof.class)
//                .addProfiler(LinuxPerfAsmProfiler.class)
//                .addProfiler(LinuxPerfProfiler.class)
                .addProfiler(LinuxPerfNormProfiler.class)
//                .addProfiler(LinuxPerfNormProfiler.class, "event=instructions,cycles,branches,branch-misses")
//                .addProfiler(AsyncProfiler.class, "output=flamegraph")
//                .resultFormat(ResultFormatType.JSON)
                ;


        if (hostName.equals("mini")) {
            builder.resultFormat(ResultFormatType.CSV)
                    .result("/opt/git/l2-geo-idea/jmh_results/GeoDriverBenchParams_" + System.currentTimeMillis() + ".csv");
        } else {
            builder.resultFormat(ResultFormatType.CSV)
                    .result("results/GeoDriverBenchParams_" + System.currentTimeMillis() + ".csv");
        }

        new Runner(builder.build()).run();
    }


    @State(Scope.Benchmark)
    public static class MyState {

        GeoDriver driverOld;
        GeoDriver driver;
        GeoDriverBytes driverBytes;
        GeoDriverBytesDirectInlO driverBytesDirectInlO;
        GeoDriverBytesDirectIf driverBytesDirectIf;
        GeoDriverBytesDirectIfCmp driverBytesDirectIfCmp;
        GeoDriverBytesGen driverBytesGen;
        GeoDriverBytes2 driverBytes2;
        GeoDriverBytesMmap driverBytesMmap;
        GeoDriverFFMStruct driverFFMStruct;
        GeoDriverFFMLong driverFFMLong;
        GeoDriverFFMT driverFFMT;
        GeoDriverFFMT2 driverFFMT2;
        Point[] checkPointsArr;

        @Param({
                "RANDOM",
                "FLAT_BLOCK",
                "COMPLEX_BLOCK",
                "MULTILAYER_BLOCK",
//                "ONE_HEIGHT_COMPLEX_BLOCK",
//                "BASE_HEIGHT_COMPLEX_BLOCK",
//                "BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK",
//                "FEW_HEIGHTS_COMPLEX_BLOCK",
////            "FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK",
//                "NO_HOLES_MULTILAYER_BLOCK",
////            "INDEXED_MULTILAYER_BLOCK",
//                "INDEXED_32_MULTILAYER_BLOCK",
        })
        String blockTypeStr;
        GeoDriverBytesConstants.E blockType;
        boolean loadFromFile = true;
        boolean saveToFile = false;

        @SneakyThrows
        @Setup(Level.Trial)
        public void setup() {
            blockType = toBlockType(blockTypeStr);

            GeoConfig geoConfigOld = new GeoConfig();
            geoConfigOld.setReuseFlatBlockEnabled(false);

            driverOld = new GeoDriver(geoConfigOld);

            GeoConfig geoConfig = new GeoConfig();
//            geoConfig.setOneHeightComplexBlockEnabled(true);
//            geoConfig.setBaseHeightComplexBlockEnabled(true);
//            geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
//            geoConfig.setFewHeightsComplexBlockEnabled(true);
//            geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
//            geoConfig.setNoHolesMultilayerBlockEnabled(true);
//            geoConfig.setIndexedMultilayerBlockEnabled(true);
//            geoConfig.setIndexed32MultilayerBlockEnabled(true);

            driver = new GeoDriver(geoConfig);

            File resource = new File(GeoDriverBenchParams.class.getClassLoader().getResource(TST_REGION).getFile());
//            try {
//                driverOld.loadRegion(resource.toPath());
//                driver.loadRegion(resource.toPath());
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//            driverBytes = new GeoDriverBytes(geoConfig);
//            driverBytes.loadFromL2J(List.of(resource.toPath()));
//            driverBytesDirectInlO = new GeoDriverBytesDirectInlO(geoConfig);
//            driverBytesDirectInlO.loadFromL2J(List.of(resource.toPath()));
//            driverBytesDirectIf = new GeoDriverBytesDirectIf(geoConfig);
//            driverBytesDirectIf.loadFromL2J(List.of(resource.toPath()));
//            driverBytesDirectIfCmp = new GeoDriverBytesDirectIfCmp(geoConfig);
//            driverBytesDirectIfCmp.loadFromL2J(List.of(resource.toPath()));
//            driverBytesGen = new GeoDriverBytesGen(geoConfig);
//            driverBytesGen.loadFromL2J(List.of(resource.toPath()));
//            driverBytes2 = new GeoDriverBytes2(geoConfig);
//            driverBytes2.loadFromL2J(List.of(resource.toPath()));

            driverFFMT = new GeoDriverFFMT(geoConfig, List.of(resource.toPath()));
            driverFFMT2 = new GeoDriverFFMT2(geoConfig, List.of(resource.toPath()));
//            driverFFMStruct = new GeoDriverFFMStruct(geoConfig, List.of(resource.toPath()));
//            driverFFMLong = new GeoDriverFFMLong(geoConfig, List.of(resource.toPath()));

//            Path binGeoData = Path.of(GEODATA_BIN_DIR);
//            Files.createDirectories(binGeoData);
//            driverBytes.writeToFiles(binGeoData);
//
//            driverBytesMmap = new GeoDriverBytesMmap();
//            driverBytesMmap.loadBin(binGeoData);

            List<Point> checkPoints = loadPoints(this);
            checkPointsArr = checkPoints.toArray(Point[]::new);
        }

        private GeoDriverBytesConstants.E toBlockType(String blockTypeStr) {
            if (blockTypeStr == null || blockTypeStr.equals("RANDOM")) {
                return null;
            }
            return GeoDriverBytesConstants.E.valueOf(blockTypeStr);
        }

    }

    record Point(int x, int y, int geoX, int geoY, byte nswe, byte type) {
    }

    //    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWE_base_black(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(checkPoint.geoX());
//            blackhole.consume(checkPoint.geoY());
//            blackhole.consume(-3000);
//            blackhole.consume(checkPoint.nswe());
//        }
//    }
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWE_base(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE_base(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }


    //----  geo old driver


//    @Benchmark
//    public void hasGeoPos_old(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driverOld;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }

    //    @Benchmark
//    public void getNearestZ_old(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driverOld;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextLowerZ_old(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driverOld;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextHigherZ_old(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driverOld;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWE_old(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }

    //----  geo with config

//    @Benchmark
//    public void hasGeoPos(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }

//    @Benchmark
//    public void getNearestZ(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextLowerZ(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }

    //    @Benchmark
//    public void getNextHigherZ(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWE(Blackhole blackhole, MyState state) {
//        GeoDriver driver = state.driver;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }


    //----  geo bytes

//    @Benchmark
//    public void hasGeoPosBytes(Blackhole blackhole, MyState state) {
//        GeoDriverBytes driver = state.driverBytes;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }

    //        @Benchmark
//    public void getNearestZBytes(Blackhole blackhole, MyState state) {
//        GeoDriverBytes driver = state.driverBytes;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextLowerZBytes(Blackhole blackhole, MyState state) {
//        GeoDriverBytes driver = state.driverBytes;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextHigherZBytes(Blackhole blackhole, MyState state) {
//        GeoDriverBytes driver = state.driverBytes;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//


//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWEBytes(Blackhole blackhole, MyState state) {
//        GeoDriverBytes driver = state.driverBytes;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }

//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWEBytesDirectInlO(Blackhole blackhole, MyState state) {
//        GeoDriverBytesDirectInlO driver = state.driverBytesDirectInlO;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWEBytesDirectIf(Blackhole blackhole, MyState state) {
//        GeoDriverBytesDirectIf driver = state.driverBytesDirectIf;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWEBytesDirectIfCmp(Blackhole blackhole, MyState state) {
//        GeoDriverBytesDirectIfCmp driver = state.driverBytesDirectIfCmp;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWEBytesGen(Blackhole blackhole, MyState state) {
//        GeoDriverBytesGen driver = state.driverBytesGen;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWEBytes2(Blackhole blackhole, MyState state) {
//        GeoDriverBytes2 driver = state.driverBytes2;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }

    //----  geo bytes mmap

    //    @Benchmark
//    public void hasGeoPosBytesMmap(Blackhole blackhole, MyState state) {
//        GeoDriverBytesMmap driver = state.driverBytesMmap;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }
//
//    @Benchmark
//    public void getNearestZBytesMmap(Blackhole blackhole, MyState state) {
//        GeoDriverBytesMmap driver = state.driverBytesMmap;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextLowerZBytesMmap(Blackhole blackhole, MyState state) {
//        GeoDriverBytesMmap driver = state.driverBytesMmap;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextHigherZBytesMmap(Blackhole blackhole, MyState state) {
//        GeoDriverBytesMmap driver = state.driverBytesMmap;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWEBytesMmap(Blackhole blackhole, MyState state) {
//        GeoDriverBytesMmap driver = state.driverBytesMmap;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }

    //----  geo ffm

    //    @Benchmark
//    public void hasGeoPosFFM(Blackhole blackhole, MyState state) {
//        GeoDriverFFM driver = state.driverFFM;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }
//
//    @Benchmark
//    public void getNearestZFFM(Blackhole blackhole, MyState state) {
//        GeoDriverFFM driver = state.driverFFM;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextLowerZFFM(Blackhole blackhole, MyState state) {
//        GeoDriverFFM driver = state.driverFFM;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextHigherZFFM(Blackhole blackhole, MyState state) {
//        GeoDriverFFM driver = state.driverFFM;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//
//    @Benchmark
//    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
//    public void checkNearestNSWEFFMStruct(Blackhole blackhole, MyState state) {
//        GeoDriverFFMStruct driver = state.driverFFMStruct;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }


    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void checkNearestNSWEFFM_T(Blackhole blackhole, MyState state) {
        GeoDriverFFMT driver = state.driverFFMT;
        for (Point checkPoint : state.checkPointsArr) {
            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
        }
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void checkNearestNSWEFFM_T2(Blackhole blackhole, MyState state) {
        GeoDriverFFMT2 driver = state.driverFFMT2;
        for (Point checkPoint : state.checkPointsArr) {
            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
        }
    }


    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public void checkNearestNSWEFFMLong(Blackhole blackhole, MyState state) {
        GeoDriverFFMLong driver = state.driverFFMLong;
        for (Point checkPoint : state.checkPointsArr) {
            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
        }
    }

    //----  geo ffm t

//    @Benchmark
//    public void hasGeoPosFFM_T(Blackhole blackhole, MyState state) {
//        GeoDriverFFMT driver = state.driverFFMT;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.hasGeoPos(checkPoint.geoX(), checkPoint.geoY()));
//        }
//    }

    //        @Benchmark
//    public void getNearestZFFM_T(Blackhole blackhole, MyState state) {
//        GeoDriverFFMT driver = state.driverFFMT;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNearestZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextLowerZFFM_T(Blackhole blackhole, MyState state) {
//        GeoDriverFFMT driver = state.driverFFMT;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextLowerZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void getNextHigherZFFM_T(Blackhole blackhole, MyState state) {
//        GeoDriverFFMT driver = state.driverFFMT;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.getNextHigherZ(checkPoint.geoX(), checkPoint.geoY(), -3000));
//        }
//    }
//
//    @Benchmark
//    public void checkNearestNSWEFFM_T(Blackhole blackhole, MyState state) {
//        GeoDriverFFMT driver = state.driverFFMT;
//        for (Point checkPoint : state.checkPointsArr) {
//            blackhole.consume(driver.checkNearestNSWE(checkPoint.geoX(), checkPoint.geoY(), -3000, checkPoint.nswe()));
//        }
//    }


}
