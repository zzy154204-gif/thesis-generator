package com.example.thesisgenerator.service;

import com.example.thesisgenerator.entity.Reference;
import org.springframework.stereotype.Component;

/**
 * GB/T 7714-2015 参考文献格式化器
 * <p>
 * 将 Reference 实体按国家标准格式化为引用字符串。
 * <p>
 * 支持格式：
 * <ul>
 *   <li>[J] 期刊文章 — 作者. 标题[J]. 刊物, 年份, 卷(期): 页码.</li>
 *   <li>[M] 专著     — 作者. 标题[M]. 出版地: 出版社, 年份.</li>
 *   <li>[C] 会议论文  — 作者. 标题[C]// 会议名称. 会议地点, 年份: 页码.</li>
 *   <li>[D] 学位论文  — 作者. 标题[D]. 授予单位, 年份.</li>
 *   <li>[EB] 网络资源 — 作者. 标题[EB/OL]. 链接, 引用日期.</li>
 * </ul>
 */
@Component
public class Gbt7714Formatter {

    /**
     * 将一条参考文献格式化为 GB/T 7714 字符串
     *
     * @param ref 参考文献实体
     * @return 格式化后的引用文本
     */
    public String format(Reference ref) {
        if (ref == null) {
            return "";
        }

        return switch (ref.getType()) {
            case J -> formatJournal(ref);
            case M -> formatBook(ref);
            case C -> formatConference(ref);
            case D -> formatDissertation(ref);
            case EB -> formatElectronic(ref);
        };
    }

    /**
     * 期刊文章 [J]
     * 格式：作者. 标题[J]. 刊物名称, 年份, 卷(期): 页码.
     */
    private String formatJournal(Reference ref) {
        StringBuilder sb = new StringBuilder();
        sb.append(ref.getAuthors()).append(". ");
        sb.append(ref.getTitle()).append("[J]. ");
        sb.append(ref.getJournal()).append(", ");
        sb.append(ref.getYear());

        if (ref.getVolume() != null && !ref.getVolume().isEmpty()) {
            sb.append(", ").append(ref.getVolume());
            if (ref.getIssue() != null && !ref.getIssue().isEmpty()) {
                sb.append("(").append(ref.getIssue()).append(")");
            }
        }

        if (ref.getPages() != null && !ref.getPages().isEmpty()) {
            sb.append(": ").append(ref.getPages());
        }
        sb.append(".");
        return sb.toString();
    }

    /**
     * 专著/书籍 [M]
     * 格式：作者. 标题[M]. 出版地: 出版社, 年份.
     */
    private String formatBook(Reference ref) {
        StringBuilder sb = new StringBuilder();
        sb.append(ref.getAuthors()).append(". ");
        sb.append(ref.getTitle()).append("[M]. ");

        if (ref.getAddress() != null && !ref.getAddress().isEmpty()) {
            sb.append(ref.getAddress()).append(": ");
        }
        sb.append(ref.getPublisher()).append(", ");
        sb.append(ref.getYear()).append(".");
        return sb.toString();
    }

    /**
     * 会议论文 [C]
     * 格式：作者. 标题[C]// 会议名称. 会议地点, 年份: 页码.
     */
    private String formatConference(Reference ref) {
        StringBuilder sb = new StringBuilder();
        sb.append(ref.getAuthors()).append(". ");
        sb.append(ref.getTitle()).append("[C]// ");
        sb.append(ref.getConference()).append(". ");
        sb.append(ref.getYear());

        if (ref.getPages() != null && !ref.getPages().isEmpty()) {
            sb.append(": ").append(ref.getPages());
        }
        sb.append(".");
        return sb.toString();
    }

    /**
     * 学位论文 [D]
     * 格式：作者. 标题[D]. 授予单位, 年份.
     */
    private String formatDissertation(Reference ref) {
        StringBuilder sb = new StringBuilder();
        sb.append(ref.getAuthors()).append(". ");
        sb.append(ref.getTitle()).append("[D]. ");
        sb.append(ref.getInstitution()).append(", ");
        sb.append(ref.getYear()).append(".");
        return sb.toString();
    }

    /**
     * 网络资源 [EB/OL]
     * 格式：作者. 标题[EB/OL]. 链接, 引用日期.
     */
    private String formatElectronic(Reference ref) {
        StringBuilder sb = new StringBuilder();
        sb.append(ref.getAuthors()).append(". ");
        sb.append(ref.getTitle()).append("[EB/OL]. ");
        sb.append(ref.getUrl()).append(", ");
        sb.append(ref.getAccessDate()).append(".");
        return sb.toString();
    }
}
