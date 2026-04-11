
Replace
`int blockIndexInRegion = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);`
to
`int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);`

Boost:
- random: 2%
- flat: 6.5%
- complex: 2.3%
- multi: ~+0%

```
Benchmark                                             (blockTypeStr)   Mode  Cnt      Score     Error  Units
GeoDriverBenchParams.checkNearestNSWEBytesDirect              RANDOM  thrpt    5   3206.787 ±  12.277  ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirect          FLAT_BLOCK  thrpt    5  20827.344 ± 552.166  ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirect       COMPLEX_BLOCK  thrpt    5   4005.558 ±  35.087  ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirect    MULTILAYER_BLOCK  thrpt    5    714.589 ±   2.297  ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirectIR            RANDOM  thrpt    5   3267.479 ±  21.187  ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirectIR        FLAT_BLOCK  thrpt    5  22172.575 ± 348.498  ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirectIR     COMPLEX_BLOCK  thrpt    5   4100.401 ±  18.611  ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirectIR  MULTILAYER_BLOCK  thrpt    5    714.981 ±   2.245  ops/s
```