package scl.oms.outagemap;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Provides for basic application logging. One log file is created for each day,
 * and appended thereafter during that day.
 *
 * @author jstewart
 */
public class Log {

    private static Logger appLogger;
    private static FileHandler appFileHandler;

    /**
     * Creates logger.
     */
    private Log() {
        super();
    }

    /**
     * Obtains a singleton instance of a logger.
     *
     * @return this logger
     * @throws IOException
     */
    public static Logger getLogger() throws IOException {
        if (appLogger == null) {
            throw new IOException("Logger has not yet been created.");
        }
        return appLogger;
    }

    /**
     * Obtains a singleton instance of a logger.
     *
     * @param applicationPath the path to the application's main class or .jar
     * file
     * @return this logger
     * @throws IOException
     */
    public static Logger getLogger(String applicationPath) throws IOException {
        if (appLogger == null) {

            DateFormat applicationDateFormat;
            applicationDateFormat = new SimpleDateFormat("yyyy-MM-dd");

            Date todaysDate;
            todaysDate = new Date();

            SimpleFormatter simpleFormater = new SimpleFormatter();

            try {
                appLogger = Logger.getLogger("scl.oms.outagemap");

                String outputFilePath;
                if (Config.INSTANCE.getLogDirectory().length() == 0) {
                    outputFilePath = applicationPath + "log/";
                } else {
                    outputFilePath = Config.INSTANCE.getLogDirectory() + '/';
                }
                appLogger.log(Level.INFO, "Establish logging at {0}. (environment={1})",
                        new Object[]{outputFilePath, Config.INSTANCE.getEnvironmentLabel()});

                try {
                    appFileHandler = new FileHandler((outputFilePath + "outagemap_log_" + applicationDateFormat.format(todaysDate) + ".txt"), true);
                } catch (IOException e) {
                    System.out.println("ERROR: Unable to create logging file handler in scl.oms.outagemap");
                    throw e;
                }

                appFileHandler.setFormatter(simpleFormater);
                appLogger.addHandler(appFileHandler);
                appLogger.setLevel(Level.ALL); //
            } catch (IOException e) {
                System.out.println("ERROR: Unable to create Logger in scl.oms.outagemap.");
                throw e;
            }
        }
        return appLogger;
    }

    /**
     * Closes the log file.
     */
    public static void closeLogger() {
        appFileHandler.close();
    }
}
