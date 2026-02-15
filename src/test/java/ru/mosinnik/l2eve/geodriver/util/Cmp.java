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

package ru.mosinnik.l2eve.geodriver.util;

import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.abstraction.IGeoDriver;

import static org.junit.Assert.assertEquals;

public class Cmp {

    public static void compareBlocks(IBlock block1, IBlock block2) {
        for (int i = 0; i < IBlock.BLOCK_CELLS_X; i++) {
            for (int j = 0; j < IBlock.BLOCK_CELLS_Y; j++) {
                for (int k = -16000; k < 16000; k += 1000) {
//                    System.out.println(i + " : " + j + " : " + k);
                    for (int l = 0; l < 16; l++) {
                        assertEquals(
                            block1.checkNearestNSWE(i, j, k, (byte) l),
                            block2.checkNearestNSWE(i, j, k, (byte) l)
                        );
                    }
                    assertEquals(
                        block1.getNearestZ(i, j, k),
                        block2.getNearestZ(i, j, k)
                    );
                    assertEquals(
                        block1.getNextLowerZ(i, j, k),
                        block2.getNextLowerZ(i, j, k)
                    );
                    assertEquals(
                        block1.getNextHigherZ(i, j, k),
                        block2.getNextHigherZ(i, j, k)
                    );
                }

            }
        }
    }

    public static void compareBlocks(IBlock block1, IBlock block2, int minHeight, int maxHeight) {
        for (int i = 0; i < IBlock.BLOCK_CELLS_X; i++) {
            for (int j = 0; j < IBlock.BLOCK_CELLS_Y; j++) {
                for (int k = minHeight - 100; k < maxHeight + 100; k += 1000) {
//                    System.out.println(i + " : " + j + " : " + k);
                    for (int l = 0; l < 16; l++) {
                        assertEquals(
                            block1.checkNearestNSWE(i, j, k, (byte) l),
                            block2.checkNearestNSWE(i, j, k, (byte) l)
                        );
                    }
                    assertEquals(
                        block1.getNearestZ(i, j, k),
                        block2.getNearestZ(i, j, k)
                    );
                    assertEquals(
                        block1.getNextLowerZ(i, j, k),
                        block2.getNextLowerZ(i, j, k)
                    );
                    assertEquals(
                        block1.getNextHigherZ(i, j, k),
                        block2.getNextHigherZ(i, j, k)
                    );
                }

            }
        }
    }

    public static void compareBlocksHeavy(IBlock block1, IBlock block2, int minHeight, int maxHeight) {
        for (int i = 0; i < IBlock.BLOCK_CELLS_X; i++) {
            for (int j = 0; j < IBlock.BLOCK_CELLS_Y; j++) {
                for (int k = minHeight - 100; k < maxHeight + 100; k += 1) {
//                    System.out.println(i + " : " + j + " : " + k);
                    for (int l = 0; l < 16; l++) {
                        assertEquals(
                            block1.checkNearestNSWE(i, j, k, (byte) l),
                            block2.checkNearestNSWE(i, j, k, (byte) l)
                        );
                    }
                    assertEquals(
                        block1.getNearestZ(i, j, k),
                        block2.getNearestZ(i, j, k)
                    );
                    assertEquals(
                        block1.getNextLowerZ(i, j, k),
                        block2.getNextLowerZ(i, j, k)
                    );
                    assertEquals(
                        block1.getNextHigherZ(i, j, k),
                        block2.getNextHigherZ(i, j, k)
                    );
                }

            }
        }
    }


    public static void compareDrivers(IGeoDriver driver1, IGeoDriver driver2, int cornerMinX, int cornerMaxX, int cornerMinY, int cornerMaxY) {
        int stepX = 100;
        int stepY = 100;
        int stepZ = 100;

        for (int worldX = cornerMinX; worldX < cornerMaxX; worldX += stepX) {
            int x = driver1.getGeoX(worldX);
//            System.out.println("x = " + x);
            for (int worldY = cornerMinY; worldY < cornerMaxY; worldY += stepY) {
                int y = driver1.getGeoY(worldY);
//                System.out.println("y = " + y);
                for (int z = -16000; z < 16000; z += stepZ) {
                    for (int l = 0; l < 16; l++) {
                        assertEquals(
                            driver2.checkNearestNSWE(x, y, z, (byte) l),
                            driver1.checkNearestNSWE(x, y, z, (byte) l)
                        );
                    }
                    assertEquals(
                        driver2.getNearestZ(x, y, z),
                        driver1.getNearestZ(x, y, z)
                    );
                    assertEquals(
                        driver2.getNextLowerZ(x, y, z),
                        driver1.getNextLowerZ(x, y, z)
                    );
                    assertEquals(
                        driver2.getNextHigherZ(x, y, z),
                        driver1.getNextHigherZ(x, y, z)
                    );
                    assertEquals(
                        driver2.hasGeoPos(x, y),
                        driver1.hasGeoPos(x, y)
                    );
                }
            }
        }
    }
}
