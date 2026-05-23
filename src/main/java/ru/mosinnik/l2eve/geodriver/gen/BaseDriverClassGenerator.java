package ru.mosinnik.l2eve.geodriver.gen;

import org.objectweb.asm.*;
import ru.mosinnik.l2eve.geodriver.bytes.NullRegionBytes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static ru.mosinnik.l2eve.geodriver.gen.BlockTypes.slash;

/**
 * Трансформирует байткод BaseDriver, заменяя метод callMe на оптимизированный switch.
 */
public class BaseDriverClassGenerator {

    public static final String DRIVER_CLASS_SLASH = slash(GeoDriverBytesGen.class);

    /**
     * Генерирует полностью новый байткод BaseDriver с оптимизированным методом callMe.
     */
    public static byte[] generateTransformedClass(MappingHolder mappingHolder) throws IOException {
        // Создаём класс с нуля, не читая оригинальный
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        // Читаем оригинальный, чтобы получить остальную структуру класса
        ClassReader cr = new ClassReader(DRIVER_CLASS_SLASH);

        // Копируем всё кроме метода callMe
        ClassVisitor transformer = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                // Пропускаем оригинальный callMe
                if ("checkNearestNSWE".equals(name) && "(IIIB)Z".equals(descriptor)) {
                    return null; // пропускаем
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        cr.accept(transformer, ClassReader.EXPAND_FRAMES);

        // Добавляем public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe) {
        generateCheckNearestNSWE(cw, mappingHolder);

        byte[] byteArray = cw.toByteArray();
        Files.write(Path.of("generated_2.class"), byteArray);

        return byteArray;
    }

    /**
     *
     * Generate:
     * public boolean checkNearestNSWE(int geoX, int geoY, int worldZ, byte nswe)
     */
    private static void generateCheckNearestNSWE(ClassWriter cw, MappingHolder mappingHolder) {

        Map<Byte, Byte> oldToNew = mappingHolder.oldToNewMapping();
        Map<Byte, Byte> newToOld = mappingHolder.newToOldMapping();
        int typesCount = oldToNew.size();
        if (typesCount == 0) {
            throw new RuntimeException("Zero types after remap");
        }

        // public checkNearestNSWE(IIIB)Z
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "checkNearestNSWE",
                "(IIIB)Z",
                null,
                null
        );

        // Добавление всего до свитч блока
        addPreSwitch(mv);

        // лейблы для веток
        Label defaultLabel = new Label();
        Label returnLabel = new Label();

        // Создаём лейблы для веток каждого типа
        Label[] labels = new Label[typesCount];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label();
        }

        // Генерируем tableswitch по НОВЫМ типам
        // тип в 9 слоте
        mv.visitVarInsn(ILOAD, 9);
        mv.visitTableSwitchInsn(0, typesCount - 1, defaultLabel, labels);

        // Генерируем ветки switch - порядок не важен для tableswitch (O(1))
        // но мы генерируем их в порядке частоты для читаемости байткода
        for (int i = 0; i < typesCount; i++) {
            byte newType = (byte) i;
            byte oldType = newToOld.get(newType);

            // лейбл для прыжка из свитча
            mv.visitLabel(labels[i]);
            // Вызываем соответствующий статический метод блока по СТАРОМУ типу
            generateBlockCall(mv, oldType);
            // возвращаем результат
            mv.visitInsn(IRETURN);
        }

        // Default ветка
        mv.visitLabel(defaultLabel);
        generateThrowException(mv, "java/lang/RuntimeException", "Unknown type");
        mv.visitInsn(Opcodes.ARETURN);

