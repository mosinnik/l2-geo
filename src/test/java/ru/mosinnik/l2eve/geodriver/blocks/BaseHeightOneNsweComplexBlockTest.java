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
import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.util.BlockStat;
import ru.mosinnik.l2eve.geodriver.util.Cmp;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BaseHeightOneNsweComplexBlockTest {

    @Test
    public void testBaseHeightOneNsweComplexBlock() {
        int heightDelta = 2040;
        // must be x*8
        int step = 40;
        int heightCount = heightDelta / step;
        assertEquals(heightDelta, heightCount * step);

        int baseHeight = -512;
        List<Integer> heights = new ArrayList<>();
        for (int i = 0; i <= heightCount; i++) {
            int height = baseHeight + i * step;
            heights.add(height);
        }
        assertEquals(heightDelta, heights.getLast() - heights.getFirst());

        byte nswe = 1;
        short[] tmpData = new short[IBlock.BLOCK_CELLS];
        for (int i = 0; i < IBlock.BLOCK_CELLS; i++) {
            Integer height = heights.get(i % heightCount);
            tmpData[i] = (short) ((height << 1) | nswe);
        }

        BlockStat mm = new BlockStat();
        mm.heights.addAll(heights);
        mm.nswes.add((int) nswe);

        IBlock block1 = new ComplexBlock(tmpData);
        IBlock block2 = new BaseHeightOneNsweComplexBlock(tmpData, mm);

        Cmp.compareBlocks(block1, block2);
    }
}
