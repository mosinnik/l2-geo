package ru.mosinnik.l2eve.geodriver.gen;

import ru.mosinnik.l2eve.geodriver.bytes.*;

import java.util.HashMap;
import java.util.Map;

import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants.*;

public class BlockTypes {

    /**
     * Маппинг: тип -> внутренний имя класса блока (для ASM)
     */
    public static final Map<Byte, String> TYPE_TO_BLOCK_INTERNAL;

    static {
        TYPE_TO_BLOCK_INTERNAL = new HashMap<>();

        TYPE_TO_BLOCK_INTERNAL.put(FLAT_BLOCK, slash(FlatBlockFromOffsetGenBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(COMPLEX_BLOCK, slash(ComplexBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(MULTILAYER_BLOCK, slash(MultilayerBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(ONE_HEIGHT_COMPLEX_BLOCK, slash(OneHeightComplexBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(BASE_HEIGHT_COMPLEX_BLOCK, slash(BaseHeightComplexBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK, slash(BaseHeightOneNsweComplexBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(FEW_HEIGHTS_COMPLEX_BLOCK, slash(FewHeightsComplexBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK, slash(FewHeightsOneNsweComplexBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(NO_HOLES_MULTILAYER_BLOCK, slash(NoHolesMultilayerBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(INDEXED_MULTILAYER_BLOCK, slash(IndexedMultilayerBlockBytes.class));
        TYPE_TO_BLOCK_INTERNAL.put(INDEXED_32_MULTILAYER_BLOCK, slash(Indexed32MultilayerBlockBytes.class));
    }

    public static String slash(Class<?> clazz) {
        return clazz.getCanonicalName().replace(".", "/");
    }

}
