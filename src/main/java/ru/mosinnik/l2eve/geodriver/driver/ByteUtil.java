package ru.mosinnik.l2eve.geodriver.driver;

import lombok.experimental.UtilityClass;
import ru.mosinnik.l2eve.geodriver.abstraction.IBlock;
import ru.mosinnik.l2eve.geodriver.blocks.*;
import ru.mosinnik.l2eve.geodriver.bytes.*;

import java.nio.ByteBuffer;

import static ru.mosinnik.l2eve.geodriver.driver.GeoDriverBytesConstants.*;

@UtilityClass
public class ByteUtil {


    public static byte getType(IBlock block) {
        Class<? extends IBlock> blockClass = block.getClass();
        if (blockClass.equals(FlatBlock.class)) {
            return FLAT_BLOCK;
        } else if (blockClass.equals(ComplexBlock.class)) {
            return COMPLEX_BLOCK;
        } else if (blockClass.equals(MultilayerBlock.class)) {
            return MULTILAYER_BLOCK;
        } else if (blockClass.equals(OneHeightComplexBlock.class)) {
            return ONE_HEIGHT_COMPLEX_BLOCK;
        } else if (blockClass.equals(BaseHeightComplexBlock.class)) {
            return BASE_HEIGHT_COMPLEX_BLOCK;
        } else if (blockClass.equals(BaseHeightOneNsweComplexBlock.class)) {
            return BASE_HEIGHT_ONE_NSWE_COMPLEX_BLOCK;
        } else if (blockClass.equals(FewHeightsComplexBlock.class)) {
            return FEW_HEIGHTS_COMPLEX_BLOCK;
        } else if (blockClass.equals(FewHeightsOneNsweComplexBlock.class)) {
            return FEW_HEIGHTS_ONE_NSWE_COMPLEX_BLOCK;
        } else if (blockClass.equals(NoHolesMultilayerBlock.class)) {
            return NO_HOLES_MULTILAYER_BLOCK;
        } else if (blockClass.equals(IndexedMultilayerBlock.class)) {
            return INDEXED_MULTILAYER_BLOCK;
        } else if (blockClass.equals(Indexed32MultilayerBlock.class)) {
            return INDEXED_32_MULTILAYER_BLOCK;
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
    }

    public static byte[] toBytes(IBlock block) {
        Class<? extends IBlock> blockClass = block.getClass();
        if (blockClass.equals(FlatBlock.class)) {
            return FlatBlockFromOffsetBytes.toBytes((FlatBlock) block);
        } else if (blockClass.equals(ComplexBlock.class)) {
            return ComplexBlockBytes.toBytes((ComplexBlock) block);
        } else if (blockClass.equals(MultilayerBlock.class)) {
            return MultilayerBlockBytes.toBytes((MultilayerBlock) block);
        } else if (blockClass.equals(OneHeightComplexBlock.class)) {
            return OneHeightComplexBlockBytes.toBytes((OneHeightComplexBlock) block);
        } else if (blockClass.equals(BaseHeightComplexBlock.class)) {
            return BaseHeightComplexBlockBytes.toBytes((BaseHeightComplexBlock) block);
        } else if (blockClass.equals(BaseHeightOneNsweComplexBlock.class)) {
            return BaseHeightOneNsweComplexBlockBytes.toBytes((BaseHeightOneNsweComplexBlock) block);
        } else if (blockClass.equals(FewHeightsComplexBlock.class)) {
            return FewHeightsComplexBlockBytes.toBytes((FewHeightsComplexBlock) block);
        } else if (blockClass.equals(FewHeightsOneNsweComplexBlock.class)) {
            return FewHeightsOneNsweComplexBlockBytes.toBytes((FewHeightsOneNsweComplexBlock) block);
        } else if (blockClass.equals(NoHolesMultilayerBlock.class)) {
            return NoHolesMultilayerBlockBytes.toBytes((NoHolesMultilayerBlock) block);
        } else if (blockClass.equals(IndexedMultilayerBlock.class)) {
            return IndexedMultilayerBlockBytes.toBytes((IndexedMultilayerBlock) block);
        } else if (blockClass.equals(Indexed32MultilayerBlock.class)) {
            return Indexed32MultilayerBlockBytes.toBytes((Indexed32MultilayerBlock) block);
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
    }

    public static void appendBytes(IBlock block, ByteBuffer data) {
        Class<? extends IBlock> blockClass = block.getClass();
        if (blockClass.equals(FlatBlock.class)) {
            FlatBlockFromOffsetBytes.appendBytes((FlatBlock) block, data);
        } else if (blockClass.equals(ComplexBlock.class)) {
            ComplexBlockBytes.appendBytes((ComplexBlock) block, data);
        } else if (blockClass.equals(MultilayerBlock.class)) {
            MultilayerBlockBytes.appendBytes((MultilayerBlock) block, data);
        } else if (blockClass.equals(OneHeightComplexBlock.class)) {
            OneHeightComplexBlockBytes.appendBytes((OneHeightComplexBlock) block, data);
        } else if (blockClass.equals(BaseHeightComplexBlock.class)) {
            BaseHeightComplexBlockBytes.appendBytes((BaseHeightComplexBlock) block, data);
        } else if (blockClass.equals(BaseHeightOneNsweComplexBlock.class)) {
            BaseHeightOneNsweComplexBlockBytes.appendBytes((BaseHeightOneNsweComplexBlock) block, data);
        } else if (blockClass.equals(FewHeightsComplexBlock.class)) {
            FewHeightsComplexBlockBytes.appendBytes((FewHeightsComplexBlock) block, data);
        } else if (blockClass.equals(FewHeightsOneNsweComplexBlock.class)) {
            FewHeightsOneNsweComplexBlockBytes.appendBytes((FewHeightsOneNsweComplexBlock) block, data);
        } else if (blockClass.equals(NoHolesMultilayerBlock.class)) {
            NoHolesMultilayerBlockBytes.appendBytes((NoHolesMultilayerBlock) block, data);
        } else if (blockClass.equals(IndexedMultilayerBlock.class)) {
            IndexedMultilayerBlockBytes.appendBytes((IndexedMultilayerBlock) block, data);
        } else if (blockClass.equals(Indexed32MultilayerBlock.class)) {
            Indexed32MultilayerBlockBytes.appendBytes((Indexed32MultilayerBlock) block, data);
        } else {
            throw new RuntimeException("Unknown block class: " + blockClass.getName());
        }
    }

    public static int getBytesCount(IBlock block) {
        Class<? extends IBlock> blockClass = block.getClass();
        if (blockClass.equals(FlatBlock.class)) {
            return FlatBlockFromOffsetBytes.calcBytesCount((FlatBlock) block);
        } else if (blockClass.equals(ComplexBlock.class)) {
            return ComplexBlockBytes.calcBytesCount((ComplexBlock) block);
        } else if (blockClass.equals(MultilayerBlock.class)) {
            return MultilayerBlockBytes.calcBytesCount((MultilayerBlock) block);
        } else if (blockClass.equals(OneHeightComplexBlock.class)) {
            return OneHeightComplexBlockBytes.calcBytesCount((OneHeightComplexBlock) block);
        } else if (blockClass.equals(BaseHeightComplexBlock.class)) {
            return BaseHeightComplexBlockBytes.calcBytesCount((BaseHeightComplexBlock) block);
        } else if (blockClass.equals(BaseHeightOneNsweComplexBlock.class)) {
            return BaseHeightOneNsweComplexBlockBytes.calcBytesCount((BaseHeightOneNsweComplexBlock) block);
        } else if (blockClass.equals(FewHeightsComplexBlock.class)) {
            return FewHeightsComplexBlockBytes.calcBytesCount((FewHeightsComplexBlock) block);
        } else if (blockClass.equals(FewHeightsOneNsweComplexBlock.class)) {
            return FewHeightsOneNsweComplexBlockBytes.calcBytesCount((FewHeightsOneNsweComplexBlock) block);
        } else if (blockClass.equals(NoHolesMultilayerBlock.class)) {
            return NoHolesMultilayerBlockBytes.calcBytesCount((NoHolesMultilayerBlock) block);
        } else if (blockClass.equals(IndexedMultilayerBlock.class)) {
            return IndexedMultilayerBlockBytes.calcBytesCount((IndexedMultilayerBlock) block);
        } else if (blockClass.equals(Indexed32MultilayerBlock.class)) {
            return Indexed32MultilayerBlockBytes.calcBytesCount((Indexed32MultilayerBlock) block);
        }

        throw new RuntimeException("Unknown block class: " + blockClass.getName());
    }
}
