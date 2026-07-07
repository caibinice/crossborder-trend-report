package com.example.crossborder.service;
import com.example.crossborder.config.SourceProperties;
import com.example.crossborder.model.DataSourceStatus;
import java.net.URI;import java.net.URLEncoder;import java.net.http.*;import java.nio.charset.StandardCharsets;import java.time.Duration;import java.util.*;
import org.springframework.stereotype.Service;import org.springframework.util.StringUtils;
@Service
public class ExternalDataSourceService{
 private final SourceProperties props; private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).build();
 public ExternalDataSourceService(SourceProperties props){this.props=props;}
 public List<DataSourceStatus> statuses(){return List.of(
  new DataSourceStatus("TikTok/Apify",val(props.tiktokMode(),"demo"),has(props.apifyToken()),"TikTok 日本热视频/商品趋势采集","https://apify.com/clockworks/tiktok-scraper","需要 APIFY_TOKEN；当前无 Key 时使用 demo 候选。"),
  new DataSourceStatus("TikTok Research API","research",has(props.tiktokResearchToken()),"公开视频研究数据，适合趋势分析，不等同 TikTok Shop 商品库","https://developers.tiktok.com/doc/research-api-specs-query-videos/","通常需要申请研究权限。"),
  new DataSourceStatus("TikTok Shop Partner API","shop-api",has(props.tiktokShopApiKey()),"店铺/商品/订单等授权数据","https://partner.tiktokshop.com/docv2/","需要 TikTok Shop 开放平台应用与授权。"),
  new DataSourceStatus("Amazon/Rainforest API",val(props.amazonMode(),"rainforest"),has(props.rainforestApiKey()),"Amazon JP 搜索、商品、价格、排名","https://www.rainforestapi.com/docs/product-data-api/overview","第三方 Amazon 数据 API，适合快速接入。"),
  new DataSourceStatus("Amazon/Keepa", "keepa", has(props.keepaApiKey()), "Amazon 商品价格历史、BSR、类目、报价", "https://keepa.com/#!api", "适合做价格历史和排名趋势，但需要付费 token。"),
  new DataSourceStatus("SerpAPI", "google/amazon-search", has(props.serpapiKey()), "Google/Amazon/电商搜索结果聚合", "https://serpapi.com/", "可用于补充站外搜索与采购线索。"),
  new DataSourceStatus("1688/国内采购", val(props.supplierMode(),"search-link"), false, "1688/淘宝/拼多多搜索链接与后续开放平台接入", "https://open.1688.com/", "当前使用搜索链接；真实价格建议接 1688 开放平台或人工核价表。")
 );}
 public Optional<String> rainforestSearchUrl(String query){ if(!has(props.rainforestApiKey())) return Optional.empty(); String q=enc(query); return Optional.of("https://api.rainforestapi.com/request?api_key="+enc(props.rainforestApiKey())+"&type=search&amazon_domain=amazon.co.jp&search_term="+q); }
 public Optional<String> serpSearchUrl(String query){ if(!has(props.serpapiKey())) return Optional.empty(); return Optional.of("https://serpapi.com/search.json?engine=google&q="+enc(query)+"&api_key="+enc(props.serpapiKey())); }
 public Optional<String> keepaProductUrl(String asin){ if(!has(props.keepaApiKey())||!has(asin)) return Optional.empty(); return Optional.of("https://api.keepa.com/product?key="+enc(props.keepaApiKey())+"&domain=5&asin="+enc(asin)); }
 public Optional<String> tryGet(String url){ try{HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(25)).GET().build();HttpResponse<String> res=http.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));return res.statusCode()<300?Optional.of(res.body()):Optional.empty();}catch(Exception e){return Optional.empty();}}
 private boolean has(String s){return StringUtils.hasText(s);} private String val(String s,String d){return has(s)?s:d;} private String enc(String s){return URLEncoder.encode(s==null?"":s,StandardCharsets.UTF_8);} }
