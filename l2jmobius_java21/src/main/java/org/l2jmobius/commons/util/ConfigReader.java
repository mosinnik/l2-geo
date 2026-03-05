/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.commons.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * NOTE: removed all unused for compilation reasons
 * <p>
 * ConfigReader is a utility class that reads and provides access to configuration properties from a file.
 *
 * @author Mobius
 */
public class ConfigReader {
    private static final Logger LOGGER = Logger.getLogger(ConfigReader.class.getName());

    private final Properties _properties = new Properties();
    private final File _file;

    /**
     * Constructs a ConfigReader with the specified file path using the system's default Charset.
     *
     * @param filePath the path to the configuration file
     */
    public ConfigReader(String filePath) {
        _file = new File(filePath);

        if (!Files.exists(_file.toPath())) {
            LOGGER.warning("Configuration file not found: " + _file.getAbsolutePath());
            return;
        }

        try (InputStream input = Files.newInputStream(_file.toPath());
             InputStreamReader reader = new InputStreamReader(input, Charset.defaultCharset())) {
            _properties.load(reader);
        } catch (IOException e) {
            LOGGER.warning("Failed to load configurations from " + _file.getName() + ": " + e.getMessage());
        }
    }


    /**
     * Retrieves the value associated with the specified key as a boolean.
     *
     * @param config       the configuration key
     * @param defaultValue the default value if the key does not exist or is malformed
     * @return the property value as a boolean, or the default value if the key does not exist or is malformed
     */
    public boolean getBoolean(String config, boolean defaultValue) {
        final String value = _properties.getProperty(config);
        if (value != null) {
            try {
                return Boolean.parseBoolean(value);
            } catch (Exception e) {
                LOGGER.warning("Invalid boolean for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
            }
        } else {
            LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
        }

        return defaultValue;
    }


    /**
     * Retrieves the value associated with the specified key as an int.
     *
     * @param config       the configuration key
     * @param defaultValue the default value if the key does not exist or is malformed
     * @return the property value as an int, or the default value if the key does not exist or is malformed
     */
    public int getInt(String config, int defaultValue) {
        final String value = _properties.getProperty(config);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                LOGGER.warning("Invalid int for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
            }
        } else {
            LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
        }

        return defaultValue;
    }

    /**
     * Retrieves the value associated with the specified key as a float.
     *
     * @param config       the configuration key
     * @param defaultValue the default value if the key does not exist or is malformed
     * @return the property value as a float, or the default value if the key does not exist or is malformed
     */
    public float getFloat(String config, float defaultValue) {
        final String value = _properties.getProperty(config);
        if (value != null) {
            try {
                return Float.parseFloat(value);
            } catch (Exception e) {
                LOGGER.warning("Invalid float for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
            }
        } else {
            LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
        }

        return defaultValue;
    }

    /**
     * Retrieves the value associated with the specified key as a String.
     *
     * @param config       the configuration key
     * @param defaultValue the default value if the key does not exist
     * @return the property value as a String, or the default value if the key does not exist
     */
    public String getString(String config, String defaultValue) {
        final String value = _properties.getProperty(config);
        if (value == null) {
            LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
            return defaultValue;
        }

        return value;
    }

}
