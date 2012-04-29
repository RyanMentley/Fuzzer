package example.fuzzer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

public class Fuzzer {

	private static String targetURL = "http://apps-staging.rit.edu/cast/eave/";
	private static String targetFileExtension = ".jsp";
	private static final boolean pageDiscovery = true;
	private static final boolean pageGuessing = true;
	private static final boolean completeness = false;  //Random = false, Full = True;
	private static final boolean passwordguessing = false;
	
	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		//discoverLinks(webClient, "http://localhost:8080/bodgeit");
		authenticate(webClient);
		//doFormPost(webClient);
		webClient.closeAllWindows();
	}
	/**
	 * MAthod that provides a working username and passowrd to simulate logging into a syatem
	 */
	public static HtmlPage authenticate(WebClient client)throws IOException, MalformedURLException {
		HtmlPage page = client.getPage(targetURL);
		HtmlForm form = page.getForms().get(0);
		
		HtmlTextInput username =  form.getInputByName("username");
		HtmlPasswordInput password = form.getInputByName("password");
		username.setValueAttribute("admin");
		password.setValueAttribute("Lorax");
		
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
		List<HtmlForm> forms = page.getForms();
		for(HtmlForm form : forms){
			System.out.println(form.getNameAttribute());
			//List<HtmlInput> inputs = form.getElementsByTagName(""); //Unfinished
		}
	}
	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static List<HtmlAnchor> discoverLinks(WebClient webClient, String URL) throws IOException, MalformedURLException {
		HtmlPage page = webClient.getPage(URL);
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
}
