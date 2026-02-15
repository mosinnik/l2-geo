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

import lombok.Data;

@Data
public class GeoConfig {

    private boolean blockStatSavingEnabled;

    private boolean reuseFlatBlockEnabled = true;
    private boolean oneHeightComplexBlockEnabled;
    private boolean fewHeightsOneNsweComplexBlockEnabled;
    private boolean fewHeightsComplexBlockEnabled;
    private boolean baseHeightComplexBlockEnabled;
    private boolean baseHeightOneNsweComplexBlockEnabled;
    private boolean noHolesMultilayerBlockEnabled;
    private boolean indexedMultilayerBlockEnabled;
    private boolean indexedMultilayer32BlockEnabled;


    public static GeoConfig maxPerfBytes() {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setOneHeightComplexBlockEnabled(true);
        geoConfig.setNoHolesMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayer32BlockEnabled(true);
        return geoConfig;
    }

    public static GeoConfig lowMemory() {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setOneHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightComplexBlockEnabled(true);
        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(true);
        geoConfig.setFewHeightsComplexBlockEnabled(true);
        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(true);
        geoConfig.setNoHolesMultilayerBlockEnabled(true);
        return geoConfig;
    }

}
