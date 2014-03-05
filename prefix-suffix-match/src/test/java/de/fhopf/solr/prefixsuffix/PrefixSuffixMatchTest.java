package de.fhopf.solr.prefixsuffix;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.Before;

public class PrefixSuffixMatchTest extends SolrTestCaseJ4 {

    @BeforeClass
    public static void initTestCore() throws Exception {
        SolrTestCaseJ4.initCore("solrhome/collection1/conf/solrconfig.xml", "solrhome/collection1/conf/schema.xml", "solrhome/");
    }

    public void index(String value) {
        super.clearIndex();
        assertU(adoc("id", "1", "text", value));
        assertU(commit());

    }
    
    private static SolrQueryRequest request(String query, String... queryFields) {
        ModifiableSolrParams params = new SolrQuery(query);
        params.set("qt", "/select");
        params.set("wt", "xml");
        params.set("debug", true);
        params.set("defType", "edismax");
        if (queryFields != null && queryFields.length > 0) {
            params.add("qf", queryFields);
        }
        return req(params);
    }
    
    @Test
    public void prefixMatchWithWildcard() {
        index("Dumpling");
        assertQ(request("dump*"),
                "//result[@numFound='1']");
    }
    
    @Test
    public void suffixMatchWithWildcard() {
        index("Semmelknödel");
        assertQ(request("*knödel"),
                "//result[@numFound='1']");
    }
    
    @Test
    public void noPrefixMatchOnNonWildcard() {
        index("Dumpling");
        assertQ(request("dump"),
                "//result[@numFound='0']");
    }
    
    @Test
    public void noSufffixMatchOnNonWildcard() {
        index("Semmelknödel");
        assertQ(request("Knödel"),
                "//result[@numFound='0']");
    }
    
    
    @Test
    public void prefixMatchOnNGram() {
        index("Dumpling");
        assertQ(request("dump", "text", "text_prefix"),
                "//result[@numFound='1']");
    }
    
    @Test
    public void suffixMatchOnNGram() {
        index("Semmelknödel");
        assertQ(request("Knödel", "text", "text_prefix", "text_suffix"),
                "//result[@numFound='1']");
    }
    
}
