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
import ru.mosinnik.l2eve.geodriver.util.BlockStat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Для большого количества мультилейр блоков характерна ситуация, когда есть всего N слоев
 * без дырок, т.е. у нас всегда на каждую ячейку в пределах блока одинаковое количество слоев.
 * Поэтому можно вынести это самое количество слоев в переменную и пересобрать дату.
 * Так же за счет того что у нас фиксированное количество слоев, то легко вычисляется оффсет начала
 * цепочки высот, относящейся к ячейке.
 * На всем объеме геодаты экономия ~33Мб + ускорение на работу с данными х2.6
 */
public class NoHolesMultilayerBlock implements IBlock {
    public final short[] data;
    public final byte layersCount;


    public NoHolesMultilayerBlock(byte[] tmpData, BlockStat stat) {
        if (stat.layers.size() != 1) {
            throw new RuntimeException("MultilayerBlock with holes");
        }
        layersCount = stat.layers.stream().findFirst().orElseThrow();

        data = new short[layersCount * IBlock.BLOCK_CELLS];

        ByteBuffer buffer = ByteBuffer.wrap(tmpData).order(ByteOrder.LITTLE_ENDIAN);

        int dataOffset = 0;
        for (int blockCellOffset = 0; blockCellOffset < IBlock.BLOCK_CELLS; blockCellOffset++) {
            byte nLayer = buffer.get();
            if (nLayer != layersCount) {
                throw new RuntimeException("Different layer count " + blockCellOffset);
            }
            for (int i = 0; i < layersCount; i++) {
                data[dataOffset] = buffer.getShort();
                dataOffset++;
            }
        }
    }

    public short[] getData() {
        return data;
    }

    public byte getLayersCount() {
        return layersCount;
    }

    private short getNearestLayer(int geoX, int geoY, int worldZ) {
        int startOffset = layersCount * (((geoX & 0x07) << 3) + (geoY & 0x07));

        int nearestDZ = 0;
        short nearestData = 0;
        for (int i = 0; i < layersCount; i++) {
            short layerData = data[startOffset + i];
            int layerZ = extractLayerHeight(layerData);
            if (layerZ == worldZ) {
                return layerData; // exact z
            }

            int layerDZ = Math.abs(layerZ - worldZ);
            if (i == 0 || (layerDZ < nearestDZ)) {
                nearestDZ = layerDZ;
                nearestData = layerData;
            } else {
                return nearestData;
            }
        }

        return nearestData;
    }

    private int getNearestNSWE(int geoX, int geoY, int worldZ) {
        return extractLayerNswe(getNearestLayer(geoX, geoY, worldZ));
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
        int startOffset = layersCount * (((geoX & 0x07) << 3) + (geoY & 0x07));

        for (int i = 0; i < layersCount; i++) {
            short layerData = data[startOffset + i];
            int layerZ = extractLayerHeight(layerData);
            if (layerZ <= worldZ) {
                return layerZ;
            }
        }
        return worldZ;
    }

    @Override
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        int startOffset = layersCount * (((geoX & 0x07) << 3) + (geoY & 0x07));

        int prevLayerZ = worldZ;
        for (int i = 0; i < layersCount; i++) {
            short layerData = data[startOffset + i];
            int layerZ = extractLayerHeight(layerData);
            if (layerZ < worldZ) {
                return prevLayerZ;
            }
            prevLayerZ = layerZ;
        }
        return prevLayerZ;
    }
}
