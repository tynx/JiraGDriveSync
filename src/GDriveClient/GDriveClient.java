package GDriveClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import JiraGDriveSync.Domain.UploadFile;
import JiraGDriveSync.Utils.Logger;

import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

/**
 * This class gives an easy way to interact with the google-drive api. It also
 * takes away the "flat" structure and allows to interact in the traditional
 * FileSystem-hierarchy (folders/files) way. It does not try to log in
 * automatically, as you have to gall GoogleOAuth2.login by yourself to make
 * sure there is an valid auth.
 * @author Tim Luginbühl
 *
 */
public class GDriveClient {

	/**
	 * The logger, for messages
	 */
	private Logger logger = new Logger(this.getClass().getSimpleName());

	/**
	 * Google-Drive API Object
	 */
	private Drive drive = null;

	/**
	 * We want to cache querys as google is pretty slow. so for given path
	 * return the files/folders.
	 */
	private Map<String, List<File>> cache = new HashMap<String, List<File>>(); 

	/**
	 * The constructor tries to set up the Drive-API. This is done in cooperation
	 * with GoogleOAuth2 so we can get a valid Credential. If anything went wrong
	 * an exception is thrown, meaning the whole Drive-API is unusable! So handle
	 * properly.
	 * @throws Exception If invalid credential was found or connection couldn't
	 * be established.
	 */
	public GDriveClient() throws Exception{
		GoogleOAuth2 gAuth = new GoogleOAuth2();
		if(gAuth.getCredential() == null){
			throw new Exception("Credential not found!");
		}
		HttpTransport transport = new NetHttpTransport();
		JsonFactory factory = new JacksonFactory();
		// The actual Drive-API
		this.drive = new Drive.Builder(transport, factory, gAuth.getCredential())
			.setApplicationName("JiraGDriveSync").build();
	}

	/**
	 * As the cache may be getting out of sync, you can reset it manually.
	 */
	public void clearCache(){
		this.cache.clear();
	}

	/**
	 * This adds a list of files for a certain path (key) to the cache.
	 * @param key the path to which the list correlates
	 * @param files the list of files found at given path
	 */
	private void addFilesToCache(String key, List<File> files){
		this.logger.debug("Adding result/files to cache");
		// No overwritting!
		if(this.cache.containsKey(key))
			return;
		this.cache.put(key, files);
	}

	/**
	 * This adds a file for a certain path (key) to the cache.
	 * @param key the path of which the file correlates
	 * @param file the file foudn at given path
	 */
	private void addFileToCache(String key, File file){
		List<File> list = new ArrayList<File>();
		list.add(file);
		this.addFilesToCache(key, list);
	}

	/**
	 * Returns the files found at given path if they were already cached.
	 * @param key the path to which we need the files
	 * @return the files found at given path, or null if nothing is cached
	 */
	private List<File> getFromCache(String key){
		if(this.cache.containsKey(key))
			return this.cache.get(key);
		return null;
	}

	/**
	 * This methods makes sure, every possible provided paths gets into a
	 * uniform way of representing (in a array).
	 * /some/folder/ => [some, folder]
	 * some/folder => [some, folder]
	 * /some//folder/ => [some, folder]
	 * and so on....
	 * @param path the (maybe) invalid path
	 * @return String-Array in a uniform way
	 */
	private String[] getPathElements(String path){
		if(path.length() < 1 || path.equals("/")){
			return new String[0];
		}
		if(path.charAt(0) == '/')
			path = path.substring(1);
		if(path.charAt(path.length()-1) == '/')
			path = path.substring(0, path.length()-1);
		return path.split("/");
	}

