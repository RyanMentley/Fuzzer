package example.fuzzer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;

public class Fuzzer {

//	private static String targetURL = "http://apps-staging.rit.edu/cast/eave/";
	private static String targetURL = "http://localhost:8080/bodgeit/";
	private static String targetFileExtension = ".jsp";
	private static final String testUsername = "Fuzzer";
	private static final String testPassword = "Test";
	private static final boolean pageDiscovery = true;
	private static final boolean pageGuessing = true;
	private static final boolean completeness = false;  //Random = false, Full = True;
	private static final boolean passwordguessing = false;
	private static final boolean verbose = false;
	private static final int wait = 100;
	
	private static final String XSS_VECTORS_FILENAME = "xssvectors.txt";
	private static final String SQLI_VECTORS_FILENAME = "sqlivectors.txt";
	private static final String COMMON_PASSWORDS_FILENAME = "passwords.txt";
	private static final String SUCCESS_STRINGS_FILENAME = "success.txt";
	
	private static List<String> xssVectors = null;
	private static List<String> sqliVectors = null;
	private static List<String> commonPasswords = null;
	private static List<String> successStrings = null;
	
	private static Queue<String> toBeFuzzed = new LinkedList<String>();
	private static HashSet<String> foundLinks = new HashSet<String>();
	
	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
//		HtmlPage post_login = authenticate(webClient);
		foundLinks.add(targetURL);
		toBeFuzzed.add(targetURL);
		while(!toBeFuzzed.isEmpty()){
			String target = toBeFuzzed.remove();
			System.out.println("Fuzzing page: " + target);
			HtmlPage fuzzPage = webClient.getPage(target);
			
			postFormsAndParams(fuzzPage);
			if (pageDiscovery) {
				List<HtmlAnchor> found = discoverLinks(webClient, null, fuzzPage);
				for(HtmlAnchor anchor : found){
					String url = anchor.getHrefAttribute();
					url = concatUrl(target, url);
					if (url != null && !foundLinks.contains(url)) {
						foundLinks.add(url);
						toBeFuzzed.add(url);
					}
				}
			}
		}
		
