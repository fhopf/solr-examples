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


public class SynonymTest extends SolrTestCaseJ4 {

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
    
    public void index(String id, String field, String title) {
        assertU(adoc(field, title, "id", id));
        assertU(commit());
    }
    
    private static SolrParams queryParams(String query) {
        ModifiableSolrParams params = new SolrQuery(query);
        params.set("qt", "/select");
        params.set("wt", "xml");
        params.set("debug", true);
        params.set("defType", "edismax");
        params.set("spellcheck", "true");
        params.set("spellcheck.q", query);
        params.set("spellcheck.collate", "true");
        params.set("spellcheck.dictionary", "de");
        return params;
    }
    
    @Test
    public void indextimeSynonyms() throws SolrServerException {
        indexTestDocuments("synonyms_indextime");
        expectFruchttiger("synonyms_indextime", 2);
        expectFrucht_Tiger("synonyms_indextime", 2);
        expectSingleTermSynonym("synonyms_indextime", 2);
    }

    private void expectFruchttiger(String field, int amount) throws SolrServerException {
        expectQuery(field + ":fruchttiger", amount);
    }

    private void expectFrucht_Tiger(String field, int amount) throws SolrServerException {
        expectQuery(field + ":(frucht tiger)", amount);
    }

    private void expectSingleTermSynonym(String field, int amount) throws SolrServerException {
        expectQuery(field + ":singletermsynonym", amount);
    }

    private void expectQuery(String query, int amount) throws SolrServerException {
        QueryResponse response = embeddedSolrServer.query(queryParams(query));
        assertEquals(query, amount, response.getResults().getNumFound());
    }

    private void indexTestDocuments(String field) {
        index("1", field, "fruchttiger");
        index("2", field, "frucht tiger");
        index("3", field, "fruchtigem");
    }

    @Test
    public void querytimeSynonyms() throws SolrServerException {
        indexTestDocuments("synonyms_querytime");
        expectFruchttiger("synonyms_querytime", 1);
        expectFrucht_Tiger("synonyms_querytime", 1);
        expectSingleTermSynonym("synonyms_querytime", 1);
    }

    @Test
    public void bothSynonyms() throws SolrServerException {
        indexTestDocuments("synonyms_both");
        expectFruchttiger("synonyms_both", 2);
        //expectFrucht_Tiger("synonyms_both", 2);
        expectSingleTermSynonym("synonyms_both", 2);
    }

}
