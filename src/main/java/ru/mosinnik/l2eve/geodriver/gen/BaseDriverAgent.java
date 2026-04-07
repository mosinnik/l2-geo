package ru.mosinnik.l2eve.geodriver.gen;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;

/**
 * Java Agent для трансформации байткода BaseDriver в рантайме.
 */
public class BaseDriverAgent {

    private static volatile Instrumentation instrumentation;

    /**
     * Инициализирует Instrumentation через ByteBuddy self-attaching
     */
    public static synchronized void initialize() {
        if (instrumentation != null) {
            return;
        }

        try {
            instrumentation = ByteBuddyAgent.install();
            System.out.println("BaseDriverAgent: Instrumentation initialized via ByteBuddy");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ByteBuddy agent", e);
        }
    }

    /**
     * Проверяет, инициализирован ли Instrumentation
     */
    public static boolean isInitialized() {
        return instrumentation != null;
    }

    /**
     * Переопределяет класс BaseDriver с новым байткодом
     */
    public static void redefineBaseDriver(byte[] newClassBytes) {
        if (instrumentation == null) {
            initialize();
        }

        try {
            ClassDefinition classDef = new ClassDefinition(GeoDriverBytesGen.class, newClassBytes);
            instrumentation.redefineClasses(classDef);
            System.out.println("BaseDriverAgent: BaseDriver redefined successfully via redefineClasses");
        } catch (Exception e) {
            throw new RuntimeException("Failed to redefine BaseDriver", e);
        }
    }
}
