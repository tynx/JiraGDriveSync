package JiraClient;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.Normalizer;

import javax.net.ssl.HttpsURLConnection;

import JiraGDriveSync.JiraGDriveSync;
import JiraGDriveSync.Utils.Logger;

/**
 * This class is for a single HTTP-Request specialy crafted for the Jira-REST-
 * API. It is threadable and you should use it in combination with the JiraREST
 * Client.
 * @author Tim Luginbühl
 *
 */
public class RESTRequest implements Runnable {

	/**
	 * The URL to fetch
	 */
	private URL url = null;

	/**
	 * HTTP-Method
	 */
	private String method = "GET";

	/**
	 * HTTP-Header: Auth (basic auth)
	 */
	private String auth = null;

	/**
	 * HTTP-Header: Content-Type
	 */
	private String contentType = "application/json";

	/**
	 * The Body of the HTTP-Response as a String
	 */
	private String response = null;

	/**
	 * HTTP-Header: User-Agent
	 */
	private String userAgent = null;

	/**
	 * HTTP-Header: Accepted-Charset
	 */
	private String charset = "utf-8";

	/**
	 * If the request was issued => true
	 */
	private boolean sent = false;

	/**
	 * If the HTTP-Response-Code was 2xx => true
	 */
	private boolean successful = false;

	/**
	 * If this attribute stays null, the output will be stored as string in
	 * repsonse. otherwise it will be saved in the (here) given filename.
	 */
	private String filename = null;

	/**
	 * Constructor which sets up basic properties.
	 * @param url the url to fetch
	 */
	public RESTRequest(URL url){
		this.userAgent = JiraGDriveSync.config.getProperty("http_user_agent");
		this.url = url;
	}

	/**
	 * If you need to use auth, provide it here (HTTP-Header: Auth).
	 * @param auth the auth
	 */
	public void setAuthentication(String auth){
		this.auth = auth;
	}

	/**
	 * Default Content-Type is: application/json, if you wanna change
	 * use this method. (HTTP-Header: Content-Type)
	 * @param type
	 */
	public void setContentType(String type){
		this.contentType = type;
	}

	/**
	 * Is true when the request was already issued.
	 * @return returns true when a request was already issued
	 */
	public boolean isSent(){
		return this.sent;
	}

	/**
	 * If the HTTP-Code in the HTTP-Response was 2xx, then this will be true.
	 * @return returns true if the request was successful
	 */
	public boolean isSuccessful(){
		return this.successful;
	}

	/**
	 * If the output was not stored in a file, then this will return the body
	 * of the HTTP-Response. (also if the http-response was an error, then
	 * the error-body will be returned). So check first via isSuccessful.
	 * @return
	 */
	public String getResponse(){
		return this.response;
	}

	/**
	 * Set a filename. If this is done, the response-body will be stored in the
	 * provided in the given file.
	 * @param filename the absolute path to the file to store the http-body
	 */
	public void responseToDisk(String filename){
		this.filename = filename;
	}

	/**
	 * This method does take an InputStream and write all of it into the file
	 * provided by filename. 
	 * @param is the inputstream to read from
	 * @throws IOException if an error occurs while opening/reading/writing the
	 * file.
	 */
	private void saveToDisk(InputStream is) throws IOException{
		byte[] buffer = new byte[8 * 1024];
		OutputStream output = new FileOutputStream(this.filename);
		int bytesRead;
		while ((bytesRead = is.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		output.close();
	}

	/**
	 * This method takes an inputstream and stores the content in it into a local
	 * String.
	 * @param is the inputstream to read from
	 * @throws IOException if an error occurs while reading
	 */
	private void setResponse(InputStream is) throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String read = br.readLine();
		StringBuilder sb=new StringBuilder();
		while(read != null) {
			sb.append(read);
			read = br.readLine();
		}
		br.close();
		is.close();
		this.response = Normalizer.normalize(sb.toString(), Normalizer.Form.NFC);
	}

	/**
	 * This sends an request. It detects if the request was successful and if
	 * the body should be stored within a file or a local String.
	 */
	public void send(){
		Logger logger = new Logger(this.getClass().getSimpleName());
		this.sent = true;
		logger.debug("Requesting: " + this.url.toExternalForm());
		try {
			HttpsURLConnection c = (HttpsURLConnection) this.url.openConnection();
			c.setRequestMethod(this.method);
			if(this.method == "GET")
				c.setRequestProperty("Content-length", "0");
			c.setRequestProperty("Content-Type", this.contentType);
			c.setRequestProperty("Accept-Charset", this.charset);
			c.setRequestProperty("User-Agent", this.userAgent);
			if(this.auth != null) {
				c.setRequestProperty("Authorization", this.auth);
			}
			c.setUseCaches(false);
			c.setAllowUserInteraction(false);
			c.connect();
			if(c.getResponseCode() > 199 && c.getResponseCode() < 300){
				if(this.filename != null)
					this.saveToDisk(c.getInputStream());
				else
					this.setResponse(c.getInputStream());
				this.successful = true;
			}else{
				this.setResponse(c.getErrorStream());
				logger.warn("Failed to downlaod: " + this.url.toExternalForm());
				logger.debug("Reason: \n" + this.getResponse());
			}
		} catch (IOException e) {
			logger.err("We got an IOException whiel downloading: " + this.url.toExternalForm());
		}
	}

	/**
	 * As this class is a Runnable we need to implement run, which basically
	 * just is a alias for send();
	 */
	@Override
	public void run() {
		this.send();
	}
}
