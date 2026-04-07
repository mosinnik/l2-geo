package ru.mosinnik.l2eve.geodriver.gen;

import java.util.Map;

public record MappingHolder(
        byte[] srcTypes,
        byte[] newTypes,
        Map<Byte, Byte> oldToNewMapping,
        Map<Byte, Byte> newToOldMapping,
        Map<Byte, Counter> srcTypesCounters
) {
}
