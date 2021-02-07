import Test.Test1;

public class Test {
    public void say(String msg) {
        System.out.println(msg);
    }

    public void say(String name, String msg) {
        System.out.println(name + msg);
    }

    public void say(Test1 test1) {
        System.out.println(test1.name);
    }
}
