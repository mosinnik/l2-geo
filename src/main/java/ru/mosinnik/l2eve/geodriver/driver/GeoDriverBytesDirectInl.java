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
import ru.mosinnik.l2eve.geodriver.blocks.*;
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
 * С инлайнингом вместо вызова. В целом заметно хуже чем через вызов. Даже с С1 опциями.
 * <p>
 * Benchmark                                                            (blockTypeStr)   Mode  Cnt        Score     Error      Units
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect                             RANDOM  thrpt    5     3338.928 ±   9.741      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:CPI                         RANDOM  thrpt             1.145            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:IPC                         RANDOM  thrpt             0.873            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branch-misses               RANDOM  thrpt           871.399                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches                    RANDOM  thrpt        100020.248                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:cycles                      RANDOM  thrpt        938269.019                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions                RANDOM  thrpt        819182.173                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl                          RANDOM  thrpt    5     2808.003 ±  16.307      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:CPI                      RANDOM  thrpt             0.895            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:IPC                      RANDOM  thrpt             1.118            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:branch-misses            RANDOM  thrpt          1867.579                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:branches                 RANDOM  thrpt        164451.749                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:cycles                   RANDOM  thrpt       1118156.555                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:instructions             RANDOM  thrpt       1249884.922                 #/op
 * <p>
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect                         FLAT_BLOCK  thrpt    5    22278.767 ± 126.177      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:CPI                     FLAT_BLOCK  thrpt             0.413            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:IPC                     FLAT_BLOCK  thrpt             2.422            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branch-misses           FLAT_BLOCK  thrpt            13.366                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches                FLAT_BLOCK  thrpt         60245.396                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:cycles                  FLAT_BLOCK  thrpt        141001.034                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions            FLAT_BLOCK  thrpt        341573.494                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl                      FLAT_BLOCK  thrpt    5    15606.203 ± 236.942      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:CPI                  FLAT_BLOCK  thrpt             0.296            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:IPC                  FLAT_BLOCK  thrpt             3.381            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:branch-misses        FLAT_BLOCK  thrpt             4.307                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:branches             FLAT_BLOCK  thrpt        100291.395                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:cycles               FLAT_BLOCK  thrpt        201657.620                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:instructions         FLAT_BLOCK  thrpt        681748.322                 #/op
 * <p>
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect                      COMPLEX_BLOCK  thrpt    5     4323.578 ± 305.904      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:CPI                  COMPLEX_BLOCK  thrpt             1.191            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:IPC                  COMPLEX_BLOCK  thrpt             0.840            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branch-misses        COMPLEX_BLOCK  thrpt            24.501                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches             COMPLEX_BLOCK  thrpt         71090.733                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:cycles               COMPLEX_BLOCK  thrpt        722519.306                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions         COMPLEX_BLOCK  thrpt        606731.998                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl                   COMPLEX_BLOCK  thrpt    5     3406.471 ±   8.795      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:CPI               COMPLEX_BLOCK  thrpt             0.879            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:IPC               COMPLEX_BLOCK  thrpt             1.138            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:branch-misses     COMPLEX_BLOCK  thrpt            16.438                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:branches          COMPLEX_BLOCK  thrpt        141364.159                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:cycles            COMPLEX_BLOCK  thrpt        921246.740                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:instructions      COMPLEX_BLOCK  thrpt       1048391.122                 #/op
 * <p>
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect                   MULTILAYER_BLOCK  thrpt    5      715.282 ±   2.878      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:CPI               MULTILAYER_BLOCK  thrpt             0.976            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:IPC               MULTILAYER_BLOCK  thrpt             1.025            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branch-misses     MULTILAYER_BLOCK  thrpt         24035.739                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches          MULTILAYER_BLOCK  thrpt        666115.348                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:cycles            MULTILAYER_BLOCK  thrpt       4399736.824                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions      MULTILAYER_BLOCK  thrpt       4510044.344                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl                MULTILAYER_BLOCK  thrpt    5      704.896 ±   4.471      ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:CPI            MULTILAYER_BLOCK  thrpt             0.899            clks/insn
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:IPC            MULTILAYER_BLOCK  thrpt             1.112            insns/clk
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:branch-misses  MULTILAYER_BLOCK  thrpt         23757.956                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:branches       MULTILAYER_BLOCK  thrpt        775223.334                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:cycles         MULTILAYER_BLOCK  thrpt       4469092.530                 #/op
 * GeoDriverBenchParams.checkNearestNSWEBytesDirectInl:instructions   MULTILAYER_BLOCK  thrpt       4970013.715                 #/op
 */
@Slf4j
public final class GeoDriverBytesDirectInl implements IGeoDriver {

    private final GeoConfig config;

    // гео данные
    private ByteBuffer data;

    // по индексу содержится оффсет первого блока региона в blockTypes и blockDataOffsets
    // offset at `blockDataOffsets` array of first region block
    private final int[] regionFirstBlockIndexes = new int[GEO_REGIONS_X * GEO_REGIONS_Y]; //1024

    private byte[] blockTypes;

