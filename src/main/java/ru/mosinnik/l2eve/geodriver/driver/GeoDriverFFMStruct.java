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
import ru.mosinnik.l2eve.geodriver.ffm.FlatBlockFromOffsetFFM;
import ru.mosinnik.l2eve.geodriver.ffm.Indexed32MultilayerBlockFFM;
import ru.mosinnik.l2eve.geodriver.ffm.MultilayerBlockFFM;
import ru.mosinnik.l2eve.geodriver.regions.Region;
import ru.mosinnik.l2eve.geodriver.util.RegionCoords;

import java.io.RandomAccessFile;
import java.lang.foreign.*;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.*;
import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes.*;
import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants.*;
import static ru.mosinnik.l2eve.geodriver.ffm.MultilayerBlockFFM.INT_HANDLE;

/**
 * FFM. С использованием структуры для доступа.
 * Показал себя хуже FFMLong, видимо из-за доп логики внутри get по структуре, хотя инструкций по перфнорму меньше.
 *
 */
@Slf4j
public final class GeoDriverFFMStruct {

    public static final int REGIONS_INDEXES_SIZE = GeoConstants.GEO_REGIONS_X * GeoConstants.GEO_REGIONS_Y;
    private final GeoConfig config;

    //    // гео данные
//    private ByteBuffer data;
//
    // по индексу содержится оффсет первого блока региона в blockTypes и blockDataOffsets
    // offset at `blockDataOffsets` array of first region block
//    private final int[] regionFirstBlockIndexes = new int[GEO_REGIONS_X * GEO_REGIONS_Y]; //1024

//    private byte[] blockTypes;

    // оффсет начала блока в data
//    private int[] blockDataOffsets;

    MemorySegment regionFirstBlockIndexes;
    //    MemorySegment blockTypes;
    private MemorySegment blockDataOffsets;
    private MemorySegment data;
    private MemorySegment dataShorts;


    private final MemorySegment regionFirstBlockIndexesRO;
    //    private final MemorySegment blockTypesRO;
    private final MemorySegment blockDataOffsetsRO;
    private final MemorySegment dataRO;
    private final MemorySegment dataShortsRO;


    private static final StructLayout typedOffsetStruct = MemoryLayout.structLayout(
            ValueLayout.JAVA_BYTE.withName("type"),
            MemoryLayout.paddingLayout(3),
            JAVA_INT.withName("offset")
    );

    private static final VarHandle typeHandle = typedOffsetStruct.varHandle(MemoryLayout.PathElement.groupElement("type"));
    private static final VarHandle offsetHandle = typedOffsetStruct.varHandle(MemoryLayout.PathElement.groupElement("offset"));

    private static final long typedOffsetStructSize = typedOffsetStruct.byteSize();

    public GeoDriverFFMStruct(GeoConfig config, Path geoDataDir) {
        this.config = config;

        loadL2J(geoDataDir);

        regionFirstBlockIndexesRO = regionFirstBlockIndexes.asReadOnly();
//        blockTypesRO = blockTypes.asReadOnly();
        blockDataOffsetsRO = blockDataOffsets.asReadOnly();
        dataRO = data.asReadOnly();
        dataShortsRO = dataShorts.asReadOnly();

    }

    public GeoDriverFFMStruct(GeoConfig config, List<Path> paths) {
        this.config = config;

        loadFromL2J(paths);

        regionFirstBlockIndexesRO = regionFirstBlockIndexes.asReadOnly();
//        blockTypesRO = blockTypes.asReadOnly();
        blockDataOffsetsRO = blockDataOffsets.asReadOnly();
        dataRO = data.asReadOnly();
        dataShortsRO = dataShorts.asReadOnly();

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
            RegionCoords result = RegionCoords.extract(path);

            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
                Region region = new Region(
                        raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length()).order(ByteOrder.LITTLE_ENDIAN),
                        config
                );
                regions.add(new RegionCoordinated(region, result.regionX(), result.regionY()));
            }
        }

        int dataSize = 0;
        int dataShortSize = 0;
        int totalBlockCount = 0;
        int shortBlockCount = 0;
        for (RegionCoordinated regionCoordinated : regions) {
            Region region = regionCoordinated.region();
            for (int i = 0; i < IRegion.REGION_BLOCKS; i++) {
                IBlock block = region.getBlock(i);
                int bytesCount = getBytesCount(block);
                byte type = getType(block);
                switch (type) {
                    case COMPLEX_BLOCK -> {
                        shortBlockCount++;
                        dataShortSize += bytesCount;
                    }
                    default -> {
                        dataSize += bytesCount;
                    }
                }
                totalBlockCount++;
            }
        }
        assert totalBlockCount == regions.size() * IRegion.REGION_BLOCKS;

        data = global.allocate(JAVA_BYTE, dataSize);
        dataShorts = global.allocate(ComplexBlockFFM.SHORT_LAYOUT, dataShortSize / 2);

        blockDataOffsets = global.allocate(typedOffsetStruct, totalBlockCount);

        regionFirstBlockIndexes = global.allocate(JAVA_INT, REGIONS_INDEXES_SIZE);
        for (int i = 0; i < REGIONS_INDEXES_SIZE; i++) {
            regionFirstBlockIndexes.setAtIndex(JAVA_INT, i, NO_INDEX);
        }
