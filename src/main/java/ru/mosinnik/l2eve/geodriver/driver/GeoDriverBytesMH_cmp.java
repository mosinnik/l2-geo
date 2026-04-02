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
import ru.mosinnik.l2eve.geodriver.blocks.*;
import ru.mosinnik.l2eve.geodriver.bytes.*;
import ru.mosinnik.l2eve.geodriver.regions.Region;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.GEO_REGIONS_X;
import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.GEO_REGIONS_Y;
import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants.*;


@Slf4j
public final class GeoDriverBytesMH_cmp {

    private final GeoConfig config;

    // гео данные
    private ByteBuffer data;

    // по индексу содержится оффсет первого блока региона в blockTypes и blockDataOffsets
    // offset at `blockDataOffsets` array of first region block
    private final int[] regionFirstBlockIndexes = new int[GEO_REGIONS_X * GEO_REGIONS_Y]; //1024

    private byte[] blockTypes;

    // оффсет начала блока в data
    private int[] blockDataOffsets;

    public GeoDriverBytesMH_cmp() {
        config = new GeoConfig();
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
                    appendBytes(block, data);
                }

                blockIndex++;
            }
        }
        assert totalBlockCount == blockIndex;

        // если не добавить вызов data.capacity(), то почемуто перф тесты иногда сильно деградируют
        log.info("data size: {}", data.capacity());
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
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        }
    }

}
