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
import ru.mosinnik.l2eve.geodriver.abstraction.IRegion;
import ru.mosinnik.l2eve.geodriver.blocks.ComplexBlock;
import ru.mosinnik.l2eve.geodriver.blocks.FlatBlock;
import ru.mosinnik.l2eve.geodriver.bytes.FlatBlockFromOffsetBytes;
import ru.mosinnik.l2eve.geodriver.bytes.NullRegionBytes;
import ru.mosinnik.l2eve.geodriver.ffm.ComplexBlockFFM;
import ru.mosinnik.l2eve.geodriver.ffm.ComplexBlockFFMSlice;
import ru.mosinnik.l2eve.geodriver.ffm.FlatBlockFromOffsetFFM;
import ru.mosinnik.l2eve.geodriver.ffm.MultilayerBlockFFMSlice;
import ru.mosinnik.l2eve.geodriver.regions.Region;

import java.io.RandomAccessFile;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.*;
import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes.RegionCoordinated;
import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants.*;

/**
 * FFM для хранения данных самих блоков с использованием слайсов при вызове доп логики.
 * Буст хороший, но на рендоме не стабильно, и бывают просадки до 3к, что меньше bytes реализации.
 * <p>
 * Benchmark                                           (blockTypeStr)   Mode  Cnt      Score      Error  Units
 * GeoDriverBenchParams.checkNearestNSWE                       RANDOM  thrpt    5   2289.858 ±   10.013  ops/s
 * GeoDriverBenchParams.checkNearestNSWE                   FLAT_BLOCK  thrpt    5  15706.646 ± 1012.001  ops/s
 * GeoDriverBenchParams.checkNearestNSWE                COMPLEX_BLOCK  thrpt    5   3033.744 ±   11.591  ops/s
 * GeoDriverBenchParams.checkNearestNSWE             MULTILAYER_BLOCK  thrpt    5    727.374 ±   10.290  ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes                  RANDOM  thrpt    5   3347.495 ±    8.670  ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes              FLAT_BLOCK  thrpt    5  22334.694 ±  696.887  ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes           COMPLEX_BLOCK  thrpt    5   4355.901 ±   25.085  ops/s
 * GeoDriverBenchParams.checkNearestNSWEBytes        MULTILAYER_BLOCK  thrpt    5    712.993 ±   13.492  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFMLong                RANDOM  thrpt    5   3276.407 ±    6.122  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFMLong            FLAT_BLOCK  thrpt    5  20724.476 ±  343.211  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFMLong         COMPLEX_BLOCK  thrpt    5   4077.222 ±    3.110  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFMLong      MULTILAYER_BLOCK  thrpt    5    714.512 ±    2.190  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFMStruct              RANDOM  thrpt    5   2347.848 ±   14.640  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFMStruct          FLAT_BLOCK  thrpt    5  18903.255 ±  471.758  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFMStruct       COMPLEX_BLOCK  thrpt    5   4128.209 ±   27.536  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFMStruct    MULTILAYER_BLOCK  thrpt    5    730.066 ±    2.769  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFM_T                  RANDOM  thrpt    5   3183.753 ±   33.786  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFM_T              FLAT_BLOCK  thrpt    5  22234.669 ±  547.582  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFM_T           COMPLEX_BLOCK  thrpt    5   4466.260 ±  147.467  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFM_T        MULTILAYER_BLOCK  thrpt    5    740.382 ±    5.189  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFM_T_Slice            RANDOM  thrpt    5   4617.576 ±  420.232  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFM_T_Slice        FLAT_BLOCK  thrpt    5  22323.436 ±  664.688  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFM_T_Slice     COMPLEX_BLOCK  thrpt    5   4698.361 ±   73.367  ops/s
 * GeoDriverBenchParams.checkNearestNSWEFFM_T_Slice  MULTILAYER_BLOCK  thrpt    5    759.708 ±    0.229  ops/s
 */
@Slf4j
public final class GeoDriverFFMT_Slice {

