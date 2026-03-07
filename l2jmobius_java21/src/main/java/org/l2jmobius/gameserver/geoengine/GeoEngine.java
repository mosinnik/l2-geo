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
package org.l2jmobius.gameserver.geoengine;

import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.config.L2EveGeoDataDriverConfig;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.geoengine.geodata.Cell;
import org.l2jmobius.gameserver.geoengine.geodata.IRegion;
import org.l2jmobius.gameserver.geoengine.geodata.regions.NullRegion;
import org.l2jmobius.gameserver.geoengine.geodata.regions.Region;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator2D;
import org.l2jmobius.gameserver.geoengine.util.GridLineIterator3D;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.interfaces.ILocational;
import org.l2jmobius.gameserver.util.GeoUtils;
import ru.mosinnik.l2eve.geodriver.abstraction.IGeoDriver;
import ru.mosinnik.l2eve.geodriver.driver.GeoConfig;
import ru.mosinnik.l2eve.geodriver.driver.GeoDriver;
import ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes;
import ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesMmap;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The {@code GeoEngine} class is responsible for managing geospatial data used in the game.<br>
 * It handles geodata loading, movement validation, line-of-sight (LOS) checks and other spatial calculations related to the game's 3D world.
 * <p>
 * Key functionalities include:
 * <ul>
 * <li>Loading geodata files for specific regions.</li>
 * <li>Converting between world and geodata coordinates.</li>
 * <li>Validating movement paths and detecting obstacles.</li>
 * <li>Checking line-of-sight (LOS) between two positions.</li>
 * <li>Retrieving terrain height and handling elevation-related calculations.</li>
 * </ul>
 *
 * @author -Nemesiss-, HorridoJoho, Mobius
 */
public class GeoEngine {
    private static final Logger LOGGER = Logger.getLogger(GeoEngine.class.getName());

    // Constants.
    public static final String FILE_NAME_FORMAT = "%d_%d.l2j";
    private static final int ELEVATED_SEE_OVER_DISTANCE = 2;
    private static final int MAX_SEE_OVER_HEIGHT = 48;
    private static final int SPAWN_Z_DELTA_LIMIT = 100;
    private static final int WORLD_MIN_X = -655360;
    private static final int WORLD_MIN_Y = -589824;
    private static final int GEO_REGIONS_X = 32;
    private static final int GEO_REGIONS_Y = 32;
    private static final int GEO_REGIONS = GEO_REGIONS_X * GEO_REGIONS_Y;
    private static final int COORDINATE_SCALE = 16;
    private static final int COORDINATE_OFFSET = 8;
    private static final int HEIGHT_INCREASE_LIMIT = 40;
    private static final int SPAWN_HEIGHT_OFFSET = 20;

    // Region Management.
    private static final AtomicReferenceArray<IRegion> REGIONS = new AtomicReferenceArray<>(GEO_REGIONS);

    private final IGeoDriver geoDriver;

    protected GeoEngine() {
        // Initially set all regions to NullRegion.
        for (int i = 0; i < GEO_REGIONS; i++) {
            REGIONS.set(i, NullRegion.INSTANCE);
        }

        int loadedRegions = 0;
        if (L2EveGeoDataDriverConfig.ENABLED) {
            geoDriver = loadL2EveDriver();
            // just to pass PATHFINDING check
            loadedRegions = 1;
        } else {
            geoDriver = null;
            loadedRegions = loadRegionsDefault(loadedRegions);
        }

        // Avoid wrong configuration when no files are loaded.
        if ((loadedRegions == 0) && (GeoEngineConfig.PATHFINDING > 0)) {
            GeoEngineConfig.PATHFINDING = 0;
            LOGGER.info(getClass().getSimpleName() + ": Pathfinding is disabled.");
        }
    }

