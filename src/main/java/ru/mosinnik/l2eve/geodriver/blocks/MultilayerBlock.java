/*
 * Copyright (C) 2004-2013 L2J Server
 *
 * This file is part of L2J Server.
 *
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ru.mosinnik.l2eve.geodriver.blocks;


import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;

import java.nio.ByteBuffer;

/**
 * @author FBIagent
 * <p>
 * Not optimzable because of complex getCellDataOffset
 */
public class MultilayerBlock implements IBlock {
    public final byte[] data;

    /**
     * Initializes a new instance of this block reading the specified buffer.
     *
     * @param bb the buffer
     */
    public MultilayerBlock(ByteBuffer bb) {
        int start = bb.position();

        for (int blockCellOffset = 0; blockCellOffset < IBlock.BLOCK_CELLS; blockCellOffset++) {
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

    /**
     * Initializes a new instance of this block from bytes array.
     */
    public MultilayerBlock(byte[] data) {
        this.data = data;
    }

    public int getDataLength() {
        return data.length;
    }

    public byte[] getData() {
        return data;
    }

    public int getMinHeight() {
        int cellLayersIndex = 0;
        int minHeight = Integer.MAX_VALUE;
        for (int i = 0; i < IBlock.BLOCK_CELLS; i++) {
            byte nLayers = data[cellLayersIndex];
            short lowestLayerData = extractLayerData(cellLayersIndex + 1 + 2 * (nLayers - 1));
            minHeight = Math.min(minHeight, extractLayerHeight(lowestLayerData));
            cellLayersIndex += 1 + 2 * nLayers;
        }
        return minHeight;
    }

    public int getMaxHeight() {
        int cellLayersIndex = 0;
        int maxHeight = Integer.MIN_VALUE;
        for (int i = 0; i < IBlock.BLOCK_CELLS; i++) {
            byte nLayers = data[cellLayersIndex];
            short highestLayerData = extractLayerData(cellLayersIndex + 1);
            maxHeight = Math.max(maxHeight, extractLayerHeight(highestLayerData));
            cellLayersIndex += 1 + 2 * nLayers;
        }
        return maxHeight;
    }

    private short getNearestLayer(int geoX, int geoY, int worldZ) {
        int startOffset = getCellDataOffset(geoX, geoY);
        byte nLayers = data[startOffset];
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

    /**
     * readable:
     * int cellLocalOffset = ((geoX % IBlock.BLOCK_CELLS_X) * IBlock.BLOCK_CELLS_Y) + (geoY % IBlock.BLOCK_CELLS_Y);
     */
    private int getCellDataOffset(int geoX, int geoY) {
        int cellLocalOffset = ((geoX & 0x07) << 3) + (geoY & 0x07);
        int cellDataOffset = 0;

        // move index to cell, we need to parse on each request, OR we parse on creation and save indexes
        for (int i = 0; i < cellLocalOffset; i++) {
            cellDataOffset += 1 + (data[cellDataOffset] * 2);
        }
        // now the index points to the cell we need
        return cellDataOffset;
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
