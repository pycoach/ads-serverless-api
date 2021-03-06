package com.serverless;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class AsapDetailTest {
    @Test
    /*
     *  Valid start date and end date
     * */
    public void StartnEndDateCase() {
        AsapDetailHandler handler = new AsapDetailHandler();

        Map<String, Object> input = new HashMap<String, Object>();
        Map<String, String> queryStringParameters = new HashMap<String, String>() ;
        queryStringParameters.put("asap", "Ticket 23");
        input.put("queryStringParameters", queryStringParameters);

        ApiGatewayResponse response = handler.handleRequest(input, null);
        System.out.println(response.getBody());
        assertEquals(response.getStatusCode(), 200);
    }
}
