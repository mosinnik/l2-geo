package ru.mosinnik.l2eve.geodriver.abstraction;

/**
 * @author FBIagent
 */
public interface IRegion {
    /**
     * Blocks in a region on the x axis
     */
    int REGION_BLOCKS_X = 256;
    /**
     * Blocks in a region on the y axis
     */
    int REGION_BLOCKS_Y = 256;
    /**
     * Blocks in a region
     */
    int REGION_BLOCKS = REGION_BLOCKS_X * REGION_BLOCKS_Y;

    /**
     * Cells in a region on the x axis
     */
    int REGION_CELLS_X = REGION_BLOCKS_X * IBlock.BLOCK_CELLS_X;
    /**
     * Cells in a regioin on the y axis
     */
    int REGION_CELLS_Y = REGION_BLOCKS_Y * IBlock.BLOCK_CELLS_Y;
    /**
     * Cells in a region
     */
    int REGION_CELLS = REGION_CELLS_X * REGION_CELLS_Y;

    IBlock getBlock(int geoX, int geoY);

    boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe);

    int getNearestZ(int geoX, int geoY, int worldZ);

    int getNextLowerZ(int geoX, int geoY, int worldZ);

    int getNextHigherZ(int geoX, int geoY, int worldZ);

    boolean hasGeo();
}