    private int loadRegionsDefault(int loadedRegions) {
        try {
            for (int regionX = World.TILE_X_MIN; regionX <= World.TILE_X_MAX; regionX++) {
                for (int regionY = World.TILE_Y_MIN; regionY <= World.TILE_Y_MAX; regionY++) {
                    final Path geoFilePath = GeoEngineConfig.GEODATA_PATH.resolve(String.format(FILE_NAME_FORMAT, regionX, regionY));
                    if (Files.exists(geoFilePath)) {
                        try {
                            // LOGGER.info(getClass().getSimpleName() + ": Loading " + geoFilePath.getFileName() + "...");
                            loadRegion(geoFilePath, regionX, regionY);
                            loadedRegions++;
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to load " + geoFilePath.getFileName() + "!", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Critical error during geodata initialization!", e);
            System.exit(1);
        }

        LOGGER.info(getClass().getSimpleName() + ": Loaded " + loadedRegions + " regions.");
        return loadedRegions;
    }


    private IGeoDriver loadL2EveDriver() {
        GeoConfig geoConfig = new GeoConfig();
        geoConfig.setReuseFlatBlockEnabled(L2EveGeoDataDriverConfig.REUSE_FLAT_BLOCK_ENABLED);
        geoConfig.setOneHeightComplexBlockEnabled(L2EveGeoDataDriverConfig.ONE_HEIGHT_COMPLEX_BLOCK_ENABLED);
        geoConfig.setBaseHeightComplexBlockEnabled(L2EveGeoDataDriverConfig.FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK_ENABLED);
        geoConfig.setBaseHeightOneNsweComplexBlockEnabled(L2EveGeoDataDriverConfig.FEW_HEIGHTS_COMPLEX_BLOCK_ENABLED);
        geoConfig.setFewHeightsComplexBlockEnabled(L2EveGeoDataDriverConfig.BASE_HEIGHT_COMPLEX_BLOCK_ENABLED);
        geoConfig.setFewHeightsOneNsweComplexBlockEnabled(L2EveGeoDataDriverConfig.BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK_ENABLED);
        geoConfig.setNoHolesMultilayerBlockEnabled(L2EveGeoDataDriverConfig.NO_HOLES_MULTILAYER_BLOCK_ENABLED);
        geoConfig.setIndexedMultilayerBlockEnabled(L2EveGeoDataDriverConfig.INDEXED_MULTILAYER_BLOCK_ENABLED);
        geoConfig.setIndexed32MultilayerBlockEnabled(L2EveGeoDataDriverConfig.INDEXED_32_MULTILAYER_BLOCK_ENABLED);

        String driverClass = L2EveGeoDataDriverConfig.DRIVER_CLASS;
        try {
            LOGGER.info(getClass().getSimpleName() + ": Load L2Eve driver " + driverClass);
            if (driverClass.equals(GeoDriver.class.getCanonicalName())) {
                GeoDriver geoDriverT = new GeoDriver(geoConfig);
                for (int regionX = World.TILE_X_MIN; regionX <= World.TILE_X_MAX; regionX++) {
                    for (int regionY = World.TILE_Y_MIN; regionY <= World.TILE_Y_MAX; regionY++) {
                        final Path geoFilePath = GeoEngineConfig.GEODATA_PATH.resolve(String.format(FILE_NAME_FORMAT, regionX, regionY));
                        if (Files.exists(geoFilePath)) {
                            try {
                                geoDriverT.loadRegion(geoFilePath, regionX, regionY);
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to load " + geoFilePath.getFileName() + "!", e);
                            }
                        }
                    }
                }
                return geoDriverT;
            } else if (driverClass.equals(GeoDriverBytes.class.getCanonicalName())) {
                GeoDriverBytes geoDriverBytes = new GeoDriverBytes(geoConfig);
                if (L2EveGeoDataDriverConfig.LOAD_BYTES_FROM_L2J) {
                    geoDriverBytes.loadL2J(GeoEngineConfig.GEODATA_PATH);
                    if (L2EveGeoDataDriverConfig.GENERATE_BIN_FROM_L2J) {
                        geoDriverBytes.writeToFiles(L2EveGeoDataDriverConfig.GEODATA_BIN_PATH);
                    }
                } else {
                    geoDriverBytes.loadBin(L2EveGeoDataDriverConfig.GEODATA_BIN_PATH);
                }
                geoDriverBytes.printStats();
                return geoDriverBytes;
            } else if (driverClass.equals(GeoDriverBytesMmap.class.getCanonicalName())) {
                GeoDriverBytesMmap geoDriverBytesMmap = new GeoDriverBytesMmap();
                geoDriverBytesMmap.loadBin(L2EveGeoDataDriverConfig.GEODATA_BIN_PATH);
                return geoDriverBytesMmap;
            } else {
                throw new RuntimeException("Unknown Geodata driver class ' " + driverClass + "'");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Failed to load geodata driver!", ex);
            System.exit(1);
            return null;
        }
    }

    /**
     * Loads geodata for a specific region from the provided file path.
     *
     * @param filePath The path to the geodata file
     * @param regionX  The X coordinate of the region
     * @param regionY  The Y coordinate of the region
     * @throws IOException if an error occurs while reading the file
     */
    private void loadRegion(Path filePath, int regionX, int regionY) throws IOException {
        final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(filePath.toFile(), "r")) {
            REGIONS.set(regionOffset, new Region(randomAccessFile.getChannel().map(MapMode.READ_ONLY, 0, randomAccessFile.length()).order(ByteOrder.LITTLE_ENDIAN)));
        }
    }

    /**
     * Reloads a specific geodata region from file.
     *
     * @param regionX The X coordinate of the region
     * @param regionY The Y coordinate of the region
     * @return true if reloaded successfully, false otherwise
     */
    public boolean reloadRegion(int regionX, int regionY) {
        if (Objects.nonNull(geoDriver)) {
            LOGGER.warning(getClass().getSimpleName() + ": Cannot reload for L2EveGeoDataDriver");
            return false;
        }

        final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;
        final Path geoFilePath = GeoEngineConfig.GEODATA_PATH.resolve(String.format(FILE_NAME_FORMAT, regionX, regionY));
        if (!Files.exists(geoFilePath)) {
            LOGGER.warning(getClass().getSimpleName() + ": Cannot reload, file not found for region " + regionX + "_" + regionY);
            return false;
        }

        try {
            final IRegion region = REGIONS.get(regionOffset);
            if (region instanceof Region) {
                final ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(geoFilePath)).order(ByteOrder.LITTLE_ENDIAN);
                ((Region) region).load(buffer);
                LOGGER.info(getClass().getSimpleName() + ": Reloaded region " + regionX + "_" + regionY + " from bytes.");
                return true;
            }

            // Not a real region? fallback load.
            loadRegion(geoFilePath, regionX, regionY);
            LOGGER.info(getClass().getSimpleName() + ": Replaced NullRegion with new region " + regionX + "_" + regionY);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to reload region " + regionX + "_" + regionY + "!", e);
            return false;
        }
    }

    /**
     * Sets a geodata region at the specified coordinates.
     *
     * @param regionX The X coordinate of the region
     * @param regionY The Y coordinate of the region
     * @param region  The geodata region to set
     */
    public void setRegion(int regionX, int regionY, Region region) {
        if (Objects.nonNull(geoDriver)) {
            LOGGER.warning(getClass().getSimpleName() + ": Cannot setRegion for L2EveGeoDataDriver");
            return;
        }

        final int regionOffset = (regionX * GEO_REGIONS_Y) + regionY;
        REGIONS.set(regionOffset, region);
    }

    /**
     * Retrieves the geodata region for the specified coordinates.
     *
     * @param geoX The geodata X coordinate
     * @param geoY The geodata Y coordinate
     * @return the geodata region
     */
    public IRegion getRegion(int geoX, int geoY) {
        if (Objects.nonNull(geoDriver)) {
            LOGGER.warning(getClass().getSimpleName() + ": Cannot getRegion for L2EveGeoDataDriver");
            return null;
        }

        return REGIONS.get(((geoX / IRegion.REGION_CELLS_X) * GEO_REGIONS_Y) + (geoY / IRegion.REGION_CELLS_Y));
    }

    /**
     * Checks if geodata is available at the specified coordinates.
     *
     * @param geoX The geodata X coordinate
     * @param geoY The geodata Y coordinate
     * @return {@code true} if geodata is available, {@code false} otherwise
     */
    public boolean hasGeoPos(int geoX, int geoY) {
        if (Objects.nonNull(geoDriver)) {
            return geoDriver.hasGeoPos(geoX, geoY);
        }
        return getRegion(geoX, geoY).hasGeo();
    }

    /**
     * Checks if movement is possible in the specified direction (NSWE) from the given geodata coordinates and height.
     *
     * @param geoX   The geodata X coordinate
     * @param geoY   The geodata Y coordinate
     * @param worldZ The world Z coordinate
     * @param nswe   The direction to check
     * @return {@code true} if movement is possible, {@code false} otherwise
     */
    public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe) {
        if (Objects.nonNull(geoDriver)) {
            return geoDriver.checkNearestNSWE(geoX, geoY, worldZ, (byte) nswe);
        }
        return getRegion(geoX, geoY).checkNearestNswe(geoX, geoY, worldZ, nswe);
    }

    /**
     * Checks if movement is possible in the specified direction (NSWE) from the given geodata coordinates and height, considering anti-corner-cut logic.
     *
     * @param geoX   The geodata X coordinate
     * @param geoY   The geodata Y coordinate
     * @param worldZ The world Z coordinate
     * @param nswe   The direction to check
     * @return {@code true} if movement is possible, {@code false} otherwise
     */
    public boolean checkNearestNsweAntiCornerCut(int geoX, int geoY, int worldZ, int nswe) {
        boolean canMove = true;

        if (Objects.nonNull(geoDriver)) {
            if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST) {
                canMove = geoDriver.checkNearestNSWE(geoX, geoY - 1, worldZ, Cell.NSWE_EAST) && geoDriver.checkNearestNSWE(geoX + 1, geoY, worldZ, Cell.NSWE_NORTH);
            }

            if (canMove && ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST)) {
                canMove = geoDriver.checkNearestNSWE(geoX, geoY - 1, worldZ, Cell.NSWE_WEST) && geoDriver.checkNearestNSWE(geoX - 1, geoY, worldZ, Cell.NSWE_NORTH);
            }

            if (canMove && ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST)) {
                canMove = geoDriver.checkNearestNSWE(geoX, geoY + 1, worldZ, Cell.NSWE_EAST) && geoDriver.checkNearestNSWE(geoX + 1, geoY, worldZ, Cell.NSWE_SOUTH);
            }

