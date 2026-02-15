package ru.mosinnik.l2eve.geodriver.abstraction;

public interface IBlock {
    int TYPE_FLAT = 0;
    int TYPE_COMPLEX = 1;
    int TYPE_MULTILAYER = 2;

    /**
     * Cells in a block on the x axis
     */
    int BLOCK_CELLS_X = 8;
    /**
     * Cells in a block on the y axis
     */
    int BLOCK_CELLS_Y = 8;
    /**
     * Cells in a block
     */
    int BLOCK_CELLS = BLOCK_CELLS_X * BLOCK_CELLS_Y;


    boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe);

    int getNearestZ(int geoX, int geoY, int worldZ);

    int getNextLowerZ(int geoX, int geoY, int worldZ);

    int getNextHigherZ(int geoX, int geoY, int worldZ);
}
