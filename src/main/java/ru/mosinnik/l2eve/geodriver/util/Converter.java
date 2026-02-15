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

import java.nio.ByteBuffer;


public interface Converter {

    static byte[] asBytes(int[] intArray) {
        ByteBuffer buffer = ByteBuffer.allocate(intArray.length * 4);
        for (int value : intArray) {
            buffer.putInt(value);
        }
        return buffer.array();
    }

    static void asInts(byte[] bytes, int[] destination) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int length = bytes.length / 4;
        if (bytes.length % 4 != 0 || length != destination.length) {
            throw new IllegalArgumentException("Length does not match from file: " + length + ", in dest: " + destination.length + " or not divided by 4 evenly: " + bytes.length);
        }
        for (int i = 0; i < length; i++) {
            destination[i] = buffer.getInt();
        }
    }

    static int[] asInts(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        if (bytes.length % 4 != 0) {
            throw new IllegalArgumentException("Input file length not divided by 4 evenly: " + bytes.length);
        }
        int length = bytes.length / 4;
        int[] intArray = new int[length];
        for (int i = 0; i < length; i++) {
            intArray[i] = buffer.getInt();
        }
        return intArray;
    }

}
