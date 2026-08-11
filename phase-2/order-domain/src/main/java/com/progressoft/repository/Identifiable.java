package com.progressoft.repository;

public interface Identifiable<ID> {
    ID getId();
    void setId(ID id);
}