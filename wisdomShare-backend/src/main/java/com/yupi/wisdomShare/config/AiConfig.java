package com.yupi.wisdomShare.config;

import com.zhipu.oapi.ClientV4;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiConfig {

    /**
     *apiKey , 需要从平台获取
     */

    private String apiKey;

    @Bean
    public ClientV4 getClientV4() {
        return new ClientV4.Builder(apiKey).enableTokenCache()
                .networkConfig(300,100,100,100, TimeUnit.SECONDS)
                .connectionPool(new okhttp3.ConnectionPool(8,1,TimeUnit.SECONDS))
                .build();
    }
}
