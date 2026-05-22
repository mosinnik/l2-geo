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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.abstraction.IGeoDriver;
import ru.mosinnik.l2eve.geodriver.abstraction.IRegion;
import ru.mosinnik.l2eve.geodriver.blocks.FlatBlock;
import ru.mosinnik.l2eve.geodriver.bytes.*;
import ru.mosinnik.l2eve.geodriver.regions.Region;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.GEO_REGIONS_X;
import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.GEO_REGIONS_Y;
import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants.*;
import static ru.mosinnik.l2eve.geodriver.util.Converter.asBytes;
import static ru.mosinnik.l2eve.geodriver.util.Converter.asInts;

/**
 *
 *
 * Benchmark                                                    (blockTypeStr)   Mode  Cnt        Score     Error      Units
 * GeoDriverBenchParams.checkNearestNSWEBytes                           RANDOM  thrpt    5     3315.979 ±  17.491      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes:CPI                       RANDOM  thrpt             1.163            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytes:IPC                       RANDOM  thrpt             0.860            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytes:branch-misses             RANDOM  thrpt           847.730                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:branches                  RANDOM  thrpt         99970.084                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:cycles                    RANDOM  thrpt        944532.684                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:instructions              RANDOM  thrpt        812065.885                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes                       FLAT_BLOCK  thrpt    5    22320.394 ± 582.434      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes:CPI                   FLAT_BLOCK  thrpt             0.412            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytes:IPC                   FLAT_BLOCK  thrpt             2.426            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytes:branch-misses         FLAT_BLOCK  thrpt            12.925                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:branches              FLAT_BLOCK  thrpt         60205.271                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:cycles                FLAT_BLOCK  thrpt        140718.591                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:instructions          FLAT_BLOCK  thrpt        341356.555                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes                    COMPLEX_BLOCK  thrpt    5     4318.410 ± 183.928      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes:CPI                COMPLEX_BLOCK  thrpt             1.213            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytes:IPC                COMPLEX_BLOCK  thrpt             0.824            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytes:branch-misses      COMPLEX_BLOCK  thrpt            24.080                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:branches           COMPLEX_BLOCK  thrpt         71213.685                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:cycles             COMPLEX_BLOCK  thrpt        725388.907                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:instructions       COMPLEX_BLOCK  thrpt        598025.245                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes                 MULTILAYER_BLOCK  thrpt    5      715.002 ±   3.873      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes:CPI             MULTILAYER_BLOCK  thrpt             0.976            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytes:IPC             MULTILAYER_BLOCK  thrpt             1.024            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytes:branch-misses   MULTILAYER_BLOCK  thrpt         23970.153                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:branches        MULTILAYER_BLOCK  thrpt        665768.829                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:cycles          MULTILAYER_BLOCK  thrpt       4400924.218                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes:instructions    MULTILAYER_BLOCK  thrpt       4508662.244                 #/op
 *
 * GeoDriverBenchParams.checkNearestNSWEBytes2                          RANDOM  thrpt    5     2764.697 ±  12.075      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes2:CPI                      RANDOM  thrpt             0.898            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytes2:IPC                      RANDOM  thrpt             1.113            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytes2:branch-misses            RANDOM  thrpt          1682.921                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:branches                 RANDOM  thrpt        181400.127                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:cycles                   RANDOM  thrpt       1136258.970                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:instructions             RANDOM  thrpt       1264745.684                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2                      FLAT_BLOCK  thrpt    5    22356.444 ± 298.725      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes2:CPI                  FLAT_BLOCK  thrpt             0.422            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytes2:IPC                  FLAT_BLOCK  thrpt             2.367            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytes2:branch-misses        FLAT_BLOCK  thrpt            12.936                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:branches             FLAT_BLOCK  thrpt         50240.565                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:cycles               FLAT_BLOCK  thrpt        140063.143                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:instructions         FLAT_BLOCK  thrpt        331595.112                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2                   COMPLEX_BLOCK  thrpt    5     4442.877 ±  74.090      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes2:CPI               COMPLEX_BLOCK  thrpt             1.201            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytes2:IPC               COMPLEX_BLOCK  thrpt             0.833            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytes2:branch-misses     COMPLEX_BLOCK  thrpt            21.969                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:branches          COMPLEX_BLOCK  thrpt         71000.794                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:cycles            COMPLEX_BLOCK  thrpt        704238.538                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:instructions      COMPLEX_BLOCK  thrpt        586413.402                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2                MULTILAYER_BLOCK  thrpt    5      645.980 ±   6.895      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes2:CPI            MULTILAYER_BLOCK  thrpt             0.806            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytes2:IPC            MULTILAYER_BLOCK  thrpt             1.241            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytes2:branch-misses  MULTILAYER_BLOCK  thrpt         23913.098                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:branches       MULTILAYER_BLOCK  thrpt       1032539.120                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:cycles         MULTILAYER_BLOCK  thrpt       4883905.261                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytes2:instructions   MULTILAYER_BLOCK  thrpt       6061990.199                 #/op
 */
