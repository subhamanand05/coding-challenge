package io.bankbridge.api;

import io.bankbridge.Main;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.*;
import spark.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static spark.Service.ignite;
import static spark.Spark.awaitInitialization;
import static spark.Spark.stop;

public class ApiTest {

    private final String BANK_V1_URL = "http://localhost:8080/v1/banks/all";
    private final String BANK_V2_URL = "http://localhost:8080/v2/banks/all";
    private final String INVALID_BANK_URL = "http://localhost:8080/v3/banks/all";
    private final String INVALID_PORT_URL = "http://localhost:8090/v2/banks/all";
    private final String GET = "GET";
    private final String POST = "POST";

    @BeforeClass
    public static void setUp() throws Exception {
        Main.main(null);
        awaitInitialization();
    }


    @AfterClass
    public static void tearDown() throws Exception {
        stop();
    }

    @Test
    public void testApiNotImplementedFailureScenario() {
        int httpStatus = callBankEndpoint(INVALID_BANK_URL, GET);
        assertEquals(HttpStatus.NOT_FOUND_404, httpStatus);

    }

    @Test
    public void testApiInvalidPortFailureScenario() {
        int httpStatus = callBankEndpoint(INVALID_PORT_URL, GET);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, httpStatus);

    }

    @Test
    public void testBankV1SuccessScenario1() {
        int httpStatus = callBankEndpoint(BANK_V1_URL, GET);
        assertEquals(HttpStatus.OK_200, httpStatus);
    }

    @Test
    public void testBankV1FailureScenario1() {
        int httpStatus = callBankEndpoint(BANK_V1_URL, POST);
        assertEquals(HttpStatus.NOT_FOUND_404, httpStatus);

    }

    @Test
    public void testBankV2FailureScenario1() {
        int httpStatus = callBankEndpoint(BANK_V2_URL, POST);
        assertEquals(HttpStatus.NOT_FOUND_404, httpStatus);
    }

    @Test
    public void testBankV2SuccessScenario1() {
        Service httpMock = igniteMockScenario1();

        int httpStatus = callBankEndpoint(BANK_V2_URL, GET);
        assertEquals(HttpStatus.OK_200, httpStatus);

        httpMock.stop();
    }

    @Test
    public void testBankV2SuccessScenario2() {
        Service httpMock = igniteMockScenario2();

        int httpStatus = callBankEndpoint(BANK_V2_URL, GET);
        assertEquals(HttpStatus.PARTIAL_CONTENT_206, httpStatus);

        httpMock.stop();
    }

    @Test
    public void testBankV2FailureScenario3() {
        int httpStatus = callBankEndpoint(BANK_V2_URL, GET);
        assertEquals(HttpStatus.BAD_GATEWAY_502, httpStatus);

    }


    private int callBankEndpoint(String bankUrl, String verb) {
        HttpURLConnection con;
        URL url;
        try {
            url = new URL(bankUrl);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(verb);
            return con.getResponseCode();
        } catch (IOException e) {
            System.out.println("Error : " + e.getMessage());
            e.printStackTrace();
            return 500;
        }
    }


    private Service igniteMockScenario1 () {
        Service httpMock = ignite()
                .port(1234)
                .threadPool(20);

        httpMock.get("/rbb", (request, response) -> "{\n" +
                "\"bic\":\"1234\",\n" +
                "\"countryCode\":\"GB\",\n" +
                "\"auth\":\"OAUTH\"\n" +
                "}");

        httpMock.get("/cs", (request, response) -> "{\n" +
                "\"bic\":\"5678\",\n" +
                "\"countryCode\":\"CH\",\n" +
                "\"auth\":\"OpenID\"\n" +
                "}");
        httpMock.get("/bes", (request, response) -> "{\n" +
                "\"name\":\"Banco de espiritu santo\",\n" +
                "\"countryCode\":\"PT\",\n" +
                "\"auth\":\"SSL\"\n" +
                "}");

        return httpMock;
    }

    private Service igniteMockScenario2 () {
        Service httpMock = ignite()
                .port(1234)
                .threadPool(20);

        httpMock.get("/rbb1", (request, response) -> "{\n" +
                "\"bic\":\"1234\",\n" +
                "\"countryCode\":\"GB\",\n" +
                "\"auth\":\"OAUTH\"\n" +
                "}");
        httpMock.get("/cs", (request, response) -> "{\n" +
                "\"bic\":\"5678\",\n" +
                "\"countryCode\":\"CH\",\n" +
                "\"auth\":\"OpenID\"\n" +
                "}");
        httpMock.get("/bes", (request, response) -> "{\n" +
                "\"name\":\"Banco de espiritu santo\",\n" +
                "\"countryCode\":\"PT\",\n" +
                "\"auth\":\"SSL\"\n" +
                "}");

        return httpMock;
    }

}


