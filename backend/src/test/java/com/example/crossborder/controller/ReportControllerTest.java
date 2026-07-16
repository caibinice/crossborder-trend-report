package com.example.crossborder.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.crossborder.service.ExternalDataSourceService;
import com.example.crossborder.service.TrendReportService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ReportControllerTest {
    @Test
    void readingAnEmptyLatestReportDoesNotTriggerCollection() {
        TrendReportService reports = mock(TrendReportService.class);
        when(reports.latest()).thenReturn(Optional.empty());
        ReportController controller = new ReportController(reports, mock(ExternalDataSourceService.class));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, controller::latest);

        assertEquals(404, exception.getStatusCode().value());
        verify(reports, never()).collect(LocalDate.now());
    }
}