@Slf4j
public final class GeoDriverBytes2 implements IGeoDriver {

    private final GeoConfig config;

    // гео данные
    private ByteBuffer data;

    // по индексу содержится оффсет первого блока региона в blockTypes и blockDataOffsets
    // offset at `blockDataOffsets` array of first region block
    private final int[] regionFirstBlockIndexes = new int[GEO_REGIONS_X * GEO_REGIONS_Y]; //1024

    private byte[] blockTypes;

    // оффсет начала блока в data
    private int[] blockDataOffsets;

    private long[] blockData;
    private int[] blockDataI;

    public GeoDriverBytes2() {
        config = new GeoConfig();
    }

    public GeoDriverBytes2(GeoConfig config) {
        this.config = config;
    }

    @SneakyThrows
    public void load(Path geoDataDir, boolean l2j) {
        if (l2j) {
            loadL2J(geoDataDir);
        } else {
            loadBin(geoDataDir);
        }
    }

    @SneakyThrows
    public void loadL2J(Path geoDataDir) {
        loadFromL2JDir(geoDataDir);
    }

    @SneakyThrows
    public void loadBin(Path geoDataDir) {
        readFromFiles(geoDataDir);
    }

    @SneakyThrows
    public void writeToFiles(Path dataDir) {
        Files.write(dataDir.resolve(DATA_FILE_NAME), data.array());
        log.info("Updated data file: {}", DATA_FILE_NAME);

        Files.write(dataDir.resolve(REGION_FIRST_BLOCK_INDEXES_FILE_NAME), asBytes(regionFirstBlockIndexes));
        log.info("Updated regionFirstBlockIndexes file: {}", REGION_FIRST_BLOCK_INDEXES_FILE_NAME);

        Files.write(dataDir.resolve(BLOCK_TYPES_FILE_NAME), blockTypes);
        log.info("Updated blockTypes file: {}", BLOCK_TYPES_FILE_NAME);

        Files.write(dataDir.resolve(BLOCK_DATA_OFFSETS_FILE_NAME), asBytes(blockDataOffsets));
        log.info("Updated blockDataOffsets file: {}", BLOCK_DATA_OFFSETS_FILE_NAME);
    }

    @SneakyThrows
    public void readFromFiles(Path dataDir) {
        data = ByteBuffer.wrap(Files.readAllBytes(dataDir.resolve(DATA_FILE_NAME)));
        log.info("Read {} bytes from data file: {}", data.capacity(), DATA_FILE_NAME);

        asInts(Files.readAllBytes(dataDir.resolve(REGION_FIRST_BLOCK_INDEXES_FILE_NAME)), regionFirstBlockIndexes);
        log.info("Read {} ints from data file: {}", regionFirstBlockIndexes.length, REGION_FIRST_BLOCK_INDEXES_FILE_NAME);

        blockTypes = Files.readAllBytes(dataDir.resolve(BLOCK_TYPES_FILE_NAME));
        log.info("Read {} bytes from data file: {}", blockTypes.length, BLOCK_TYPES_FILE_NAME);

        blockDataOffsets = asInts(Files.readAllBytes(dataDir.resolve(BLOCK_DATA_OFFSETS_FILE_NAME)));
        log.info("Read {} ints from data file: {}", blockDataOffsets.length, BLOCK_DATA_OFFSETS_FILE_NAME);
    }


    @SneakyThrows
    public void loadFromL2JDir(Path geoDataDir) {
        try (Stream<Path> pathStream = Files.list(geoDataDir)) {
            List<Path> paths = pathStream
                    .filter(path -> path.getFileName().toString().endsWith(".l2j"))
                    .toList();
            loadFromL2J(paths);
        }
    }

