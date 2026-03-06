# How to use in L2J Mobius sources

## Preparation

Copy from this repo into sources:
1. `org.l2jmobius.gameserver.config.L2EveGeoDataDriverConfig` class to `org.l2jmobius.gameserver.config` package
2. `ru.mosinnik.l2eve.geodriver` package as is
3. `org.l2jmobius.gameserver.geoengine.GeoEngine` class
4. add `L2EveGeoDataDriverConfig.load()` to `rg.l2jmobius.gameserver.config.ConfigLoader`
  after `GeoEngineConfig.load()`:
```
        GeoEngineConfig.load();
        L2EveGeoDataDriverConfig.load(); // <<< 
        GrandBossConfig.load();
```

Copy into datapack:
1. `L2EveGeoDataDriver.ini` config file to data/config dir

Build sources as usually.

# Run

To use new geodriver set `L2EveGeoDataDriverEnabled` to true inside `L2EveGeoDataDriver.ini`.

On game server starting in logs u will see:
```
INFO: GeoEngine: Load L2Eve driver xxxxx
```
where `xxxxx` would be replaced with used class name.

## Choosing geodriver class

There are 3 options:
- `ru.mosinnik.l2eve.geodriver.driver.GeoDriver` - as common geoengine use java objects but with new L2Eve blocks type which 
  are better in memory/performance. Fully loaded into memory.
- `ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes` - use ByteBuffer in memory without java object. 
  Better performance but need binary files or .l2j transformation.
- `ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesMmap` - best choice, mmap-ed prepared files for Bytes geodriver. 
  Need binary files to start.

To use binaries u must generate binaries. To do that follow:
1. Choose `ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes`
2. Set `GeoDataBinPath` to some path. It will be created if not exists
3. Set `LoadBytesFromL2J = true`
4. Set `GenerateBinFromL2J = true`
5. Run server, wait until messages like `Updated xxxxx file` to appear

```
INFO: GeoEngine: Load L2Eve driver ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - data size: 539881601
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Updated data file: data.bin
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Updated regionFirstBlockIndexes file: regionFirstBlockIndexes.bin
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Updated blockTypes file: blockTypes.bin
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Updated blockDataOffsets file: blockDataOffsets.bin
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Regions data size: 1024 (ints), with offsets: 166
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Data size: 539881601 (bytes)
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Blocks offsets: 10878976 (ints)
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Blocks count: 10878976 (bytes)
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 1 -> 7562313, in data 0 bytes (0.0)  -- FLAT_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 2 -> 2674959, in data 342394752 bytes (128.0)  -- COMPLEX_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 4 -> 23821, in data 1572186 bytes (66.0)  -- ONE_HEIGHT_COMPLEX_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 9 -> 253993, in data 73512617 bytes (289.4277283232215)  -- NO_HOLES_MULTILAYER_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 11 -> 363890, in data 147032960 bytes (404.05880898073593)  -- INDEXED_32_MULTILAYER_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Multilayer data sizes count: 0
```

So now binaries created, check it in path `GeoDataBinPath` - there should be 4 files.
After that you can use loading from binaries:
1. Set `LoadBytesFromL2J = false`
2. Run server, wait messages like `Read YYYYY bytes from XXXXX file`

```
INFO: GeoEngine: Load L2Eve driver ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Read 539881601 bytes from data file: data.bin
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Read 1024 ints from data file: regionFirstBlockIndexes.bin
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Read 10878976 bytes from data file: blockTypes.bin
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Read 10878976 ints from data file: blockDataOffsets.bin
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Regions data size: 1024 (ints), with offsets: 166
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Data size: 539881601 (bytes)
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Blocks offsets: 10878976 (ints)
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Blocks count: 10878976 (bytes)
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 1 -> 7562313, in data 0 bytes (0.0)  -- FLAT_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 2 -> 2674959, in data 342394752 bytes (128.0)  -- COMPLEX_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 4 -> 23821, in data 1572186 bytes (66.0)  -- ONE_HEIGHT_COMPLEX_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 9 -> 253993, in data 73512617 bytes (289.4277283232215)  -- NO_HOLES_MULTILAYER_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - -- Block type: 11 -> 363890, in data 147032960 bytes (404.05880898073593)  -- INDEXED_32_MULTILAYER_BLOCK
[main] INFO ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytes - Multilayer data sizes count: 0
```
