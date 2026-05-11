Количество инструкций на FLAT:
```
+------------------------------------------------------------------+--------------+----+-------------------+
|Benchmark                                                         |Score         |Unit|Param: blockTypeStr|
+------------------------------------------------------------------+--------------+----+-------------------+
|GeoDriverBenchParams.checkNearestNSWEBytes2:instructions          |331967.240112 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIR:instructions   |341185.789175 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEFFM_T:instructions           |341446.463084 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:instructions|341553.949458 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions |341576.203802 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions     |341579.448399 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap:instructions       |341626.268445 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytes:instructions           |341857.690769 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:instructions   |372190.418454 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEFFMStruct:instructions       |401737.445690 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEFFMLong:instructions         |421630.401009 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesGen:instructions        |421855.556652 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWE:instructions                |461992.692884 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWE_MH:instructions             |874885.567830 |#/op|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEFFM:instructions             |7509478.511196|#/op|FLAT_BLOCK         |
+------------------------------------------------------------------+--------------+----+-------------------+
```

В bytes2 33 инструкции на вызов - самый короткий путь, получаем за счет

```
byte blockType = (byte) (blockDatum & 0x3F);
if (blockType == FLAT_BLOCK) {
    return FlatBlockFromOffsetBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe);
}
```

В комплекс это же добавляет доп 2 инструкции по сравнению с bytes. 



Все:

```
+------------------------------------------------------------------+--------------+-------------------+
|Benchmark                                                         |Score         |Param: blockTypeStr|
+------------------------------------------------------------------+--------------+-------------------+
|GeoDriverBenchParams.checkNearestNSWEBytes:instructions           |566417.215198 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEFFM_T:instructions           |586228.003531 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytes2:instructions          |586603.749522 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIR:instructions   |596295.085432 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap:instructions       |596359.847501 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:instructions   |596405.573992 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:instructions|596414.591374 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions     |596589.761245 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions |596729.355850 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWE:instructions                |618917.827557 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEFFMStruct:instructions       |647081.206363 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesGen:instructions        |696923.434184 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEFFMLong:instructions         |716718.859627 |COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytes2:instructions          |331967.240112 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIR:instructions   |341185.789175 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEFFM_T:instructions           |341446.463084 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:instructions|341553.949458 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions |341576.203802 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions     |341579.448399 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap:instructions       |341626.268445 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytes:instructions           |341857.690769 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:instructions   |372190.418454 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEFFMStruct:instructions       |401737.445690 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEFFMLong:instructions         |421630.401009 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesGen:instructions        |421855.556652 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWE:instructions                |461992.692884 |FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWE:instructions                |3703920.035522|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions |4058462.665970|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:instructions|4491567.668288|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIR:instructions   |4507888.441798|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap:instructions       |4507961.747632|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions     |4508390.416957|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:instructions   |4530480.263906|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesGen:instructions        |4568836.919616|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEFFM_T:instructions           |4847190.402693|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEFFMStruct:instructions       |5866530.700989|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEFFMLong:instructions         |6013026.666621|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytes:instructions           |6025037.794314|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytes2:instructions          |6048720.262583|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:instructions   |798958.432242 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIR:instructions   |813656.723216 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:instructions|818160.503484 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap:instructions       |819260.409585 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEFFM_T:instructions           |832553.840085 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesDirectInlO:instructions |865674.161319 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytes2:instructions          |874547.446312 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytes:instructions           |877097.267817 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEFFMStruct:instructions       |887042.816669 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEFFMLong:instructions         |887759.554495 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesGen:instructions        |890186.089529 |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions     |1013865.520177|RANDOM             |
|GeoDriverBenchParams.checkNearestNSWE:instructions                |1111994.030650|RANDOM             |
+------------------------------------------------------------------+--------------+-------------------+
```

Минимальные по количеству инструкций на вызов:
- RANDOM - checkNearestNSWEBytesDirectIf
- FLAT_BLOCK - checkNearestNSWEBytes2
- COMPLEX_BLOCK - checkNearestNSWEBytes
- MULTILAYER_BLOCK - checkNearestNSWE и checkNearestNSWEBytesDirectInlO
