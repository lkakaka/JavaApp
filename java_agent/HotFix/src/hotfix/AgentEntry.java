package hotfix;

import java.lang.instrument.Instrumentation;

public class AgentEntry {
    private static volatile Instrumentation s_instrumentation;

    public static Instrumentation getInstrumentation() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new RuntimePermission("getInstrumentation"));
        }
        Instrumentation instrumentation = s_instrumentation;
        if (instrumentation == null) {
            throw new IllegalStateException("The DynamicJavaAgent.jar is not loaded or this method is not called via the system class loader");
        }
        return instrumentation;
    }

    // public static void premain(String arguments, Instrumentation instrumentation) {
    //     s_instrumentation = instrumentation;
    // }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        s_instrumentation = instrumentation;
    }
}
