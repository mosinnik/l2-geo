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

package ru;

import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.config.L2EveGeoDataDriverConfig;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.openjdk.jol.info.GraphLayout;

public class Runner {
    public static void main(String[] args) {
        GeoEngineConfig.load();
        L2EveGeoDataDriverConfig.load();

        GeoEngine e = GeoEngine.getInstance();

        GraphLayout graphLayout = GraphLayout.parseInstance(e);

        System.out.println("footprint = " + graphLayout.toFootprint());
        System.out.println("totalCount = " + graphLayout.totalCount());
        System.out.println("totalSize = " + graphLayout.totalSize());

        int geoX = GeoEngine.getGeoX(0);
        int geoY = GeoEngine.getGeoY(0);
        int nextLowerZ = e.getNextLowerZ(geoX, geoY, 0);
        int nearestZ = e.getNearestZ(geoX, geoY, 0);
        int nextHigherZ = e.getNextHigherZ(geoX, geoY, 0);
        System.out.println("----------------------------------------");
        System.out.println("nextLowerZ  = " + nextLowerZ); // for default: -4672
        System.out.println("nearestZ    = " + nearestZ); // for default: -4672
        System.out.println("nextHigherZ = " + nextHigherZ); // for default: 0
    }
}