    // оффсет начала блока в data
    private int[] blockDataOffsets;

    public GeoDriverBytesDirectInl() {
        config = new GeoConfig();
    }

    public GeoDriverBytesDirectInl(GeoConfig config) {
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
                dataSize += getBytesCount(region.getBlock(i));
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

                byte blockType = getType(block);
                blockTypes[blockIndex] = blockType;

                if (blockType == FLAT_BLOCK) {
                    blockDataOffsets[blockIndex] = FlatBlockFromOffsetBytes.getHeight((FlatBlock) block);
                } else {
                    int blockDataOffset = data.position();
                    blockDataOffsets[blockIndex] = blockDataOffset;
                    appendBytes(block, data);
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

    public static byte getType(IBlock block) {
        Class<? extends IBlock> blockClass = block.getClass();
        if (blockClass.equals(FlatBlock.class)) {
            return FLAT_BLOCK;
        } else if (blockClass.equals(ComplexBlock.class)) {
            return COMPLEX_BLOCK;
        } else if (blockClass.equals(MultilayerBlock.class)) {
            return MULTILAYER_BLOCK;
        } else if (blockClass.equals(OneHeightComplexBlock.class)) {
            return ONE_HEIGHT_COMPLEX_BLOCK;
        } else if (blockClass.equals(BaseHeightComplexBlock.class)) {
            return BASE_HEIGHT_COMPLEX_BLOCK;
        } else if (blockClass.equals(BaseHeightOneNsweComplexBlock.class)) {
            return BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK;
        } else if (blockClass.equals(FewHeightsComplexBlock.class)) {
            return FEW_HEIGHTS_COMPLEX_BLOCK;
        } else if (blockClass.equals(FewHeightsOneNsweComplexBlock.class)) {
            return FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK;
        } else if (blockClass.equals(NoHolesMultilayerBlock.class)) {
            return NO_HOLES_MULTILAYER_BLOCK;
        } else if (blockClass.equals(IndexedMultilayerBlock.class)) {
            return INDEXED_MULTILAYER_BLOCK;
        } else if (blockClass.equals(Indexed32MultilayerBlock.class)) {
            return INDEXED_32_MULTILAYER_BLOCK;
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
    }

    public static byte[] toBytes(IBlock block) {
        Class<? extends IBlock> blockClass = block.getClass();
        if (blockClass.equals(FlatBlock.class)) {
            return FlatBlockFromOffsetBytes.toBytes((FlatBlock) block);
        } else if (blockClass.equals(ComplexBlock.class)) {
            return ComplexBlockBytes.toBytes((ComplexBlock) block);
        } else if (blockClass.equals(MultilayerBlock.class)) {
            return MultilayerBlockBytes.toBytes((MultilayerBlock) block);
        } else if (blockClass.equals(OneHeightComplexBlock.class)) {
            return OneHeightComplexBlockBytes.toBytes((OneHeightComplexBlock) block);
        } else if (blockClass.equals(BaseHeightComplexBlock.class)) {
            return BaseHeightComplexBlockBytes.toBytes((BaseHeightComplexBlock) block);
        } else if (blockClass.equals(BaseHeightOneNsweComplexBlock.class)) {
            return BaseHeightOneNsweComplexBlockBytes.toBytes((BaseHeightOneNsweComplexBlock) block);
        } else if (blockClass.equals(FewHeightsComplexBlock.class)) {
            return FewHeightsComplexBlockBytes.toBytes((FewHeightsComplexBlock) block);
        } else if (blockClass.equals(FewHeightsOneNsweComplexBlock.class)) {
            return FewHeightsOneNsweComplexBlockBytes.toBytes((FewHeightsOneNsweComplexBlock) block);
        } else if (blockClass.equals(NoHolesMultilayerBlock.class)) {
            return NoHolesMultilayerBlockBytes.toBytes((NoHolesMultilayerBlock) block);
        } else if (blockClass.equals(IndexedMultilayerBlock.class)) {
            return IndexedMultilayerBlockBytes.toBytes((IndexedMultilayerBlock) block);
        } else if (blockClass.equals(Indexed32MultilayerBlock.class)) {
            return Indexed32MultilayerBlockBytes.toBytes((Indexed32MultilayerBlock) block);
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
    }

    public static void appendBytes(IBlock block, ByteBuffer data) {
        Class<? extends IBlock> blockClass = block.getClass();
        if (blockClass.equals(FlatBlock.class)) {
            FlatBlockFromOffsetBytes.appendBytes((FlatBlock) block, data);
        } else if (blockClass.equals(ComplexBlock.class)) {
            ComplexBlockBytes.appendBytes((ComplexBlock) block, data);
        } else if (blockClass.equals(MultilayerBlock.class)) {
            MultilayerBlockBytes.appendBytes((MultilayerBlock) block, data);
        } else if (blockClass.equals(OneHeightComplexBlock.class)) {
            OneHeightComplexBlockBytes.appendBytes((OneHeightComplexBlock) block, data);
        } else if (blockClass.equals(BaseHeightComplexBlock.class)) {
            BaseHeightComplexBlockBytes.appendBytes((BaseHeightComplexBlock) block, data);
        } else if (blockClass.equals(BaseHeightOneNsweComplexBlock.class)) {
            BaseHeightOneNsweComplexBlockBytes.appendBytes((BaseHeightOneNsweComplexBlock) block, data);
        } else if (blockClass.equals(FewHeightsComplexBlock.class)) {
            FewHeightsComplexBlockBytes.appendBytes((FewHeightsComplexBlock) block, data);
        } else if (blockClass.equals(FewHeightsOneNsweComplexBlock.class)) {
            FewHeightsOneNsweComplexBlockBytes.appendBytes((FewHeightsOneNsweComplexBlock) block, data);
        } else if (blockClass.equals(NoHolesMultilayerBlock.class)) {
            NoHolesMultilayerBlockBytes.appendBytes((NoHolesMultilayerBlock) block, data);
        } else if (blockClass.equals(IndexedMultilayerBlock.class)) {
            IndexedMultilayerBlockBytes.appendBytes((IndexedMultilayerBlock) block, data);
        } else if (blockClass.equals(Indexed32MultilayerBlock.class)) {
            Indexed32MultilayerBlockBytes.appendBytes((Indexed32MultilayerBlock) block, data);
        } else {
            throw new RuntimeException("Unknown block class: " + blockClass.getName());
        }
    }

    public static int getBytesCount(IBlock block) {
        Class<? extends IBlock> blockClass = block.getClass();
        if (blockClass.equals(FlatBlock.class)) {
            return FlatBlockFromOffsetBytes.calcBytesCount((FlatBlock) block);
        } else if (blockClass.equals(ComplexBlock.class)) {
            return ComplexBlockBytes.calcBytesCount((ComplexBlock) block);
        } else if (blockClass.equals(MultilayerBlock.class)) {
            return MultilayerBlockBytes.calcBytesCount((MultilayerBlock) block);
        } else if (blockClass.equals(OneHeightComplexBlock.class)) {
            return OneHeightComplexBlockBytes.calcBytesCount((OneHeightComplexBlock) block);
        } else if (blockClass.equals(BaseHeightComplexBlock.class)) {
            return BaseHeightComplexBlockBytes.calcBytesCount((BaseHeightComplexBlock) block);
        } else if (blockClass.equals(BaseHeightOneNsweComplexBlock.class)) {
            return BaseHeightOneNsweComplexBlockBytes.calcBytesCount((BaseHeightOneNsweComplexBlock) block);
        } else if (blockClass.equals(FewHeightsComplexBlock.class)) {
            return FewHeightsComplexBlockBytes.calcBytesCount((FewHeightsComplexBlock) block);
        } else if (blockClass.equals(FewHeightsOneNsweComplexBlock.class)) {
            return FewHeightsOneNsweComplexBlockBytes.calcBytesCount((FewHeightsOneNsweComplexBlock) block);
        } else if (blockClass.equals(NoHolesMultilayerBlock.class)) {
            return NoHolesMultilayerBlockBytes.calcBytesCount((NoHolesMultilayerBlock) block);
        } else if (blockClass.equals(IndexedMultilayerBlock.class)) {
            return IndexedMultilayerBlockBytes.calcBytesCount((IndexedMultilayerBlock) block);
        } else if (blockClass.equals(Indexed32MultilayerBlock.class)) {
            return Indexed32MultilayerBlockBytes.calcBytesCount((Indexed32MultilayerBlock) block);
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
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
                int cellOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
                return (data.getShort(blockDataOffset + 2 * cellOffset) & 0x0F & nswe) == nswe;
            }
            case MULTILAYER_BLOCK -> {
                short result = 0;
                boolean finished = false;
                // локальный оффсет
                int cellLocalOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
                int cellDataOffset = 0;

                // move index to cell, we need to parse on each request, OR we parse on creation and save indexes
                for (int i = 0; i < cellLocalOffset; i++) {
                    cellDataOffset += 1 + (data.get(blockDataOffset + cellDataOffset) * 2);
                }
                // now the index points to the cell we need
                int startOffset = cellDataOffset;
                byte nLayers = data.get(blockDataOffset + startOffset);
                int endOffset = startOffset + 1 + (nLayers * 2);

                // 1 layer at least was required on loading so this is set at least once on the loop below
                int nearestDZ = 0;
                short nearestData = 0;
                for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
                    short layerData = (short) ((data.get(blockDataOffset + offset) & 0xFF) |
                            (data.get(blockDataOffset + offset + 1) << 8));
                    short layer = layerData;
                    layer = (short) (layer & 0x0fff0);
                    int layerZ = layer >> 1;
                    if (layerZ == worldZ) {
                        result = layerData;
                        finished = true;
                        break;// exact z
                    }

                    int layerDZ = Math.abs(layerZ - worldZ);
                    if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ)) {
                        nearestDZ = layerDZ;
                        nearestData = layerData;
                    }
                }
                if (!finished) {
                    result = nearestData;
                }
                return ((int) (byte) (result & 0x000F) & nswe) == nswe;
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
