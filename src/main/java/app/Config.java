package app;

import io.github.cdimascio.dotenv.Dotenv;

/*
    Helper class to hold functions for getting environment variables.
    Checks the system environment variables for a "MODE" that equals "PROD".
    If not found, attempts to load in variables from a .env file.
 */

public class Config {
    private static final Dotenv dotenv;
    private static final boolean productionMode;

    static {
        productionMode = "PROD".equals(System.getenv("MODE"));

        if (productionMode) {
            dotenv = null;
        } else {
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
        }
    }

    private static String getVar(String key) {
        String value;

        if (productionMode) {
            value = System.getenv(key);
        } else {
            value = dotenv.get(key);
        }

        return value;
    }

    public static String require(String key) {
        String value = getVar(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: " + key +
                            (productionMode ? "\nYou should set it in system environment variables." :
                                    "\nYou should add it to your .env file.")
            );
        }

        return value;
    }


}
