package JiraGDriveSync.Utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import JiraGDriveSync.JiraGDriveSync;

/**
 * This class is for providing a very simple way to do the logging via the
 * official "java.util.logging"-mechanism. As they require quite some
 * configuration this class was built.
 * @author Tim Luginbühl
 *
 */
public class Logger {

	/**
	 * This is the actual java-logger
	 */
	private static java.util.logging.Logger logger = null;

	/**
	 * The current category we want to log in
	 */
	private String category = null;

	/**
	 * If we should log the debug-messages or not
	 */
	private boolean debug = false;

	/**
	 * The constructor inits the logger
	 */
	public Logger(){
		try {
			this.init();
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The constructor inits the logger, as saving the current category
	 * @param category the category of app that logs
	 */
	public Logger(String category){
		this.category = category;
		try {
			this.init();
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This inits the logger. It does so with singleton in mind, as there
	 * may be a lot of instances of this class.
	 * @throws SecurityException 
	 * @throws IOException
	 */
	private void init() throws SecurityException, IOException{
		if(JiraGDriveSync.config.getProperty("debug_messages").equals("yes"))
			this.debug = true;
		if(Logger.logger == null){
			Logger.logger = java.util.logging.Logger.getGlobal();
			Logger.logger.setLevel(Level.FINEST);
			Logger.logger.setUseParentHandlers(false);
			String filename = JiraGDriveSync.config.getProperty("log_file");
			FileHandler fh = new FileHandler(filename, true);
			fh.setFormatter(new CustomFormatter());
			logger.addHandler(fh);
		}
	}

	/**
	 * This method does the actual logging of the message
	 * @param msg the message to log
	 * @param level the level for the message
	 */
	private void log(String msg, Level level){
		if(level == Level.FINE && !this.debug)
			return;
		if(Logger.logger == null)
			return;
		if(this.category != null)
			Logger.logger.log(level, "[" + this.category + "] " + msg);
		else
			Logger.logger.log(level, msg);
	}

	/**
	 * Logs a debug message
	 * @param msg the message to log
	 */
	public void debug(String msg){
		this.log(msg, Level.FINE);
	}

	/**
	 * Logs a info message
	 * @param msg the message to log
	 */
	public void info(String msg){
		this.log(msg, Level.INFO);
	}

	/**
	 * Logs a warning message
	 * @param msg the message to log
	 */
	public void warn(String msg){
		this.log(msg, Level.WARNING);
	}

	/**
	 * Logs an error message
	 * @param msg the message to log
	 */
	public void err(String msg){
		this.log(msg, Level.SEVERE);
	}

	/**
	 * This class is just for having a custom (nice) way of logging.
	 * @author Tim Luginbühl
	 *
	 */
	private class CustomFormatter extends Formatter{
		/**
		 * Cusom line-formating
		 */
		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder();
			sb.append(new java.util.Date());
			sb.append(' ');
			sb.append(record.getLevel());
			sb.append(' ');
			sb.append(formatMessage(record));
			sb.append('\n');
			return sb.toString();
		}
	}
}
