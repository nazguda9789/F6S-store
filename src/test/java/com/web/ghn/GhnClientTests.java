package com.web.ghn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GhnClientTests {
    private GhnClient client;
    private MockRestServiceServer server;
    @BeforeEach void setup() {
        RestTemplate template = new RestTemplate();
        server = MockRestServiceServer.createServer(template);
        client = new GhnClient(template);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "shopId", "123");
        ReflectionTestUtils.setField(client, "baseUrl", "https://dev-online-gateway.ghn.vn/shiip/public-api/");
    }
    @Test void missingTokenDoesNotCallProvider() {
        ReflectionTestUtils.setField(client, "apiKey", "");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class, client::getProvince).getStatusCode());
        server.verify();
    }
    @Test void usesConfiguredEnvironmentAndCorrectWardEndpoint() {
        server.expect(requestTo("https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/ward"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Token", "test-key"))
                .andExpect(content().json("{\"district_id\":12}"))
                .andRespond(withSuccess("{\"code\":200,\"data\":[]}", MediaType.APPLICATION_JSON));
        assertNotNull(client.getWard(12).get("data"));
        server.verify();
    }
    @Test void upstreamFailureIsReportedAsUnavailable() {
        server.expect(anything()).andRespond(withServerError());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class, client::getProvince).getStatusCode());
        server.verify();
    }
    @Test void malformedResponseIsNotAccepted() {
        server.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(ResponseStatusException.class, client::getProvince);
        server.verify();
    }
    @Test void invalidShippingInputIsRejectedBeforeRequest() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> client.calculateShippingFee(0, 12, "abc")).getStatusCode());
        server.verify();
    }
}
