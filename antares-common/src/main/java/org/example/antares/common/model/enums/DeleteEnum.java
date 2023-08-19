package org.example.antares.common.model.enums;

public enum DeleteEnum {
    Y(1),
    N(0);

    public int isDelete;

    DeleteEnum(int isDelete){
            this.isDelete = isDelete;
        }

    public int getIsDelete() {
        return isDelete;
    }
}
