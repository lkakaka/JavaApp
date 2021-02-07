import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

//大多数情况下，我们使用Instrumentation都是使用其字节码插桩的功能，或者笼统说就是类重定义(Class Redefine)的功能，但是有以下的局限性：
//
//        premain和agentmain两种方式修改字节码的时机都是类文件加载之后，也就是说必须要带有Class类型的参数，不能通过字节码文件和自定义的类名重新定义一个本来不存在的类。
//        类的字节码修改称为类转换(Class Transform)，类转换其实最终都回归到类重定义Instrumentation#redefineClasses()方法，此方法有以下限制：
//        新类和老类的父类必须相同；
//        新类和老类实现的接口数也要相同，并且是相同的接口；
//        新类和老类访问符必须一致。 新类和老类字段数和字段名要一致；
//        新类和老类新增或删除的方法必须是private static/final修饰的；
//        可以修改方法体。

public class AgentMainTest {
    public static void agentmain(String agentArgs, Instrumentation instrumentation) throws UnmodifiableClassException {
        System.out.println("agentmain attach");
        instrumentation.addTransformer(new DefineTransformer(), true);

        Class classes[] = instrumentation.getAllLoadedClasses();
        for (int i = 0; i < classes.length; i++) {
            System.out.println(classes[i].getName());
            if (classes[i].getName().equals("Test")) {
                System.out.println("Reloading: " + classes[i].getName());
                instrumentation.retransformClasses(classes[i]);
                break;
            }
        }
    }

    static class DefineTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            System.out.println("agent premain load Class:" + className);

            try {
                // 从ClassPool获得CtClass对象
                final ClassPool classPool = ClassPool.getDefault();
                final CtClass clazz = classPool.get(className);

                updateMethod(classPool, clazz);
//                addNewMethod(clazz);


                // 返回字节码，并且detachCtClass对象
                byte[] byteCode = clazz.toBytecode();
                //detach的意思是将内存中曾经被javassist加载过的Date对象移除，如果下次有需要在内存中找不到会重新走javassist加载
                clazz.detach();
                System.out.println("finish hotfix " + className);
                return byteCode;
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }

            return classfileBuffer;
        }

        private void updateMethod(ClassPool classPool, CtClass ctClass) throws Exception {
//            CtMethod ctMethod = ctClass.getDeclaredMethod("say");
//            CtClass ctStrClass = classPool.get("java.lang.String");
//            CtClass[] params = new CtClass[] {ctStrClass, ctStrClass};
//            classPool.importPackage();
            CtClass ctStrClass = classPool.get("Test.Test1");
            CtClass[] params = new CtClass[] {ctStrClass};
            CtMethod ctMethod = ctClass.getDeclaredMethod("say", params);
            ctMethod.insertBefore("System.out.println(\"before\");");
            ctMethod.insertAfter("System.out.println(\"after\");");

//            String methodBody = "{System.out.println($1 + \" world\");}";
//            ctMethod.setBody(methodBody);
        }

        private void addNewMethod(CtClass ctClass) throws Exception {
            System.out.println("add new method---");
            //增加一个新方法 新类和老类新增或删除的方法必须是private static/final修饰的；
            String methodStr = "private static void showParameters(int a,int b){"
                    + " System.out.println(\"First parameter: \"+a);"
                    + " System.out.println(\"Second parameter: \"+b);"
                    + "}";
            CtMethod newMethod = CtMethod.make(methodStr, ctClass);
            ctClass.addMethod(newMethod);
        }
    }

}
