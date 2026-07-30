package com.example.crossborder.service;

import com.example.crossborder.model.TrendCandidate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DemoJapanTrendSource {
    public List<TrendCandidate> fetch(LocalDate date) {
        List<TrendCandidate> items = new ArrayList<>();
        add(items, "Toys", "ミニチュアブラインドボックス", "Miniature blind-box figure", "miniature blind box figure", 96, 1980, "Highly visual unboxing format with impulse-purchase appeal.");
        add(items, "Toys", "カプセルトイ収納", "Capsule toy display case", "capsule toy display case", 92, 1580, "Display and organization use cases support repeat purchases.");
        add(items, "Toys", "光る組み立てブロック", "Illuminated building blocks", "light-up building blocks", 88, 2380, "The light-up effect creates strong short-form video demonstrations.");
        add(items, "Home & Living", "折りたたみ水切りラック", "Foldable over-sink drying rack", "foldable sink drying rack", 89, 2480, "Addresses storage constraints in compact kitchens.");
        add(items, "Home & Living", "圧縮トラベルポーチ", "Compression travel organizer", "compression travel organizer", 81, 1880, "Clear before-and-after travel packing demonstration.");
        add(items, "Home & Living", "マグネット収納", "Magnetic storage rack", "magnetic storage rack", 78, 1680, "Tool-free installation suits rentals and small homes.");
        add(items, "Beauty", "前髪セルフカット", "Fringe trimming guide", "hair fringe trimming guide", 87, 980, "Compact beauty tool with favorable shipping economics.");
        add(items, "Beauty", "メイクブラシ洗浄", "Makeup brush cleaning cup", "makeup brush cleaning cup", 80, 1280, "Cleaning results are easy to demonstrate visually.");
        add(items, "Pet Supplies", "猫用自動じゃらし", "Automatic interactive cat toy", "automatic interactive cat toy", 86, 2980, "Pet interaction naturally produces shareable content.");
        add(items, "Pet Supplies", "ペット抜け毛クリーナー", "Reusable pet hair remover", "reusable pet hair remover", 82, 1480, "Solves a recurring household cleaning need.");
        add(items, "Electronics", "スマホ冷却ファン", "Phone cooling fan", "mobile phone cooling fan", 84, 3280, "Clear use case for summer gaming and live streaming.");
        add(items, "Electronics", "スマホ撮影ライト", "Clip-on phone video light", "clip-on phone video light", 79, 1880, "Targets a broad short-form content creator segment.");
        add(items, "Outdoors", "首掛け扇風機", "Wearable neck fan", "wearable neck fan", 83, 2580, "Strong summer commuting and outdoor use case.");
        add(items, "Outdoors", "携帯虫よけクリップ", "Portable mosquito-repellent clip", "portable mosquito repellent clip", 76, 980, "Seasonal fit for camping and outdoor activities.");
        add(items, "Baby", "こぼれないおやつカップ", "Spill-resistant snack cup", "spill resistant toddler snack cup", 79, 1280, "The parent pain point is immediate and easy to demonstrate.");
        add(items, "Kitchen", "油はね防止ガード", "Foldable stove splatter guard", "foldable stove splatter guard", 78, 1780, "Addresses a common kitchen cleaning problem.");
        add(items, "Kitchen", "多機能野菜カッター", "Multi-function vegetable slicer", "multi-function vegetable slicer", 77, 2380, "Time-saving benefit is simple to communicate.");
        add(items, "Home & Living", "透明付箋", "Transparent sticky-note set", "transparent sticky notes", 73, 780, "Useful across study, office, and planning contexts.");
        add(items, "Fashion", "冷感アームカバー", "Cooling UV arm sleeves", "cooling UV arm sleeves", 81, 1280, "Strong seasonal sun-protection use case.");
        add(items, "Home & Living", "姿勢サポートクッション", "Ergonomic posture support cushion", "posture support seat cushion", 75, 2680, "Targets discomfort from prolonged desk work.");
        add(items, "Food", "抹茶チョコレート", "Matcha chocolate snack box", "matcha chocolate snack box", 74, 1480, "Distinctive Japanese flavor with giftable packaging.");
        return items.stream().sorted(Comparator.comparingDouble(TrendCandidate::heatScore).reversed()).toList();
    }

    private void add(
        List<TrendCandidate> items, String category, String sourceTitle, String nameEn, String keywords,
        double heat, int jpy, String reason
    ) {
        String url = "https://www.tiktok.com/search?q=" + keywords.replace(" ", "%20");
        items.add(new TrendCandidate(
            category, sourceTitle, nameEn, keywords, "TikTok JP / demo", url, null, heat, heat,
            heat * jpy, 50D, BigDecimal.valueOf(jpy), "JPY", reason
        ));
    }
}
