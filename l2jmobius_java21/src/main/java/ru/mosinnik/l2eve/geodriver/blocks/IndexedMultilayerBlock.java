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


import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;

import java.nio.ByteBuffer;

public class IndexedMultilayerBlock implements IBlock {
    public final byte[] data;
    public final short[] index;

    /**
     * Initializes a new instance of this block reading the specified buffer.
     *
     * @param bb the buffer
     */
    public IndexedMultilayerBlock(ByteBuffer bb) {
        int start = bb.position();

        index = new short[IBlock.BLOCK_CELLS];
        for (int i = 0; i < IBlock.BLOCK_CELLS; i++) {
            index[i] = (short) (bb.position() - start);
            byte nLayers = bb.get();
            if ((nLayers <= 0) || (nLayers > 125)) {
                throw new RuntimeException("L2JGeoDriver: Geo file corrupted! Invalid layers count!");
            }
            bb.position(bb.position() + (nLayers * 2));
        }

        data = new byte[bb.position() - start];
        bb.position(start);
        bb.get(data);
    }

    public IndexedMultilayerBlock(byte[] data) {
        this(ByteBuffer.wrap(data));
    }

    public byte[] getData() {
        return data;
    }

    public short[] getIndex() {
        return index;
    }

    /**
     * readable:
     * int cellLocalOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private int getCellDataOffset(int geoX, int geoY) {
        int cellLocalOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        return index[cellLocalOffset];
    }

    private short extractLayerData(int dataOffset) {
        return (short) ((data[dataOffset] & 0xFF) | (data[dataOffset + 1] << 8));
    }

    private int getNearestNSWE(int geoX, int geoY, int worldZ) {
        short nearestLayer = getNearestLayer(geoX, geoY, worldZ);
        return extractLayerNswe(nearestLayer);
    }

    private int extractLayerNswe(short layer) {
        return (byte) (layer & 0x000F);
    }

    private int extractLayerHeight(short layer) {
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }

    private short getNearestLayer(int geoX, int geoY, int worldZ) {
        int startOffset = getCellDataOffset(geoX, geoY);
        byte nLayers = data[startOffset];
        if (nLayers == 1) {
            return extractLayerData(startOffset + 1);
        }
        int endOffset = startOffset + 1 + (nLayers * 2);

        // 1 layer at least was required on loading so this is set at least once on the loop below
        int nearestDZ = 0;
        short nearestData = 0;
        for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
            short layerData = extractLayerData(offset);
            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerData; // exact z
            }

            int layerDZ = Math.abs(layerZ - worldZ);
            if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ)) {
                nearestDZ = layerDZ;
                nearestData = layerData;
            }
        }
        return nearestData;
    }


    @Override
    public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        return (getNearestNSWE(geoX, geoY, worldZ) & nswe) == nswe;
    }

    @Override
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        return extractLayerHeight(getNearestLayer(geoX, geoY, worldZ));
    }

    @Override
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        int startOffset = getCellDataOffset(geoX, geoY);
        byte nLayers = data[startOffset];
        int endOffset = startOffset + 1 + (nLayers * 2);

        int lowerZ = Integer.MIN_VALUE;
        for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
            short layerData = extractLayerData(offset);

            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerZ;                // exact z
            }

            if ((layerZ < worldZ) && (layerZ > lowerZ)) {
                lowerZ = layerZ;
            }
        }
        return lowerZ == Integer.MIN_VALUE ? worldZ : lowerZ;
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        int startOffset = getCellDataOffset(geoX, geoY);
        byte nLayers = data[startOffset];
        int endOffset = startOffset + 1 + (nLayers * 2);

        int higherZ = Integer.MAX_VALUE;
        for (int offset = startOffset + 1; offset < endOffset; offset += 2) {
            short layerData = extractLayerData(offset);

            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerZ; // exact z
            }

            if ((layerZ > worldZ) && (layerZ < higherZ)) {
                higherZ = layerZ;
            }
        }
        return higherZ == Integer.MAX_VALUE ? worldZ : higherZ;
    }
}
