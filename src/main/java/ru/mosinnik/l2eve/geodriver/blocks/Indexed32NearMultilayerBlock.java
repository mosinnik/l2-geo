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
import ru.mosinnik.l2eve.geodriver.util.OrderType;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static ru.mosinnik.l2eve.geodriver.util.OrderType.detectOrder;

/**
 *
 */
public class Indexed32NearMultilayerBlock implements IBlock {
    public final short[] index;
    public final short[] data;
    public final short[] midHeights;


    /**
     * Initializes a new instance of this block reading the specified buffer.
     *
     * @param bb the buffer
     */
    public Indexed32NearMultilayerBlock(ByteBuffer bb) {
        int start = bb.position();

        index = new short[IBlock.BLOCK_CELLS];

        int layersCountBefore = 0;

        for (int i = 0; i < IBlock.BLOCK_CELLS; i++) {
            byte nLayers = bb.get();
            if ((nLayers <= 0) || (nLayers > 32)) {
                throw new RuntimeException("Unexpected layer count: " + nLayers);
            }
            index[i] = (short) ((nLayers << 11) | ((layersCountBefore) & 0x07FF));
            layersCountBefore += nLayers;
            bb.position(bb.position() + (nLayers * 2));
        }

        // reset buffer to start position to read data
        bb.position(start);
        data = new short[layersCountBefore];
        midHeights = new short[layersCountBefore];

        // copy data without numb layers
        int dataIndex = 0;
        for (int i = 0; i < IBlock.BLOCK_CELLS; i++) {
            byte nLayers = bb.get();
            short[] layerDatum = new short[nLayers];
            int[] heights = new int[nLayers];
            int prevHeight = Integer.MIN_VALUE;
            for (int j = 0; j < nLayers; j++) {
                short layerData = Short.reverseBytes(bb.getShort());
                layerDatum[j] = layerData;

                int height = extractLayerHeight(layerData);
                heights[j] = height;

                if (prevHeight != Integer.MIN_VALUE) {
                    int midHeight = (prevHeight + height) / 2;
                    midHeights[dataIndex + j - 1] = (short) midHeight;
                }
                prevHeight = height;
            }


            OrderType orderType = detectOrder(heights);
            if (orderType != OrderType.DESC && orderType != OrderType.ONE) {
                throw new RuntimeException("Order type " + orderType + " forbidden for layers heights in geo: layerData: " + Arrays.toString(layerDatum) + ", heights: " + Arrays.toString(heights));
            }

            System.arraycopy(layerDatum, 0, data, dataIndex, layerDatum.length);
            dataIndex += layerDatum.length;
        }
    }

    public Indexed32NearMultilayerBlock(byte[] data) {
        this(ByteBuffer.wrap(data));
    }

    public short[] getData() {
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

    private int extractLayerNswe(short layer) {
        return (byte) (layer & 0x000F);
    }

    private int extractLayerHeight(short layer) {
        layer = (short) (layer & 0x0fff0);
        return layer >> 1;
    }

    private short getNearestLayer(int geoX, int geoY, int worldZ) {
        int cellDataOffset = getCellDataOffset(geoX, geoY);
        int startOffset = cellDataOffset & 0x07FF;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        if (nLayers == 1) {
            return data[startOffset];
        }
        if (nLayers == 2) {
            short layerDataH = data[startOffset];
            int layerZH = extractLayerHeight(layerDataH);
            if (layerZH <= worldZ) {
                return layerDataH;
            }

            short layerDataL = data[startOffset + 1];
            int layerZL = extractLayerHeight(layerDataL);
            if (layerZL >= worldZ) {
                return layerDataL;
            }

            int layerDZH = layerZH - worldZ;
            int layerDZL = worldZ - layerZL;
            if (layerDZH <= layerDZL) {
                return layerDataH;
            } else {
                return layerDataL;
            }
        }
        int endOffset = startOffset + nLayers;

//        // 1 layer at least was required on loading so this is set at least once on the loop below
//        int nearestDZ = 0;
//        short nearestData = 0;
//        for (int offset = startOffset; offset < endOffset; offset++) {
//            short layerData = data[offset];
//            int layerZ = extractLayerHeight(layerData);
//            if (layerZ == worldZ) {
//                return layerData; // exact z
//            }
//
//            int layerDZ = Math.abs(layerZ - worldZ);
//            if ((offset == startOffset) || (layerDZ < nearestDZ)) {
//                nearestDZ = layerDZ;
//                nearestData = layerData;
//            } else {
//                return nearestData;
//            }
//        }
//        return nearestData;


        for (int offset = startOffset; offset < endOffset; offset++) {
            short midZ = midHeights[offset];
            if (midZ <= worldZ) {
                return data[offset];
            }
        }
        return data[endOffset - 1];

    }

    private int getNearestNSWE(int geoX, int geoY, int worldZ) {
        short nearestLayer = getNearestLayer(geoX, geoY, worldZ);
        return extractLayerNswe(nearestLayer);
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
        int cellDataOffset = getCellDataOffset(geoX, geoY);
        int startOffset = cellDataOffset & 0x07FF;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        int endOffset = startOffset + nLayers;
        for (int offset = startOffset; offset < endOffset; offset++) {
            short layerData = data[offset];
            int layerZ = extractLayerHeight(layerData);
            if (layerZ <= worldZ) {
                return layerZ;
            }
        }
        return worldZ;
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        int cellDataOffset = getCellDataOffset(geoX, geoY);
        int startOffset = cellDataOffset & 0x07FF;
        int nLayers = (cellDataOffset >> 11) & 0x01F;
        int endOffset = startOffset + nLayers;
        int prevLayerZ = worldZ;
        for (int offset = startOffset; offset < endOffset; offset++) {
            short layerData = data[offset];
            int layerZ = extractLayerHeight(layerData);
            if (layerZ < worldZ) {
                return prevLayerZ;
            }
            prevLayerZ = layerZ;
        }
        return prevLayerZ;
    }

}
