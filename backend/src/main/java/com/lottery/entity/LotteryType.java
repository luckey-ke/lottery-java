package com.lottery.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LotteryType {

    SSQ("ssq", "双色球"),
    DLT("dlt", "大乐透"),
    FC3D("fc3d", "福彩3D"),
    PL3("pl3", "排列三"),
    PL5("pl5", "排列五"),
    QLC("qlc", "七乐彩");

    private final String code;
    private final String name;

    public static LotteryType fromCode(String code) {
        for (LotteryType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知彩种: " + code);
    }
}
