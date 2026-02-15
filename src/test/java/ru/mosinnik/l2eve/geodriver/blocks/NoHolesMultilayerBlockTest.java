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

package ru.mosinnik.l2eve.geodriver.blocks;

import org.junit.Test;
import ru.mosinnik.l2eve.geodriver.driver.GeoDriverTest;
import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.driver.GeoConfig;
import ru.mosinnik.l2eve.geodriver.driver.GeoDriver;
import ru.mosinnik.l2eve.geodriver.regions.BlockManager;
import ru.mosinnik.l2eve.geodriver.util.BlockStat;
import ru.mosinnik.l2eve.geodriver.util.Cmp;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static ru.mosinnik.l2eve.geodriver.GeoDriverTestConstants.TST_BLOCK_RESOURCE_BIGGEST;

public class NoHolesMultilayerBlockTest {

    @Test
    public void compareOneBlock() throws IOException {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setBlockStatSavingEnabled(true);

        GeoDriver driver = new GeoDriver(geoConfig);

        String tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
        File resource = new File(GeoDriverTest.class.getClassLoader().getResource(tstRegion).getFile());
        driver.loadRegion(resource.toPath(), 1, 1);

        BlockStat blockStat = BlockManager.stats.values().stream()
                .filter(stat -> stat.layers.size() == 1)
                .findFirst().orElseThrow();

        MultilayerBlock block1 = (MultilayerBlock) blockStat.block;
        IBlock block2 = new NoHolesMultilayerBlock(block1.data, blockStat);
        Cmp.compareBlocks(block1, block2);
    }

    @Test
    public void compare() throws IOException {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setBlockStatSavingEnabled(true);

        GeoDriver driver = new GeoDriver(geoConfig);

        String tstRegion = TST_BLOCK_RESOURCE_BIGGEST;
        File resource = new File(GeoDriverTest.class.getClassLoader().getResource(tstRegion).getFile());
        driver.loadRegion(resource.toPath(), 1, 1);

        List<BlockStat> blockStats = BlockManager.stats.values().stream()
                .filter(stat -> stat.layers.size() == 1)
                .toList();

        for (BlockStat blockStat : blockStats) {
            MultilayerBlock block1 = (MultilayerBlock) blockStat.block;
            NoHolesMultilayerBlock block2 = new NoHolesMultilayerBlock(block1.data, blockStat);

            int minHeight = block1.getMinHeight();
            int maxHeight = block1.getMaxHeight();
//            System.out.println("minHeight: " + minHeight + ", maxHeight: " + maxHeight);
            Cmp.compareBlocks(block1, block2, minHeight, maxHeight);
        }
    }
}
