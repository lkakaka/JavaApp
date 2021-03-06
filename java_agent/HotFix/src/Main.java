import hotfix.AgentHotFix;
import hotfix.AgentInstaller;

public class Main {
    public static void main(String[] args) {
        AgentInstaller.install();
        AgentHotFix.init();
        Input.start();
    }
}
