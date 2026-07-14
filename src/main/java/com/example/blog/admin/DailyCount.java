package com.example.blog.admin;

import java.time.LocalDate;

public record DailyCount(LocalDate day, long count) {
}