		//doFormPost(webClient);
		webClient.closeAllWindows();
	}
	
	private static List<String> loadVectorsFromFile(String filename) {
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			System.err.println("Ya dun goofed, that file doesn't exist.");
			e.printStackTrace();
			System.exit(1);
			return null;
		}
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		LinkedList<String> list = new LinkedList<String>();
		try {
			String line = br.readLine();
			while (line != null) {
				list.add(line);
				line = br.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			// Do nothing, just print the error and return whatever we have so far 
		}
		return list;
	}
	
	private static List<String> getXssVectors() {
		if (xssVectors == null) {
			xssVectors = loadVectorsFromFile(XSS_VECTORS_FILENAME);
		}
		return xssVectors;
	}
	
	private static List<String> getSqliVectors() {
		if (sqliVectors == null) {
			sqliVectors = loadVectorsFromFile(SQLI_VECTORS_FILENAME);
		}
		return sqliVectors;
	}
	
	private static List<String> getCommonPasswords() {
		if (commonPasswords == null) {
			commonPasswords = loadVectorsFromFile(COMMON_PASSWORDS_FILENAME);
		}
		return commonPasswords;
	}
	
	private static List<String> getSuccessStrings() {
		if (successStrings == null) {
			successStrings = loadVectorsFromFile(SUCCESS_STRINGS_FILENAME);
		}
		return successStrings;
	}
	
	private static String concatUrl (String url1, String url2) {
		URL _url1, _url2;
		try {
			_url1 = new URL(url1);
			_url2 = new URL(_url1, url2);
		} catch (MalformedURLException e) {
			return null;
		}
		return _url2.toString();
	}
	
	/**
	 * Returns a list of the children of the given element (recursively)
	 * @param elem
	 * @return
	 */
	public static List<HtmlElement> getChildren (HtmlElement elem) {
		List<HtmlElement> list = new ArrayList<HtmlElement>();
		getChildren(elem, list);
		return list;
	}
	
	/**
	 * Adds all the children of elem to list
	 * @param elem
	 * @param list
	 */
	public static void getChildren (HtmlElement elem, List<HtmlElement> list) {
		for (HtmlElement child :elem.getChildElements()) {
			System.out.println(child.toString());
			list.add(child);
			getChildren(child, list);
		}
	}
	
	/**
	 * Returns a list of the children of the given element (recursively) that are inputs
	 * @param elem
	 * @return
	 */
	public static List<HtmlInput> getInputs (HtmlElement elem) {
		List<HtmlInput> list = new ArrayList<HtmlInput>();
		getInputs(elem, list);
		return list;
	}
	
	/**
	 * Adds all the children of elem that are inputs to list
	 * @param elem
	 * @param list
	 */
	public static void getInputs (HtmlElement elem, List<HtmlInput> list) {
		for (HtmlElement child :elem.getChildElements()) {
			if (child instanceof HtmlInput) {
				System.out.println("Discovered input: " + child.toString());
				list.add((HtmlInput)child);
			}
			getInputs(child, list);
		}
	}
	
	public static HtmlSubmitInput getSubmit (List<HtmlInput> inputs) {
		for (HtmlInput input : inputs) {
			if (input instanceof HtmlSubmitInput) {
				return (HtmlSubmitInput)input;
			}
		}
		return null;
	}
	/**
	 * MAthod that provides a working username and passowrd to simulate logging into a syatem
	 */
	public static HtmlPage authenticate(WebClient client)throws IOException, MalformedURLException {
		HtmlPage page = client.getPage(targetURL);
		HtmlForm form = page.getForms().get(0);
		
		HtmlTextInput username =  form.getInputByName("username");
		HtmlPasswordInput password = form.getInputByName("password");
		username.setValueAttribute(testUsername);
		password.setValueAttribute(testPassword);
		
		HtmlSubmitInput submit = form.getInputByValue("Login");
		
		HtmlPage post_login = submit.click();
		
		System.out.println("Post login page name: "+ post_login.asText());
	
		return post_login;
		/*List<HtmlForm> forms = page.getForms();
		for(HtmlForm form : forms){
			System.out.println("Form: "+form.asText() + "\n");
		}*/
	}
	private static void postFormsAndParams(HtmlPage page)throws IOException, MalformedURLException{
		String url = page.getUrl().toString();
		List<HtmlForm> forms = page.getForms();
		for(HtmlForm form : forms){
			System.out.println("Discovered form: " + form.getNameAttribute());
			List<HtmlInput> inputs = getInputs(form);
			HtmlSubmitInput submit = getSubmit(inputs);
			for (HtmlInput input : inputs) {
				String originalValue = input.getValueAttribute();
				for (String vector : getSqliVectors()) {
					if (verbose) {
						System.out.println("Testing vector [" + vector + "] on input [" + input + "]");
					}
					input.setValueAttribute(vector);
					Page p;
					try {
						p = submit.click();
						String result = p.getWebResponse().getContentAsString();
						for (String str : getSuccessStrings()) {
							if (result.contains(str)) {
								System.out.println("SUCCESS!  Fuzz vector found: [" + vector + "] on input [" + input + "] triggering success condition [" + str + "] on page [" + url + "]");
							}
						}
					} catch (FailingHttpStatusCodeException e1) {
						// TODO Auto-generated catch block
						System.out.println("SUCCESS!  Fuzz vector found: [" + vector + "] on input [" + input + "] triggering success condition [Failing HTTP status code] on page [" + url + "]");
					}
					
					try {
						Thread.sleep(wait);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				input.setValueAttribute(originalValue);
			}
			
		}
	}
	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static List<HtmlAnchor> discoverLinks(WebClient webClient, String URL, HtmlPage postLogin) throws IOException, MalformedURLException {
		HtmlPage page = null;
		if (URL != null){
			page = webClient.getPage(URL);
		}else{
			page= postLogin;
		}
		List<HtmlAnchor> links = page.getAnchors();
		for (HtmlAnchor link : links) {
			System.out.println("Link discovered: " + link.asText() + " @URL=" + link.getHrefAttribute());
		}
		return links;
	}


	/**
	 * This code is for demonstrating techniques for submitting an HTML form. Fuzzer code would need to be
	 * more generalized
	 * @param webClient
	 * @throws FailingHttpStatusCodeException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private static void doFormPost(WebClient webClient) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		HtmlPage page = webClient.getPage("http://localhost:8080/bodgeit/product.jsp?prodid=26");
		List<HtmlForm> forms = page.getForms();
		for (HtmlForm form : forms) {
			HtmlInput input = form.getInputByName("quantity");
			input.setValueAttribute("2");
			HtmlSubmitInput submit = (HtmlSubmitInput) form.getFirstByXPath("//input[@id='submit']");
			System.out.println(submit.<HtmlPage> click().getWebResponse().getContentAsString());
		}
	}
	
	private static void checkAlerts(WebClient webClient, String URL) throws Exception {
		List<String> collectedAlerts = new ArrayList<String>();
		webClient.setAlertHandler(new CollectingAlertHandler(collectedAlerts));
		HtmlPage page = webClient.getPage(URL);
		for (String alert : collectedAlerts) {
			System.out.println("Alert Got: " + alert + ", Alert Expected: XSS");
		}
	}
}
