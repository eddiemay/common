package com.digitald4.common.model;

import com.google.protobuf.ByteString;

public class DataFile {
    private long id;
    private String name;
    private String type;
    private int size;
    private ByteString data;

    public long getId() {
        return id;
    }

    public DataFile setId(long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DataFile setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public DataFile setType(String type) {
        this.type = type;
        return this;
    }

    public int getSize() {
        return size;
    }

    public DataFile setSize(int size) {
        this.size = size;
        return this;
    }

    public ByteString getData() {
        return data;
    }

    public DataFile setData(ByteString data) {
        this.data = data;
        return this;
    }
}
