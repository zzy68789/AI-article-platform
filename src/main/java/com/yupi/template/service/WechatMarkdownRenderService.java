package com.yupi.template.service;

import cn.hutool.core.util.StrUtil;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.yupi.template.exception.BusinessException;
import com.yupi.template.exception.ErrorCode;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
public class WechatMarkdownRenderService {

    @Resource
    private WechatApiClient wechatApiClient;

    private final Parser parser;

    private final HtmlRenderer renderer;

    public WechatMarkdownRenderService() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    public String renderForWechat(String markdown) {
        return renderForWechat(markdown, null);
    }

    public String renderForWechat(String markdown, String accessToken) {
        if (StrUtil.isBlank(markdown)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文章内容不能为空");
        }
        String rawHtml = renderer.render(parser.parse(markdown));
        Document doc = Jsoup.parseBodyFragment(rawHtml);
        replaceImages(doc, accessToken);
        applyWechatStyles(doc);
        return cleanHtml(doc.body().html());
    }

    private void replaceImages(Document doc, String accessToken) {
        for (Element img : doc.select("img")) {
            String src = img.attr("src");
            if (StrUtil.isBlank(src)) {
                continue;
            }
            String wechatUrl = StrUtil.isBlank(accessToken)
                    ? wechatApiClient.uploadContentImage(src)
                    : wechatApiClient.uploadContentImage(accessToken, src);
            img.attr("src", wechatUrl);
        }
    }

    private void applyWechatStyles(Document doc) {
        for (Element h1 : doc.select("h1")) {
            h1.attr("style", "font-size:22px;font-weight:700;line-height:1.5;margin:24px 0 12px;color:#111;");
        }
        for (Element h2 : doc.select("h2")) {
            h2.attr("style", "font-size:20px;font-weight:700;line-height:1.5;margin:22px 0 10px;color:#111;");
        }
        for (Element h3 : doc.select("h3")) {
            h3.attr("style", "font-size:18px;font-weight:700;line-height:1.5;margin:18px 0 8px;color:#111;");
        }
        for (Element p : doc.select("p")) {
            p.attr("style", "font-size:16px;line-height:1.9;margin:0 0 14px;color:#333;");
        }
        for (Element li : doc.select("li")) {
            li.attr("style", "font-size:16px;line-height:1.8;margin:0 0 8px;color:#333;");
        }
        for (Element img : doc.select("img")) {
            img.attr("style", "max-width:100%;height:auto;display:block;margin:16px auto;");
        }
    }

    private String cleanHtml(String html) {
        Safelist safelist = Safelist.relaxed()
                .addAttributes(":all", "style")
                .addAttributes("img", "src", "alt", "title")
                .addProtocols("img", "src", "http", "https");
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        return Jsoup.clean(html, "", safelist, settings);
    }
}