//        Arrays.fill(regionFirstBlockIndexes, NO_INDEX);

        int blockIndex = 0;
        int blockDataOffset = 0;
        int blockDataShortOffset = 0;
        for (RegionCoordinated regionCoordinated : regions) {
            Region region = regionCoordinated.region();
            int regionFirstBlockIndex = blockIndex;

            final int regionIndex = (regionCoordinated.regionX() * GEO_REGIONS_Y) + regionCoordinated.regionY();
            regionFirstBlockIndexes.setAtIndex(JAVA_INT, regionIndex, regionFirstBlockIndex);
//            regionFirstBlockIndexes[regionIndex] = regionFirstBlockIndex;

            for (int i = 0; i < IRegion.REGION_BLOCKS; i++) {
                IBlock block = region.getBlock(i);

                byte blockType = getType(block);

                int blockOffset;

                switch (blockType) {
                    case FLAT_BLOCK -> {
                        blockOffset = FlatBlockFromOffsetBytes.getHeight((FlatBlock) block);
                    }
                    case COMPLEX_BLOCK -> {
                        blockOffset = blockDataShortOffset;

                        short[] shorts = ((ComplexBlock) block).getData();
                        MemorySegment.copy(shorts, 0, dataShorts, ComplexBlockFFM.SHORT_LAYOUT, blockDataShortOffset, shorts.length);

                        // blockDataShortOffset - in bytes
                        blockDataShortOffset += 2 * shorts.length;
                    }
                    default -> {
                        blockOffset = blockDataOffset;

                        byte[] bytes = toBytes(block);
                        MemorySegment.copy(bytes, 0, data, JAVA_BYTE, blockDataOffset, bytes.length);
                        blockDataOffset += bytes.length;
                    }
                }

                typeHandle.set(blockDataOffsets, blockIndex * typedOffsetStructSize, (byte) blockType);
                offsetHandle.set(blockDataOffsets, blockIndex * typedOffsetStructSize, (int) blockOffset);

                blockIndex++;
            }
        }
        assert totalBlockCount == blockIndex;

        log.info("data size: {}", data.byteSize());
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

        int regionFirstBlockIndex = (int) INT_HANDLE.get(regionFirstBlockIndexesRO, 4L * regionIndex);
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe);
        }

        int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);

        long index = regionFirstBlockIndex + blockIndexInRegion;
        long offset = 8L * index;
        byte blockType = (byte) typeHandle.get(blockDataOffsetsRO, offset);
        int blockDataOffset = (int) offsetHandle.get(blockDataOffsetsRO, offset);
        switch (blockType) {
            case FLAT_BLOCK -> {
                return FlatBlockFromOffsetFFM.checkNearestNSWE(geoX, geoY, worldZ, nswe);
            }
            case COMPLEX_BLOCK -> {
                return ComplexBlockFFM.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, dataShortsRO);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockFFM.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, dataRO);
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
                return Indexed32MultilayerBlockFFM.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, dataRO);
//                throw new UnsupportedOperationException("Not supported yet: Indexed32MultilayerBlockBytes");
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }


    //    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));

        int regionFirstBlockIndex = (int) INT_HANDLE.get(regionFirstBlockIndexesRO, 4L * regionIndex);
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.getNearestZ(geoX, geoY, worldZ);
        }

        int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);

//        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
//        int blockDataOffset = blockDataOffsets[regionFirstBlockIndex + blockIndexInRegion];

//        byte blockType = blockTypes.getAtIndex(JAVA_BYTE, regionFirstBlockIndex + blockIndexInRegion);
//        int blockDataOffset = blockDataOffsets.getAtIndex(JAVA_INT, regionFirstBlockIndex + blockIndexInRegion);

        long offset = typedOffsetStructSize * (regionFirstBlockIndex + blockIndexInRegion);
        byte blockType = (byte) typeHandle.get(blockDataOffsetsRO, offset);
        int blockDataOffset = (int) offsetHandle.get(blockDataOffsetsRO, offset);

        switch (blockType) {
            case FLAT_BLOCK -> {
                return FlatBlockFromOffsetFFM.getNearestZ(geoX, geoY, worldZ, blockDataOffset, dataRO);
            }
            case COMPLEX_BLOCK -> {
                return ComplexBlockFFM.getNearestZ(geoX, geoY, worldZ, blockDataOffset, dataShortsRO);
//                return ComplexBlockFFM.getNearestZ(geoX, geoY, worldZ, blockDataOffset, dataRO);
            }
            case MULTILAYER_BLOCK -> {
                return MultilayerBlockFFM.getNearestZ(geoX, geoY, worldZ, blockDataOffset, dataRO);
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
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockFFM.getNearestZ(geoX, geoY, worldZ, blockDataOffset, dataRO);
//                throw new UnsupportedOperationException("Not supported yet: Indexed32MultilayerBlockBytes");
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }
}
