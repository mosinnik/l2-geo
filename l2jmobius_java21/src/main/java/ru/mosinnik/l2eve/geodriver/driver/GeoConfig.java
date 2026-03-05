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
    private boolean indexed32MultilayerBlockEnabled;


    public static GeoConfig maxPerfBytes() {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setOneHeightComplexBlockEnabled(true);
        geoConfig.setNoHolesMultilayerBlockEnabled(true);
        geoConfig.setIndexedMultilayerBlockEnabled(true);
        geoConfig.setIndexed32MultilayerBlockEnabled(true);
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

    public boolean isBlockStatSavingEnabled() {
        return blockStatSavingEnabled;
    }

    public void setBlockStatSavingEnabled(boolean blockStatSavingEnabled) {
        this.blockStatSavingEnabled = blockStatSavingEnabled;
    }

    public boolean isReuseFlatBlockEnabled() {
        return reuseFlatBlockEnabled;
    }

    public void setReuseFlatBlockEnabled(boolean reuseFlatBlockEnabled) {
        this.reuseFlatBlockEnabled = reuseFlatBlockEnabled;
    }

    public boolean isOneHeightComplexBlockEnabled() {
        return oneHeightComplexBlockEnabled;
    }

    public void setOneHeightComplexBlockEnabled(boolean oneHeightComplexBlockEnabled) {
        this.oneHeightComplexBlockEnabled = oneHeightComplexBlockEnabled;
    }

    public boolean isFewHeightsOneNsweComplexBlockEnabled() {
        return fewHeightsOneNsweComplexBlockEnabled;
    }

    public void setFewHeightsOneNsweComplexBlockEnabled(boolean fewHeightsOneNsweComplexBlockEnabled) {
        this.fewHeightsOneNsweComplexBlockEnabled = fewHeightsOneNsweComplexBlockEnabled;
    }

    public boolean isFewHeightsComplexBlockEnabled() {
        return fewHeightsComplexBlockEnabled;
    }

    public void setFewHeightsComplexBlockEnabled(boolean fewHeightsComplexBlockEnabled) {
        this.fewHeightsComplexBlockEnabled = fewHeightsComplexBlockEnabled;
    }

    public boolean isBaseHeightComplexBlockEnabled() {
        return baseHeightComplexBlockEnabled;
    }

    public void setBaseHeightComplexBlockEnabled(boolean baseHeightComplexBlockEnabled) {
        this.baseHeightComplexBlockEnabled = baseHeightComplexBlockEnabled;
    }

    public boolean isBaseHeightOneNsweComplexBlockEnabled() {
        return baseHeightOneNsweComplexBlockEnabled;
    }

    public void setBaseHeightOneNsweComplexBlockEnabled(boolean baseHeightOneNsweComplexBlockEnabled) {
        this.baseHeightOneNsweComplexBlockEnabled = baseHeightOneNsweComplexBlockEnabled;
    }

    public boolean isNoHolesMultilayerBlockEnabled() {
        return noHolesMultilayerBlockEnabled;
    }

    public void setNoHolesMultilayerBlockEnabled(boolean noHolesMultilayerBlockEnabled) {
        this.noHolesMultilayerBlockEnabled = noHolesMultilayerBlockEnabled;
    }

    public boolean isIndexedMultilayerBlockEnabled() {
        return indexedMultilayerBlockEnabled;
    }

    public void setIndexedMultilayerBlockEnabled(boolean indexedMultilayerBlockEnabled) {
        this.indexedMultilayerBlockEnabled = indexedMultilayerBlockEnabled;
    }

    public boolean isIndexed32MultilayerBlockEnabled() {
        return indexed32MultilayerBlockEnabled;
    }

    public void setIndexed32MultilayerBlockEnabled(boolean indexed32MultilayerBlockEnabled) {
        this.indexed32MultilayerBlockEnabled = indexed32MultilayerBlockEnabled;
    }
}
