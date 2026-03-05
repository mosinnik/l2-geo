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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockStat {
    public int min = Integer.MAX_VALUE;
    public int max = Integer.MIN_VALUE;
    public IBlock block;
    public final Set<Integer> heights = new TreeSet<>();
    public final Set<Integer> nswes = new TreeSet<>();

    /**
     * Содержит в себе количества слоев в cell-столбах для мультилеейр блоков.
     * Необходимо для понимания есть ли "дырки", где слои не полностью перекрываются (NoHolesMulti)
     * Например мультелеер блок состоит из 3 полных слоев (по 64 ячейки) и например там дерево
     * с доп двумя слоями, частично перекрываемые.
     * Тогда в layers будут лежать 3, 4 и 5, т.к. в cell-столбах будет в общем по 3, но будут еще
     * столбы где 4 и 5 слоев.
     */
    public final Set<Byte> layers = new TreeSet<>();

    /**
     * Надо понять как много слоев бывает в мультилеер блоках в cell-столбах.
     * Считаем количество слоев в cell-столбах.
     * Мапа кол-во слоев -> счетчик
     */
    public final Map<Byte, AtomicInteger> cellLayersCounter = new HashMap<>();


    public int delta() {
        if (max == Integer.MIN_VALUE) {
            return -1;
        }
        return max - min;
    }

    public void addCellLayerNumber(Byte layersNumber) {
        cellLayersCounter.computeIfAbsent(layersNumber, aByte -> new AtomicInteger()).incrementAndGet();
    }

    @Override
    public String toString() {
        return "MinMax{" +
            "min=" + min +
            ", max=" + max +
            ", delta=" + delta() +
            ", heights.size=" + heights.size() +
            ", nswes.size=" + nswes.size() +
            '}';
    }
}
