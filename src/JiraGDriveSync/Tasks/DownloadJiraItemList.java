package JiraGDriveSync.Tasks;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import JiraGDriveSync.JiraGDriveSync;
import JiraGDriveSync.Domain.JiraItem;
import JiraGDriveSync.Utils.Logger;

/**
 * This API downloads all the items/attachments from a JIRA-Env. It handles
 * pagination and other little "misbehaviour" of the Jira-API (like UTF8-
 * normalization as this is just not done by Jira). This class works, but
 * definitely needs improvement if used for bigger projects/backends than this
 * one.
 *
 */
public class DownloadJiraItemList extends Task {

	/**
	 * The amount of retries for the single requests before failing
	 */
	private final static int MAX_RETRIES = 3;

	/**
	 * The maximum number of items returned by the API. if more than this
	 * number are provided by the API, then this class will automatically handle
	 * the pagination.
	 */
	private final static int MAX_RESULTS = 500;

	/**
	 * The logger, for the messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * The base jira-api-url
	 */
	private String baseURL = null;

	/**
	 * The query part of the url, to find only the relevant issues/attachments
	 */
	private String query = null;

	/**
	 * The JSONParser used to parse the downloaded json
	 */
	private JSONParser parser = new JSONParser();

	/**
	 * The list of the parsed items/attachments
	 */
	private List<JiraItem> items = new ArrayList<JiraItem>();
	
	public DownloadJiraItemList(){
		this.baseURL = JiraGDriveSync.config.getProperty("jira_api_url");
		this.query = JiraGDriveSync.config.getProperty("jira_api_query");
	}

	/**
	 * This method basically returns the found attachments provided by the JIRA-
	 * API. But it does filter out duplicates, as JIRA allows to upload multiple
	 * files with the same filename. As most (if not all) FSs currently only
	 * support one file with the same name, we need to distinct those duplicates.
	 * The rule is simple: only the newest one is returned.
	 * @return
	 */
	public List<JiraItem> getItems(){
		if(this.items.size() == 0)
			return null;
		Map<String, Date> timestamps = new HashMap<String, Date>();
		for(JiraItem item : this.items){
			if(timestamps.containsKey(item.getProjectKey() + item.getIssueKey() + item.getName())){
				Date stored = timestamps.get(item.getProjectKey() + item.getIssueKey() + item.getName());
				if(stored.before(item.getCreated()))
					timestamps.put(item.getProjectKey() + item.getIssueKey() + item.getName(), item.getCreated());
			}else{
				timestamps.put(item.getProjectKey() + item.getIssueKey() + item.getName(), item.getCreated());
			}
		}
		List<JiraItem> result = new ArrayList<JiraItem>();
		for(JiraItem item : this.items){
			if(item.getCreated().getTime() == timestamps.get(item.getProjectKey() + item.getIssueKey() + item.getName()).getTime()){
				result.add(item);
			}
		}
		return result;
	}

	/**
	 * This method downloads a single url and returns the response as a String.
	 * @param url the url to download
	 * @return the content of the url
	 */
	private String download(String url){
		URL download;
		try {
			download = new URL(url);
		} catch (MalformedURLException e) {
			this.logger.err("URL is invalid. We got a MalformedURLException!");
			return null;
		}
		JiraGDriveSync.jiraClient.sendRequest(download);
		return JiraGDriveSync.jiraClient.getResponse(download);
	}

	/**
	 * This method does download (based on the query) all the issues/attachments
	 * wanted. As the JIRA-API does do pagination (by default) this method
	 * also handles that. It tries to download every single item provided. Be
	 * careful: If there are >1k items, it starts to get very slow. Didn't in-
	 * vestigate further as to why exactly (probably again RTT), as we don't
	 * ever have more than >200 items.
	 */
	@Override
	public int doTask(){
		this.logger.info("Downloading Jira-JSON-list.");
		String itemURL = this.baseURL + query + "&maxResults=" + MAX_RESULTS;
		boolean finished = false;
		int start = 0;
		int retries = 0;
		Object answer = null;
		while(!finished){
			String result = this.download(itemURL + "&startAt=" + start);
			if(result.isEmpty()){
				retries++;
				if(retries == MAX_RETRIES)
					return -1;
				continue;
			}
			try {
				this.logger.debug("Parsing result of download.");
				answer = this.parser.parse(result);
			} catch (ParseException e) {
				return -2;
			}
			if(!(answer instanceof JSONObject)){
				return -3;
			}
			Object obIssues = ((JSONObject)answer).get("issues");
			if(!(obIssues instanceof JSONArray))
				return -4;
			JSONArray issues = (JSONArray)obIssues;
			for(int i=0; i<issues.size(); i++){
				if(!(issues.get(i) instanceof JSONObject))
					continue;
				this.items.addAll(JiraItem.populate((JSONObject)issues.get(i)));
			}
			Object obTotal = ((JSONObject)answer).get("total");
			if(!(obTotal instanceof Number))
				return -5;
			int total = ((Number)obTotal).intValue();
			if(total < start + MAX_RESULTS)
				finished = true;
			else
				start += MAX_RESULTS;
		}
		this.logger.info("Downloading Jira-JSON-list finished.");
		return 0;
	}
}
