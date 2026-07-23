package com.example.thesisgenerator.config;

import com.example.thesisgenerator.entity.*;
import com.example.thesisgenerator.repository.*;
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
    private final ReferenceRepository referenceRepository;
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

        // 3. 创建示例参考文献（独立于 hasData 检查，仅当参考文献表为空时写入）
        createSampleReferences();

        // 已有数据时不再创建模板（避免覆盖手动修改）
        if (hasData) {
            log.info("数据库已有数据，跳过模板初始化");
            return;
        }

        // 4. 创建模板
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

        // 4. 创建示例参考文献（期刊、专著、会议、学位论文、网络资源等）
        createSampleReferences();

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

    /**
     * 创建示例参考文献（涵盖 5 种类型）
     */
    private void createSampleReferences() {
        // 仅当参考文献表为空时写入，避免重复插入
        if (referenceRepository.count() > 0) {
            log.info("参考文献表已有 {} 条数据，跳过初始化", referenceRepository.count());
            return;
        }

        List<Reference> refs = List.of(
                // ===== 期刊文章 [J] =====
                Reference.builder()
                        .authors("张伟, 李明, 王强")
                        .title("基于深度学习的图像语义分割方法综述")
                        .type(ReferenceType.J).year(2023)
                        .journal("计算机学报").volume("46").issue("3").pages("530-558")
                        .build(),
                Reference.builder()
                        .authors("刘洋, 陈静")
                        .title("面向知识图谱的关系抽取研究进展")
                        .type(ReferenceType.J).year(2022)
                        .journal("软件学报").volume("33").issue("6").pages("2058-2088")
                        .build(),
                Reference.builder()
                        .authors("Goodfellow, I., Pouget-Abadie, J., Mirza, M., et al.")
                        .title("Generative Adversarial Nets")
                        .type(ReferenceType.J).year(2014)
                        .journal("Advances in Neural Information Processing Systems").volume("27").pages("2672-2680")
                        .build(),
                Reference.builder()
                        .authors("Vaswani, A., Shazeer, N., Parmar, N., et al.")
                        .title("Attention Is All You Need")
                        .type(ReferenceType.J).year(2017)
                        .journal("Advances in Neural Information Processing Systems").volume("30").pages("5998-6008")
                        .build(),
                Reference.builder()
                        .authors("Devlin, J., Chang, M.-W., Lee, K., et al.")
                        .title("BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding")
                        .type(ReferenceType.J).year(2019)
                        .journal("Proceedings of NAACL-HLT").pages("4171-4186")
                        .build(),
                Reference.builder()
                        .authors("He, K., Zhang, X., Ren, S., et al.")
                        .title("Deep Residual Learning for Image Recognition")
                        .type(ReferenceType.J).year(2016)
                        .journal("Proceedings of the IEEE Conference on Computer Vision and Pattern Recognition")
                        .pages("770-778")
                        .build(),
                Reference.builder()
                        .authors("张敏, 李峰, 赵志宏")
                        .title("基于注意力机制的短文本分类方法")
                        .type(ReferenceType.J).year(2023)
                        .journal("中文信息学报").volume("37").issue("2").pages("89-97")
                        .build(),
                Reference.builder()
                        .authors("周明, 孙茂松")
                        .title("大语言模型综述：从GPT到ChatGPT")
                        .type(ReferenceType.J).year(2024)
                        .journal("计算机研究与发展").volume("61").issue("1").pages("1-25")
                        .build(),

                // ===== 专著/书籍 [M] =====
                Reference.builder()
                        .authors("周志华")
                        .title("机器学习")
                        .type(ReferenceType.M).year(2016)
                        .publisher("清华大学出版社").address("北京")
                        .build(),
                Reference.builder()
                        .authors("李航")
                        .title("统计学习方法（第2版）")
                        .type(ReferenceType.M).year(2019)
                        .publisher("清华大学出版社").address("北京")
                        .build(),
                Reference.builder()
                        .authors("Russell, S., Norvig, P.")
                        .title("Artificial Intelligence: A Modern Approach (4th Edition)")
                        .type(ReferenceType.M).year(2020)
                        .publisher("Pearson").address("Hoboken, NJ")
                        .build(),
                Reference.builder()
                        .authors("Goodfellow, I., Bengio, Y., Courville, A.")
                        .title("Deep Learning")
                        .type(ReferenceType.M).year(2016)
                        .publisher("MIT Press").address("Cambridge, MA")
                        .build(),
                Reference.builder()
                        .authors("王珊, 萨师煊")
                        .title("数据库系统概论（第5版）")
                        .type(ReferenceType.M).year(2014)
                        .publisher("高等教育出版社").address("北京")
                        .build(),
                Reference.builder()
                        .authors("Tanenbaum, A. S., Van Steen, M.")
                        .title("Distributed Systems: Principles and Paradigms (3rd Edition)")
                        .type(ReferenceType.M).year(2023)
                        .publisher("Pearson").address("London")
                        .build(),

                // ===== 会议论文 [C] =====
                Reference.builder()
                        .authors("陈思远, 李航, 张敏")
                        .title("融合规则与深度学习的中文命名实体识别")
                        .type(ReferenceType.C).year(2023)
                        .conference("中国计算语言学大会 (CCL 2023)").pages("156-167")
                        .build(),
                Reference.builder()
                        .authors("Brown, T., Mann, B., Ryder, N., et al.")
                        .title("Language Models are Few-Shot Learners")
                        .type(ReferenceType.C).year(2020)
                        .conference("Advances in Neural Information Processing Systems (NeurIPS 2020)")
                        .volume("33").pages("1877-1901")
                        .build(),
                Reference.builder()
                        .authors("Dosovitskiy, A., Beyer, L., Kolesnikov, A., et al.")
                        .title("An Image is Worth 16x16 Words: Transformers for Image Recognition at Scale")
                        .type(ReferenceType.C).year(2021)
                        .conference("International Conference on Learning Representations (ICLR 2021)")
                        .build(),

                // ===== 学位论文 [D] =====
                Reference.builder()
                        .authors("杨磊")
                        .title("面向知识图谱的复杂问答方法研究")
                        .type(ReferenceType.D).year(2022)
                        .institution("清华大学")
                        .build(),
                Reference.builder()
                        .authors("王思远")
                        .title("基于图神经网络的代码克隆检测方法")
                        .type(ReferenceType.D).year(2023)
                        .institution("北京大学")
                        .build(),
                Reference.builder()
                        .authors("赵雪")
                        .title("基于对比学习的跨模态检索研究")
                        .type(ReferenceType.D).year(2024)
                        .institution("浙江大学")
                        .build(),

                // ===== 网络资源 [EB] =====
                Reference.builder()
                        .authors("OpenAI")
                        .title("GPT-4 Technical Report")
                        .type(ReferenceType.EB).year(2023)
                        .url("https://arxiv.org/abs/2303.08774")
                        .accessDate("2024-06-15")
                        .build(),
                Reference.builder()
                        .authors("Python Software Foundation")
                        .title("Python 3.12 官方文档")
                        .type(ReferenceType.EB).year(2024)
                        .url("https://docs.python.org/3.12/")
                        .accessDate("2024-05-10")
                        .build(),
                Reference.builder()
                        .authors("Spring")
                        .title("Spring Framework Documentation (6.x)")
                        .type(ReferenceType.EB).year(2024)
                        .url("https://docs.spring.io/spring-framework/reference/")
                        .accessDate("2024-06-01")
                        .build(),
                Reference.builder()
                        .authors("阮一峰")
                        .title("TypeScript 教程")
                        .type(ReferenceType.EB).year(2023)
                        .url("https://typescript.p6p.net/")
                        .accessDate("2024-04-20")
                        .build(),
                Reference.builder()
                        .authors("中国互联网络信息中心")
                        .title("第53次中国互联网络发展状况统计报告")
                        .type(ReferenceType.EB).year(2024)
                        .url("https://www.cnnic.net.cn/n4/2024/0322/c88-10964.html")
                        .accessDate("2024-06-10")
                        .build()
        );

        referenceRepository.saveAll(refs);
        log.info("已初始化 {} 条示例参考文献", refs.size());
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
        // 基础章节结构——只保留一级章标题，不包含任何默认子章节
        tv.setChapterStructure("[" +
                "{\"title\":\"摘要\",\"level\":1,\"children\":[]}," +
                "{\"title\":\"第一章 绪论\",\"level\":1,\"children\":[]}," +
                "{\"title\":\"第二章 相关技术概述\",\"level\":1,\"children\":[]}," +
                "{\"title\":\"第三章 系统设计与实现\",\"level\":1,\"children\":[]}," +
                "{\"title\":\"第四章 实验与分析\",\"level\":1,\"children\":[]}," +
                "{\"title\":\"第五章 总结与展望\",\"level\":1,\"children\":[]}," +
                "{\"title\":\"参考文献\",\"level\":1,\"children\":[]}" +
                "]");
        templateVersionRepository.save(tv);
    }
}
