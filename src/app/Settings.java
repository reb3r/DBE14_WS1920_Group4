package app;

public class Settings {
    // Singleton - Pattern - Foo
    private static volatile Settings instance;

    private Settings() {
    }

    public static Settings getInstance() {
        if (Settings.instance == null) {
            synchronized (Settings.class) {
                if (Settings.instance == null) {
                    Settings.instance = new Settings();
                }
            }
        }
        return Settings.instance;
    }

    // Actual Settings getter
    public String getMulticastAddress() {
        return "230.0.0.0";
    }

    public int getPort() {
        return 4446;
    }

    public int getNodeTimeout() {
        return 5000;
    }
    
    public int getHearbeatDelay() {
        return 20000;
    }
    
    public int getHearbeatPeriod() {
        return 20000;
    }

    public int getLogLevel() {
        return Log.DEBUG;
    }

}