package org.nqm.model;

public record GisProcessDto(String output, int exitCode) {

    public static final GisProcessDto EMPTY = new GisProcessDto("", 0);

}
