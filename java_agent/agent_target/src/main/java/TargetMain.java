import java.lang.reflect.Method;
import Test.Test1;

public class TargetMain {

    static Test test = new Test();
    static Test1 test1 = new Test1("test1");

    public static void main(String[] args) {
        Thread t = new Thread(TargetMain::threadFunc);
        t.start();
        System.out.println("target main run......");
        try {
            t.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void threadFunc() {
        while (true) {
            try {
                Thread.sleep(1000);
//                Test test = new Test();
//                test.say("hello", "world");
                test.say(test1);
                try {
                    Method[] methods = Test.class.getDeclaredMethods();
                    Method method = Test.class.getDeclaredMethod("showParameters", int.class, int.class);
                    if (method != null) {
                        System.out.println("find method");
                        method.setAccessible(true);
                        method.invoke(test, 10, 20);
                    }
                } catch (NoSuchMethodException nme) {
                    System.out.println("not method");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
