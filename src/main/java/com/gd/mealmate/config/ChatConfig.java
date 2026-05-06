package com.gd.mealmate.config;

import com.gd.mealmate.function.CreateMealRecordFunction;
import com.gd.mealmate.function.GetMealHistoryFunction;
import com.gd.mealmate.function.GetUserPreferencesFunction;
import com.gd.mealmate.function.SaveUserPreferencesFunction;
import com.gd.mealmate.function.SearchRestaurantsFunction;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

@Configuration
public class ChatConfig {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Value("${spring.ai.openai.embedding.base-url:${spring.ai.openai.base-url}}")
    private String embeddingBaseUrl;

    @Value("${spring.ai.openai.embedding.api-key:${spring.ai.openai.api-key}}")
    private String embeddingApiKey;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-v3}")
    private String embeddingModel;

    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi(baseUrl, apiKey);
    }

    @Bean
    public OpenAiApi embeddingOpenAiApi() {
        return new OpenAiApi(embeddingBaseUrl, embeddingApiKey);
    }

    @Bean
    public OpenAiEmbeddingModel embeddingModel() {
        return new OpenAiEmbeddingModel(embeddingOpenAiApi(),
                MetadataMode.ALL,
                org.springframework.ai.openai.OpenAiEmbeddingOptions.builder()
                        .withModel(embeddingModel)
                        .build(),
                RetryTemplate.builder().build());
    }

    @Bean
    public List<FunctionCallback> functionCallbacks(
            GetUserPreferencesFunction getUserPreferencesFunction,
            GetMealHistoryFunction getMealHistoryFunction,
            SearchRestaurantsFunction searchRestaurantsFunction,
            CreateMealRecordFunction createMealRecordFunction,
            SaveUserPreferencesFunction saveUserPreferencesFunction) {
        return List.of(
                FunctionCallbackWrapper.builder(getUserPreferencesFunction)
                        .withName("getUserPreferencesFunction")
                        .withDescription("获取用户的口味偏好设置")
                        .build(),
                FunctionCallbackWrapper.builder(getMealHistoryFunction)
                        .withName("getMealHistoryFunction")
                        .withDescription("获取用户的用餐历史记录")
                        .build(),
                FunctionCallbackWrapper.builder(searchRestaurantsFunction)
                        .withName("searchRestaurantsFunction")
                        .withDescription("搜索附近的餐厅。参数：latitude（纬度，必填）、longitude（经度，必填）、radius（搜索半径，米，可选，默认3000）、cuisineType（菜系类型如中餐/西餐/日料等，可选）、keywords（搜索关键词如美食/快餐等，可选）。当用户问附近美食或餐厅时必须调用此函数。")
                        .build(),
                FunctionCallbackWrapper.builder(createMealRecordFunction)
                        .withName("createMealRecordFunction")
                        .withDescription("创建用餐记录")
                        .build(),
                FunctionCallbackWrapper.builder(saveUserPreferencesFunction)
                        .withName("saveUserPreferencesFunction")
                        .withDescription("保存用户的口味偏好，当用户提到喜欢或不喜欢的口味时主动调用")
                        .build()
        );
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi,
                                           FunctionCallbackContext functionCallbackContext,
                                           List<FunctionCallback> functionCallbacks) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel(model)
                .withTemperature(temperature)
                .build();

        return new OpenAiChatModel(openAiApi, options, functionCallbackContext, functionCallbacks, RetryTemplate.builder().build());
    }
}
