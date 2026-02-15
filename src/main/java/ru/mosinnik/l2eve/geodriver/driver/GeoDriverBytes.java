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
 * Идея заключается в том, чтобы убрать расходы памяти на ссылки объектов регионов и блоков,
 * упаковав их в общий массив байт. Работа над массивом байт осуществляется в зависимости от типа,
 * хранимого в индексе регионов.
 */
@Slf4j
public final class GeoDriverBytes implements IGeoDriver {

    private final GeoConfig config;

    // гео данные
    private ByteBuffer data;

    // по индексу содержится оффсет первого блока региона в blockTypes и blockDataOffsets
    // offset at `blockDataOffsets` array of first region block
    private final int[] regionFirstBlockIndexes = new int[GEO_REGIONS_X * GEO_REGIONS_Y]; //1024

    private byte[] blockTypes;

    // оффсет начала блока в data
    private int[] blockDataOffsets;

    public GeoDriverBytes() {
        config = new GeoConfig();
    }

    public GeoDriverBytes(GeoConfig config) {
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
                dataSize += getBytesCount(region.getBlock(i));
                totalBlockCount++;
            }
        }
        assert totalBlockCount == regions.size() * IRegion.REGION_BLOCKS;

        data = ByteBuffer.allocate(dataSize);

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
                    data.put(toBytes(block));
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
                return MultilayerIndexedBlockBytes.getSize(blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_32_BLOCK -> {
                return MultilayerIndexed32BlockBytes.getSize(blockDataOffset, data);
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
        } else if (blockClass.equals(MultilayerIndexedBlock.class)) {
            return INDEXED_MULTILAYER_BLOCK;
        } else if (blockClass.equals(MultilayerIndexed32Block.class)) {
            return INDEXED_MULTILAYER_32_BLOCK;
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
    }

    private static byte[] toBytes(IBlock block) {
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
        } else if (blockClass.equals(MultilayerIndexedBlock.class)) {
            return MultilayerIndexedBlockBytes.toBytes((MultilayerIndexedBlock) block);
        } else if (blockClass.equals(MultilayerIndexed32Block.class)) {
            return MultilayerIndexed32BlockBytes.toBytes((MultilayerIndexed32Block) block);
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
    }

    private static int getBytesCount(IBlock block) {
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
        } else if (blockClass.equals(MultilayerIndexedBlock.class)) {
            return MultilayerIndexedBlockBytes.calcBytesCount((MultilayerIndexedBlock) block);
        } else if (blockClass.equals(MultilayerIndexed32Block.class)) {
            return MultilayerIndexed32BlockBytes.calcBytesCount((MultilayerIndexed32Block) block);
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
    }

    record RegionCoordinated(Region region, int regionX, int regionY) {
    }

    @Override
    public void loadRegion(Path filePath, int regionX, int regionY) {
        throw new RuntimeException("Not implemented");
    }


    @Override
    public boolean hasGeoPos(int geoX, int geoY) {
        int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return false;
        }
        return true;
    }

    public static Map<Integer, AtomicInteger> blockTypesCount = new HashMap<>();

    public int getBlockType(int geoX, int geoY) {
        int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return -1;
        }

        int blockIndexInRegion = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);

        return blockTypes[regionFirstBlockIndex + blockIndexInRegion];
    }

    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        // 1. get block type by geo x/y
        // 2. get block offset by geo x/y
        // 2.1 get region offset of first region block
        // 2.2 calc
        // 3. call block logic with offset

        int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe);
        }

        int blockIndexInRegion = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);

        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
        blockTypesCount.computeIfAbsent((int) blockType, k -> new AtomicInteger()).incrementAndGet();

        int blockDataOffset = blockDataOffsets[regionFirstBlockIndex + blockIndexInRegion];
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
                return MultilayerIndexedBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_32_BLOCK -> {
                return MultilayerIndexed32BlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }


    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.getNearestZ(geoX, geoY, worldZ);
        }

        int blockIndexInRegion = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);

        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
        int blockDataOffset = blockDataOffsets[regionFirstBlockIndex + blockIndexInRegion];
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
                return MultilayerIndexedBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_32_BLOCK -> {
                return MultilayerIndexed32BlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.getNextLowerZ(geoX, geoY, worldZ);
        }

        int blockIndexInRegion = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);

        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
        int blockDataOffset = blockDataOffsets[regionFirstBlockIndex + blockIndexInRegion];
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
                return MultilayerIndexedBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_32_BLOCK -> {
                return MultilayerIndexed32BlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.getNextHigherZ(geoX, geoY, worldZ);
        }

        int blockIndexInRegion = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);

        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
        int blockDataOffset = blockDataOffsets[regionFirstBlockIndex + blockIndexInRegion];
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
                return MultilayerIndexedBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_MULTILAYER_32_BLOCK -> {
                return MultilayerIndexed32BlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

}