        // Метка возврата (для полноты)
        mv.visitLabel(returnLabel);
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }


    /**
     * LOCALVARIABLE geoX I L0 L20 1
     * LOCALVARIABLE geoY I L0 L20 2
     * LOCALVARIABLE worldZ I L0 L20 3
     * LOCALVARIABLE nswe B L0 L20 4
     * LOCALVARIABLE regionIndex I L1 L20 5
     * LOCALVARIABLE regionFirstBlockIndex I L2 L20 6
     * LOCALVARIABLE blockIndexInRegion I L5 L20 7
     * LOCALVARIABLE blockType B L6 L20 8
     * LOCALVARIABLE blockDataOffset I L7 L20 9
     */
    private static void addPreSwitch(MethodVisitor mv) {
        Label l0 = new Label();

        // int regionIndex = ((geoX >> 6) & 0x03E0) | ((geoY >> 11));
        // -----------------------------------------------------------
        mv.visitVarInsn(ILOAD, 1); // geoX
        mv.visitIntInsn(BIPUSH, 6); // сохраняем в local 6
        mv.visitInsn(ISHR); // geoX >> 6
        mv.visitIntInsn(SIPUSH, 0x03E0); // сохраняем в local 0x03E0
        mv.visitInsn(IAND); // (geoX >> 6) &
        mv.visitVarInsn(ILOAD, 2); // geoY
        mv.visitIntInsn(BIPUSH, 11); // сохраняем в local 11
        mv.visitInsn(ISHR); // geoY >> 11
        mv.visitInsn(IOR); // |
        mv.visitVarInsn(ISTORE, 5); // сохраняем результат regionIndex в 5 слот

        // int regionFirstBlockIndex = this.regionFirstBlockIndexes[regionIndex];
        // -----------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, DRIVER_CLASS_SLASH, "regionFirstBlockIndexes", "[I");
        mv.visitVarInsn(ILOAD, 5);
        mv.visitInsn(IALOAD);
        mv.visitVarInsn(ISTORE, 6);

        // if (regionFirstBlockIndex == NO_INDEX) {
        // -----------------------------------------------------------
        mv.visitVarInsn(ILOAD, 6);
        mv.visitInsn(ICONST_M1);
        mv.visitJumpInsn(IF_ICMPNE, l0);

        //    return NullRegionBytes.checkNearestNSWE(geoX, geoY, worldZ, nswe);
        // -----------------------------------------------------------
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitVarInsn(ILOAD, 4);
        mv.visitMethodInsn(INVOKESTATIC, slash(NullRegionBytes.class), "checkNearestNSWE", "(IIIB)Z", false);
        mv.visitInsn(IRETURN);

        // int blockIndexInRegion = ((geoX & 0x07F8) << 5) | ((geoY >> 3) & 0xFF);
        // -----------------------------------------------------------
        mv.visitLabel(l0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitIntInsn(SIPUSH, 2040);
        mv.visitInsn(IAND);
        mv.visitInsn(ICONST_5);
        mv.visitInsn(ISHL);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitInsn(ICONST_3);
        mv.visitInsn(ISHR);
        mv.visitIntInsn(SIPUSH, 255);
        mv.visitInsn(IAND);
        mv.visitInsn(IOR);
        mv.visitVarInsn(ISTORE, 7);

        // int blockIndex = regionFirstBlockIndex + blockIndexInRegion;
        // -----------------------------------------------------------
        mv.visitVarInsn(ILOAD, 6);
        mv.visitVarInsn(ILOAD, 7);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, 8);

        // byte blockType = blockTypes[blockIndex];
        // -----------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, DRIVER_CLASS_SLASH, "blockTypes", "[B");
        mv.visitVarInsn(ILOAD, 8);
        mv.visitInsn(BALOAD);
        mv.visitVarInsn(ISTORE, 9);

        // int blockDataOffset = blockDataOffsets[blockIndex];
        // -----------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, DRIVER_CLASS_SLASH, "blockDataOffsets", "[I");
        mv.visitVarInsn(ILOAD, 8);
        mv.visitInsn(IALOAD);
        mv.visitVarInsn(ISTORE, 10);
    }

    private static void generateBlockCall(MethodVisitor mv, byte oldType) {
        String blockInternal = BlockTypes.TYPE_TO_BLOCK_INTERNAL.get(oldType);
        if (blockInternal == null) {
            throw new IllegalArgumentException("Unknown block type: " + oldType);
        }

        // Загружаем offset
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ILOAD, 3);
        mv.visitVarInsn(ILOAD, 4);
        mv.visitVarInsn(ILOAD, 10);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, DRIVER_CLASS_SLASH, "data", "Ljava/nio/ByteBuffer;");

        // Вызываем .checkNearestNSWE(geoX, geoY, worldZ, nswe, blockDataOffset, data);
        mv.visitMethodInsn(INVOKESTATIC, blockInternal, "checkNearestNSWE", "(IIIBILjava/nio/ByteBuffer;)Z", false);
    }

    private static void generateThrowException(MethodVisitor mv, String exceptionInternal, String message) {
        mv.visitTypeInsn(NEW, exceptionInternal);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(message);
        mv.visitMethodInsn(
                INVOKESPECIAL,
                exceptionInternal,
                "<init>",
                "(Ljava/lang/String;)V",
                false
        );
        mv.visitInsn(ATHROW);
    }
}
