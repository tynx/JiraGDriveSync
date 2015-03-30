package GDriveClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import JiraGDriveSync.JiraGDriveSync;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;

/**
 * This class handles the Google OAuth2 authentication. It stores the
 * credentials via a datastore (filebased), so an actual login/authorization
 * is only needed once. So after doing GoogleOAuth2.login you're good to go
 * with just using GoogleOAuth2.getCredential.
 * @author Tim Luginbühl
 *
 */
public class GoogleOAuth2 {

	/**
	 * The client ID provided by config
	 */
	private String clientId = null;

	/**
	 * the client Secret provided by config
	 */
	private String clientSecret = null;

	/**
	 * The redirect uri, which is not actually useful in this scenario, but needed
	 * by the API.
	 */
	private String redirectURI = null;

	/**
	 * Where to store the credentials. Provided by config
	 */
	private String storeLocation = null;

	/**
	 * The authorization flow of the Google-API
	 */
	private GoogleAuthorizationCodeFlow flow = null;

	/**
	 * The actual store of the credentials
	 */
	private DataStoreFactory store = null;

	/**
	 * As we only use one credential for this app, we make a shortcut
	 */
	private Credential credential = null;

	/**
	 * The constructor sets up everything for the whole auth. Mostly just fetching
	 * configs and setting up the AuthFlow.
	 * @throws IOException Means, something went wrong in the Google-API
	 */
	public GoogleOAuth2() throws IOException{
		this.clientId = JiraGDriveSync.config.getProperty("google_oauth_client_id");
		this.clientSecret = JiraGDriveSync.config.getProperty("google_oauth_client_secret");
		this.redirectURI = JiraGDriveSync.config.getProperty("google_oauth_redirect_uri");
		this.storeLocation = JiraGDriveSync.config.getProperty("google_oauth_store_location");
		this.store = new FileDataStoreFactory(new java.io.File(this.storeLocation));
		HttpTransport transport = new NetHttpTransport();
		JsonFactory factory = new JacksonFactory();
		this.flow = new GoogleAuthorizationCodeFlow.Builder(
				transport,
				factory,
				this.clientId,
				this.clientSecret,
				Arrays.asList(DriveScopes.DRIVE)
			)
			.setAccessType("offline")
			.setDataStoreFactory(this.store)
			.setApprovalPrompt("auto").build();
		this.credential = flow.loadCredential("gdrive-sync");
	}

	/**
	 * Return the loaded credential. This may be false in case of no successful
	 * login in a previous run.
	 * @return
	 */
	public Credential getCredential(){
		return this.credential;
	}

	/**
	 * This method does a login via the OAuth2 by google. Its interactive, so
	 * make sure it doesnt mess up your workflow (especially when threading).
	 * @throws IOException If an error occured in the google-API
	 */
	public void loginOAuth() throws IOException{
		String url = this.flow.newAuthorizationUrl().setRedirectUri(this.redirectURI).build();
		System.out.println("Please open the following URL in your browser:");
		System.out.println(url);
		System.out.println("You will be forwarded to a localhost-address. copy the get argument code into this terminal:");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String code = br.readLine();
		GoogleAuthorizationCodeTokenRequest tokenRequest = this.flow.newTokenRequest(code);
		tokenRequest.setRedirectUri(this.redirectURI);
		GoogleTokenResponse tokenResponse = tokenRequest.execute();
		this.flow.createAndStoreCredential(tokenResponse, "gdrive-sync");
		this.credential = this.flow.loadCredential("gdrive-sync");
	}

}
