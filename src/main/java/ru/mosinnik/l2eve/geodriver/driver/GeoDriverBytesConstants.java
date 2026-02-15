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


public interface GeoDriverBytesConstants {

    int NO_INDEX = -1;

    byte NO_DATA_BLOCK = 0;
    byte FLAT_BLOCK = 1;
    byte COMPLEX_BLOCK = 2;
    byte MULTILAYER_BLOCK = 3;
    byte ONE_HEIGHT_COMPLEX_BLOCK = 4;
    byte BASE_HEIGHT_COMPLEX_BLOCK = 5;
    byte BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK = 6;
    byte FEW_HEIGHTS_COMPLEX_BLOCK = 7;
    byte FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK = 8;
    byte NO_HOLES_MULTILAYER_BLOCK = 9;
    byte INDEXED_MULTILAYER_BLOCK = 10;
    byte INDEXED_MULTILAYER_32_BLOCK = 11;

    String DATA_FILE_NAME = "data.bin";
    String REGION_FIRST_BLOCK_INDEXES_FILE_NAME = "regionFirstBlockIndexes.bin";
    String BLOCK_TYPES_FILE_NAME = "blockTypes.bin";
    String BLOCK_DATA_OFFSETS_FILE_NAME = "blockDataOffsets.bin";

    static String blockTypeToName(byte blockType) {
        return switch (blockType) {
            case NO_DATA_BLOCK -> "NO_DATA_BLOCK";
            case FLAT_BLOCK -> "FLAT_BLOCK";
            case COMPLEX_BLOCK -> "COMPLEX_BLOCK";
            case MULTILAYER_BLOCK -> "MULTILAYER_BLOCK";
            case ONE_HEIGHT_COMPLEX_BLOCK -> "ONE_HEIGHT_COMPLEX_BLOCK";
            case BASE_HEIGHT_COMPLEX_BLOCK -> "BASE_HEIGHT_COMPLEX_BLOCK";
            case BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK -> "BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK";
            case FEW_HEIGHTS_COMPLEX_BLOCK -> "FEW_HEIGHTS_COMPLEX_BLOCK";
            case FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK -> "FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK";
            case NO_HOLES_MULTILAYER_BLOCK -> "NO_HOLES_MULTILAYER_BLOCK";
            case INDEXED_MULTILAYER_BLOCK -> "INDEXED_MULTILAYER_BLOCK";
            case INDEXED_MULTILAYER_32_BLOCK -> "INDEXED_MULTILAYER_32_BLOCK";
            default -> throw new RuntimeException("Unknown block type: " + blockType);
        };
    }

    static byte blockNameToType(String blockName) {
        return switch (blockName) {
            case "NO_DATA_BLOCK" -> NO_DATA_BLOCK;
            case "FLAT_BLOCK" -> FLAT_BLOCK;
            case "COMPLEX_BLOCK" -> COMPLEX_BLOCK;
            case "MULTILAYER_BLOCK" -> MULTILAYER_BLOCK;
            case "ONE_HEIGHT_COMPLEX_BLOCK" -> ONE_HEIGHT_COMPLEX_BLOCK;
            case "BASE_HEIGHT_COMPLEX_BLOCK" -> BASE_HEIGHT_COMPLEX_BLOCK;
            case "BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK" -> BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK;
            case "FEW_HEIGHTS_COMPLEX_BLOCK" -> FEW_HEIGHTS_COMPLEX_BLOCK;
            case "FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK" -> FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK;
            case "NO_HOLES_MULTILAYER_BLOCK" -> NO_HOLES_MULTILAYER_BLOCK;
            case "INDEXED_MULTILAYER_BLOCK" -> INDEXED_MULTILAYER_BLOCK;
            case "INDEXED_MULTILAYER_32_BLOCK" -> INDEXED_MULTILAYER_32_BLOCK;
            default -> throw new RuntimeException("Unknown block name: " + blockName);
        };
    }

    enum E {
        NO_DATA_BLOCK,
        FLAT_BLOCK,
        COMPLEX_BLOCK,
        MULTILAYER_BLOCK,
        ONE_HEIGHT_COMPLEX_BLOCK,
        BASE_HEIGHT_COMPLEX_BLOCK,
        BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK,
        FEW_HEIGHTS_COMPLEX_BLOCK,
        FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK,
        NO_HOLES_MULTILAYER_BLOCK,
        INDEXED_MULTILAYER_BLOCK,
        INDEXED_MULTILAYER_32_BLOCK,
    }
}
