package com.kongzhong.mrpc.spring.parser;

import com.kongzhong.mrpc.client.RpcSpringClient;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class ClientConfigBeanDefinitionParser extends AbstractBeanDefinitionParser {

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RpcSpringClient.class);

        String appId = element.getAttribute("appId");
        String transport = element.getAttribute("transport");
        String serialize = element.getAttribute("serialize");
        String directAddress = element.getAttribute("directAddress");

        builder.addPropertyValue("appId", appId);
        builder.addPropertyValue("transport", transport);
        builder.addPropertyValue("serialize", serialize);
        builder.addPropertyValue("directAddress", directAddress);

        return builder.getBeanDefinition();
    }

    @Override
    protected boolean shouldGenerateId() {
        return true;
    }
}