    public static final int REGIONS_INDEXES_SIZE = GeoConstants.GEO_REGIONS_X * GeoConstants.GEO_REGIONS_Y;
    private final GeoConfig config;

    //    // гео данные
//    private ByteBuffer data;
//
    // по индексу содержится оффсет первого блока региона в blockTypes и blockDataOffsets
    // offset at `blockDataOffsets` array of first region block
    private final int[] regionFirstBlockIndexes = new int[GEO_REGIONS_X * GEO_REGIONS_Y]; //1024

    private byte[] blockTypes;

    // оффсет начала блока в data
    private int[] blockDataOffsets;

    //    MemorySegment regionFirstBlockIndexes;
//    MemorySegment blockTypes;
//    MemorySegment blockDataOffsets;
    MemorySegment dataComplex;
    MemorySegment dataMulti;


    //    private final MemorySegment regionFirstBlockIndexesRO;
//    private final MemorySegment blockTypesRO;
//    private final MemorySegment blockDataOffsetsRO;
    private final MemorySegment dataComplexRO;
    private final MemorySegment dataMultiRO;

//    public GeoDriverFFM() {
//        config = new GeoConfig();
//    }
//
//    public GeoDriverFFM(GeoConfig config) {
//        this.config = config;
//    }

    public GeoDriverFFMT_Slice(GeoConfig config, Path geoDataDir) {
        this.config = config;

        loadL2J(geoDataDir);

//        regionFirstBlockIndexesRO = regionFirstBlockIndexes.asReadOnly();
//        blockTypesRO = blockTypes.asReadOnly();
//        blockDataOffsetsRO = blockDataOffsets.asReadOnly();
        dataMultiRO = dataMulti.asReadOnly();
        dataComplexRO = dataComplex.asReadOnly();

    }

    public GeoDriverFFMT_Slice(GeoConfig config, List<Path> paths) {
        this.config = config;

        loadFromL2J(paths);

//        regionFirstBlockIndexesRO = regionFirstBlockIndexes.asReadOnly();
//        blockTypesRO = blockTypes.asReadOnly();
//        blockDataOffsetsRO = blockDataOffsets.asReadOnly();
        dataMultiRO = dataMulti.asReadOnly();
        dataComplexRO = dataComplex.asReadOnly();

    }

