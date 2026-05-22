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
 * С инлайнингом вместо вызова, с оптимизациями.
 * Результат чуть лучше, на Complex заметный прирост. По тестам связан с тем, что нет лишнего параметра worldZ при вызове.
 * <p>
 * Benchmark                                                             (blockTypeStr)   Mode  Cnt        Score     Error      Units
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect                              RANDOM  thrpt    5     3332.566 ±  41.340      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:CPI                          RANDOM  thrpt             1.158            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:IPC                          RANDOM  thrpt             0.863            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branch-misses                RANDOM  thrpt           783.804                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches                     RANDOM  thrpt        100283.464                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:cycles                       RANDOM  thrpt        943389.400                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions                 RANDOM  thrpt        814460.821                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO                          RANDOM  thrpt    5     3387.257 ±   7.357      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:CPI                      RANDOM  thrpt             1.068            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:IPC                      RANDOM  thrpt             0.936            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:branch-misses            RANDOM  thrpt           960.834                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:branches                 RANDOM  thrpt        126401.379                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:cycles                   RANDOM  thrpt        924910.969                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions             RANDOM  thrpt        865748.990                 #/op
 * <p>
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect                          FLAT_BLOCK  thrpt    5    22144.272 ± 146.823      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:CPI                      FLAT_BLOCK  thrpt             0.415            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:IPC                      FLAT_BLOCK  thrpt             2.408            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branch-misses            FLAT_BLOCK  thrpt            12.810                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches                 FLAT_BLOCK  thrpt         60250.054                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:cycles                   FLAT_BLOCK  thrpt        141878.472                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions             FLAT_BLOCK  thrpt        341606.490                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO                      FLAT_BLOCK  thrpt    5    22287.466 ± 713.400      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:CPI                  FLAT_BLOCK  thrpt             0.413            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:IPC                  FLAT_BLOCK  thrpt             2.422            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:branch-misses        FLAT_BLOCK  thrpt            13.113                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:branches             FLAT_BLOCK  thrpt         60292.818                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:cycles               FLAT_BLOCK  thrpt        141123.155                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions         FLAT_BLOCK  thrpt        341846.722                 #/op
 * <p>
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect                       COMPLEX_BLOCK  thrpt    5     4296.291 ±  46.046      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:CPI                   COMPLEX_BLOCK  thrpt             1.219            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:IPC                   COMPLEX_BLOCK  thrpt             0.820            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branch-misses         COMPLEX_BLOCK  thrpt            23.883                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches              COMPLEX_BLOCK  thrpt         71044.184                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:cycles                COMPLEX_BLOCK  thrpt        726924.588                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions          COMPLEX_BLOCK  thrpt        596433.100                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO                   COMPLEX_BLOCK  thrpt    5     4381.864 ±  96.057      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:CPI               COMPLEX_BLOCK  thrpt             1.196            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:IPC               COMPLEX_BLOCK  thrpt             0.836            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:branch-misses     COMPLEX_BLOCK  thrpt            22.688                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:branches          COMPLEX_BLOCK  thrpt         71023.325                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:cycles            COMPLEX_BLOCK  thrpt        713183.192                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions      COMPLEX_BLOCK  thrpt        596307.292                 #/op
 * <p>
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect                    MULTILAYER_BLOCK  thrpt    5      715.673 ±   3.154      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:CPI                MULTILAYER_BLOCK  thrpt             0.976            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:IPC                MULTILAYER_BLOCK  thrpt             1.025            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branch-misses      MULTILAYER_BLOCK  thrpt         24026.659                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches           MULTILAYER_BLOCK  thrpt        666376.332                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:cycles             MULTILAYER_BLOCK  thrpt       4401822.776                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions       MULTILAYER_BLOCK  thrpt       4511756.347                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO                MULTILAYER_BLOCK  thrpt    5      716.069 ±   1.934      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:CPI            MULTILAYER_BLOCK  thrpt             1.084            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:IPC            MULTILAYER_BLOCK  thrpt             0.923            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:branch-misses  MULTILAYER_BLOCK  thrpt         26278.172                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:branches       MULTILAYER_BLOCK  thrpt        661332.473                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:cycles         MULTILAYER_BLOCK  thrpt       4398824.042                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions   MULTILAYER_BLOCK  thrpt       4058526.784                 #/op
 */
@Slf4j
public final class GeoDriverBytesDirectInlO implements IGeoDriver {

    private final GeoConfig config;

