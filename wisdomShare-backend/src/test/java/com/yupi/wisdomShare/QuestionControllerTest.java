package com.yupi.wisdomShare;

import com.yupi.wisdomShare.service.QuestionService;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class QuestionControllerTest {
    @Resource
    private QuestionService QuestionService;

}
