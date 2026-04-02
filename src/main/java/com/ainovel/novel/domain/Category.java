package com.ainovel.novel.domain;

public enum Category {
    XUANHUAN(1001L, "玄幻"),
    XIANXIA(1002L, "仙侠"),
    WUXIA(1003L, "武侠"),
    DUSHI(1004L, "都市"),
    KEHUAN(1005L, "科幻"),
    XUANYI(1006L, "悬疑"),
    LISHI(1007L, "历史"),
    YOUXI(1008L, "游戏"),
    QINGXIAOSHUO(1009L, "轻小说"),
    ERCIYUAN(1010L, "二次元"),
    ZHICHANG(1011L, "职场");

    private final Long id;
    private final String name;

    Category(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static String getNameById(Long id) {
        if (id == null) {
            return null;
        }
        for (Category category : values()) {
            if (category.getId().equals(id)) {
                return category.getName();
            }
        }
        return "category-" + id;
    }
}
