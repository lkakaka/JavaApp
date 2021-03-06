package hotfix;

import javax.tools.JavaFileObject.Kind;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentHotFix {
    private static String s_strCodePath;
    private static String s_strClassPath;
    private static Path s_classPath;
    private static final ConcurrentHashMap<String, String> s_mpJavaCodeMd5 = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> s_mpClassCodeMd5 = new ConcurrentHashMap<>();
    private static final Pattern s_pkgPattern = Pattern.compile("^\\s*package\\s*([\\w\\.]+);\\s*$");

    @FunctionalInterface
    static interface FileHandler {
        void apply(File f);
    }

    public static void init() {
        String rootPath = System.getProperty("user.dir");
//        String rootPath = "E:\\project\\JavaApp\\java_agent";
        s_strCodePath = rootPath + "/HotFix/src";
        s_strClassPath = rootPath + "/out/production/HotFix";
        File f = new File(s_strClassPath);
        s_classPath = f.toPath();
        System.out.println("codePath:" + s_strCodePath);
        System.out.println("classPath:" + s_strClassPath);
        initJavaCodeFileMd5();
        initClassCodeFileMd5();
    }

    private static void forEachFileAtom(String path, FileHandler handler, String suffix) {
        File file = new File(path);
        File[] fs = file.listFiles();
        for (File f : fs) {
            if (f.isDirectory()) {
                forEachFileAtom(f.getPath(), handler, suffix);
                continue;
            }
            if (!f.getName().endsWith(suffix)) {
                continue;
            }
            handler.apply(f);
        }
    }

    private static void forEachCodeFile(FileHandler handler) {
        forEachFileAtom(s_strCodePath, handler, Kind.SOURCE.extension);
    }

    private static void forEachClassFile(FileHandler handler) {
        forEachFileAtom(s_strClassPath, handler, Kind.CLASS.extension);
    }

    private static void initJavaCodeFileMd5() {
        forEachCodeFile((f) -> {
            try {
                String md5 = calcFileMd5(f);
                s_mpJavaCodeMd5.put(f.getPath(), md5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void initClassCodeFileMd5() {
        forEachClassFile((f) -> {
            try {
                String md5 = calcFileMd5(f);
                s_mpClassCodeMd5.put(f.getPath(), md5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static String md5(byte[] strTemp) {
        try {
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] b = mdTemp.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < b.length; i++) {
                sb.append(String.format("%02x", b[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String calcFileMd5(File f) throws Exception {
        return md5(getSrcCodeString(f).getBytes());
    }

    private static String findClassName(File f) throws Exception {
        String strTmp;
        BufferedReader reader = new BufferedReader(new FileReader(f));
        while ((strTmp = reader.readLine()) != null) {
            Matcher matcher = s_pkgPattern.matcher(strTmp);
            if (!matcher.matches()) {
                continue;
            }

            String clsName = f.getName();
            clsName = clsName.substring(0, clsName.lastIndexOf("."));
            return matcher.group(1) + "." + clsName;
        }

        return null;
    }

    private static String getSrcCodeString(File f) throws Exception {
        String code = "";
        String strTmp;
        BufferedReader reader = new BufferedReader(new FileReader(f));
        while ((strTmp = reader.readLine()) != null) {
            code += strTmp + "\n";
        }
        return code;
    }

    // 通过源码热更
    public static void hotfixByCode() {
        System.out.println("hotfix start by code");
        hotfixByCodeAtom();
        System.out.println("hotfix end by code");
    }

    private static void hotfixByCodeAtom() {
//        forEachCodeFile((f) -> {
//            try {
//                String md5 = calcFileMd5(f);
//                String oldMd5 = s_mpJavaCodeMd5.get(f.getPath());
//                if (md5.equals(oldMd5)) return;
//                String clsName = findClassName(f);
//
//                Map<Class<?>, byte[]> mpClasses = new HashMap<>();
//                Map<String, String> mpClassFullName2SourceCode = new HashMap<>();
//                mpClassFullName2SourceCode.put(clsName, getSrcCodeString(f));
//                Map<String, byte[]> mpCompiledCode = MyJavaCompiler.compile(mpClassFullName2SourceCode, s_strCodePath);
//                for (Entry<String, byte[]> entry : mpCompiledCode.entrySet()) {
//                    // 新加文件，写入字节码(
//                    // 如果其他类依赖这个新增类，或者新增类依赖其他新增类,可能会出现编译错误, 可以全工程编译
//                    if (oldMd5 == null) {
//                        String clsFileName = s_strClassPath + clsName.replace('.', '/') + Kind.CLASS.extension;
//                        DataOutputStream out = new DataOutputStream(new FileOutputStream(clsFileName));
//                        out.write(entry.getValue());
//                        out.close();
//                    }
//                    Class<?> cls = Class.forName(entry.getKey());
//                    mpClasses.put(cls, entry.getValue());
//                    System.out.println("***hotfix class " + entry.getKey());
//                }
//                AgentStart.redefineClasses(mpClasses);
//                s_mpJavaCodeMd5.put(f.getPath(), md5);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
    }

    // 通过类名(全名)更新
    public static void hotfixByClassName(Set<String> lstClsName) {
        System.out.println("hotfix start by class name");
        try {
            List<Class<?>> lstClass = new ArrayList<>();
            for (String clsName : lstClsName) {
                Class<?> cls = Class.forName(clsName);
                if (cls == null) {
                    System.out.println("not found hotfix class " + clsName);
                    continue;
                }
                lstClass.add(cls);
                System.out.println("***hotfix class " + clsName);
            }
            AgentInstaller.redefineClasses(lstClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("hotfix end by class name");
    }

    // 通过.class字节码文件更新
    public static void hotfixByClass() {
        System.out.println("hotfix start by class");
        hotfixByClassAtom();
        System.out.println("hotfix end by class");
    }

    public static String pathToStringStandard(Path p) {
        return p.toString().replace('\\', '/');
    }

    private static void hotfixByClassAtom() {
        List<Class<?>> lstClasses = new ArrayList<>();
        forEachClassFile((f) -> {
            try {
                String md5 = calcFileMd5(f);
                String oldMd5 = s_mpClassCodeMd5.get(f.getPath());
                if (md5.equals(oldMd5)) return;

                Path relativePath = s_classPath.relativize(f.toPath());
                String clsName = pathToStringStandard(relativePath).replace('/', '.');
                if (clsName.endsWith(Kind.CLASS.extension)) {
                    clsName = clsName.substring(0, clsName.lastIndexOf(Kind.CLASS.extension));
                }
                Class<?> cls = Class.forName(clsName);
                lstClasses.add(cls);
                s_mpClassCodeMd5.put(f.getPath(), md5);
                System.out.println("***hotfix class " + clsName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        if (lstClasses.isEmpty()) return;
        AgentInstaller.redefineClasses(lstClasses);
    }
}
