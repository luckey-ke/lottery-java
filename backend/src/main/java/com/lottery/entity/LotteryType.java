package com.lottery.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public enum LotteryType {

    SSQ("ssq", "双色球", List.of(0, 2, 5)),
    DLT("dlt", "大乐透", List.of(0, 2, 5)),
    FC3D("fc3d", "福彩3D", List.of(0, 1, 2, 3, 4, 5, 6)),
    PL3("pl3", "排列三", List.of(0, 1, 2, 3, 4, 5, 6)),
    PL5("pl5", "排列五", List.of(0, 1, 2, 3, 4, 5, 6)),
    QLC("qlc", "七乐彩", List.of(0, 2, 4));

    private final String code;
    private final String name;
    private final List<Integer> drawDays;

    public static LotteryType fromCode(String code) {
        for (LotteryType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知彩种: " + code);
    }

    public boolean hasDrawToday() {
        int today = java.time.LocalDate.now().getDayOfWeek().getValue() - 1; // 0=Mon
        return drawDays.contains(today);
    }
}
