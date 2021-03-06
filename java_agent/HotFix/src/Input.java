import hotfix.AgentHotFix;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Input {

    public static void start() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    System.out.println("input:");
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String cmd = br.readLine();
                    dispatchCmd(cmd);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private static void dispatchCmd(String cmd) {
        switch (cmd) {
            case "hotfix":
                AgentHotFix.hotfixByClass();
                break;
            case "test":
                Test.test();
                break;
            default:
                System.out.println("not impl cmd " + cmd);
        }
    }
}
