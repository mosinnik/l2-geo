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

package ru.mosinnik.l2eve.geodriver.gen;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.abstraction.IRegion;
import ru.mosinnik.l2eve.geodriver.blocks.*;
import ru.mosinnik.l2eve.geodriver.bytes.*;
import ru.mosinnik.l2eve.geodriver.driver.GeoConfig;
import ru.mosinnik.l2eve.geodriver.regions.Region;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.*;
import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants.*;
import static ru.mosinnik.l2eve.geodriver.util.Converter.asBytes;
import static ru.mosinnik.l2eve.geodriver.util.Converter.asInts;


@Slf4j
public final class GeoDriverBytesGen {

    private final GeoConfig config;

    // гео данные
    private ByteBuffer data;

    // по индексу содержится оффсет первого блока региона в blockTypes и blockDataOffsets
    // offset at `blockDataOffsets` array of first region block
    private final int[] regionFirstBlockIndexes = new int[GEO_REGIONS_X * GEO_REGIONS_Y]; //1024

    private byte[] blockTypes;

    // оффсет начала блока в data
    private int[] blockDataOffsets;

    public GeoDriverBytesGen() {
        config = new GeoConfig();
    }

    public GeoDriverBytesGen(GeoConfig config) {
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

        rebuildMethods();

        // если не добавить вызов data.capacity(), то почемуто перф тесты иногда сильно деградируют
        log.info("data size: {}", data.capacity());
    }


    public void rebuildMethods() {
        log.info("Start rebuilding method");
        // Генерируем трансформированный байткод и применяем его
        try {
            // Инициализируем ByteBuddy агент если нужно
            if (!BaseDriverAgent.isInitialized()) {
                BaseDriverAgent.initialize();
            }

            MappingHolder mappingHolder = BlockTypeMapper.remapTypes(blockTypes);
            byte[] transformedBytes = BaseDriverClassGenerator.generateTransformedClass(mappingHolder);
            BaseDriverAgent.redefineBaseDriver(transformedBytes);

            log.info("OK rebuilding method");
            blockTypes = mappingHolder.newTypes();
            log.info("Block types array replaced");
        } catch (Exception e) {
            throw new RuntimeException("Failed to transform BaseDriver", e);
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

    public void loadRegion(Path filePath) {
        throw new RuntimeException("Not implemented");
    }


    public boolean hasGeoPos(int geoX, int geoY) {
        int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return false;
        }
        return true;
    }


    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
        int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        if (regionFirstBlockIndex == NO_INDEX) {
            return NullRegionBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe);
        }
        int blockIndexInRegion = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);
        byte blockType = blockTypes[regionFirstBlockIndex + blockIndexInRegion];
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
                return IndexedMultilayerBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }


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
                return IndexedMultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.getNearestZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

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
                return IndexedMultilayerBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.getNextLowerZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

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
                return IndexedMultilayerBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            case INDEXED_32_MULTILAYER_BLOCK -> {
                return Indexed32MultilayerBlockBytes.getNextHigherZ(geoX, geoY, worldZ, blockDataOffset, data);
            }
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }


    public int getGeoX(int worldX) {
        if ((worldX < WORLD_MIN_X) || (worldX > WORLD_MAX_X)) {
            throw new IllegalArgumentException();
        }
        return (worldX - WORLD_MIN_X) >> 4;
    }

    public int getGeoY(int worldY) {
        if ((worldY < WORLD_MIN_Y) || (worldY > WORLD_MAX_Y)) {
            throw new IllegalArgumentException();
        }
        return (worldY - WORLD_MIN_Y) >> 4;
    }

}
