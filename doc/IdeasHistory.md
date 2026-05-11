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

## If
//todo
Замена свитча на иф