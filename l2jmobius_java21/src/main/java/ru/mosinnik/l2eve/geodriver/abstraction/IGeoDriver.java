package ru.mosinnik.l2eve.geodriver.abstraction;


import java.io.IOException;
import java.nio.file.Path;

import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.*;


/**
 * @author FBIagent
 */
public interface IGeoDriver {

    /**
     * Translates world x into geo x.
     * readable:
     * (worldX - WORLD_MIN_X) / 16;
     *
     * @param worldX world x
     * @return geo x
     */
    default int getGeoX(int worldX) {
        if ((worldX < WORLD_MIN_X) || (worldX > WORLD_MAX_X)) {
            throw new IllegalArgumentException();
        }
        return (worldX - WORLD_MIN_X) >> 4;
    }

    /**
     * Translates world y into geo y.
     * readable:
     * (worldY - WORLD_MIN_Y) / 16;
     *
     * @param worldY world y
     * @return geo y
     */
    default int getGeoY(int worldY) {
        if ((worldY < WORLD_MIN_Y) || (worldY > WORLD_MAX_Y)) {
            throw new IllegalArgumentException();
        }
        return (worldY - WORLD_MIN_Y) >> 4;
    }

    /**
     * Translates geo x into world x.
     *
     * @param geoX geo x
     * @return world x
     */
    default int getWorldX(int geoX) {
        if ((geoX < 0) || (geoX >= GEO_CELLS_X)) {
            throw new IllegalArgumentException();
        }
        return (geoX * 16) + WORLD_MIN_X + 8;
    }

    /**
     * Translates geo y into world y.
     *
     * @param geoY geo y
     * @return world y
     */
    default int getWorldY(int geoY) {
        if ((geoY < 0) || (geoY >= GEO_CELLS_Y)) {
            throw new IllegalArgumentException();
        }
        return (geoY * 16) + WORLD_MIN_Y + 8;
    }


    void loadRegion(Path filePath, int regionX, int regionY) throws IOException;


    /**
     * Checks the specified geodata position for available geodata.
     *
     * @param geoX geo x
     * @param geoY geo y
     * @return true when geodata is available, false otherwise
     */
    boolean hasGeoPos(int geoX, int geoY);

    /**
     * Method to get the nearest z value. If there is no geodata available<br>
     * at the specified position, {@code worldZ} is returned.
     *
     * @param geoX   geo x
     * @param geoY   geo y
     * @param worldZ world z
     * @return nearest z or worldZ(see description above)
     */
    int getNearestZ(int geoX, int geoY, int worldZ);

    /**
     * Method to get the next lower z value. If there is a layer with a z<br>
     * equals to {@code worldZ} or there is no lower z, {@code worldZ} is<br>
     * returned.
     *
     * @param geoX   geo x
     * @param geoY   geo y
     * @param worldZ world z
     * @return next lower z or worldZ(see description above)
     */
    int getNextLowerZ(int geoX, int geoY, int worldZ);

    /**
     * Method to get the next higher z value. If there is a layer with a z<br>
     * equals to {@code worldZ} or there is no higher z, {@code worldZ} is<br>
     * returned.
     *
     * @param geoX   geo x
     * @param geoY   geo y
     * @param worldZ world z
     * @return next higher z or worldZ(see description above)
     */
    int getNextHigherZ(int geoX, int geoY, int worldZ);

    boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe);
}
