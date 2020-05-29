package com.android.db;

import org.litepal.crud.LitePalSupport;

public class province extends LitePalSupport {
    private int id;
    private String provinceName;//记录省名字
    private String provinceCode;//记录省代号

    public int getId() {
        return id;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }
}
