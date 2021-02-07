import javassist.ClassPool;
import javassist.CtClass;

public class Main {
    public static void main(String[] args) {
        System.out.println("start");
        final ClassPool classPool = ClassPool.getDefault();
        String className = "Test";
        System.out.println("get pool1:" + className);
//        final CtClass clazz = classPool.get(className);
    }
}