    // гео данные
    private ByteBuffer data;

    // по индексу содержится оффсет первого блока региона в blockTypes и blockDataOffsets
    // offset at `blockDataOffsets` array of first region block
    private final int[] regionFirstBlockIndexes = new int[GEO_REGIONS_X * GEO_REGIONS_Y]; //1024

    private byte[] blockTypes;

    // оффсет начала блока в data
    private int[] blockDataOffsets;

    public GeoDriverBytesDirectInlO() {
        config = new GeoConfig();
    }

    public GeoDriverBytesDirectInlO(GeoConfig config) {
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
        byte[] bytes = Files.readAllBytes(dataDir.resolve(DATA_FILE_NAME));
        data = ByteBuffer.allocateDirect(bytes.length);
        data.put(bytes);
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
                dataSize += ByteUtil.getBytesCount(region.getBlock(i));
                totalBlockCount++;
            }
        }
        assert totalBlockCount == regions.size() * IRegion.REGION_BLOCKS;

        data = ByteBuffer.allocateDirect(dataSize);

        blockTypes = new byte[totalBlockCount];
        blockDataOffsets = new int[totalBlockCount];
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
                blockTypes[blockIndex] = blockType;

                if (blockType == FLAT_BLOCK) {
                    blockDataOffsets[blockIndex] = FlatBlockFromOffsetBytes.getHeight((FlatBlock) block);
                } else {
                    int blockDataOffset = data.position();
                    blockDataOffsets[blockIndex] = blockDataOffset;
                    ByteUtil.appendBytes(block, data);
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

        byte blockType = blockTypes[blockIndex];
        int blockDataOffset = blockDataOffsets[blockIndex];
        switch (blockType) {
            case FLAT_BLOCK -> {
                return true;
            }
            case COMPLEX_BLOCK -> {
                return complex(geoX, geoY, nswe, blockDataOffset);
            }
            case MULTILAYER_BLOCK -> {
                return multilayer(geoX, geoY, worldZ, nswe, blockDataOffset);
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
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

    private boolean complex(int geoX, int geoY, byte nswe, int blockDataOffset) {
        int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return (data.getShort(blockDataOffset + 2 * cellOffset) & nswe) == nswe;
    }

    private boolean multilayer(int geoX, int geoY, int worldZ, byte nswe, int blockDataOffset) {
        int cellLocalOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int cellDataOffset = 0;
        for (int i = 0; i < cellLocalOffset; i++) {
            cellDataOffset += 1 + (data.get(blockDataOffset + cellDataOffset) * 2);
        }
        int startOffset = cellDataOffset;
        int nLayers = data.get(blockDataOffset + startOffset);

        int worldZSh = worldZ << 1;
        int nearestDZ = 0;
        int nearestData = 0;
        int tempStartOffset = blockDataOffset + startOffset + 1;
        int endOffset = tempStartOffset + (nLayers * 2);
        for (int offset = tempStartOffset; offset < endOffset; offset += 2) {
            int layerData = (data.get(offset) | (data.get(offset + 1) << 8));
            int layerZSh = layerData & 0x0fff0;
            if (layerZSh == worldZSh) {
                nearestData = layerData;
                break;// exact z
            }

            int layerDZ = Math.abs(layerZSh - worldZSh);
            if ((offset == tempStartOffset) || (layerDZ < nearestDZ)) {
                nearestDZ = layerDZ;
                nearestData = layerData;
            }
        }
        return (nearestData & nswe) == nswe;
    }


    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.getNearestZ(geoX, geoY, worldZ);
        }

        int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);
        int blockIndex = regionFirstBlockIndex + blockIndexInRegion;

        byte blockType = blockTypes[blockIndex];
        int blockDataOffset = blockDataOffsets[blockIndex];
        switch (blockType) {
            case FLAT_BLOCK -> {
                return FlatBlockFromOffsetBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
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
        int blockIndex = regionFirstBlockIndex + blockIndexInRegion;

        byte blockType = blockTypes[blockIndex];
        int blockDataOffset = blockDataOffsets[blockIndex];
        switch (blockType) {
            case FLAT_BLOCK -> {
                return FlatBlockFromOffsetBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
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
        int blockIndex = regionFirstBlockIndex + blockIndexInRegion;

        byte blockType = blockTypes[blockIndex];
        int blockDataOffset = blockDataOffsets[blockIndex];
        switch (blockType) {
            case FLAT_BLOCK -> {
                return FlatBlockFromOffsetBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
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
