package com.cqu.drs.protocol;

import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Requests/responses must survive Java serialisation over the socket. */
class ProtocolTest {

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    @Test
    @DisplayName("TC-P01 request with parameters and token is serialisable")
    void requestRoundTrip() throws Exception {
        Request r = new Request(Command.REPORT_DISASTER).with("type", DisasterType.FIRE).with("location", "CBD");
        r.setSessionToken("abc");
        Request copy = roundTrip(r);
        assertEquals(Command.REPORT_DISASTER, copy.getCommand());
        assertEquals(DisasterType.FIRE, copy.get("type"));
        assertEquals("abc", copy.getSessionToken());
    }

    @Test
    @DisplayName("TC-P02 response carries the model object")
    void responseRoundTrip() throws Exception {
        DisasterReport rep = new DisasterReport(DisasterType.FLOOD, "Yeppoon", "Rising water", "Tom", null);
        Response copy = roundTrip(Response.ok("saved", rep));
        assertTrue(copy.isSuccess());
        assertEquals("Yeppoon", ((DisasterReport) copy.getData()).getLocation());
        assertNotNull(copy.getServerTime());
    }

    @Test
    @DisplayName("TC-P03 numeric parameters are validated")
    void numericParams() {
        Request r = new Request(Command.ALLOCATE_RESOURCE).with("quantity", "12").with("bad", "x");
        assertEquals(12, r.getInt("quantity"));
        assertThrows(IllegalArgumentException.class, () -> r.getInt("bad"));
        assertThrows(IllegalArgumentException.class, () -> r.getInt("missing"));
        assertEquals(1500.5, new Request(Command.RECORD_DAMAGE).with("c", "$1,500.50").getDouble("c"), 1e-9);
    }
}
