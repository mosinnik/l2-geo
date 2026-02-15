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

import org.junit.Test;
import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.blocks.ComplexBlock;
import ru.mosinnik.l2eve.geodriver.blocks.MultilayerBlock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BlockManagerTest {

    @Test
    public void multiToComplexCmp() {
        int heightCount = 12;
        List<Integer> heights = new ArrayList<>();
        for (int i = 0; i < heightCount; i++) {
            int height = (10 * (i - 3)) << 3;
            heights.add(height);
        }

        byte[] tmpDataMulti = new byte[3 * IBlock.BLOCK_CELLS];
        short[] tmpData = new short[IBlock.BLOCK_CELLS];
        for (int i = 0; i < IBlock.BLOCK_CELLS; i++) {
            Integer height = heights.get(i % heightCount);
            byte nswe = (byte) (i % 16);
            tmpData[i] = (short) ((height << 1) | nswe);

            tmpDataMulti[3 * i] = 1;
            tmpDataMulti[3 * i + 1] = (byte) (tmpData[i] & 0xFF);
            tmpDataMulti[3 * i + 2] = (byte) ((tmpData[i] >> 8) & 0xFF);
        }

        IBlock complex = new ComplexBlock(tmpData);
        IBlock multi = new MultilayerBlock(tmpDataMulti);

        for (int i = 0; i < IBlock.BLOCK_CELLS_X; i++) {
            for (int j = 0; j < IBlock.BLOCK_CELLS_Y; j++) {
                int num = i * IBlock.BLOCK_CELLS_X + j;
                int height = heights.get(num % heightCount);
                byte nswe = (byte) (num % 16);

                assertEquals(height, complex.getNearestZ(i, j, 3000));
                assertEquals(height, multi.getNearestZ(i, j, 3000));

                assertTrue(complex.checkNearestNSWE(i, j, 3000, nswe));
                assertTrue(multi.checkNearestNSWE(i, j, 3000, nswe));

                assertEquals(
                        complex.checkNearestNSWE(i, j, 3000, nswe),
                        multi.checkNearestNSWE(i, j, 3000, nswe)
                );
                assertEquals(
                        complex.getNearestZ(i, j, 3000),
                        multi.getNearestZ(i, j, 3000)
                );
            }
        }
    }
}