    @SneakyThrows
    public void loadFromL2J(List<Path> paths) {

        List<RegionCoordinated> regions = new ArrayList<>();

        for (Path path : paths) {
            String fileName = path.getFileName().toString();
            String[] split = fileName.split("[_.]");
            int regionX = Integer.parseInt(split[0]);
            int regionY = Integer.parseInt(split[1]);

            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
                Region region = new Region(
                        raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length()).order(ByteOrder.LITTLE_ENDIAN),
                        config
                );
                regions.add(new RegionCoordinated(region, regionX, regionY));
            }
        }

        int dataSize = 0;
        int totalBlockCount = 0;
        for (RegionCoordinated regionCoordinated : regions) {
            Region region = regionCoordinated.region();
            for (int i = 0; i < IRegion.REGION_BLOCKS; i++) {
                int bytesCount = ByteUtil.getBytesCount(region.getBlock(i));
                int shift = 6;
                int bytesCount256 = ((bytesCount >> shift) + 1) << shift;

                dataSize += bytesCount256;
                totalBlockCount++;
            }
            log.info("Load {} bytes from {} blocks", dataSize, totalBlockCount);
        }
        assert totalBlockCount == regions.size() * IRegion.REGION_BLOCKS;
        log.info("totalBlockCount = {}", totalBlockCount);

        data = ByteBuffer.allocate(dataSize);

