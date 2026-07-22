package com.example.thesisgenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class ThesisGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThesisGeneratorApplication.class, args);
    }

    /**
     * 用于导出时下载网络图片的 RestTemplate（5s 连接超时，15s 读取超时）
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}
