package com.cqu.drs.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Reading the DRS-Initial (Assessment 2) CSV export. */
class LegacyDataReaderTest {

    @Test
    @DisplayName("TC-I01 quoted fields with commas are kept together")
    void quotedCsv() {
        String[] f = LegacyDataReader.splitCsv("FIRE,Rockhampton CBD,\"Warehouse fire, smoke\",Alex,0400,HIGH");
        assertEquals(6, f.length);
        assertEquals("Warehouse fire, smoke", f[2]);
        assertEquals("", LegacyDataReader.splitCsv("a,")[1]);
    }

    @Test
    @DisplayName("TC-I02 bundled DRS-Initial data set is complete")
    void bundledData() throws Exception {
        Map<String, List<String[]>> data = LegacyDataReader.readBundled();
        assertEquals(6, data.get("departments").size());
        assertEquals(8, data.get("resources").size());
        assertEquals(6, data.get("volunteers").size());
        assertEquals(5, data.get("reports").size());
    }
}
