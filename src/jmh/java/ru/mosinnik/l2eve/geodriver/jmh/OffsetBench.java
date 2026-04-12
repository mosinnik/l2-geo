package ru.mosinnik.l2eve.geodriver.jmh;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.LinuxPerfAsmProfiler;
import org.openjdk.jmh.profile.LinuxPerfNormProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Threads(1)
@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Timeout(time = 100)
public class OffsetBench {

    public static final Random r = new Random(1);

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
//            argsAppend.add("-XX:TieredStopAtLevel=1");
//            argsAppend.add("-Xint");
//            argsAppend.add("-XX:+PrintFlagsFinal");
        }

        ChainedOptionsBuilder builder = new OptionsBuilder()
                .include(OffsetBench.class.getSimpleName())
                .jvmArgsAppend(argsAppend.toArray(new String[0]))
//                .addProfiler(WinPerfAsmProfiler.class, "tooBigThreshold=2500")
//                .addProfiler(Prof.class)
                .addProfiler(LinuxPerfAsmProfiler.class)
//                .addProfiler(LinuxPerfProfiler.class)
                .addProfiler(LinuxPerfNormProfiler.class)
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


    @Benchmark()
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int withPlus(Blackhole blackhole) {
//        blackhole.consume(withPlus_(r.nextInt(Short.MAX_VALUE), r.nextInt(Short.MAX_VALUE)));
//        blackhole.consume(withPlus_(r.nextInt(Short.MAX_VALUE), r.nextInt(Short.MAX_VALUE)));
        return withOr_(12345, 4321);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int dsdsdwithOr(Blackhole blackhole) {
//        blackhole.consume(withOr_(r.nextInt(Short.MAX_VALUE), r.nextInt(Short.MAX_VALUE)));
//        blackhole.consume(withOr_(r.nextInt(Short.MAX_VALUE), r.nextInt(Short.MAX_VALUE)));
       return withPlus_(12345, 4321);
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int withPlus_(int geoX, int geoY) {
        return (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);
//        return (((geoX >> 11) << 5) + (geoY >> 11));
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int withOr_(int geoX, int geoY) {
        return (((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF));
//        return (((geoX >> 11) << 5) | (geoY >> 11));
    }
}
