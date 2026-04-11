package ru.mosinnik.l2eve.geodriver.driver;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.mosinnik.l2eve.geodriver.driver.GeoConstants.*;

public class OffsetsTests {

    @Ignore("Just info")
    @Test
    public void blockIndexInRegionInfo() {
//        int worldX = r.nextInt(cornerMaxWorldX - cornerMinWorldX) + cornerMinWorldX;
//        int worldY = r.nextInt(cornerMaxWorldY - cornerMinWorldY) + cornerMinWorldY;
//        int geoX = myState.driver.getGeoX(worldX);
//        int geoY = myState.driver.getGeoY(worldY);

//        // world dimensions: 1048576 * 1048576 = 1_099_511_627_776
//        int WORLD_MIN_X = -655360;
//        int WORLD_MAX_X = 393215;
//        int WORLD_MIN_Y = -589824;
//        int WORLD_MAX_Y = 458751;

        // WORLD_MIN_X <= worldX <= WORLD_MAX_X
        int worldX = -12314;
        // WORLD_MIN_Y <= worldY <= WORLD_MAX_Y
        int worldY = 42141;

        // [0, 2^16)
        int geoX = getGeoX(worldX);
        // [0, 2^16)
        int geoY = getGeoY(worldY);

        // [0, 2^13)
        int tX = geoX >> 3;

        // [0, 2^8)
        int tX2 = tX & 0xFF;

        // [2^8, 2^16)
        int tX3 = tX2 << 8;

        // [0, 2^13)
        int tY = geoY >> 3;

        // [0, 2^8)
        int tY2 = tY & 0xFF;

        // [0, 2^16)
        int expected = tX3 + tY2;

//        int expected = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);

    }

    @Test
    public void blockIndexInRegionInfoRework() {

//        // world dimensions: 1048576 * 1048576 = 1_099_511_627_776
//        int WORLD_MIN_X = -655360;
//        int WORLD_MAX_X = 393215;
//        int WORLD_MIN_Y = -589824;
//        int WORLD_MAX_Y = 458751;

        // WORLD_MIN_X <= worldX <= WORLD_MAX_X
        int worldX = -12314;
        // WORLD_MIN_Y <= worldY <= WORLD_MAX_Y
        int worldY = 42141;

        // [0, 2^16)
        int geoX = getGeoX(worldX);
        // [0, 2^16)
        int geoY = getGeoY(worldY);

        int expected = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);

        // [2^3, 2^11)
        int tX = geoX & 0x000007F8;

        // [2^8, 2^16)
        int tX2 = tX << 5;

        // [0, 2^13)
        int tY = geoY >> 3;

        // [0, 2^8)
        int tY2 = tY & 0xFF;

        // [0, 2^16)
        int actual = tX2 | tY2;

        assertEquals(expected, actual);

    }


    @Ignore("Heavy")
    @Test
    public void blockIndexInRegionInfoReworkShouldBeSame() {
        for (int geoX = 0; geoX < Short.MAX_VALUE; geoX++) {
            for (int geoY = 0; geoY < Short.MAX_VALUE; geoY++) {
                assertEquals(
                        (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF),
                        (((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF))
                );
            }
        }
    }


    /**
     * result in [0, (WORLD_MAX_X-WORLD_MIN_X) >> 4]
     * = [0, (2^20) >> 4)
     * = [0, 2^16)
     */
    static int getGeoX(int worldX) {
        if ((worldX < WORLD_MIN_X) || (worldX > WORLD_MAX_X)) {
            throw new IllegalArgumentException();
        }
        return (worldX - WORLD_MIN_X) >> 4;
    }

    /**
     * result in [0, (WORLD_MAX_Y-WORLD_MIN_Y) >> 4]
     * = [0, (2^20) >> 4)
     * = [0, 2^16)
     */
    static int getGeoY(int worldY) {
        if ((worldY < WORLD_MIN_Y) || (worldY > WORLD_MAX_Y)) {
            throw new IllegalArgumentException();
        }
        return (worldY - WORLD_MIN_Y) >> 4;
    }
}
