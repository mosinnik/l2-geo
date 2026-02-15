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

package ru.mosinnik.l2eve.geodriver.regions;

import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.blocks.*;
import ru.mosinnik.l2eve.geodriver.driver.GeoConfig;
import ru.mosinnik.l2eve.geodriver.util.BlockStat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class BlockManager {

    public static final Map<Short, FlatBlock> heightToFlatBlockMap = new ConcurrentHashMap<>();

    // for stats analyze
    public static final Map<IBlock, BlockStat> stats = new ConcurrentHashMap<>();

    public static final List<FlatBlock> allFlatBlocks = new ArrayList<>();
    public static final List<OneHeightComplexBlock> allOneHeightComplexBlocks = new ArrayList<>();
    public static final List<FewHeightsComplexBlock> allFewHeightComplexBlocks = new ArrayList<>();
    public static final List<FewHeightsOneNsweComplexBlock> allFewHeightOneNsweComplexBlocks = new ArrayList<>();
    public static final List<BaseHeightComplexBlock> allBaseHeightComplexBlocks = new ArrayList<>();
    public static final List<BaseHeightOneNsweComplexBlock> allBaseHeightOneNsweComplexBlocks = new ArrayList<>();
    public static final List<ComplexBlock> allComplexBlocks = new ArrayList<>();
    public static final List<MultilayerBlock> allMultilayerBlocks = new ArrayList<>();
    public static final List<MultilayerIndexedBlock> allMultilayerIndexedBlocks = new ArrayList<>();
    public static final List<NoHolesMultilayerBlock> allNoHolesMultilayerBlocks = new ArrayList<>();
    public static final List<MultilayerIndexed32Block> allMultilayerIndexed32Blocks = new ArrayList<>();

    public static final List<List<? extends IBlock>> allBlocksLists = List.of(
            allFlatBlocks,
            allOneHeightComplexBlocks,
            allFewHeightComplexBlocks,
            allFewHeightOneNsweComplexBlocks,
            allBaseHeightComplexBlocks,
            allBaseHeightOneNsweComplexBlocks,
            allComplexBlocks,
            allMultilayerBlocks,
            allNoHolesMultilayerBlocks,
            allMultilayerIndexedBlocks,
            allMultilayerIndexed32Blocks
    );


    private final boolean blockStatSavingEnabled;

    private final boolean reuseFlatBlockEnabled;
    private final boolean oneHeightComplexBlockEnabled;
    private final boolean fewHeightsOneNsweComplexBlockEnabled;
    private final boolean fewHeightsComplexBlockEnabled;
    private final boolean baseHeightComplexBlockEnabled;
    private final boolean baseHeightOneNsweComplexBlockEnabled;
    private final boolean noHolesMultilayerBlockEnabled;
    private final boolean indexedMultilayerBlockEnabled;
    private final boolean indexedMultilayer32BlockEnabled;


    public BlockManager(GeoConfig geoConfig) {
        this.blockStatSavingEnabled = geoConfig.isBlockStatSavingEnabled();

        this.reuseFlatBlockEnabled = geoConfig.isReuseFlatBlockEnabled();
        this.oneHeightComplexBlockEnabled = geoConfig.isOneHeightComplexBlockEnabled();
        this.fewHeightsOneNsweComplexBlockEnabled = geoConfig.isFewHeightsOneNsweComplexBlockEnabled();
        this.fewHeightsComplexBlockEnabled = geoConfig.isFewHeightsComplexBlockEnabled();
        this.baseHeightComplexBlockEnabled = geoConfig.isBaseHeightComplexBlockEnabled();
        this.baseHeightOneNsweComplexBlockEnabled = geoConfig.isBaseHeightOneNsweComplexBlockEnabled();
        this.noHolesMultilayerBlockEnabled = geoConfig.isNoHolesMultilayerBlockEnabled();
        this.indexedMultilayerBlockEnabled = geoConfig.isIndexedMultilayerBlockEnabled();
        this.indexedMultilayer32BlockEnabled = geoConfig.isIndexedMultilayer32BlockEnabled();
    }


    public IBlock flatBlock(ByteBuffer bb) {
        short height = bb.getShort();
        FlatBlock result;
        if (reuseFlatBlockEnabled) {
            result = heightToFlatBlockMap.computeIfAbsent(height, FlatBlock::new);
        } else {
            result = new FlatBlock(height);
        }
        saveBlockForMemoryStat(allFlatBlocks, result);
        return result;
    }

    public IBlock complexBlock(ByteBuffer bb) {
        short[] tmpData = new short[IBlock.BLOCK_CELLS];
        for (int cellOffset = 0; cellOffset < IBlock.BLOCK_CELLS; cellOffset++) {
            tmpData[cellOffset] = bb.getShort();
        }
        return complexBlock(tmpData);
    }

    public IBlock complexBlock(short[] tmpData) {
        BlockStat blockStat = new BlockStat();
        for (int cellOffset = 0; cellOffset < IBlock.BLOCK_CELLS; cellOffset++) {
            short height = (short) (tmpData[cellOffset] & 0x0FFF0);
            int heightI = height >> 1;
            int nswe = tmpData[cellOffset] & 0x000F;

            blockStat.min = Math.min(blockStat.min, heightI);
            blockStat.max = Math.max(blockStat.max, heightI);
            blockStat.heights.add(heightI);
            blockStat.nswes.add(nswe);
        }

        IBlock result;
        int size = blockStat.heights.size();
        if (size == 1 && oneHeightComplexBlockEnabled) {
            result = new OneHeightComplexBlock(tmpData, blockStat);
            saveBlockForMemoryStat(allOneHeightComplexBlocks, (OneHeightComplexBlock) result);
        } else if (blockStat.delta() <= 2040 && blockStat.nswes.size() == 1 && baseHeightOneNsweComplexBlockEnabled) {
            result = new BaseHeightOneNsweComplexBlock(tmpData, blockStat);
            saveBlockForMemoryStat(allBaseHeightOneNsweComplexBlocks, (BaseHeightOneNsweComplexBlock) result);
        } else if (blockStat.delta() <= 120 && baseHeightComplexBlockEnabled) {
            result = new BaseHeightComplexBlock(tmpData, blockStat);
            saveBlockForMemoryStat(allBaseHeightComplexBlocks, (BaseHeightComplexBlock) result);
        } else if (blockStat.heights.size() < 16 && fewHeightsComplexBlockEnabled) {
            if (blockStat.nswes.size() == 1 && fewHeightsOneNsweComplexBlockEnabled) {
                result = new FewHeightsOneNsweComplexBlock(tmpData, blockStat);
                saveBlockForMemoryStat(allFewHeightOneNsweComplexBlocks, (FewHeightsOneNsweComplexBlock) result);
            } else {
                result = new FewHeightsComplexBlock(tmpData, blockStat);
                saveBlockForMemoryStat(allFewHeightComplexBlocks, (FewHeightsComplexBlock) result);
            }
        } else {
            result = new ComplexBlock(tmpData);
            saveBlockForMemoryStat(allComplexBlocks, (ComplexBlock) result);
        }
        saveStat(result, blockStat);
        return result;
    }

    public IBlock multilayerBlock(ByteBuffer bb) {

        BlockStat blockStat = new BlockStat();

        int start = bb.position();

        for (int blockCellOffset = 0; blockCellOffset < IBlock.BLOCK_CELLS; blockCellOffset++) {
            byte nLayers = bb.get();
            if ((nLayers <= 0) || (nLayers > 125)) {
                throw new RuntimeException("L2JGeoDriver: Geo file corrupted! Invalid layers count!");
            }

            blockStat.layers.add(nLayers);
            blockStat.addCellLayerNumber(nLayers);
            bb.position(bb.position() + (nLayers * 2));
        }

        byte[] data = new byte[bb.position() - start];
        bb.position(start);
        bb.get(data);

        IBlock result;
        if (blockStat.layers.size() == 1 && noHolesMultilayerBlockEnabled) {
            result = new NoHolesMultilayerBlock(data, blockStat);
            saveBlockForMemoryStat(allNoHolesMultilayerBlocks, (NoHolesMultilayerBlock) result);
        } else if (blockStat.layers.stream().max(Byte::compareTo).orElseThrow() < 32 && indexedMultilayer32BlockEnabled) {
            result = new MultilayerIndexed32Block(data);
            saveBlockForMemoryStat(allMultilayerIndexed32Blocks, (MultilayerIndexed32Block) result);
        } else if (indexedMultilayerBlockEnabled) {
            result = new MultilayerIndexedBlock(data);
            saveBlockForMemoryStat(allMultilayerIndexedBlocks, (MultilayerIndexedBlock) result);
        } else {
            result = new MultilayerBlock(data);
            saveBlockForMemoryStat(allMultilayerBlocks, (MultilayerBlock) result);
        }
        saveStat(result, blockStat);
        return result;
    }


    private <B> void saveBlockForMemoryStat(List<B> list, B result) {
        if (blockStatSavingEnabled) {
            list.add(result);
        }
    }

    private void saveStat(IBlock result, BlockStat blockStat) {
        if (blockStatSavingEnabled) {
            blockStat.block = result;
            stats.put(result, blockStat);
        }
    }

}
