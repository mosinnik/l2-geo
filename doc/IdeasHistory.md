# История разных подходов

Описываются разные опробованные подходы и были откинуты по различным причинам.


## MethodHandle

Реализация в GeoDriverBytesMH. 
Идея заключалась в следующем: мы можем сохранить MethodHandle на методы разных реализаций блоков и хранить их вместо типов блоков.
Это позволяет избавиться от свитча по типу и вызову статик метода и сделать код чище. Плюс предполагалось, что не будет ветвления
на свитче.

Замеры:
```
+--------------------------------------------+-------------------+------------+-------------------+-----+
|Benchmark                                   |Param: blockTypeStr|Score       |Score Error (99.9%)|Unit |
+--------------------------------------------+-------------------+------------+-------------------+-----+
|GeoDriverBenchParams.checkNearestNSWE_MH    |COMPLEX_BLOCK      |4241.294381 |111.436047         |ops/s|
|GeoDriverBenchParams.checkNearestNSWE_MH_CMP|COMPLEX_BLOCK      |4388.924528 |62.929943          |ops/s|
|GeoDriverBenchParams.checkNearestNSWE_MH    |FLAT_BLOCK         |9984.593137 |412.221254         |ops/s|
|GeoDriverBenchParams.checkNearestNSWE_MH_CMP|FLAT_BLOCK         |22133.114752|177.852885         |ops/s|
|GeoDriverBenchParams.checkNearestNSWE_MH    |MULTILAYER_BLOCK   |626.024146  |8.742567           |ops/s|
|GeoDriverBenchParams.checkNearestNSWE_MH_CMP|MULTILAYER_BLOCK   |640.348726  |3.175186           |ops/s|
|GeoDriverBenchParams.checkNearestNSWE_MH    |RANDOM             |2333.445073 |23.832106          |ops/s|
|GeoDriverBenchParams.checkNearestNSWE_MH_CMP|RANDOM             |2778.168721 |37.877455          |ops/s|
+--------------------------------------------+-------------------+------------+-------------------+-----+
```

`checkNearestNSWE_MH_CMP` - `GeoDriverBytesMH_cmp.checkNearestNSWE()`, Bytes реализация, гдк в switch оставлены только три базовых
типа блоков, как и только три типа методхендлоб было в релизации MH.

Результаты:
- производительность в целом хуже, на FLAT падение производительности 2 раза
- код да стал удобнее, как если бы использовались интерфейсы
- объем памяти увеличился, т.к. был массив из 1 байтов, стал из ссылок 4 байта

Почему:
- если глянуть байткод, то вызов invoke на MH это `INVOKEVIRTUAL`, что дорого
- если потом глянуть еще и асм код, то там видны еще доп проверки
- так же из бенча видно, что на флете, который зависит только от скорости работы части до вызова логики блока, самая большая просадка.
  Что говорит о том, что цена кода в методе `checkNearestNSWE` в самом дравере стала в два раза дороже.

Потенциал:
- можно было бы использовать, если нужно как-то автоматически собрать ссылки на методы по разным объектам и вместо генерации кода 
  использовать недоинтерфейсные вызовы статических методов


## FFM

Реализация в GeoDriverFFM.
Идея заключалась в следующем: используем FFM как замену UNSAFE, ожидалось, что будет быстрее Bytes реализаций, но неучтенные особенности
вызова FFM привели к очень медленной реализации. Проблема в том, что *_HANDLE требует long оффсет. Без лонга - крайне сильное падение 
производительности. 

```
+--------------------------------------------+-------------------+------------+-------------------+-----+
|Benchmark                                   |Param: blockTypeStr|Score       |Score Error (99.9%)|Unit |
+--------------------------------------------+-------------------+------------+-------------------+-----+
|GeoDriverBenchParams.checkNearestNSWEBytes  |COMPLEX_BLOCK      |4382.733940 |31.708573          |ops/s|
|GeoDriverBenchParams.checkNearestNSWEFFM    |COMPLEX_BLOCK      |1081.609738 |11.713680          |ops/s|
|GeoDriverBenchParams.checkNearestNSWEFFMLong|COMPLEX_BLOCK      |4151.389962 |20.675147          |ops/s|
|GeoDriverBenchParams.checkNearestNSWEBytes  |FLAT_BLOCK         |22088.908950|326.176265         |ops/s|
|GeoDriverBenchParams.checkNearestNSWEFFM    |FLAT_BLOCK         |1580.415723 |1.627524           |ops/s|
|GeoDriverBenchParams.checkNearestNSWEFFMLong|FLAT_BLOCK         |20788.669907|375.099825         |ops/s|
|GeoDriverBenchParams.checkNearestNSWEBytes  |MULTILAYER_BLOCK   |642.566830  |1.693848           |ops/s|
|GeoDriverBenchParams.checkNearestNSWEFFM    |MULTILAYER_BLOCK   |522.630426  |1.116651           |ops/s|
|GeoDriverBenchParams.checkNearestNSWEFFMLong|MULTILAYER_BLOCK   |724.563216  |4.258058           |ops/s|
|GeoDriverBenchParams.checkNearestNSWEBytes  |RANDOM             |3220.186393 |3.595698           |ops/s|
|GeoDriverBenchParams.checkNearestNSWEFFM    |RANDOM             |1033.076881 |6.955372           |ops/s|
|GeoDriverBenchParams.checkNearestNSWEFFMLong|RANDOM             |3210.012264 |8.897162           |ops/s|
+--------------------------------------------+-------------------+------------+-------------------+-----+
```

