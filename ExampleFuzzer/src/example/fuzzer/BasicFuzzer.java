package example.fuzzer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

public class BasicFuzzer {

	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
//		discoverLinks(webClient, "http://localhost:8080/bodgeit");
//		doFormPost(webClient);
		HtmlPage page = webClient.getPage("http://localhost:8080/bodgeit/login.jsp");
		List<HtmlForm> forms = page.getForms();
		for (HtmlForm form : forms) {
			List<HtmlElement> children = new ArrayList<HtmlElement>();
			addChildren(form, children);
		}
		webClient.closeAllWindows();
	}
	
	public static void addChildren (HtmlElement elem, List<HtmlElement> list) {
		for (HtmlElement child :elem.getChildElements()) {
			System.out.println(child.toString());
			list.add(child);
			addChildren(child, list);
		}
	}

	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static List<HtmlAnchor>discoverLinks(WebClient webClient, String URL) throws IOException, MalformedURLException {
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
