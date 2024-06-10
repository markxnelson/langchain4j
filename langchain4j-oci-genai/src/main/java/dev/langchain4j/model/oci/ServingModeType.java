package dev.langchain4j.model.oci;

import lombok.Getter;

@Getter
public enum ServingModeType {

    ON_DEMAND("on-demand"),
    DEDICATED("dedicated");

    private final String mode;

    ServingModeType(String mode) {
        this.mode = mode;
    }
}