//        blockTypes = new byte[totalBlockCount];
//        blockDataOffsets = new int[totalBlockCount];
//        blockData = new long[totalBlockCount];
        blockDataI = new int[totalBlockCount];
        Arrays.fill(regionFirstBlockIndexes, NO_INDEX);

        int blockIndex = 0;
        for (RegionCoordinated regionCoordinated : regions) {
            Region region = regionCoordinated.region();
            int regionFirstBlockIndex = blockIndex;

            final int regionIndex = (regionCoordinated.regionX() * GEO_REGIONS_Y) + regionCoordinated.regionY();
            regionFirstBlockIndexes[regionIndex] = regionFirstBlockIndex;

            for (int i = 0; i < IRegion.REGION_BLOCKS; i++) {
                IBlock block = region.getBlock(i);

                byte blockType = ByteUtil.getType(block);
//                blockTypes[blockIndex] = blockType;

                if (blockType == FLAT_BLOCK) {
                    int height = FlatBlockFromOffsetBytes.getHeight((FlatBlock) block);
//                    blockDataOffsets[blockIndex] = height;

//                    blockData[blockIndex] = (((long) blockType) << 32) | (((long) height) & 0xFFFFFFFFL);
                    blockDataI[blockIndex] = (height << 6) | blockType;
                } else {
                    int blockDataOffset = data.position();
//                    blockDataOffsets[blockIndex] = blockDataOffset;
//                    blockData[blockIndex] = (((long) blockType) << 32) | blockDataOffset;
                    blockDataI[blockIndex] = (blockDataOffset & 0xFFFFFFC0) | blockType;
                    ByteUtil.appendBytes(block, data);
                    data.position(((data.position() >> 6) + 1) << 6);
                }

                blockIndex++;
            }
        }
        assert totalBlockCount == blockIndex;

        // если не добавить вызов data.capacity(), то почемуто перф тесты иногда сильно деградируют
        log.info("data size: {}", data.capacity());
    }

    public void printStats() {
        int regionCount = 0;
        for (int regionFirstBlockIndex : regionFirstBlockIndexes) {
            if (regionFirstBlockIndex != NO_INDEX) {
                regionCount++;
            }
        }
        Map<Byte, AtomicInteger> typesCount = new TreeMap<>();
        for (byte blockType : blockTypes) {
            typesCount.computeIfAbsent(blockType, k -> new AtomicInteger()).incrementAndGet();
        }
        Map<Byte, AtomicInteger> typesSizes = new TreeMap<>();
        Map<Integer, AtomicInteger> multilayerSizes = new TreeMap<>();
        for (int i = 0; i < blockTypes.length; i++) {
            byte blockType = blockTypes[i];
            int size = getSize(blockType, blockDataOffsets[i]);
            typesSizes.computeIfAbsent(blockType, k -> new AtomicInteger()).addAndGet(size);
            if (blockType == MULTILAYER_BLOCK) {
                multilayerSizes.computeIfAbsent(size, k -> new AtomicInteger()).incrementAndGet();
            }
        }

        log.info("Regions data size: {} (ints), with offsets: {}", regionFirstBlockIndexes.length, regionCount);
        log.info("Data size: {} (bytes)", data.capacity());
        log.info("Blocks offsets: {} (ints)", blockDataOffsets.length);
        log.info("Blocks count: {} (bytes)", blockTypes.length);
        for (Map.Entry<Byte, AtomicInteger> entry : typesCount.entrySet()) {
            int size = typesSizes.get(entry.getKey()).get();
            int blockCount = entry.getValue().get();
            log.info("-- Block type: {} -> {}, in data {} bytes ({})  -- {}",
                    entry.getKey(), blockCount, size, (double) size / blockCount,
                    GeoDriverBytesConstants.blockTypeToName(entry.getKey())
            );
        }
        log.info("Multilayer data sizes count: {}", multilayerSizes.size());
        for (Map.Entry<Integer, AtomicInteger> entry : multilayerSizes.entrySet()) {
            int blockCount = entry.getValue().get();
            log.info("-- Multilayer size: {} -> {}",
                    entry.getKey(), blockCount
            );
        }
    }

    /**
     * Used only in printStats().
     * Return block data size.
     */
    private int getSize(byte blockType, int blockDataOffset) {
        switch (blockType) {
            case FLAT_BLOCK -> {
                return FlatBlockFromOffsetBytes.getSize(blockDataOffset, data);
            }
            case COMPLEX_BLOCK -> {
                return ComplexBlockBytes.getSize(blockDataOffset, data);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockBytes.getSize(blockDataOffset, data);
            }
            case ONE_HEIGHT_COMPLEX_BLOCK -> {
                return OneHeightComplexBlockBytes.getSize(blockDataOffset, data);
            }
            case BASE_HEIGHT_COMPLEX_BLOCK -> {
                return BaseHeightComplexBlockBytes.getSize(blockDataOffset, data);
            }
            case BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK -> {
                return BaseHeightOneNsweComplexBlockBytes.getSize(blockDataOffset, data);
            }
            case FEW_HEIGHTS_COMPLEX_BLOCK -> {
                return FewHeightsComplexBlockBytes.getSize(blockDataOffset, data);
            }
            case FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK -> {
                return FewHeightsOneNsweComplexBlockBytes.getSize(blockDataOffset, data);
            }
            case NO_HOLES_MULTILAYER_BLOCK -> {
                return NoHolesMultilayerBlockBytes.getSize(blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_BLOCK -> {
                return IndexedMultilayerBlockBytes.getSize(blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.getSize(blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

    record RegionCoordinated(Region region, int regionX, int regionY) {
    }

    @Override
    public void loadRegion(Path filePath) {
        throw new RuntimeException("Not implemented");
    }


    @Override
    public boolean hasGeoPos(int geoX, int geoY) {
        int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return false;
        }
        return true;
    }

    public int getBlockType(int geoX, int geoY) {
        int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return -1;
        }

        int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);

        return blockTypes[regionFirstBlockIndex + blockIndexInRegion];
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe);
        }

        int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);
        int blockIndex = regionFirstBlockIndex + blockIndexInRegion;

//        long blockDatum = blockData[blockIndex];
//        byte blockType = (byte) ((blockDatum >> 32) & 0xFF);
//        int blockDataOffset = (int) (blockDatum & 0xFFFFFFFFL);

        int blockDatum = blockDataI[blockIndex];
        int blockType = blockDatum & 0x3F;
//        if (blockType == FLAT_BLOCK) {
//            return FlatBlockFromOffsetBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe);
//        }

        int blockDataOffset = blockDatum & 0xFFFFFFC0;

//        byte blockType = blockTypes[blockIndex];
// //        blockTypesCount.computeIfAbsent((int) blockType, k -> new AtomicInteger()).incrementAndGet();
//
//        int blockDataOffset = blockDataOffsets[blockIndex];
        switch (blockType) {
            case FLAT_BLOCK -> {
                return FlatBlockFromOffsetBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe);
            }
            case COMPLEX_BLOCK -> {
                return ComplexBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case ONE_HEIGHT_COMPLEX_BLOCK -> {
                return OneHeightComplexBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case BASE_HEIGHT_COMPLEX_BLOCK -> {
                return BaseHeightComplexBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK -> {
                return BaseHeightOneNsweComplexBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case FEW_HEIGHTS_COMPLEX_BLOCK -> {
                return FewHeightsComplexBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK -> {
                return FewHeightsOneNsweComplexBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case NO_HOLES_MULTILAYER_BLOCK -> {
                return NoHolesMultilayerBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_BLOCK -> {
                return IndexedMultilayerBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType + " " + blockDatum);
        }
    }


    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.getNearestZ(geoX, geoY, worldZ);
        }

        int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);

//        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
//        int blockDataOffset = blockDataOffsets[regionFirstBlockIndex + blockIndexInRegion];

//        long blockDatum = blockData[regionFirstBlockIndex + blockIndexInRegion];
//        byte blockType = (byte) ((blockDatum >> 32) & 0xFF);
//        int blockDataOffset = (int) (blockDatum & 0xFFFFFFFFL);

        int blockDatum = blockDataI[regionFirstBlockIndex + blockIndexInRegion];
        byte blockType = (byte) (blockDatum & 0x3F);
        int blockDataOffset = blockDatum & 0xFFFFFFC0;

        switch (blockType) {
            case FLAT_BLOCK -> {
//                return FlatBlockFromOffsetBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
                return FlatBlockFromOffsetBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset >> 6, data);
            }
            case COMPLEX_BLOCK -> {
                return ComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case ONE_HEIGHT_COMPLEX_BLOCK -> {
                return OneHeightComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case BASE_HEIGHT_COMPLEX_BLOCK -> {
                return BaseHeightComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK -> {
                return BaseHeightOneNsweComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case FEW_HEIGHTS_COMPLEX_BLOCK -> {
                return FewHeightsComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK -> {
                return FewHeightsOneNsweComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case NO_HOLES_MULTILAYER_BLOCK -> {
                return NoHolesMultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_BLOCK -> {
                return IndexedMultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.getNextLowerZ(geoX, geoY, worldZ);
        }

        int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);

//        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
//        int blockDataOffset = blockDataOffsets[regionFirstBlockIndex + blockIndexInRegion];

//        long blockDatum = blockData[regionFirstBlockIndex + blockIndexInRegion];
//        byte blockType = (byte) ((blockDatum >> 32) & 0xFF);
//        int blockDataOffset = (int) (blockDatum & 0xFFFFFFFFL);

        int blockDatum = blockDataI[regionFirstBlockIndex + blockIndexInRegion];
        byte blockType = (byte) (blockDatum & 0x3F);
        int blockDataOffset = blockDatum & 0xFFFFFFC0;

        switch (blockType) {
            case FLAT_BLOCK -> {
//                return FlatBlockFromOffsetBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
                return FlatBlockFromOffsetBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset >> 6, data);
            }
            case COMPLEX_BLOCK -> {
                return ComplexBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case ONE_HEIGHT_COMPLEX_BLOCK -> {
                return OneHeightComplexBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case BASE_HEIGHT_COMPLEX_BLOCK -> {
                return BaseHeightComplexBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK -> {
                return BaseHeightOneNsweComplexBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case FEW_HEIGHTS_COMPLEX_BLOCK -> {
                return FewHeightsComplexBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK -> {
                return FewHeightsOneNsweComplexBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case NO_HOLES_MULTILAYER_BLOCK -> {
                return NoHolesMultilayerBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_BLOCK -> {
                return IndexedMultilayerBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.getNextHigherZ(geoX, geoY, worldZ);
        }

        int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);

//        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
//        int blockDataOffset = blockDataOffsets[regionFirstBlockIndex + blockIndexInRegion];

//        long blockDatum = blockData[regionFirstBlockIndex + blockIndexInRegion];
//        byte blockType = (byte) ((blockDatum >> 32) & 0xFF);
//        int blockDataOffset = (int) (blockDatum & 0xFFFFFFFFL);

        int blockDatum = blockDataI[regionFirstBlockIndex + blockIndexInRegion];
        byte blockType = (byte) (blockDatum & 0x3F);
        int blockDataOffset = blockDatum & 0xFFFFFFC0;

        switch (blockType) {
            case FLAT_BLOCK -> {
//                return FlatBlockFromOffsetBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
                return FlatBlockFromOffsetBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset >> 6, data);
            }
            case COMPLEX_BLOCK -> {
                return ComplexBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case ONE_HEIGHT_COMPLEX_BLOCK -> {
                return OneHeightComplexBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case BASE_HEIGHT_COMPLEX_BLOCK -> {
                return BaseHeightComplexBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK -> {
                return BaseHeightOneNsweComplexBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case FEW_HEIGHTS_COMPLEX_BLOCK -> {
                return FewHeightsComplexBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK -> {
                return FewHeightsOneNsweComplexBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case NO_HOLES_MULTILAYER_BLOCK -> {
                return NoHolesMultilayerBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_BLOCK -> {
                return IndexedMultilayerBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

}