            if (canMove && ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST)) {
                canMove = geoDriver.checkNearestNSWE(geoX, geoY + 1, worldZ, Cell.NSWE_WEST) && geoDriver.checkNearestNSWE(geoX - 1, geoY, worldZ, Cell.NSWE_SOUTH);
            }

            return canMove && geoDriver.checkNearestNSWE(geoX, geoY, worldZ, (byte) nswe);
        }

        final IRegion region = getRegion(geoX, geoY);
        if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST) {
            canMove = region.checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_EAST) && region.checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_NORTH);
        }

        if (canMove && ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST)) {
            canMove = region.checkNearestNswe(geoX, geoY - 1, worldZ, Cell.NSWE_WEST) && region.checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_NORTH);
        }

        if (canMove && ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST)) {
            canMove = region.checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_EAST) && region.checkNearestNswe(geoX + 1, geoY, worldZ, Cell.NSWE_SOUTH);
        }

        if (canMove && ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST)) {
            canMove = region.checkNearestNswe(geoX, geoY + 1, worldZ, Cell.NSWE_WEST) && region.checkNearestNswe(geoX - 1, geoY, worldZ, Cell.NSWE_SOUTH);
        }

        return canMove && region.checkNearestNswe(geoX, geoY, worldZ, nswe);
    }

    /**
     * Sets the nearest NSWE data at the specified coordinates and height.
     *
     * @param geoX   The geodata X coordinate
     * @param geoY   The geodata Y coordinate
     * @param worldZ The world Z coordinate
     * @param nswe   The direction data to set
     */
    public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
        if (Objects.nonNull(geoDriver)) {
            LOGGER.warning(getClass().getSimpleName() + ": Cannot setNearestNswe for L2EveGeoDataDriver");
            return;
        }

        getRegion(geoX, geoY).setNearestNswe(geoX, geoY, worldZ, nswe);
    }

    /**
     * Removes the nearest NSWE data at the specified coordinates and height.
     *
     * @param geoX   The geodata X coordinate
     * @param geoY   The geodata Y coordinate
     * @param worldZ The world Z coordinate
     * @param nswe   The direction data to remove
     */
    public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe) {
        if (Objects.nonNull(geoDriver)) {
            LOGGER.warning(getClass().getSimpleName() + ": Cannot unsetNearestNswe for L2EveGeoDataDriver");
            return;
        }

        getRegion(geoX, geoY).unsetNearestNswe(geoX, geoY, worldZ, nswe);
    }

    /**
     * Retrieves the nearest Z coordinate at the specified geodata coordinates and height.
     *
     * @param geoX   The geodata X coordinate
     * @param geoY   The geodata Y coordinate
     * @param worldZ The world Z coordinate
     * @return the nearest Z coordinate
     */
    public int getNearestZ(int geoX, int geoY, int worldZ) {
        if (Objects.nonNull(geoDriver)) {
            return geoDriver.getNearestZ(geoX, geoY, worldZ);
        }

        return getRegion(geoX, geoY).getNearestZ(geoX, geoY, worldZ);
    }

    /**
     * Retrieves the next lower Z coordinate at the specified geodata coordinates and height.
     *
     * @param geoX   The geodata X coordinate
     * @param geoY   The geodata Y coordinate
     * @param worldZ The world Z coordinate
     * @return the next lower Z coordinate
     */
    public int getNextLowerZ(int geoX, int geoY, int worldZ) {
        if (Objects.nonNull(geoDriver)) {
            return geoDriver.getNextLowerZ(geoX, geoY, worldZ);
        }

        return getRegion(geoX, geoY).getNextLowerZ(geoX, geoY, worldZ);
    }

    /**
     * Retrieves the next higher Z coordinate at the specified geodata coordinates and height.
     *
     * @param geoX   The geodata X coordinate
     * @param geoY   The geodata Y coordinate
     * @param worldZ The world Z coordinate
     * @return the next higher Z coordinate
     */
    public int getNextHigherZ(int geoX, int geoY, int worldZ) {
        if (Objects.nonNull(geoDriver)) {
            return geoDriver.getNextHigherZ(geoX, geoY, worldZ);
        }

        return getRegion(geoX, geoY).getNextHigherZ(geoX, geoY, worldZ);
    }

    /**
     * Converts a world X coordinate to a geodata X coordinate.
     *
     * @param worldX The world X coordinate
     * @return the corresponding geodata X coordinate
     */
    public static int getGeoX(int worldX) {
        return (worldX - WORLD_MIN_X) / COORDINATE_SCALE;
    }

    /**
     * Converts a world Y coordinate to a geodata Y coordinate.
     *
     * @param worldY The world Y coordinate
     * @return the corresponding geodata Y coordinate
     */
    public static int getGeoY(int worldY) {
        return (worldY - WORLD_MIN_Y) / COORDINATE_SCALE;
    }

    /**
     * Converts a geodata X coordinate to a world X coordinate.
     *
     * @param geoX The geodata X coordinate
     * @return the corresponding world X coordinate
     */
    public static int getWorldX(int geoX) {
        return (geoX * COORDINATE_SCALE) + WORLD_MIN_X + COORDINATE_OFFSET;
    }

    /**
     * Converts a geodata Y coordinate to a world Y coordinate.
     *
     * @param geoY The geodata Y coordinate
     * @return the corresponding world Y coordinate
     */
    public static int getWorldY(int geoY) {
        return (geoY * COORDINATE_SCALE) + WORLD_MIN_Y + COORDINATE_OFFSET;
    }

    /**
     * Gets the height at the specified coordinates.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return the height
     */
    public int getHeight(int x, int y, int z) {
        return getNearestZ(getGeoX(x), getGeoY(y), z);
    }

    /**
     * Gets the spawn height at the specified coordinates.<br>
     * Calculates appropriate spawn position based on terrain height and spawn limits.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param z The z coordinate
     * @return the spawn height
     */
    public int getSpawnHeight(int x, int y, int z) {
        final int geoX = getGeoX(x);
        final int geoY = getGeoY(y);

        if (!hasGeoPos(geoX, geoY)) {
            return z;
        }

        final int nextLowerZ = getNextLowerZ(geoX, geoY, z + SPAWN_HEIGHT_OFFSET);
        return Math.abs(nextLowerZ - z) <= SPAWN_Z_DELTA_LIMIT ? nextLowerZ : z;
    }

    /**
     * Gets the spawn height at the specified location.
     *
     * @param location The location
     * @return the spawn height
     */
    public int getSpawnHeight(Location location) {
        return getSpawnHeight(location.getX(), location.getY(), location.getZ());
    }

    /**
     * Checks if creature can see target.<br>
     * Doors as target always return true and checks doors between positions.
     *
     * @param creature The creature
     * @param target   The target
     * @return {@code true} if the creature can see the target (LOS), {@code false} otherwise
     */
    public boolean canSeeTarget(WorldObject creature, WorldObject target) {
        return (target != null) && (target.isDoor() || target.isArtefact() || canSeeTarget(creature.getX(), creature.getY(), creature.getZ(), creature.getInstanceWorld(), target.getX(), target.getY(), target.getZ(), target.getInstanceWorld()));
    }

    /**
     * Checks if creature can see target at the given world position.
     *
     * @param creature      The creature
     * @param worldPosition The world position
     * @return {@code true} if the creature can see the target at the given world position, {@code false} otherwise
     */
    public boolean canSeeTarget(WorldObject creature, ILocational worldPosition) {
        return canSeeTarget(creature.getX(), creature.getY(), creature.getZ(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), creature.getInstanceWorld());
    }

    /**
     * Checks if position can see target position with instance validation.
     *
     * @param x              The x coordinate
     * @param y              The y coordinate
     * @param z              The z coordinate
     * @param instance       The instance
     * @param targetX        The target's x coordinate
     * @param targetY        The target's y coordinate
     * @param targetZ        The target's z coordinate
     * @param targetInstance The target's instance
     * @return true if there is line of sight between positions in same instance, false otherwise
     */
    public boolean canSeeTarget(int x, int y, int z, Instance instance, int targetX, int targetY, int targetZ, Instance targetInstance) {
        return (instance == targetInstance) && canSeeTarget(x, y, z, targetX, targetY, targetZ, instance);
    }

    /**
     * Checks if position can see target position with door and fence validation.
     *
     * @param x        The x coordinate
     * @param y        The y coordinate
     * @param z        The z coordinate
     * @param targetX  The target's x coordinate
     * @param targetY  The target's y coordinate
     * @param targetZ  The target's z coordinate
     * @param instance The instance
     * @return {@code true} if there is line of sight between the given coordinate sets, {@code false} otherwise
     */
    public boolean canSeeTarget(int x, int y, int z, int targetX, int targetY, int targetZ, Instance instance) {
        // Door checks.
        if (DoorData.getInstance().checkIfDoorsBetween(x, y, z, targetX, targetY, targetZ, instance, true)) {
            return false;
        }

        // Fence checks.
        if (FenceData.getInstance().checkIfFenceBetween(x, y, z, targetX, targetY, targetZ, instance)) {
            return false;
        }

        return canSeeTarget(x, y, z, targetX, targetY, targetZ);
    }

    /**
     * Calculates line-of-sight Z coordinate for movement validation.
     *
     * @param previousX    The previous X coordinate
     * @param previousY    The previous Y coordinate
     * @param previousGeoZ The previous geo Z coordinate
     * @param currentX     The current X coordinate
     * @param currentY     The current Y coordinate
     * @param nswe         The movement direction
     * @return the calculated geo Z coordinate
     */
    private int getLosGeoZ(int previousX, int previousY, int previousGeoZ, int currentX, int currentY, int nswe) {
        if ((((nswe & Cell.NSWE_NORTH) != 0) && ((nswe & Cell.NSWE_SOUTH) != 0)) || (((nswe & Cell.NSWE_WEST) != 0) && ((nswe & Cell.NSWE_EAST) != 0))) {
            throw new RuntimeException("Multiple directions specified in NSWE: " + nswe);
        }

        return checkNearestNsweAntiCornerCut(previousX, previousY, previousGeoZ, nswe) ? getNearestZ(currentX, currentY, previousGeoZ) : getNextHigherZ(currentX, currentY, previousGeoZ);
    }

    /**
     * Checks if position can see target position without door and fence validation.
     *
     * @param x       The x coordinate
     * @param y       The y coordinate
     * @param z       The z coordinate
     * @param targetX The target's x coordinate
     * @param targetY The target's y coordinate
     * @param targetZ The target's z coordinate
     * @return {@code true} if there is line of sight between the given coordinate sets, {@code false} otherwise
     */
    public boolean canSeeTarget(int x, int y, int z, int targetX, int targetY, int targetZ) {
        int geoX = getGeoX(x);
        int geoY = getGeoY(y);
        int targetGeoX = getGeoX(targetX);
        int targetGeoY = getGeoY(targetY);

        int nearestFromZ = getNearestZ(geoX, geoY, z);
        int nearestToZ = getNearestZ(targetGeoX, targetGeoY, targetZ);

        // Fastpath for same coordinates.
        if ((geoX == targetGeoX) && (geoY == targetGeoY)) {
            return !hasGeoPos(targetGeoX, targetGeoY) || (nearestFromZ == nearestToZ);
        }

        int fromX = targetX;
        int fromY = targetY;
        int toX = targetX;
        int toY = targetY;
        if (nearestToZ > nearestFromZ) {
            int temp = toX;
            toX = fromX;
            fromX = temp;

            temp = toY;
            toY = fromY;
            fromY = temp;

            temp = nearestToZ;
            nearestToZ = nearestFromZ;
            nearestFromZ = temp;

            temp = targetGeoX;
            targetGeoX = geoX;
            geoX = temp;

            temp = targetGeoY;
            targetGeoY = geoY;
            geoY = temp;
        }

        final GridLineIterator3D pointIterator = new GridLineIterator3D(geoX, geoY, nearestFromZ, targetGeoX, targetGeoY, nearestToZ);

        // First point is guaranteed to be available, skip it, we can always see our own position.
        pointIterator.next();
        int previousX = pointIterator.x();
        int previousY = pointIterator.y();
        int previousGeoZ = pointIterator.z();
        int pointIndex = 0;
        while (pointIterator.next()) {
            final int currentX = pointIterator.x();
            final int currentY = pointIterator.y();

            if ((currentX == previousX) && (currentY == previousY)) {
                continue;
            }

            final int beeCurrentZ = pointIterator.z();
            int currentGeoZ = previousGeoZ;

            // Check if the position has geodata.
            if (hasGeoPos(currentX, currentY)) {
                final int nswe = GeoUtils.computeNswe(previousX, previousY, currentX, currentY);
                currentGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, currentX, currentY, nswe);
                final int maxHeight = pointIndex < ELEVATED_SEE_OVER_DISTANCE ? nearestFromZ + MAX_SEE_OVER_HEIGHT : beeCurrentZ + MAX_SEE_OVER_HEIGHT;
                boolean canSeeThrough = false;
                if (currentGeoZ <= maxHeight) {
                    if ((nswe & Cell.NSWE_NORTH_EAST) == Cell.NSWE_NORTH_EAST) {
                        final int northGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX, previousY - 1, Cell.NSWE_EAST);
                        final int eastGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX + 1, previousY, Cell.NSWE_NORTH);
                        canSeeThrough = (northGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(previousX, previousY - 1, beeCurrentZ)) && (eastGeoZ <= getNearestZ(previousX + 1, previousY, beeCurrentZ));
                    } else if ((nswe & Cell.NSWE_NORTH_WEST) == Cell.NSWE_NORTH_WEST) {
                        final int northGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX, previousY - 1, Cell.NSWE_WEST);
                        final int westGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX - 1, previousY, Cell.NSWE_NORTH);
                        canSeeThrough = (northGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (northGeoZ <= getNearestZ(previousX, previousY - 1, beeCurrentZ)) && (westGeoZ <= getNearestZ(previousX - 1, previousY, beeCurrentZ));
                    } else if ((nswe & Cell.NSWE_SOUTH_EAST) == Cell.NSWE_SOUTH_EAST) {
                        final int southGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX, previousY + 1, Cell.NSWE_EAST);
                        final int eastGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX + 1, previousY, Cell.NSWE_SOUTH);
                        canSeeThrough = (southGeoZ <= maxHeight) && (eastGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(previousX, previousY + 1, beeCurrentZ)) && (eastGeoZ <= getNearestZ(previousX + 1, previousY, beeCurrentZ));
                    } else if ((nswe & Cell.NSWE_SOUTH_WEST) == Cell.NSWE_SOUTH_WEST) {
                        final int southGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX, previousY + 1, Cell.NSWE_WEST);
                        final int westGeoZ = getLosGeoZ(previousX, previousY, previousGeoZ, previousX - 1, previousY, Cell.NSWE_SOUTH);
                        canSeeThrough = (southGeoZ <= maxHeight) && (westGeoZ <= maxHeight) && (southGeoZ <= getNearestZ(previousX, previousY + 1, beeCurrentZ)) && (westGeoZ <= getNearestZ(previousX - 1, previousY, beeCurrentZ));
                    } else {
                        canSeeThrough = true;
                    }
                }

                if (!canSeeThrough) {
                    return false;
                }
            }

            previousX = currentX;
            previousY = currentY;
            previousGeoZ = currentGeoZ;
            ++pointIndex;
        }

        return true;
    }

    /**
     * Verifies if there is a path between origin and destination locations.<br>
     * Returns the destination if there is a path or the closest valid location.
     *
     * @param origin      The origin
     * @param destination The destination
     * @return the destination if there is a path or the closest location
     */
    public Location getValidLocation(ILocational origin, ILocational destination) {
        return getValidLocation(origin.getX(), origin.getY(), origin.getZ(), destination.getX(), destination.getY(), destination.getZ(), null);
    }

    /**
     * Performs movement validation and returns the last valid location before obstacles.
     *
     * @param x        The x coordinate
     * @param y        The y coordinate
     * @param z        The z coordinate
     * @param targetX  The target's x coordinate
     * @param targetY  The target's y coordinate
     * @param targetZ  The target's z coordinate
     * @param instance The instance
     * @return the last Location where player can walk - just before wall
     */
    public Location getValidLocation(int x, int y, int z, int targetX, int targetY, int targetZ, Instance instance) {
        final int geoX = getGeoX(x);
        final int geoY = getGeoY(y);
        final int nearestFromZ = getNearestZ(geoX, geoY, z);
        final int targetGeoX = getGeoX(targetX);
        final int targetGeoY = getGeoY(targetY);
        final int nearestToZ = getNearestZ(targetGeoX, targetGeoY, targetZ);

        // Door checks.
        if (DoorData.getInstance().checkIfDoorsBetween(x, y, nearestFromZ, targetX, targetY, nearestToZ, instance, false)) {
            return new Location(x, y, getHeight(x, y, nearestFromZ));
        }

        // Fence checks.
        if (FenceData.getInstance().checkIfFenceBetween(x, y, nearestFromZ, targetX, targetY, nearestToZ, instance)) {
            return new Location(x, y, getHeight(x, y, nearestFromZ));
        }

        final GridLineIterator2D pointIterator = new GridLineIterator2D(geoX, geoY, targetGeoX, targetGeoY);

        // First point is guaranteed to be available.
        pointIterator.next();
        int previousX = pointIterator.x();
        int previousY = pointIterator.y();
        int previousZ = nearestFromZ;

        while (pointIterator.next()) {
            final int currentX = pointIterator.x();
            final int currentY = pointIterator.y();
            final int currentZ = getNearestZ(currentX, currentY, previousZ);
            if ((currentZ - previousZ) > HEIGHT_INCREASE_LIMIT) // Check for sudden height increase.
            {
                // Can't move, return previous location.
                return new Location(getWorldX(previousX), getWorldY(previousY), previousZ);
            }

            if (hasGeoPos(previousX, previousY)) {
                if (isCompletelyBlocked(currentX, currentY, currentZ)) {
                    // Can't move, return previous location.
                    return new Location(getWorldX(previousX), getWorldY(previousY), previousZ);
                }

                if (!checkNearestNsweAntiCornerCut(previousX, previousY, previousZ, GeoUtils.computeNswe(previousX, previousY, currentX, currentY))) {
                    // Can't move, return previous location.
                    return new Location(getWorldX(previousX), getWorldY(previousY), previousZ);
                }
            }

            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
        }

        return hasGeoPos(previousX, previousY) && (previousZ != nearestToZ) ? new Location(x, y, nearestFromZ) : new Location(targetX, targetY, nearestToZ);
    }

    /**
     * Checks if it is possible to move from one location to another.
     *
     * @param fromX    The X coordinate to start checking from
     * @param fromY    The Y coordinate to start checking from
     * @param fromZ    The Z coordinate to start checking from
     * @param toX      The X coordinate to end checking at
     * @param toY      The Y coordinate to end checking at
     * @param toZ      The Z coordinate to end checking at
     * @param instance The instance
     * @return {@code true} if the creature at start coordinates can move to end coordinates, {@code false} otherwise
     */
    public boolean canMoveToTarget(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, Instance instance) {
        final int geoX = getGeoX(fromX);
        final int geoY = getGeoY(fromY);
        final int nearestFromZ = getNearestZ(geoX, geoY, fromZ);
        final int targetGeoX = getGeoX(toX);
        final int targetGeoY = getGeoY(toY);
        final int nearestToZ = getNearestZ(targetGeoX, targetGeoY, toZ);

        // Door checks.
        if (DoorData.getInstance().checkIfDoorsBetween(fromX, fromY, nearestFromZ, toX, toY, nearestToZ, instance, false)) {
            return false;
        }

        // Fence checks.
        if (FenceData.getInstance().checkIfFenceBetween(fromX, fromY, nearestFromZ, toX, toY, nearestToZ, instance)) {
            return false;
        }

        final GridLineIterator2D pointIterator = new GridLineIterator2D(geoX, geoY, targetGeoX, targetGeoY);

        // First point is guaranteed to be available.
        pointIterator.next();
        int previousX = pointIterator.x();
        int previousY = pointIterator.y();
        int previousZ = nearestFromZ;

        while (pointIterator.next()) {
            final int currentX = pointIterator.x();
            final int currentY = pointIterator.y();
            final int currentZ = getNearestZ(currentX, currentY, previousZ);
            if ((currentZ - previousZ) > HEIGHT_INCREASE_LIMIT) // Check for sudden height increase.
            {
                return false;
            }

            if (hasGeoPos(previousX, previousY)) {
                if (GeoEngineConfig.AVOID_OBSTRUCTED_PATH_NODES && !checkNearestNswe(currentX, currentY, currentZ, Cell.NSWE_ALL)) {
                    return false;
                }

                if (!checkNearestNsweAntiCornerCut(previousX, previousY, previousZ, GeoUtils.computeNswe(previousX, previousY, currentX, currentY))) {
                    return false;
                }
            }

            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
        }

        return !hasGeoPos(previousX, previousY) || (previousZ == nearestToZ);
    }

    /**
     * Checks if it is possible to move from one location to another.
     *
     * @param from The {@code ILocational} to start checking from
     * @param toX  The X coordinate to end checking at
     * @param toY  The Y coordinate to end checking at
     * @param toZ  The Z coordinate to end checking at
     * @return {@code true} if the creature at start coordinates can move to end coordinates, {@code false} otherwise
     */
    public boolean canMoveToTarget(ILocational from, int toX, int toY, int toZ) {
        return canMoveToTarget(from.getX(), from.getY(), from.getZ(), toX, toY, toZ, null);
    }

    /**
     * Checks if it is possible to move from one location to another.
     *
     * @param from The {@code ILocational} to start checking from
     * @param to   The {@code ILocational} to end checking at
     * @return {@code true} if the creature at start coordinates can move to end coordinates, {@code false} otherwise
     */
    public boolean canMoveToTarget(ILocational from, ILocational to) {
        return canMoveToTarget(from, to.getX(), to.getY(), to.getZ());
    }

    /**
     * Checks the specified position for available geodata.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @return {@code true} if there is geodata for the given coordinates, {@code false} otherwise
     */
    public boolean hasGeo(int x, int y) {
        return hasGeoPos(getGeoX(x), getGeoY(y));
    }

    /**
     * Checks if all directions (North, South, East, West) are blocked at the specified geo coordinates.
     *
     * @param geoX The geo X coordinate to check
     * @param geoY The geo Y coordinate to check
     * @param geoZ The geo Z coordinate to check
     * @return {@code true} if all directions (North, South, East, West) are blocked, {@code false} otherwise.
     */
    public boolean isCompletelyBlocked(int geoX, int geoY, int geoZ) {
        if (GeoEngineConfig.PATHFINDING < 1) {
            return false;
        }

        final IRegion region = getRegion(geoX, geoY);
        if (region != null) {
            return region.hasGeo() && !region.checkNearestNswe(geoX, geoY, geoZ, Cell.NSWE_NORTH) && !region.checkNearestNswe(geoX, geoY, geoZ, Cell.NSWE_SOUTH) && !region.checkNearestNswe(geoX, geoY, geoZ, Cell.NSWE_EAST) && !region.checkNearestNswe(geoX, geoY, geoZ, Cell.NSWE_WEST);
        }

        return true;
    }

    public static GeoEngine getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final GeoEngine INSTANCE = new GeoEngine();
    }
}
