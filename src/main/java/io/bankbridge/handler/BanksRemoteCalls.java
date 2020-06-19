package io.bankbridge.handler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.bankbridge.model.BankModel;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

public class BanksRemoteCalls {

	private static Map config;

	public static void init() throws Exception {
		config = new ObjectMapper()
				.readValue(Thread.currentThread().getContextClassLoader().getResource("banks-v2.json"), Map.class);
	}

	public static String handle(Request request, Response response) {
		System.out.println(config);
		AtomicBoolean anyBackendCallFailed = new AtomicBoolean(false);
		List<Map> bankv2Response = new ArrayList<>();
		Map<String,BankModel> individualResponseMap = new HashMap<>();
		config.forEach((configKey, configValue) -> {
			// call each endpoint from config file and store response in a map for further processing
			try {
				individualResponseMap.put(configKey.toString(),callGet(configValue.toString()));
			} catch (IOException e) {
				// setting http status to 206 as the content will be partial if any request fails
                anyBackendCallFailed.set(true);
				System.out.println("Error while invoking backend");
			}
		});
		if (anyBackendCallFailed.get()) {
            response.status(HttpStatus.PARTIAL_CONTENT_206);
            response.type("application/json");
        }

		// Create v2 response similar to v1 data structure
		individualResponseMap.forEach((key, value) -> {
			if (value != null) {
				Map map = new HashMap<>();
				map.put("id", value.bic);
				map.put("name", key.toString());
				bankv2Response.add(map);
			} else {
				// setting http status to 206 as the content will be partial if value is null
				response.status(HttpStatus.PARTIAL_CONTENT_206);
				response.type("application/json");
			}
		});
		if (!bankv2Response.isEmpty()) {
			try {
				response.type("application/json");
				return new ObjectMapper().writeValueAsString(bankv2Response);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Error occurred while processing request");
			}
		} else {
			response.status(HttpStatus.BAD_GATEWAY_502);
            response.type("text/html;charset=utf-8");
			return("<html><body><h2>502 BAD Gateway</h2><h2>Remote calls towards backend system were not successful</h2></body></html>");
		}

	}


	public static BankModel callGet(String getUri) throws IOException {
		URL url = new URL(getUri);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");

		int httpStatus = con.getResponseCode();
		System.out.println("HTTP Status Code is " + httpStatus);
		if (httpStatus == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer backendResponse = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				backendResponse.append(inputLine);
			}
			in.close();

			// print response
			System.out.println(backendResponse.toString());
			// Parse Json string to BankModel
			ObjectMapper mapper = new ObjectMapper();
			try {
				return mapper.readValue(backendResponse.toString(), BankModel.class);
			} catch (JsonMappingException | JsonParseException e) {
				System.out.println("Error while processing response for : " + getUri + ". Error Details : " + e.toString());
				return null;
			}

		} else {
			System.out.println("Get call was not successful for : " + getUri);
			return null;
		}
	}

}
