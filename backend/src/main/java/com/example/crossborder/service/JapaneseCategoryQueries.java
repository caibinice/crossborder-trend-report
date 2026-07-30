package com.example.crossborder.service;

import java.util.Map;

final class JapaneseCategoryQueries {
    private static final Map<String, String> QUERIES = Map.ofEntries(
        Map.entry("Toys", "おもちゃ 人気"),
        Map.entry("Home & Living", "収納 便利グッズ"),
        Map.entry("Beauty", "美容 コスメ"),
        Map.entry("Pet Supplies", "ペット用品"),
        Map.entry("Electronics", "スマホ デジタル"),
        Map.entry("Outdoors", "アウトドア キャンプ"),
        Map.entry("Baby", "ベビー用品"),
        Map.entry("Kitchen", "キッチン 便利グッズ"),
        Map.entry("Fashion", "ファッション 小物"),
        Map.entry("Food", "お菓子 人気"),
        Map.entry("Automotive", "車 便利グッズ"),
        Map.entry("Stationery", "文房具"),
        Map.entry("Health", "健康グッズ"),
        Map.entry("玩具", "おもちゃ 人気"),
        Map.entry("家居", "収納 便利グッズ"),
        Map.entry("美妆", "美容 コスメ"),
        Map.entry("宠物", "ペット用品"),
        Map.entry("数码", "スマホ グッズ"),
        Map.entry("户外", "アウトドア 便利グッズ"),
        Map.entry("母婴", "ベビー用品"),
        Map.entry("汽车", "カー用品"),
        Map.entry("厨房", "キッチン 便利グッズ"),
        Map.entry("文具", "文房具"),
        Map.entry("服饰", "ファッション 小物"),
        Map.entry("健康", "健康グッズ"),
        Map.entry("食品", "お菓子 人気")
    );

    private JapaneseCategoryQueries() {}

    static String forCategory(String category) {
        return QUERIES.getOrDefault(category, category);
    }
}