	/**
	 * This method creates a folder at given path. Recursive means, that if
	 * any parent provided is not present, it will be created as well.
	 * @param path the path of which all folders should be created
	 * @return true if successful and on fail false
	 */
	public boolean createFolderRecursive(String path){
		String[] pathElements = this.getPathElements(path);
		StringBuilder newPath = new StringBuilder("/");
		for(int i=0; i<pathElements.length; i++){
			newPath.append(pathElements[i] + "/");
			if(!this.createFolder(newPath.toString())){
				this.logger.warn("We don't have all necessary childs. Quitting recursion. Current Path: " + newPath.toString());
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates a folder at given path. It expects that every parent (in the path)
	 * exists already. if the path exists (all of it, so no new folders needed)
	 * it'll skip the operation and just return true. (as if it would have been
	 * created)
	 * @param path the folder to create
	 * @return true if successful and on fail false
	 */
	public boolean createFolder(String path){
		if(this.getFolderByPath(path) != null){
			this.logger.debug("Creating folder ignored, as already exists: " + path);
			return true;
		}
		String[] pathElements = this.getPathElements(path);
		StringBuilder parentPath = new StringBuilder("/");
		for(int i=0; i<pathElements.length-1; i++){
			parentPath.append(pathElements[i] + "/");
		}
		String name = pathElements[pathElements.length-1];
		File folder = new File();
		folder.setTitle(name);
		folder.setMimeType("application/vnd.google-apps.folder");
		
		if(!parentPath.toString().equals("/")){
			File parent = this.getFolderByPath(parentPath.toString());
			if(parent == null){
				this.logger.warn("Failed to get folder: " + parentPath.toString());
				return false;
			}
			ParentReference r = new ParentReference();
			r.setId(parent.getId());
			List<ParentReference> l = new ArrayList<ParentReference>();
			l.add(r);
			folder.setParents(l);
		}
		try {
			folder = this.drive.files().insert(folder).execute();
			if(folder != null){
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * This method retuns a file representing a folder at given path. if no
	 * folder (or even if a file is found) it'll return null. If folder
	 * was found at given path, the folder is returned.
	 * @param path the path to search for
	 * @return the file-object representing folder at given path
	 */
	public File getFolderByPath(String path){
		File file = this.getFileByPath(path);
		if(file == null){
			this.logger.debug("Fail to get file/folder by path: " + path);
			return null;
		}
		if(file.getMimeType().equals("application/vnd.google-apps.folder"))
			return file;
		return null;
	}

	/**
	 * This returns a file given by a certain path. This may be a folder or a
	 * file.
	 * @param path the path to search for the file/folder
	 * @return the file/folder for given path
	 */
	private File getFileByPath(String path){
		File file = null;
		String[] pathElements = this.getPathElements(path);
		for(String part : pathElements){
			if(part.equals(""))
				continue;
			String query = "trashed = false and title = '" + part + "'";
			if(part.equals(pathElements[0]))
				query += " and 'root' in parents";
			else
				query += " and '" + file.getId() + "' in parents";
			file = this.getFileByQuery(query);
		}
		return file;
	}

	/**
	 * This returns a list of file given by a certain path. This list may
	 * contain folder and/or files.
	 * @param path the search for files/folders
	 * @return the list of files/folders
	 */
	public List<File> getFilesByPath(String path){
		File parent = this.getFolderByPath(path);
		if(parent == null)
			return null;
		String query = "trashed = false and '" + parent.getId() + "' in parents";
		return this.getFilesByQuery(query);
	}

	/**
	 * This method will search for a file for the given query (see google-API-doc
	 * for more details). If multiple entries (or zero entries) ar found the
	 * method will return null. This is to make sure, we have exactly the file
	 * we want.
	 * @param query the query to find file for
	 * @return the found file or null
	 */
	private File getFileByQuery(String query){
		List<File> list = this.getFilesByQuery(query);
		if(list.size() == 1)
			return list.get(0);
		return null;
	}

	/**
	 * This method will search for files for the given query (see google-AOI-
	 * docs for more details). The return-list may contain files and/or folders.
	 * @param query The query to find files for
	 * @return the found files/folder or null if none found
	 */
	private List<File> getFilesByQuery(String query){
		if(this.getFromCache(query) != null)
			return this.getFromCache(query);
		List<File> result = new ArrayList<File>();
		try {
			Files.List request = this.drive.files().list();
			request.setQ(query);
			result.addAll(request.execute().getItems());
			if(result.size() != 0)
				this.addFilesToCache(query, result);
			return result;
		} catch (IOException e) {
			this.logger.err("Couldn't fetch data: getFileByQuery");
		}
		return result;
	}

	/**
	 * This method will delete a file or folder by the given Google-Drive ID.
	 * @param id representing a file or folder
	 * @return ture if deletion was successful, otherwise false
	 */
	public boolean deleteFile(String id){
		try {
			this.drive.files().delete(id).execute();
			return true;
		} catch (IOException e) {
			this.logger.err("Failed to delete file with ID: " + id);
		}
		return false;
	}

	/**
	 * This method will upload a file based on the given "UploadFile"-Object.
	 * If the file already exists, the method will automatically check which
	 * of the file is the newer one (either on GDrive or local) and if the local
	 * one is newer, it will overwrite the existing one in GDrive. If the lcoal
	 * one is older, the method will just skip the upload.
	 * @param upload the file to upload
	 * @return true if the upload was successful otherwise false
	 */
	public boolean uploadFile(UploadFile upload){
		File remoteFile = this.getFileByPath(upload.getPath() + upload.getName());
		
		File parent = this.getFolderByPath(upload.getPath());
		ParentReference r = new ParentReference();
		List<ParentReference> l = new ArrayList<ParentReference>();
		r.setId(parent.getId());
		l.add(r);
		
		//Insert a file  
		File body = new File();
		body.setTitle(upload.getName());
		body.setDescription(upload.getDescription());
		body.setMimeType(upload.getMimeType());
		body.setModifiedDate(new DateTime(upload.getModifiedDate().getTime()));
		body.setParents(l);

		FileContent mediaContent = new FileContent(upload.getMimeType(), upload.getLocalFile());

		try {
			File file = null;
			if(remoteFile != null){
				Date remoteDate = new Date(remoteFile.getModifiedDate().getValue());
				if(remoteDate.before(upload.getModifiedDate())){
					file = drive.files().update(remoteFile.getId(), body, mediaContent).execute();
				}else{
					this.logger.debug("Ignoring upload, as newer version is online...");
					return true;
				}
			}else{
				file = drive.files().insert(body, mediaContent).execute();
			}
			this.addFileToCache(upload.getPath() + upload.getName(), file);
			return true;
		} catch (IOException e) {
			this.logger.debug("Failed to upload file with name: " + upload.getName());
			return false;
		}
	}
}
