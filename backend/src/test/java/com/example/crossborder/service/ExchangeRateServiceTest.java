package com.example.crossborder.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.crossborder.model.ExchangeRateSnapshot;
import com.example.crossborder.repository.DataIngestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ExchangeRateServiceTest {
    @Test
    void parsesFrankfurterV2Rate() {
        ExternalDataSourceService external = mock(ExternalDataSourceService.class);
        when(external.frankfurterRateUrl("JPY", "CNY")).thenReturn("https://rates.test/JPY/CNY");
        when(external.get("https://rates.test/JPY/CNY"))
            .thenReturn("{\"date\":\"2026-07-17\",\"base\":\"JPY\",\"quote\":\"CNY\",\"rate\":0.04169}");
        ExchangeRateService service = new ExchangeRateService(
            external, mock(DataIngestionRepository.class), new ObjectMapper()
        );

        ExchangeRateSnapshot rate = service.fetch("JPY", "CNY");

        assertEquals(LocalDate.of(2026, 7, 17), rate.rateDate());
        assertEquals(0, new BigDecimal("0.04169").compareTo(rate.rateValue()));
        assertEquals("Frankfurter", rate.provider());
    }
}
