package app;

public class Settings {
    // Singleton - Pattern - Foo
    private static Settings instance;

    private Settings() {
    }

    public static Settings getInstance() {
        if (Settings.instance == null) {
            Settings.instance = new Settings();
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

}