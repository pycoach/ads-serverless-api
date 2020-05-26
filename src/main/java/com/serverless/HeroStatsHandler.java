package com.serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class HeroStatsHandler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

    private final Logger LOG = LogManager.getLogger(this.getClass());

    @Override
    public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
        LOG.info("input: {}", input);

        String startDate = null;
        String endDate = null;
        Map<String, String> queryStringParameters = (Map<String, String>)input.get("queryStringParameters");
        if(queryStringParameters != null ){
            startDate = queryStringParameters.get("start");
            endDate = queryStringParameters.get("end");
        }

        if(startDate == null)startDate = "1900-01-01";
        if(endDate == null)endDate = "2100-01-01";

        int statusCode = 400;
        JSONObject retObject = new JSONObject();
        JSONObject data = new JSONObject();

        if( Validator.isValidateDate(startDate) && Validator.isValidateDate(endDate)) {
            DBCredentials dbCreds = new DBCredentials();
            dbCreds.setDbHost("covid-oracle.cewagdn2zv2j.us-west-2.rds.amazonaws.com");
            dbCreds.setDbPort("1521");
            dbCreds.setUserName("admin");
            dbCreds.setPassword("8iEkGjQgFJzOblCihFaz");
            dbCreds.setDbName("orcl");

            DBConnection dbConnection = new DBConnection();
            Connection connection = dbConnection.getConnection(dbCreds);

            try {
                if (Optional.ofNullable(connection).isPresent()) {
                    data = runQuery(connection, startDate, endDate);
                    statusCode = 200;
                    retObject.put("data", data);
                } else {
                    statusCode = 501;
                    data.put("message", "Server error!");
                    retObject.put("data", data);
                }
            } catch (JSONException e) {
                LOG.info("Error: {}", e);
            }
        } else {
            statusCode = 400;
            try {
                data.put("message", "Invalid date format");
                retObject.put("data", data);
            } catch (JSONException e) {
                LOG.info("Error: {}", e);
            }
        }

        return ApiGatewayResponse.builder()
                .setStatusCode(statusCode)
                .setRawBody(retObject.toString())
                .setHeaders(Collections.singletonMap("Content-Type", "application/json"))
                .setHeaders(Collections.singletonMap("Access-Control-Allow-Origin", "*"))
                .build();
    }

    public JSONObject runQuery(Connection connection, String startDate, String endDate) {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        JSONObject result = new JSONObject();

        String query = "" +
                "--Total ASAP Requests\n" +
                "SELECT\n" +
                "\t'Total ASAP Requests' AS metric,\n" +
                "\tcount(DISTINCT (CASE_NUMBER)) AS count\n" +
                "FROM\n" +
                "\tADMIN. \"sample_data_2\"\n" +
                "WHERE\n" +
                String.format("\t\"ASAP CREATED\" >= TO_DATE('%s', 'yyyy-MM-dd')\n", startDate) +
                String.format("\tAND \"ASAP CREATED\" < TO_DATE('%s', 'yyyy-MM-dd')\n", endDate) +
                "UNION ALL\n" +
                "--Active ASAP Requests\n" +
                "SELECT\n" +
                "\t'Active ASAP Requests' AS metric,\n" +
                "\tcount(DISTINCT (CASE_NUMBER)) AS count\n" +
                "FROM\n" +
                "\tADMIN. \"sample_data_2\"\n" +
                "WHERE\n" +
                "\t\"ASAP Status\" = 'Active'\n" +
                String.format("\tAND \"ASAP CREATED\" >= TO_DATE('%s', 'yyyy-MM-dd')\n", startDate) +
                String.format("\tAND \"ASAP CREATED\" < TO_DATE('%s', 'yyyy-MM-dd')\n", endDate) +
                "UNION ALL\n" +
                "--Completed ASAP Requets\n" +
                "SELECT\n" +
                "\t'Completed ASAP Requets' AS metric,\n" +
                "\tcount(DISTINCT (CASE_NUMBER)) AS count\n" +
                "FROM\n" +
                "\tADMIN. \"sample_data_2\"\n" +
                "WHERE\n" +
                "\t\"ASAP Status\" = 'Completed'\n" +
                String.format("\tAND \"ASAP CREATED\" >= TO_DATE('%s', 'yyyy-MM-dd')\n", startDate) +
                String.format("\tAND \"ASAP CREATED\" < TO_DATE('%s', 'yyyy-MM-dd')\n", endDate) +
                "UNION ALL\n" +
                "--Average ASAP Cycle Time\n" +
                "SELECT\n" +
                "\t'Average ASAP Cycle Time' AS metric,\n" +
                "\tAVG(\"ASAP Total Cycle Time\") AS count\n" +
                "FROM (\n" +
                "\tSELECT\n" +
                "\t\tMAX(\"ASAP Total Cycle Time\") AS \"ASAP Total Cycle Time\",\n" +
                "\t\tCASE_NUMBER\n" +
                "\tFROM\n" +
                "\t\tADMIN. \"sample_data_2\"\n" +
                "\tWHERE\n" +
                String.format("\t\t\"ASAP CREATED\" >= TO_DATE('%s', 'yyyy-MM-dd')\n", startDate) +
                String.format("\t\tAND \"ASAP CREATED\" < TO_DATE('%s', 'yyyy-MM-dd')\n", endDate) +
                "\tGROUP BY\n" +
                "\t\tCASE_NUMBER)\n" +
                "UNION ALL\n" +
                "--Fastest ASAP Total Cycle Time\n" +
                "SELECT\n" +
                "\t'Fastest ASAP Total Cycle Time' AS metric,\n" +
                "\tMIN(\"ASAP Total Cycle Time\") AS count\n" +
                "FROM (\n" +
                "\tSELECT\n" +
                "\t\tMAX(CAST(\"ASAP Total Cycle Time\" AS FLOAT)) AS \"ASAP Total Cycle Time\",\n" +
                "\t\tCASE_NUMBER,\n" +
                "\t\t\"ASAP Status\"\n" +
                "\tFROM\n" +
                "\t\tADMIN. \"sample_data_2\"\n" +
                "\tWHERE\n" +
                String.format("\t\t\"ASAP CREATED\" >= TO_DATE('%s', 'yyyy-MM-dd')\n", startDate) +
                String.format("\t\tAND \"ASAP CREATED\" < TO_DATE('%s', 'yyyy-MM-dd')\n", endDate) +
                "\tGROUP BY\n" +
                "\t\tCASE_NUMBER,\n" +
                "\t\t\"ASAP Status\")\n" +
                "WHERE\n" +
                "\t\"ASAP Status\" = 'Completed'\n" +
                "\tAND \"ASAP Total Cycle Time\" > 0";

        System.out.println(query);
        try {
            prepStmt = connection.prepareStatement(query);
            rs = prepStmt.executeQuery();
            while(rs.next()) {
                String key = rs.getString("metric");
                if(key.equals("Fastest ASAP Total Cycle Time") || key.equals("Average ASAP Cycle Time"))
                    result.put(rs.getString("metric"), rs.getFloat("count"));
                else
                    result.put(rs.getString("metric"), rs.getInt("count"));
            }

        } catch (SQLException | JSONException e) {
            e.printStackTrace();
        }

        return result;
    }
}