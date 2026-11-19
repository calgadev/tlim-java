package com.tlim.config;

import com.tlim.auth.JwtAuthFilter;
import com.tlim.auth.JwtService;
import com.tlim.item.ItemController;
import com.tlim.item.ItemService;
import com.tlim.item.dto.ItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Browser-facing behaviour: CORS preflights, JSON error bodies for security/routing errors, item limit. */
@WebMvcTest(controllers = ItemController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, GlobalExceptionHandler.class})
class ApiErrorAndCorsTest {

    private static final String DEV_ORIGIN = "http://localhost:5173";

    @Autowired MockMvc mvc;

    @MockitoBean ItemService itemService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    void preflightFromDevServerIsAllowedWithoutToken() throws Exception {
        mvc.perform(options("/api/items")
                        .header(HttpHeaders.ORIGIN, DEV_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN));
    }

    @Test
    void preflightFromUnknownOriginIsRejected() throws Exception {
        mvc.perform(options("/api/items")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingTokenReturnsJson401() throws Exception {
        mvc.perform(get("/api/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "USER")
    void forbiddenRouteReturnsJson403() throws Exception {
        mvc.perform(delete("/api/items/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void unknownRouteReturnsJson404() throws Exception {
        mvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "USER")
    void limitSortsByNameAndCaps() throws Exception {
        when(itemService.getAllItems(any(Optional.class))).thenReturn(List.of(
                item(1L, "Dragon Shield"), item(2L, "dragon ham"), item(3L, "Dragon Lance")));

        mvc.perform(get("/api/items").param("name", "dragon").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("dragon ham"))
                .andExpect(jsonPath("$[1].name").value("Dragon Lance"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonPositiveLimitIsRejected() throws Exception {
        mvc.perform(get("/api/items").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit must be a positive number"));
    }

    private static ItemResponse item(Long id, String name) {
        return new ItemResponse(id, name, name.toLowerCase(), null, null, null, null, null,
                false, false, false, false, List.of(), Instant.EPOCH);
    }
}
