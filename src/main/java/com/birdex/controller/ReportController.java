package com.birdex.controller;

import com.birdex.domain.SaveReportRequest;
import com.birdex.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Registro y consulta de reportes")
public class ReportController {

    private final ReportService reportService;

    @PostMapping()
    public ResponseEntity<String> saveReport(@RequestBody SaveReportRequest saveReportRequest) {
        return ResponseEntity.ok(reportService.reportSave(saveReportRequest).toString());
    }
}
