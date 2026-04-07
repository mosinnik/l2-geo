package ru.mosinnik.l2eve.geodriver.gen;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeWork {

    public static MappingHolder remapTypes(byte[] srcTypes) {
        MappingHolder holder = getOldToNewMapping(srcTypes);
        Map<Byte, Byte> oldToNewMapping = holder.oldToNewMapping();

        byte[] result = new byte[srcTypes.length];

        for (int i = 0; i < srcTypes.length; i++) {
            byte oldType = srcTypes[i];
            byte newType = oldToNewMapping.get(oldType);
            result[i] = newType;
        }

        return new MappingHolder(
                holder.srcTypes(),
                result,
                holder.oldToNewMapping(),
                holder.newToOldMapping(),
                holder.srcTypesCounters()
        );
    }

    public static MappingHolder getOldToNewMapping(byte[] srcTypes) {
        Map<Byte, Counter> counters = new HashMap<>();
        for (byte srcType : srcTypes) {
            counters.computeIfAbsent(srcType, k -> new Counter()).increment();
        }

        List<Map.Entry<Byte, Counter>> list = counters.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .toList();

        Map<Byte, Byte> oldToNewMapping = new HashMap<>();
        Map<Byte, Byte> newToOldMapping = new HashMap<>();
        byte newType = 0;
        for (Map.Entry<Byte, Counter> entry : list) {
            byte oldType = entry.getKey();
            oldToNewMapping.put(oldType, newType);
            newToOldMapping.put(newType, oldType);
            System.out.println("new = " + newType + " <--> old = " + oldType);
            newType++;
        }
        return new MappingHolder(
                srcTypes,
                null,
                oldToNewMapping,
                newToOldMapping,
                counters
        );
    }
}