    @SneakyThrows
    public void loadL2J(Path geoDataDir) {
        loadFromL2JDir(geoDataDir);
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

        Arena global = Arena.global();
//        Arena global = Arena.ofShared();

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
        int dataComplexSize = 0;
        int dataMultiSize = 0;
        int totalBlockCount = 0;
        int shortBlockCount = 0;
        for (RegionCoordinated regionCoordinated : regions) {
            Region region = regionCoordinated.region();
            for (int i = 0; i < IRegion.REGION_BLOCKS; i++) {
                IBlock block = region.getBlock(i);
                int bytesCount = ByteUtil.getBytesCount(block);
                byte type = ByteUtil.getType(block);
                switch (type) {
                    case COMPLEX_BLOCK -> {
                        shortBlockCount++;
                        dataComplexSize += bytesCount;
                    }
                    case MULTILAYER_BLOCK -> {
                        shortBlockCount++;
//                        dataMultiSize += 4096;
                        dataMultiSize += bytesCount;
                    }
                    default -> {
                        dataSize += bytesCount;
                    }
                }
                totalBlockCount++;
            }
        }
        assert totalBlockCount == regions.size() * IRegion.REGION_BLOCKS;

//        regionFirstBlockIndexes = global.allocate(JAVA_INT, REGIONS_INDEXES_SIZE);

//        data = ByteBuffer.allocate(dataSize);
        blockTypes = new byte[totalBlockCount];
        blockDataOffsets = new int[totalBlockCount];

        dataMulti = global.allocate(JAVA_BYTE, dataMultiSize);
        dataComplex = global.allocate(ComplexBlockFFM.SHORT_LAYOUT, dataComplexSize / 2);

//        blockTypes = global.allocate(JAVA_BYTE, totalBlockCount);
//        blockDataOffsets = global.allocate(JAVA_INT, totalBlockCount);

        Arrays.fill(regionFirstBlockIndexes, NO_INDEX);
//        for (int i = 0; i < REGIONS_INDEXES_SIZE; i++) {
//            regionFirstBlockIndexes.(JAVA_INT, i, NO_INDEX);
//        }

        int blockIndex = 0;
        int blockDataOffset = 0;
        int blockDataShortOffset = 0;
        int blockDataMultiOffset = 0;
        for (RegionCoordinated regionCoordinated : regions) {
            Region region = regionCoordinated.region();
            int regionFirstBlockIndex = blockIndex;

            final int regionIndex = (regionCoordinated.regionX() * GEO_REGIONS_Y) + regionCoordinated.regionY();
            regionFirstBlockIndexes[regionIndex] = regionFirstBlockIndex;
//            regionFirstBlockIndexes.setAtIndex(JAVA_INT, regionIndex, regionFirstBlockIndex);

            for (int i = 0; i < IRegion.REGION_BLOCKS; i++) {
                IBlock block = region.getBlock(i);

                byte blockType = ByteUtil.getType(block);
                blockTypes[blockIndex] = blockType;
//                blockTypes.set(JAVA_BYTE, blockIndex, blockType);

                switch (blockType) {
                    case FLAT_BLOCK -> {
                        blockDataOffsets[blockIndex] = FlatBlockFromOffsetBytes.getHeight((FlatBlock) block);
//                        blockDataOffsets.setAtIndex(JAVA_INT, blockIndex, FlatBlockFromOffsetBytes.getHeight((FlatBlock) block));
                    }
                    case COMPLEX_BLOCK -> {
                        blockDataOffsets[blockIndex] = blockDataShortOffset;
//                        blockDataOffsets.setAtIndex(JAVA_INT, blockIndex, blockDataShortOffset);

                        short[] shorts = ((ComplexBlock) block).getData();
                        MemorySegment.copy(shorts, 0, dataComplex, ComplexBlockFFM.SHORT_LAYOUT, blockDataShortOffset, shorts.length);

                        // blockDataShortOffset - in bytes
                        blockDataShortOffset += 2 * shorts.length;
                    }
                    case MULTILAYER_BLOCK -> {
//                        blockDataOffsets[blockIndex] = blockDataMultiOffset;
//
//                        byte[] bytes = ByteUtil.toBytes(block);
//                        MemorySegment.copy(bytes, 0, dataMulti, JAVA_BYTE, blockDataMultiOffset, bytes.length);
//
//                        // blockDataMultiOffset - in bytes
//                        blockDataMultiOffset += 4096;
                        blockDataOffsets[blockIndex] = blockDataMultiOffset;

                        byte[] bytes = ByteUtil.toBytes(block);
                        MemorySegment.copy(bytes, 0, dataMulti, JAVA_BYTE, blockDataMultiOffset, bytes.length);

                        // blockDataMultiOffset - in bytes
                        blockDataMultiOffset += bytes.length;
                    }
                    default -> {
                        throw new RuntimeException("Unknown block type: " + blockType);
                    }
                }

                blockIndex++;
            }
        }
        assert totalBlockCount == blockIndex;

        log.info("data size: {}", dataMulti.byteSize());
    }


    /**
     * Translates world x into geo x.
     * readable:
     * (worldX - WORLD_MIN_X) / 16;
     *
     * @param worldX world x
     * @return geo x
     */
    public int getGeoX(int worldX) {
        if ((worldX < WORLD_MIN_X) || (worldX > WORLD_MAX_X)) {
            throw new IllegalArgumentException();
        }
        return (worldX - WORLD_MIN_X) >> 4;
    }

