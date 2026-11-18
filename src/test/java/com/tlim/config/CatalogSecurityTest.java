package com.tlim.config;

import com.tlim.auth.JwtAuthFilter;
import com.tlim.auth.JwtService;
import com.tlim.item.ItemController;
import com.tlim.item.ItemService;
import com.tlim.item.ServerItemPriceController;
import com.tlim.item.ServerItemPriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Catalog writes are ADMIN-only; market prices stay open to any authenticated user. */
@WebMvcTest(controllers = {ItemController.class, ServerItemPriceController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class})
class CatalogSecurityTest {

    private static final String ITEM_JSON = "{\"name\":\"Dragon Ham\"}";
    private static final String PRICE_JSON = "{\"marketPrice\":100}";

    @Autowired MockMvc mvc;

    @MockitoBean ItemService itemService;
    @MockitoBean ServerItemPriceService serverItemPriceService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/items")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanReadCatalog() throws Exception {
        mvc.perform(get("/api/items")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotChangeCatalog() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(ITEM_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/items/1").contentType(MediaType.APPLICATION_JSON).content(ITEM_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/items/1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanChangeCatalog() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(ITEM_JSON))
                .andExpect(status().isCreated());
        mvc.perform(delete("/api/items/1")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanSetMarketPrices() throws Exception {
        mvc.perform(put("/api/servers/1/item-prices/2").contentType(MediaType.APPLICATION_JSON).content(PRICE_JSON))
                .andExpect(status().isOk());
        mvc.perform(get("/api/servers/1/item-prices")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void negativeMarketPriceIsRejected() throws Exception {
        mvc.perform(put("/api/servers/1/item-prices/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marketPrice\":-5}"))
                .andExpect(status().isBadRequest());
    }
}
