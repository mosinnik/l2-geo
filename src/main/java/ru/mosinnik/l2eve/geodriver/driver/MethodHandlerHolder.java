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

import lombok.experimental.UtilityClass;
import ru.mosinnik.l2eve.geodriver.bytes.ComplexBlockBytes;
import ru.mosinnik.l2eve.geodriver.bytes.FlatBlockBytesMH;
import ru.mosinnik.l2eve.geodriver.bytes.MultilayerBlockBytes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;

@UtilityClass
public class MethodHandlerHolder {

    public static final MethodHandle checkNearestNSWEFlatBlockBytes;
    public static final MethodHandle checkNearestNSWEComposeBlockBytes;
    public static final MethodHandle checkNearestNSWEMultilayerBlockBytes;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        try {
            MethodType checkNearestNSWEBytesType = MethodType.methodType(boolean.class, int.class, int.class, int.class, byte.class, int.class, ByteBuffer.class);
            String checkNearestNSWEName = "checkNearestNSWE";

            checkNearestNSWEFlatBlockBytes = lookup.findStatic(
                    FlatBlockBytesMH.class,
                    checkNearestNSWEName,
                    checkNearestNSWEBytesType
            );

            checkNearestNSWEComposeBlockBytes = lookup.findStatic(
                    ComplexBlockBytes.class,
                    checkNearestNSWEName,
                    checkNearestNSWEBytesType
            );

            checkNearestNSWEMultilayerBlockBytes = lookup.findStatic(
                    MultilayerBlockBytes.class,
                    checkNearestNSWEName,
                    checkNearestNSWEBytesType
            );

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
