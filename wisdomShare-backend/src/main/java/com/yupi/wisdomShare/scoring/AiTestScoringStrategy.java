package com.yupi.wisdomShare.scoring;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.wisdomShare.config.RedissonConfig;
import com.yupi.wisdomShare.manager.AiManager;
import com.yupi.wisdomShare.model.dto.question.QuestionAnswerDTO;
import com.yupi.wisdomShare.model.dto.question.QuestionContentDTO;
import com.yupi.wisdomShare.model.entity.App;
import com.yupi.wisdomShare.model.entity.Question;
import com.yupi.wisdomShare.model.entity.UserAnswer;
import com.yupi.wisdomShare.model.vo.QuestionVO;
import com.yupi.wisdomShare.service.QuestionService;
import io.lettuce.core.RedisClient;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AI 测评类应用评分策略
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@ScoringStrategyConfig(appType = 1, scoringStrategy = 1)
public class AiTestScoringStrategy implements ScoringStrategy {

    @Resource
    private QuestionService questionService;
    @Resource
    private AiManager aiManager;
    @Resource
    private RedissonClient redissonClient;

    //分布式锁的key
    private static final String AI_ANSWER_LOCK ="AI_ANSWER_LOCK";
    /**
     * AI 评分系统消息
     */
    private static final String AI_TEST_SCORING_SYSTEM_MESSAGE = "你是一位严谨的判题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "题目和用户回答的列表：格式为 [{\"title\": \"题目\",\"answer\": \"用户回答\"}]\n" +
            "```\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来对用户进行评价：\n" +
            "1. 要求：需要给出一个明确的评价结果，包括评价名称（尽量简短）和评价描述（尽量详细，大于 200 字）\n" +
            "2. 严格按照下面的 json 格式输出评价名称和评价描述\n" +
            "```\n" +
            "{\"resultName\": \"评价名称\", \"resultDesc\": \"评价描述\"}\n" +
            "```\n" +
            "3. 返回格式必须为 JSON 对象";
    /**
     * AI 评分结果本地缓存
     */
    private final Cache<String, String> answerCacheMap =
            Caffeine.newBuilder().initialCapacity(1024)
                    // 缓存5分钟移除
                    .expireAfterAccess(5L, TimeUnit.MINUTES)
                    .build();


    @Override
    public UserAnswer doScore(List<String> choices, App app) throws Exception {
        Long appId = app.getId();
        String jsonStr = JSONUtil.toJsonStr(choices);
        String cacheKey = buildCacheKey(appId, jsonStr);
        String answerJson = answerCacheMap.getIfPresent(cacheKey);
        if(StrUtil.isNotBlank(answerJson)) {
            //构造返回值，填充答案对象的属性
            UserAnswer userAnswer = JSONUtil.toBean(answerJson, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(jsonStr);
            return userAnswer;
        }

        //定义锁
        RLock lock =redissonClient.getLock(AI_ANSWER_LOCK + cacheKey);
        try{
            //竞争锁
            boolean res = lock.tryLock(3,15,TimeUnit.SECONDS);
            if(!res){
                return null;
            }
            // 抢到锁了，执行后续业务逻辑
            // 1. 根据 id 查询到题目
            Question question = questionService.getOne(
                    Wrappers.lambdaQuery(Question.class).eq(Question::getAppId, appId)
            );
            QuestionVO questionVO = QuestionVO.objToVo(question);
            List<QuestionContentDTO> questionContent = questionVO.getQuestionContent();

            // 2. 调用 AI 获取结果
            // 封装 Prompt
            String userMessage = getAiTestScoringUserMessage(app, questionContent, choices);
            // AI 生成
            String result = aiManager.doSyncStableRequest(AI_TEST_SCORING_SYSTEM_MESSAGE, userMessage);
            System.out.println(result);
            // 截取需要的 JSON 信息
            int firstBrace = result.indexOf('{');
            int secondBrace = result.indexOf('{', firstBrace + 1);
            int thirdBrace = result.indexOf('{', secondBrace + 1);
            int lastBrace = result.lastIndexOf('}');
            int secondLastBrace = result.lastIndexOf('}', lastBrace - 1);
            int thirdLastBrace = result.lastIndexOf('}', secondLastBrace - 1);
            String json = result.substring(thirdBrace, thirdLastBrace + 1).replace("\\n", "").replace("\\", "");

            // 缓存结果
            answerCacheMap.put(cacheKey, json);

            // 3. 构造返回值，填充答案对象的属性
            UserAnswer userAnswer = JSONUtil.toBean(json, UserAnswer.class);
            userAnswer.setAppId(appId);
            userAnswer.setAppType(app.getAppType());
            userAnswer.setScoringStrategy(app.getScoringStrategy());
            userAnswer.setChoices(jsonStr);
            return userAnswer;
        }finally {
            if(lock != null && lock.isLocked()){
                if(lock.isHeldByCurrentThread()){
                    lock.unlock();
                }
            }
        }


    }

    /**
     * AI 评分用户消息封装
     *
     * @param app
     * @param questionContentDTOList
     * @param choices
     * @return
     */
    private String getAiTestScoringUserMessage(App app, List<QuestionContentDTO> questionContentDTOList, List<String> choices) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        List<QuestionAnswerDTO> questionAnswerDTOList = new ArrayList<>();
        for (int i = 0; i < questionContentDTOList.size(); i++) {
            QuestionAnswerDTO questionAnswerDTO = new QuestionAnswerDTO();
            questionAnswerDTO.setTitle(questionContentDTOList.get(i).getTitle());
            questionAnswerDTO.setUserAnswer(choices.get(i));
            questionAnswerDTOList.add(questionAnswerDTO);
        }
        userMessage.append(JSONUtil.toJsonStr(questionAnswerDTOList));
        return userMessage.toString();
    }

    /**
     * 构建缓存Key
     * @param appId
     * @param choices
     * @return
     */
    private String buildCacheKey(Long appId ,String choices) {
        return DigestUtil.md5Hex(appId +":"+ choices);
    }
}