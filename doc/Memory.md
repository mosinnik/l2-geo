# JOG Layouts

## Total old L2J

805 Mb 
14.2kk objects

```
footprint = ru.mosinnik.l2eve.geodriver.driver.GeoDriver@3b71a0b2d footprint:
     COUNT       AVG       SUM   DESCRIPTION
    617883       322 199541800   [B
       166    262160  43518560   [Lru.mosinnik.l2eve.geodriver.abstraction.IBlock;
         1      4112      4112   [Lru.mosinnik.l2eve.geodriver.abstraction.IRegion;
   2698780       144 388624320   [S
   2698780        16  43180480   ru.mosinnik.l2eve.geodriver.blocks.ComplexBlock
   7562313        16 120997008   ru.mosinnik.l2eve.geodriver.blocks.FlatBlock
    617883        16   9886128   ru.mosinnik.l2eve.geodriver.blocks.MultilayerBlock
         1        24        24   ru.mosinnik.l2eve.geodriver.driver.GeoConfig
         1        24        24   ru.mosinnik.l2eve.geodriver.driver.GeoDriver
         1        16        16   ru.mosinnik.l2eve.geodriver.regions.NullRegion
       166        16      2656   ru.mosinnik.l2eve.geodriver.regions.Region
  14195975           805755128   (total)


totalCount = 14195975
totalSize = 805755128
-------------------------
--- allFlatBlocks
java.util.ArrayList@17eb784dd footprint:
     COUNT       AVG       SUM   DESCRIPTION
         1  36920416  36920416   [Ljava.lang.Object;
         1        24        24   java.util.ArrayList
   7562313        16 120997008   ru.mosinnik.l2eve.geodriver.blocks.FlatBlock
   7562315           157917448   (total)


--- allComplexBlocks
java.util.ArrayList@5765a86cd footprint:
     COUNT       AVG       SUM   DESCRIPTION
         1  10939400  10939400   [Ljava.lang.Object;
   2698780       144 388624320   [S
         1        24        24   java.util.ArrayList
   2698780        16  43180480   ru.mosinnik.l2eve.geodriver.blocks.ComplexBlock
   5397562           442744224   (total)


--- allMultilayerBlocks
java.util.ArrayList@327e60dd footprint:
     COUNT       AVG       SUM   DESCRIPTION
    617883       322 199541800   [B
         1   3241320   3241320   [Ljava.lang.Object;
         1        24        24   java.util.ArrayList
    617883        16   9886128   ru.mosinnik.l2eve.geodriver.blocks.MultilayerBlock
   1235768           212669272   (total)

```


## Total optimized for memory

557 Mb 
6.7kk objects

```
footprint = ru.mosinnik.l2eve.geodriver.driver.GeoDriver@5af01860d footprint:
     COUNT       AVG       SUM   DESCRIPTION
   2437593       111 271821128   [B
       166    262160  43518560   [Lru.mosinnik.l2eve.geodriver.abstraction.IBlock;
         1      4112      4112   [Lru.mosinnik.l2eve.geodriver.abstraction.IRegion;
    966214       176 170727912   [S
    430841        24  10340184   ru.mosinnik.l2eve.geodriver.blocks.BaseHeightComplexBlock
   1531897        24  36765528   ru.mosinnik.l2eve.geodriver.blocks.BaseHeightOneNsweComplexBlock
    625077        16  10001232   ru.mosinnik.l2eve.geodriver.blocks.ComplexBlock
     85930        24   2062320   ru.mosinnik.l2eve.geodriver.blocks.FewHeightsComplexBlock
      1214        24     29136   ru.mosinnik.l2eve.geodriver.blocks.FewHeightsOneNsweComplexBlock
      1498        16     23968   ru.mosinnik.l2eve.geodriver.blocks.FlatBlock
    363890        16   5822240   ru.mosinnik.l2eve.geodriver.blocks.MultilayerBlock
    253993        24   6095832   ru.mosinnik.l2eve.geodriver.blocks.NoHolesMultilayerBlock
     23821        24    571704   ru.mosinnik.l2eve.geodriver.blocks.OneHeightComplexBlock
         1        24        24   ru.mosinnik.l2eve.geodriver.driver.GeoConfig
         1        24        24   ru.mosinnik.l2eve.geodriver.driver.GeoDriver
         1        16        16   ru.mosinnik.l2eve.geodriver.regions.NullRegion
       166        16      2656   ru.mosinnik.l2eve.geodriver.regions.Region
   6722304           557786576   (total)


totalCount = 6722304
totalSize = 557786576
```

## Total optimized for memory GeoDriverBytes 

447 Mb 
7 objects

```
footprint = ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes@1cab0bfbd footprint:
     COUNT       AVG       SUM   DESCRIPTION
         2 201732060 403464120   [B
         2  21760016  43520032   [I
         1        56        56   java.nio.HeapByteBuffer
         1        24        24   ru.mosinnik.l2eve.geodriver.driver.GeoConfig
         1        32        32   ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes
         7           446984264   (total)


totalCount = 7
totalSize = 446984264
```
