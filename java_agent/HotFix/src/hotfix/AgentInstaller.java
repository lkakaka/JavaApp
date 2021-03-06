package hotfix;

import com.sun.tools.attach.VirtualMachine;

import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class AgentInstaller {
    private static volatile boolean s_hasInstalled;
    private static Instrumentation s_instrumentation;

    public static void install() {
        if (!s_hasInstalled) {
            installImpl();
        }
    }

    private static Instrumentation getInstrumentation() {
        if (!s_hasInstalled) {
            throw new IllegalStateException("DynamicJavaAgent not installed");
        }
        return s_instrumentation;
    }

    private synchronized static void installImpl() {
        if (s_hasInstalled) {
            return;
        }
        try {
            // load the agent
            File agentJarFile = createAgentJarFile();

            // Loading an agent requires the PID of the JVM to load the agent to. Find out our PID.
            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));

            // load the agent
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(agentJarFile.getAbsolutePath(), "");
            vm.detach();

            s_instrumentation = (Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(AgentEntry.class.getName())
                    .getMethod("getInstrumentation")
                    .invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            s_hasInstalled = true;
        }
    }

    private static File createAgentJarFile() throws IOException {
        String installerClassFilePath = AgentEntry.class.getName().replace('.', '/') + ".class";

        try (InputStream installerClassFileInputStream = AgentEntry.class.getResourceAsStream('/' + installerClassFilePath)) {
            if (installerClassFileInputStream == null) {
                throw new IllegalStateException("Cannot locate class file for DynamicJavaAgentInstaller");
            }

            File agentJarFile = File.createTempFile("DynamicJavaAgent", "jar");
            agentJarFile.deleteOnExit(); // Agent jar is required until VM shutdown due to lazy class loading.

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(new Attributes.Name("Agent-Class"), AgentEntry.class.getName());
            manifest.getMainAttributes().put(new Attributes.Name("Can-Redefine-Classes"), "true");
            manifest.getMainAttributes().put(new Attributes.Name("Can-Retransform-Classes"), "true");
            // manifest.getMainAttributes().put(new Attributes.Name("Can-Set-Native-Method-Prefix"), "true");

            byte[] b = new byte[1024];
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(agentJarFile), manifest)) {
                jos.putNextEntry(new JarEntry(installerClassFilePath));
                int len;
                while((len = installerClassFileInputStream.read(b, 0, 1024)) > 0) {
                    jos.write(b, 0, len);
                }
//                ByteStreams.copy(installerClassFileInputStream, jos);
                jos.closeEntry();
            }
            return agentJarFile;
        }
    }

    private static String getBaseClassName(String strClasName) {
        int loc = strClasName.lastIndexOf(".");
        return (loc < 0) ? strClasName : strClasName.substring(loc + 1);
    }

    private static Path getRealPath(URI uri) {
        return Paths.get(uri).toAbsolutePath().normalize();
    }

    private static Path getClassFilePath(Class<?> cls) throws URISyntaxException {
        String clsName = getBaseClassName(cls.getName());
        return getRealPath(cls.getResource(clsName + JavaFileObject.Kind.CLASS.extension).toURI());
    }

    private static ClassDefinition makeClassDefinition(Class<?> cls) {
        try {
            File file = getClassFilePath(cls).toFile();
            if (file.isFile() && file.getName().endsWith(JavaFileObject.Kind.CLASS.extension)) {
                byte[] buf = Files.readAllBytes(file.toPath());
                return new ClassDefinition(cls, buf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void redefineClasses(List<Class<?>> classes) {
        List<ClassDefinition> classDefinitions = new ArrayList<>();
        for (Class<?> cls : classes) {
            ClassDefinition classDefinition = makeClassDefinition(cls);
            if (classDefinition != null) {
                classDefinitions.add(classDefinition);
            }
        }
        try {
            getInstrumentation().redefineClasses(classDefinitions.toArray(new ClassDefinition[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void redefineClasses(Map<Class<?>, byte[]> mpClasses) {
        List<ClassDefinition> classDefinitions = new ArrayList<>();
        mpClasses.forEach((k, v) -> {
            ClassDefinition classDefinition = new ClassDefinition(k, v);
            classDefinitions.add(classDefinition);
        });

        try {
            getInstrumentation().redefineClasses(classDefinitions.toArray(new ClassDefinition[0]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void redefineClass(Class<?> cls, byte[] buf) {
        ClassDefinition classDefinition = new ClassDefinition(cls, buf);
        if (classDefinition == null) {
            System.out.println(String.format("new ClassDefinition %s failed", cls.getSimpleName()));
            return;
        }
        try {
            getInstrumentation().redefineClasses(classDefinition);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
