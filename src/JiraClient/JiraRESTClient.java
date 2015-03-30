package JiraClient;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;

import JiraGDriveSync.JiraGDriveSync;
import JiraGDriveSync.Utils.Logger;

/**
 * This class provides an easy way to talk to the Jira REST-API. We need
 * authentication and it also allows to thread multiple requests, as the 
 * limitation is not the network-speed, but the API which takes some time
 * to respond.
 * @author Tim Luginbühl
 *
 */
public class JiraRESTClient {

	/**
	 * The logger, to log some messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * If we want to send multiple request in parallel, you can register
	 * request, which are stored in here. This way, we are capable of
	 * returning the output of the request.
	 */
	private Map<URL, RESTRequest> requests = new HashMap<URL, RESTRequest>();

	/**
	 * The final authentication (basic-auth with Base64)
	 */
	private String authentication = null;

	/**
	 * The constructor sets up the authentication based on the config
	 */
	public JiraRESTClient(){
		String user = JiraGDriveSync.config.getProperty("jira_api_user");
		String pass = JiraGDriveSync.config.getProperty("jira_api_pass");
		String base64 = Base64.encodeBase64String((user + ":" + pass).getBytes());
		this.authentication = "Basic " + base64;
	}

	/**
	 * This sends an request immediately. The answer can be fetched via
	 * getResponse and isSuccessful by providing the same URL-Object.
	 * @param url the url to fetch
	 */
	public void sendRequest(URL url){
		this.sendRequest(url, null);
	}

	/**
	 * This method allows to send a request and save the output in a file. If
	 * the provided File is null, the output will be stored as a String inside
	 * the request-object an can be fetched via getResonse() or isSuccessful().
	 * @param url the url to fetch
	 * @param file the file to store the data in
	 */
	public void sendRequest(URL url, File file){
		this.logger.debug("Sending request to URL: " + url.toExternalForm());
		RESTRequest request = new RESTRequest(url);
		if(file != null)
			request.responseToDisk(file.getAbsolutePath());
		request.setAuthentication(this.authentication);
		request.send();
		this.requests.put(url, request);
	}

	/**
	 * This method registers a request. It will not be sent until
	 * sendRegisteredRequest() is called. This makes it possible to parallelize
	 * the request.
	 * @param url the url to fetch
	 */
	public void registerRequest(URL url){
		this.registerRequest(url, null);
	}

	/**
	 * This method registers a request. It will not be sent until
	 * sendRegisteredRequest() is called. This makes it possible to parallelize
	 * the request. If the provided File is null, the output will be stored as
	 * a String inside the request-object an can be fetched via getResonse() or
	 * isSuccessful().
	 * @param url the url to fetch
	 */
	public void registerRequest(URL url, File file){
		RESTRequest request = new RESTRequest(url);
		if(file != null)
			request.responseToDisk(file.getAbsolutePath());
		request.setAuthentication(this.authentication);
		this.requests.put(url, request);
	}

	/**
	 * Tells you if the sent request was successful or not. Provide the same
	 * URL-object.
	 * @param url the URL which was fetched
	 * @return returns true if the request was successful (HTTP-Code 2xx)
	 */
	public boolean isSuccessful(URL url){
		return this.requests.get(url).isSuccessful();
	}

	/**
	 * Returns the content of the fetched URL. Make sure you didn't store the
	 * content into a file, because this will return null if you did so.
	 * @param url the url which was fetched
	 * @return the body of the HTTP-Response (not depending on HTTP-Code, so you
	 * also will get error-bodys)
	 */
	public String getResponse(URL url){
		return this.requests.get(url).getResponse();
	}

	/**
	 * This will send all you registered request in parallel (8 at max). It is
	 * also blocking, meaning the method will only return if all requests-thread
	 * have terminated.
	 * @return
	 */
	public int sendRegisteredRequests(){
		this.logger.debug("Sending registered Requests. Amount: " + this.requests.size());
		ExecutorService executor = Executors.newFixedThreadPool(8);
		for (Entry<URL, RESTRequest> entry : this.requests.entrySet()) {
			if(!entry.getValue().isSent())
				executor.execute(entry.getValue());
		}
		executor.shutdown();
		try {
			executor.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			System.err.println("Awaiting failed. Reason: ");
			e.printStackTrace();
			return -2;
		}
		this.logger.debug("Finished sending registered Requests.");
		for (Entry<URL, RESTRequest> entry : this.requests.entrySet()) {
			if(!entry.getValue().isSuccessful()){
				this.logger.err("Some requests didn't succed!");
				return -1;
			}
		}
		return 0;
		
	}
}
