import java.util.ArrayList;
import java.util.List;

import javax.swing.JEditorPane;

public class TestStr {
	public static void main(String[] args) {

		JEditorPane entry = new JEditorPane();
		entry.setContentType("text/html");

		String x = "Tell *me about* it! my *friend*";
		System.out.println(x.contains("_"));
		String txt[] = x.split(" ");
		List<String> nList = new ArrayList<String>();

		for (int i = 0; i < txt.length; i++) {
			String word = txt[i];

			System.out.print(word + " ");

			/*
			 * if (word.contains("*")) { nList.add("<b>" + word + "</b>"); } else {
			 * nList.add(word); }
			 * 
			 * System.out.println(nList);
			 */
		}
	}
}
