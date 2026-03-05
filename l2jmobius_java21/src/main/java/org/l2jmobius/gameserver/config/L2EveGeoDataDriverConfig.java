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
package org.l2jmobius.gameserver.config;

import org.l2jmobius.commons.util.ConfigReader;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author mosinnnik
 */
public class L2EveGeoDataDriverConfig {
    // File
    private static final String CONFIG_FILE = "./config/L2EveGeoDataDriver.ini";

    // Constants
    public static boolean ENABLED;
    public static String DRIVER_CLASS;
    public static Path GEODATA_BIN_PATH;
    public static boolean LOAD_BYTES_FROM_L2J;
    public static boolean GENERATE_BIN_FROM_L2J;

    // GeoConfig flags
    public static boolean REUSE_FLAT_BLOCK_ENABLED;
    public static boolean ONE_HEIGHT_COMPLEX_BLOCK_ENABLED;
    public static boolean FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK_ENABLED;
    public static boolean FEW_HEIGHTS_COMPLEX_BLOCK_ENABLED;
    public static boolean BASE_HEIGHT_COMPLEX_BLOCK_ENABLED;
    public static boolean BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK_ENABLED;
    public static boolean NO_HOLES_MULTILAYER_BLOCK_ENABLED;
    public static boolean INDEXED_MULTILAYER_BLOCK_ENABLED;
    public static boolean INDEXED_32_MULTILAYER_BLOCK_ENABLED;


    public static void load() {
        final ConfigReader config = new ConfigReader(CONFIG_FILE);

        ENABLED = config.getBoolean("L2EveGeoDataDriverEnabled", false);
        DRIVER_CLASS = config.getString("L2EveGeoDataDriverClass", "ru.mosinnik.l2eve.geodriver.driver.GeoDriver");
        GEODATA_BIN_PATH = Paths.get(ServerConfig.DATAPACK_ROOT.getPath() + "/" + config.getString("GeoDataBinPath", "geodata_bin"));
        LOAD_BYTES_FROM_L2J = config.getBoolean("LoadBytesFromL2J", true);
        GENERATE_BIN_FROM_L2J = config.getBoolean("GenerateBinFromL2J", false);

        // GeoConfig flags
        REUSE_FLAT_BLOCK_ENABLED = config.getBoolean("REUSE_FLAT_BLOCK_ENABLED", true);
        ONE_HEIGHT_COMPLEX_BLOCK_ENABLED = config.getBoolean("ONE_HEIGHT_COMPLEX_BLOCK_ENABLED", true);
        FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK_ENABLED = config.getBoolean("FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK_ENABLED", false);
        FEW_HEIGHTS_COMPLEX_BLOCK_ENABLED = config.getBoolean("FEW_HEIGHTS_COMPLEX_BLOCK_ENABLED", false);
        BASE_HEIGHT_COMPLEX_BLOCK_ENABLED = config.getBoolean("BASE_HEIGHT_COMPLEX_BLOCK_ENABLED", false);
        BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK_ENABLED = config.getBoolean("BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK_ENABLED", false);
        NO_HOLES_MULTILAYER_BLOCK_ENABLED = config.getBoolean("NO_HOLES_MULTILAYER_BLOCK_ENABLED", true);
        INDEXED_MULTILAYER_BLOCK_ENABLED = config.getBoolean("INDEXED_MULTILAYER_BLOCK_ENABLED", true);
        INDEXED_32_MULTILAYER_BLOCK_ENABLED = config.getBoolean("INDEXED_32_MULTILAYER_BLOCK_ENABLED", true);

    }
}