    /**
     * Translates world y into geo y.
     * readable:
     * (worldY - WORLD_MIN_Y) / 16;
     *
     * @param worldY world y
     * @return geo y
     */
    public int getGeoY(int worldY) {
        if ((worldY < WORLD_MIN_Y) || (worldY > WORLD_MAX_Y)) {
            throw new IllegalArgumentException();
        }
        return (worldY - WORLD_MIN_Y) >> 4;
    }


    //    @Override
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
                return FlatBlockFromOffsetFFM.checkNearestNSWE(geoX, geoY, worldZ, nswe);
            }
            case COMPLEX_BLOCK -> {
//                return ComplexBlockFFM.checkNearestNSWE_Slice(geoX, geoY, worldZ, nswe, dataShortsRO.asSlice(blockDataOffset));
                return ComplexBlockFFMSlice.checkNearestNSWE(geoX, geoY, worldZ, nswe, dataComplexRO.asSlice(blockDataOffset, 128L));
//                return ComplexBlockFFM.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, dataRO);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockFFMSlice.checkNearestNSWE(geoX, geoY, worldZ, nswe, dataMultiRO.asSlice(blockDataOffset));
//                return MultilayerBlockFFMSlice.checkNearestNSWE(geoX, geoY, worldZ, nswe, dataMultiRO.asSlice(blockDataOffset, 4096L));
            }
            case ONE_HEIGHT_COMPLEX_BLOCK -> {
                throw new UnsupportedOperationException("Not supported yet: ONE_HEIGHT_COMPLEX_BLOCK");
            }
            case BASE_HEIGHT_COMPLEX_BLOCK -> {
                throw new UnsupportedOperationException("Not supported yet: BaseHeightComplexBlockBytes");
            }
            case BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK -> {
                throw new UnsupportedOperationException("Not supported yet: BaseHeightOneNsweComplexBlockBytes");
            }
            case FEW_HEIGHTS_COMPLEX_BLOCK -> {
                throw new UnsupportedOperationException("Not supported yet: FewHeightsComplexBlockBytes");
            }
            case FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK -> {
                throw new UnsupportedOperationException("Not supported yet: FewHeightsOneNsweComplexBlockBytes");
            }
            case NO_HOLES_MULTILAYER_BLOCK -> {
                throw new UnsupportedOperationException("Not supported yet: NoHolesMultilayerBlockBytes");
            }
            case INDEXED_MULTILAYER_BLOCK -> {
                throw new UnsupportedOperationException("Not supported yet: IndexedMultilayerBlockBytes");
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                throw new UnsupportedOperationException("Not supported yet: Indexed32MultilayerBlockBytes");
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }


    //    @Override
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
                return FlatBlockFromOffsetFFM.getNearestZ(geoX, geoY, worldZ, blockDataOffset, dataMultiRO);
            }
            case COMPLEX_BLOCK -> {
                return ComplexBlockFFMSlice.getNearestZ(geoX, geoY, worldZ, dataComplexRO.asSlice(blockDataOffset, 128L));
//                return ComplexBlockFFM.getNearestZ(geoX, geoY, worldZ, blockDataOffset, dataRO);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockFFMSlice.getNearestZ(geoX, geoY, worldZ, dataMultiRO.asSlice(blockDataOffset));
            }
//            case ONE_HEIGHT_COMPLEX_BLOCK -> {
//                return OneHeightComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
//            }
//            case BASE_HEIGHT_COMPLEX_BLOCK -> {
//                return BaseHeightComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
//            }
//            case BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK -> {
//                return BaseHeightOneNsweComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
//            }
//            case FEW_HEIGHTS_COMPLEX_BLOCK -> {
//                return FewHeightsComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
//            }
//            case FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK -> {
//                return FewHeightsOneNsweComplexBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
//            }
//            case NO_HOLES_MULTILAYER_BLOCK -> {
//                return NoHolesMultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
//            }
//            case INDEXED_MULTILAYER_BLOCK -> {
//                return IndexedMultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
//            }
//            case INDEXED_32_MULTILAYER_BLOCK -> {
//                return Indexed32MultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
//            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }
}
