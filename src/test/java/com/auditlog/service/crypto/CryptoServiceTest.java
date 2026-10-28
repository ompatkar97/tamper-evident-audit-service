package com.auditlog.service.crypto;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

class CryptoServiceTest {
    CryptoService c = new CryptoService(new ObjectMapper());

    @Test
    void canonicalJsonIgnoresObjectKeyOrder() throws Exception {
        var o = new ObjectMapper();
        assertEquals(c.canonical(o.readTree("{\"b\":2,\"a\":1}")), c.canonical(o.readTree("{\"a\":1,\"b\":2}")));
    }

    @Test
    void changedValueChangesHash() {
        assertNotEquals(c.sha256("a"), c.sha256("b"));
    }
}