`checkNearestNSWEFFMLong` - реализация в GeoDriverFFMLong, та же логика, но испольузется long оффсет и указание типа возврата на хендле.

Результаты:
- производительность крайне плохая
- код менее читаемый

Почему:
- если глянуть байткод, то hHandle.get там `INVOKEVIRTUAL`, что дорого само по себе, но под ним еще куча логики
- если глянуть перфнорм, то количество инструкций кратно больше Bytes реализации 

Потенциал:
- в таком виде нет, нужно правильно использовать типы, чтобы JVM подставляла вызов уже оптимизированной реализации get


## GeoDriverBytesDirect

Нужен был для сравнения с просто Bytes с простой аллокацией

В Bytes:
```
data = ByteBuffer.allocate(dataSize);
```

В BytesDirect:
```
data = ByteBuffer.allocateDirect(dataSize);
```

Тесты:
```
+------------------------------------------------+-----+-------+-------+------------+-------------------+-----+-------------------+
|Benchmark                                       |Mode |Threads|Samples|Score       |Score Error (99.9%)|Unit |Param: blockTypeStr|
+------------------------------------------------+-----+-------+-------+------------+-------------------+-----+-------------------+
|GeoDriverBenchParams.checkNearestNSWEBytes      |thrpt|4      |5      |3072.019839 |17.721235          |ops/s|RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytes      |thrpt|4      |5      |22588.692226|196.367394         |ops/s|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytes      |thrpt|4      |5      |4266.889195 |146.113485         |ops/s|COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytes      |thrpt|4      |5      |631.006946  |2.588672           |ops/s|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect|thrpt|4      |5      |3193.721821 |18.856030          |ops/s|RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect|thrpt|4      |5      |22505.896806|153.397689         |ops/s|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect|thrpt|4      |5      |4244.745054 |206.961012         |ops/s|COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect|thrpt|4      |5      |699.276550  |2.820715           |ops/s|MULTILAYER_BLOCK   |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap  |thrpt|4      |5      |3166.568647 |14.693397          |ops/s|RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap  |thrpt|4      |5      |22533.233742|176.378750         |ops/s|FLAT_BLOCK         |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap  |thrpt|4      |5      |4287.074307 |195.556767         |ops/s|COMPLEX_BLOCK      |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap  |thrpt|4      |5      |701.594391  |3.710670           |ops/s|MULTILAYER_BLOCK   |
+------------------------------------------------+-----+-------+-------+------------+-------------------+-----+-------------------+
```

С директ аллокацией как и в mmap. Раньше bytes выделялся на хипе и обращение к нему немного дороже

```
+-------------------------------------------------------------+-----+-------+-------+-------------+-------------------+-----+-------------------+
|Benchmark                                                    |Mode |Threads|Samples|Score        |Score Error (99.9%)|Unit |Param: blockTypeStr|
+-------------------------------------------------------------+-----+-------+-------+-------------+-------------------+-----+-------------------+
|GeoDriverBenchParams.checkNearestNSWEBytes                   |thrpt|4      |5      |3072.019839  |17.721235          |ops/s|RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytes:branches          |thrpt|4      |1      |115275.863100|NaN                |#/op |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytes:instructions      |thrpt|4      |1      |870862.596804|NaN                |#/op |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect             |thrpt|4      |5      |3193.721821  |18.856030          |ops/s|RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect:branches    |thrpt|4      |1      |100021.879711|NaN                |#/op |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesDirect:instructions|thrpt|4      |1      |811977.523570|NaN                |#/op |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap               |thrpt|4      |5      |3166.568647  |14.693397          |ops/s|RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap:branches      |thrpt|4      |1      |100120.388986|NaN                |#/op |RANDOM             |
|GeoDriverBenchParams.checkNearestNSWEBytesMmap:instructions  |thrpt|4      |1      |813113.619221|NaN                |#/op |RANDOM             |
+-------------------------------------------------------------+-----+-------+-------+-------------+-------------------+-----+-------------------+
```

По бранч и инструкция директ и ммап одинаковы, а bytes на 6 инструкций дороже и на 1 бранч больше.

Потенциал: нужно переносить в Bytes и убирать Direct.  

## GeoDriverBytesDirectInl

Нужен был для проверки будет ли полный инлайнинг кода под свитч таким же или лучше, чем в Direct/Bytes.

Результаты показали, что стало заметно хуже (см. коммент в GeoDriverBytesDirectInl) даже с включением более агрессивных С1 опций компиляции.

## GeoDriverBytesDirectIR

