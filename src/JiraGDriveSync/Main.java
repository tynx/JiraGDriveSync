package JiraGDriveSync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import GDriveClient.GDriveClient;
import GDriveClient.GoogleOAuth2;
import JiraClient.JiraRESTClient;

/**
 * Entry point of the sync application. It does load the config and validates
 * the arguments provided. 2 are possible: sync and login.
 * A login is required before being able to sync.
 * @author Tim Luginbühl
 *
 */
public class Main {

	/**
	 * The location of the config-file
	 */
	public static final String CONFIG_LOCATION = "/etc/JiraGDriveSync/config.properties"; 

	/**
	 * The actual entry point. It does read the config. based on the given
	 * args it does either sync or perform a login against the GDrive-Api.
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 1){
			System.err.println("Insufficent arguments! provide: login or sync");
			System.exit(-1);
		}
		Main.loadConfig();

		// SYNC
		if(args[0].equals("sync")){
			Main.initClients();
			new JiraGDriveSync().sync();
		// LOGIN
		}else if(args[0].equals("login")){
			try {
				GoogleOAuth2 auth = new GoogleOAuth2();
				auth.loginOAuth();
				System.out.println("Login successful! You're now able to sync!");
			} catch (IOException e) {
				System.err.println("Login failed! Reason: ");
				e.printStackTrace();
				System.exit(-1);
			}
		// Nothing to do
		}else{
			System.err.println("Invalid arguments! provide: login or sync");
			System.exit(-1);
		}
		System.exit(0);
	}

	/**
	 * This method tries to load the config-file
	 */
	public static void loadConfig(){
		JiraGDriveSync.config = new Properties();
		FileInputStream input = null;
		try {
			if(!(new File(Main.CONFIG_LOCATION)).exists())
				input = new FileInputStream("./config.properties");
			else
				input = new FileInputStream(Main.CONFIG_LOCATION);
			JiraGDriveSync.config.load(input);
			input.close();
		} catch (IOException e) {
			System.out.println("Couldn't load config file. Exiting!");
			System.exit(-1);
		}
	}

	/**
	 * This method tries to init both clients. Especially with GDrive and the
	 * GoogleOAuth there is a lot that can go wrong...
	 */
	public static void initClients(){
		try {
			JiraGDriveSync.gdriveClient = new GDriveClient();
		} catch (Exception e) {
			System.out.println("Couldn't init google-drive-client. Have you logged in?");
			System.exit(-1);
		}
		JiraGDriveSync.jiraClient = new JiraRESTClient();
	}
}
