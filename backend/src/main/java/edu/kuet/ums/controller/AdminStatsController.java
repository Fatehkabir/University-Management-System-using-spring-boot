package edu.kuet.ums.controller;

import edu.kuet.ums.dto.Responses.StatsResponse;
import edu.kuet.ums.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService service;

    @GetMapping
    public StatsResponse stats() {
        return service.stats();
    }
}

