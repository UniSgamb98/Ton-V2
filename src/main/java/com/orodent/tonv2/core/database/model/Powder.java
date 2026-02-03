package com.orodent.tonv2.core.database.model;

public record Powder(
        int id,
        String code,
        String name,
        Double strength,
        Double translucency,
        int yttria,
        String notes
) {
    @Override
    public String toString(){
        return name + ", codice: " + code + ", resistenza:" + strength + ", traslucenza: " + translucency;
    }

}
