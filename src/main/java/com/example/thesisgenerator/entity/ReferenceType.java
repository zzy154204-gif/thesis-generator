package com.example.thesisgenerator.entity;

/**
 * 参考文献类型枚举
 * <p>
 * 对应 GB/T 7714-2015 标注法：
 * J — 期刊文章（Journal）
 * M — 专著/书籍（Monograph）
 * C — 会议论文（Conference）
 * D — 学位论文（Dissertation）
 * EB — 网络资源（Electronic Bulletin）
 */
public enum ReferenceType {
    J("期刊文章"),
    M("专著/书籍"),
    C("会议论文"),
    D("学位论文"),
    EB("网络资源");

    private final String displayName;

    ReferenceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
