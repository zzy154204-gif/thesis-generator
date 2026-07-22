package com.example.thesisgenerator.config;

import com.example.thesisgenerator.entity.College;
import com.example.thesisgenerator.entity.Template;
import com.example.thesisgenerator.entity.TemplateVersion;
import com.example.thesisgenerator.entity.User;
import com.example.thesisgenerator.repository.CollegeRepository;
import com.example.thesisgenerator.repository.TemplateRepository;
import com.example.thesisgenerator.repository.TemplateVersionRepository;
import com.example.thesisgenerator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CollegeRepository collegeRepository;
    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 确保管理员账号存在（已有数据则只补充缺失的账号）
        boolean hasData = userRepository.count() > 0;
        if (hasData) {
            log.info("数据库已有数据，仅补充缺失的账号/学院/模板");
        } else {
            log.info("========== 开始初始化种子数据 ==========");
        }

        // 1. 创建学院（仅当不存在时）
        College cs = findOrCreateCollege("计算机科学与技术学院", "CS");
        College se = findOrCreateCollege("软件工程学院", "SE");
        College ee = findOrCreateCollege("电子与信息工程学院", "EE");
        College ma = findOrCreateCollege("数学与统计学院", "MA");

        // 2. 创建用户（仅当不存在时）
        createUserIfNotExists("admin", "123456", "管理员", "ADMIN", cs.getId());
        createUserIfNotExists("teacher1", "123456", "李教授", "TEACHER", cs.getId());
        createUserIfNotExists("teacher2", "123456", "张教授", "TEACHER", se.getId());
        createUserIfNotExists("student1", "123456", "张三", "STUDENT", cs.getId());
        createUserIfNotExists("student2", "123456", "李四", "STUDENT", se.getId());
        createUserIfNotExists("student3", "123456", "王五", "STUDENT", ee.getId());

        // 已有数据时不再创建模板（避免覆盖手动修改）
        if (hasData) {
            log.info("数据库已有数据，跳过模板初始化");
            return;
        }

        // 3. 创建模板
        createTemplate("本科毕业论文模板", "GRADUATION", cs.getId(), true,
                "适用于计算机相关专业本科毕业论文，包含封面、摘要、目录、正文、参考文献等标准结构",
                "1.0");
        createTemplate("硕士毕业论文模板", "GRADUATION", cs.getId(), true,
                "适用于计算机相关专业硕士毕业论文，包含中英文摘要、目录、各章节、参考文献、致谢等",
                "1.0");
        createTemplate("课程论文模板", "COURSE", null, true,
                "适用于课程期末论文/报告，包含题目、摘要、正文、参考文献",
                "1.0");
        createTemplate("项目实践报告模板", "PROJECT", se.getId(), true,
                "适用于软件工程项目实践报告，包含项目背景、需求分析、系统设计、实现与测试等章节",
                "1.0");
        createTemplate("毕业设计（论文）模板-文史类", "GRADUATION", ma.getId(), false,
                "适用于文史类本科毕业论文，已停用示例",
                "1.0");

        log.info("========== 种子数据初始化完成 ==========");
    }

    private College findOrCreateCollege(String name, String code) {
        // 先按 code 查找，不存在则创建
        return collegeRepository.findByCode(code).orElseGet(() -> {
            // 尝试按名称查找（兼容旧数据未设置 code 的情况）
            College existing = collegeRepository.findByNameContaining(name).stream().findFirst().orElse(null);
            if (existing != null) {
                return existing;
            }
            College c = new College();
            c.setName(name);
            c.setCode(code);
            return collegeRepository.save(c);
        });
    }

    private void createUserIfNotExists(String username, String password, String realName,
                                        String role, Long collegeId) {
        if (userRepository.findByUsername(username).isPresent()) return;
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setRealName(realName);
        u.setRole(role);
        u.setCollegeId(collegeId);
        userRepository.save(u);
    }

    private void createTemplate(String name, String type, Long collegeId, boolean enabled,
                                 String description, String version) {
        Template t = new Template();
        t.setName(name);
        t.setType(type);
        t.setCollegeId(collegeId);
        t.setEnabled(enabled);
        t = templateRepository.save(t);

        // 创建初始版本
        TemplateVersion tv = new TemplateVersion();
        tv.setTemplateId(t.getId());
        tv.setVersionNumber(version);
        tv.setIsCurrent(true);
        // 基础章节结构
        tv.setChapterStructure("[" +
                "{\"title\":\"摘要\",\"level\":1}," +
                "{\"title\":\"第一章 绪论\",\"level\":1}," +
                "{\"title\":\"1.1 研究背景\",\"level\":2}," +
                "{\"title\":\"1.2 研究意义\",\"level\":2}," +
                "{\"title\":\"1.3 国内外研究现状\",\"level\":2}," +
                "{\"title\":\"第二章 相关技术概述\",\"level\":1}," +
                "{\"title\":\"第三章 系统设计与实现\",\"level\":1}," +
                "{\"title\":\"第四章 实验与分析\",\"level\":1}," +
                "{\"title\":\"第五章 总结与展望\",\"level\":1}," +
                "{\"title\":\"参考文献\",\"level\":1}" +
                "]");
        templateVersionRepository.save(tv);
    }
}
