package org.jsoup;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by erlantz on 7/07/16.
 */
public class VeTest {

    private static HashMap<String, ArrayList<String>> formMappings = new HashMap<String, ArrayList<String>>();

    @BeforeClass
    public static void initClass() {
        URL csvUrl = Thread.currentThread().getContextClassLoader().getResource("form_mappings.csv");
        BufferedReader br = null;
        String line;
        String cvsSplitBy = "\t";
        String[] formMappingStr;

        try {
            br = new BufferedReader(new FileReader(csvUrl.getFile()));
            while ((line = br.readLine()) != null) {

                // use comma as separator
                formMappingStr = line.split(cvsSplitBy);

                if (formMappingStr[0].equals("NULL") || formMappingStr[0].equals("#")) continue;

                try {
                    URL url = new URL(formMappingStr[0]);
                    ArrayList<String> formMapingList = formMappings.get(formMappingStr[0]);
                    if (formMapingList != null) {
                        formMapingList.add(formMappingStr[2]);
                    } else {
                        ArrayList<String> fmList = new ArrayList<String>();
                        fmList.add(formMappingStr[2]);
                        formMappings.put(formMappingStr[0], fmList);
                    }
                }  catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(line);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void ApplyFormMapping() throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("results.csv", true));
        formMappings.keySet().parallelStream().forEach((url) -> {
            try {
                try {
                    Document document = Jsoup.parse(new URL(url), 5000);
                    for (String formMapping : formMappings.get(url)) {
                        if (formMapping.equals("window.location.href") || formMapping.equals("")) continue;
                        System.out.println(String.format("Processing -> url: %s query: %s", url, formMapping));
                        Elements elements = document.select(formMapping);
                        if (elements.size() == 0) {
                            //System.out.println(String.format("Failed to apply %s on %s", formMapping, url));
                            bw.append(String.format("%s\t%s\tNo formmappings encountered\n", url, formMapping));
                        } else if (elements.size() > 1) {
                            bw.append(String.format("%s\t%s\tMultiple formmappings encountered: %s\n", url, formMapping, elements.size()));
                        } else {
                            bw.append(String.format("%s\t%s\t%s\n", url, formMapping, elements.first().toString().replaceAll("(\\r|\\n)", "")));
                        }
                        elements.clear();
                        elements = null;
                    }
                    document = null;
                } catch (IOException e) {
                    //System.out.println(String.format("Failed to parse %s", url));
                    bw.append(String.format("%s\tnull\tFailed to parse url\n", url));
                }
                //bw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        bw.close();
    }

    @Test
    public void ApplySpecificFormMapping() {
        try {
            Document document = Jsoup.parse(new URL("http://www.tennerstore.com/collections/bags-purses/products/3d-effect-leopard-printed-box-clutch"), 5000);
            Elements elements = document.select(".selector-wrapper select :checked");
            assert elements.size() > 0;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static class FormMapping {
        URL url;
        String query;

        FormMapping(URL url, String query) {
            this.url = url;
            this.query = query;
        }
    }
}
