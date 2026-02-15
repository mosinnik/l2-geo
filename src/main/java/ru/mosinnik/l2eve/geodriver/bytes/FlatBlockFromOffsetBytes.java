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

package ru.mosinnik.l2eve.geodriver.bytes;


import ru.mosinnik.l2eve.geodriver.blocks.FlatBlock;

import java.nio.ByteBuffer;

/**
 * blockDataOffset хранит высоту как есть, нет необходимости что-то хранить в data
 */
public class FlatBlockFromOffsetBytes {

    public static int calcBytesCount(FlatBlock block) {
        return 0;
    }

    public static void appendBytes(FlatBlock block, ByteBuffer data) {
    }

    public static byte[] toBytes(FlatBlock block) {
        return new byte[0];
    }

    public static int getHeight(FlatBlock block) {
        return block.getHeight();
    }

    public static int getSize(int blockDataOffset, ByteBuffer data) {
        return 0;
    }

    //---------------------------------------------------------------


    public static boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return true;
    }

    public static int getNearestZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        return blockDataOffset;
    }

    public static int getNextLowerZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        return Math.min(blockDataOffset, worldZ);
    }

    public static int getNextHigherZ(int geoX, int geoY, int worldZ, int blockDataOffset, ByteBuffer data) {
        return Math.max(blockDataOffset, worldZ);
    }

}
