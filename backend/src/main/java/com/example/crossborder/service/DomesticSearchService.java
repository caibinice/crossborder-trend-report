package com.example.crossborder.service;
import com.example.crossborder.model.*;import java.math.*;import java.net.URLEncoder;import java.nio.charset.StandardCharsets;import java.util.List;import org.springframework.stereotype.Service;
@Service
public class DomesticSearchService{
 public List<DomesticLink> search(TrendCandidate c,BigDecimal jpPriceCny){BigDecimal base=estimate(c.category(),jpPriceCny);String q=URLEncoder.encode(c.keywords(),StandardCharsets.UTF_8);return List.of(
  new DomesticLink(0,0,"1688",c.productNameCn()+" - 1688搜索","https://s.1688.com/selloffer/offer_search.htm?keywords="+q,base,"默认给搜索链接；真实价格需接入开放平台或人工核价。"),
  new DomesticLink(0,0,"淘宝",c.productNameCn()+" - 淘宝搜索","https://s.taobao.com/search?q="+q,base.multiply(BigDecimal.valueOf(1.25)).setScale(2,RoundingMode.HALF_UP),"适合查看零售竞品与主图卖点。"),
  new DomesticLink(0,0,"拼多多",c.productNameCn()+" - 拼多多搜索","https://mobile.yangkeduo.com/search_result.html?search_key="+q,base.multiply(BigDecimal.valueOf(1.10)).setScale(2,RoundingMode.HALF_UP),"适合参考低价带与套装策略。"));}
 private BigDecimal estimate(String cat,BigDecimal jp){double r=switch(cat){case "玩具"->0.30;case "家居"->0.34;case "美妆"->0.28;case "宠物"->0.36;case "数码"->0.42;case "户外"->0.38;case "母婴"->0.33;case "汽车"->0.35;default->0.36;};return jp.multiply(BigDecimal.valueOf(r)).max(BigDecimal.valueOf(6)).setScale(2,RoundingMode.HALF_UP);} }