Нужен был для переработки вычисления индекса с
```
int regionIndex = ((geoX >> 11) << 5) + (geoY >> 11);
int blockIndexInRegion = (((geoX >> 3) & 0xFF) << 8) + ((geoY >> 3) & 0xFF);
```
на
```
int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);
```

Дало небольшой буст:
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

Переработка уже применена по всем драйверам кроме базового GeoDriver.


## GeoDriverFFMT2

Нужен был для проверки, дает ли буст чтение aligned short из dataShort против чтения unaligned из просто data.

Цифры сначала показывали, что GeoDriverFFMT был лучше, но на запуске 5*60 на комплекс блоке стало одинаково.

В целом имеет смысл в будущем вернуться к этой идее для более плотных тестов, т.к. с точки зрения количества инструкций GeoDriverFFMT2 выигрывает.

## GeoDriverFFMT_MBT

Вместе с MultilayerBlockFFMT нужен был для проверки, дает ли буст уход от лишних конвертаций в short при работе с layerData в Multilayer.
А также влияние использования withInvokeExactBehavior().

Тесты показали легкое, но заметное, улучшение от ухода от short. А withInvokeExactBehavior влияет на уровне погрешности.

Уход от short в Multilayer уже запланирован, но пока остается базовая реализация L2J. 


## GeoDriverBytesDirectIf

Вместе с GeoDriverBytesDirectIfCmp нужен был для проверки, дает ли буст использование if конструкции вместо switch.

Тесты показали плюс-минус одинаковую производительность на однотипных запросах, на рендоме было слегка лучше.
```
Benchmark                                                            (blockTypeStr)   Mode  Cnt       Score    Error      Units
GeoDriverBenchParams.checkNearestNSWEBytes                                   RANDOM  thrpt    5    3361.054 ± 12.054      ops/s
GeoDriverBenchParams.checkNearestNSWEBytes:CPI                               RANDOM  thrpt            1.149           clks/insn
GeoDriverBenchParams.checkNearestNSWEBytes:IPC                               RANDOM  thrpt            0.871           insns/clk
GeoDriverBenchParams.checkNearestNSWEBytes:branch-misses                     RANDOM  thrpt          718.294                #/op
GeoDriverBenchParams.checkNearestNSWEBytes:branches                          RANDOM  thrpt        99767.058                #/op
GeoDriverBenchParams.checkNearestNSWEBytes:cycles                            RANDOM  thrpt       931404.372                #/op
GeoDriverBenchParams.checkNearestNSWEBytes:instructions                      RANDOM  thrpt       810894.099                #/op
GeoDriverBenchParams.checkNearestNSWEBytesDirectIf                           RANDOM  thrpt    5    3399.126 ± 17.039      ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:CPI                       RANDOM  thrpt            1.160           clks/insn
GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:IPC                       RANDOM  thrpt            0.862           insns/clk
GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:branch-misses             RANDOM  thrpt          466.488                #/op
GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:branches                  RANDOM  thrpt       104449.101                #/op
GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:cycles                    RANDOM  thrpt       921277.211                #/op
GeoDriverBenchParams.checkNearestNSWEBytesDirectIf:instructions              RANDOM  thrpt       794202.082                #/op
GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp                        RANDOM  thrpt    5    3355.158 ± 12.521      ops/s
GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:CPI                    RANDOM  thrpt            1.151           clks/insn
GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:IPC                    RANDOM  thrpt            0.869           insns/clk
GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:branch-misses          RANDOM  thrpt          714.787                #/op
GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:branches               RANDOM  thrpt        99765.991                #/op
GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:cycles                 RANDOM  thrpt       933356.691                #/op
GeoDriverBenchParams.checkNearestNSWEBytesDirectIfCmp:instructions           RANDOM  thrpt       810890.770                #/op
```

IfCmp содержит в себе то же что и просто Bytes, но в свитче только 3 варианта, а не все 11, чтобы ыло столько же как и веток в if.

По cycles и instructions видно, что вариант с if дешевле на 1 цикл и на 2 операции в пересчете на 10к итераций.
Потенциально можно будет вернуться к этому варианту, но это улучшение на около 1%.


## GeoDriverBytesDirectInlO

Как и GeoDriverBytesDirectInl нужен был для проверки возможности инлайнинга. Но по сравнению с простым инлайнингом в GeoDriverBytesDirectInl
тут добавлены простейшие оптимизации для мультилеера, которые не делаются C1, видимо, из-за размеров получившегося метода.

Тесты показывают похожую производительность, но в различных запусках бывает срабатывает какая-то оптимизация что на байтс что на инл
и в итоге разницы какой-то не замечено на кучи тестов.

Использование в будущем бессмысленно.


## GeoDriverBytes2

GeoDriverBytes2 нужен был для проверки, будет ли буст если упаковать тип и смещение в один int и использовать только одно чтение
вместо двух.

Тесты (см. GeoDriverBytes2) показали, что на комплексе лучше, но мульти и рендом сильно хуже.

Потенциально можно вернуться для уменьшения памяти, но скорость сильно хуже.
