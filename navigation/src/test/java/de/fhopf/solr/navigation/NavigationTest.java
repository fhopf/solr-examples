package de.fhopf.solr.navigation;

import java.util.Arrays;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class NavigationTest extends SolrTestCaseJ4 {

    private EmbeddedSolrServer embeddedSolrServer;
    
    @BeforeClass
    public static void initTestCore() throws Exception {
        SolrTestCaseJ4.initCore("solrhome/collection1/conf/solrconfig.xml", "solrhome/collection1/conf/schema.xml", "solrhome/");
    }
    
    @Before
    public void initSolrServer() {
        embeddedSolrServer = new EmbeddedSolrServer(h.getCoreContainer(), h.getCore().getName());
    }

    @After
    public void clearTheIndex() {
        super.clearIndex();
    }
    
    public void index(String title, String category) {
        assertU(adoc("title", title, "category", category));
        assertU(commit());
    }
    
    private static SolrParams queryParams(String categoryQuery) {
        ModifiableSolrParams params = new SolrQuery("*:*");
        params.set("qt", "/select");
        params.set("wt", "xml");
        params.set("debug", true);
        params.set("facet", true);
        params.add("facet.field", "category");
        if (categoryQuery != null) {
            params.add("fq", categoryQuery);
        }
        params.set("defType", "edismax");
        return params;
    }
    
    @Test
    public void indexingAndRetrievingWorks() throws SolrServerException {
        index("What I Talk About When I Talk About Running", "Books/Non-Fiction/Haruki Murakami");
        QueryResponse response = embeddedSolrServer.query(queryParams(null));
        assertEquals(1, response.getResults().getNumFound());
        FacetField facetField = response.getFacetField("category");
        // 3 categories
        assertEquals(3, facetField.getValueCount());
        assertContains(facetField, "Books", "Books/Non-Fiction", "Books/Non-Fiction/Haruki Murakami");
    }

    @Test
    public void hierarchies() throws SolrServerException {
        index("What I Talk About When I Talk About Running", "100|Books/110|Non-Fiction/111|Haruki Murakami");
        index("South of the Border, West of the Sun", "100|Books/120|Fiction/121|Haruki Murakami");
        index("Own Your Ghost", "200|Music/210|Downbeat/211|13&God");
        QueryResponse response = embeddedSolrServer.query(queryParams(null));
        assertEquals(3, response.getResults().getNumFound());
        FacetField facetField = response.getFacetField("category");
        // books is there twice
        assertEquals(8, facetField.getValueCount());
        assertContains(facetField, 
                "100|Books", 
                "100|Books/110|Non-Fiction", 
                "100|Books/110|Non-Fiction/111|Haruki Murakami",
                "100|Books/120|Fiction",
                "100|Books/120|Fiction/121|Haruki Murakami",
                "200|Music",
                "200|Music/210|Downbeat",
                "200|Music/210|Downbeat/211|13&God");
    }
    
    @Test
    public void filtering() throws SolrServerException {
        index("What I Talk About When I Talk About Running", "100|Books/110|Non-Fiction/111|Haruki Murakami");
        index("South of the Border, West of the Sun", "100|Books/120|Fiction/121|Haruki Murakami");
        index("Own Your Ghost", "200|Music/210|Downbeat/211|13&God");
        QueryResponse response = embeddedSolrServer.query(queryParams("category:100|Books/110|Non-Fiction"));
        assertEquals(1, response.getResults().getNumFound());
        response = embeddedSolrServer.query(queryParams("category:100|Books"));
        assertEquals(2, response.getResults().getNumFound());
    }
    
    private void assertContains(FacetField field, String... values) {
        boolean [] found = new boolean[values.length];
        for (FacetField.Count count: field.getValues()) {
            int indexPosition = Arrays.binarySearch(values, count.getName());
            if (indexPosition == -1) {
                fail("Unexpected facet value: " + count.getName());
            }
            found[indexPosition] = true;
        }
        for (boolean flag: found) {
            assertTrue("Some values not found: " + field, flag);
        }
    }
}
