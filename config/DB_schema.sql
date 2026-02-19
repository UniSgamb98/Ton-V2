------------------------------------------------------------
-- TABLE: depot
------------------------------------------------------------
CREATE TABLE depot (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(100) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

------------------------------------------------------------
-- TABLE: product
------------------------------------------------------------
CREATE TABLE product (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    color VARCHAR(30) NOT NULL,
    UNIQUE (type, color)
);

------------------------------------------------------------
-- TABLE: item
------------------------------------------------------------
CREATE TABLE item (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    product_id INTEGER NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,

    PRIMARY KEY (id),

    CONSTRAINT fk_product
            FOREIGN KEY (product_id) REFERENCES product(id)
);

------------------------------------------------------------
-- TABLE: powder
------------------------------------------------------------
CREATE TABLE powder (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,

    strength DOUBLE,
    translucency DOUBLE,
    yttria INTEGER,
    notes VARCHAR(500),

    PRIMARY KEY (id),
    CONSTRAINT uq_powder_code UNIQUE(code)
);

------------------------------------------------------------
-- TABLE: powder_oxide
------------------------------------------------------------
CREATE TABLE powder_oxide (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    powder_id INTEGER NOT NULL,
    oxide_name VARCHAR(100) NOT NULL,
    percentage DOUBLE NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_oxide_powder
        FOREIGN KEY (powder_id) REFERENCES powder(id)
);

------------------------------------------------------------
-- TABLE: composition
------------------------------------------------------------
CREATE TABLE composition (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    item_id INTEGER NOT NULL,

    version INTEGER NOT NULL,
    is_active SMALLINT DEFAULT 1,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT fk_comp_item
        FOREIGN KEY (item_id) REFERENCES item(id),

    CONSTRAINT fk_product
        FOREIGN KEY (product_id) REFERENCES product(id),

    CONSTRAINT uq_comp_item_version
        UNIQUE (item_id, version)
);

------------------------------------------------------------
-- TABLE: composition_layer
------------------------------------------------------------
CREATE TABLE composition_layer (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    composition_id INTEGER NOT NULL,
    layer_number INTEGER NOT NULL,
    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT fk_layer_comp
        FOREIGN KEY (composition_id)
        REFERENCES composition(id),

    CONSTRAINT uq_layer_order
        UNIQUE (composition_id, layer_number)
);


------------------------------------------------------------
-- TABLE: composition_layer_ingredient
------------------------------------------------------------
CREATE TABLE composition_layer_ingredient (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    layer_id INTEGER NOT NULL,
    powder_id INTEGER NOT NULL,

    percentage DOUBLE NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_cli_layer
        FOREIGN KEY (layer_id) REFERENCES composition_layer(id),

    CONSTRAINT fk_cli_powder
        FOREIGN KEY (powder_id) REFERENCES powder(id)
);
------------------------------------------------------------
-- TABLE: production
------------------------------------------------------------
CREATE TABLE production (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    item_id INTEGER NOT NULL,
    composition_id INTEGER NOT NULL,
    blank_model_id INTEGER NOT NULL,

    produced_qty INTEGER NOT NULL,
    production_date DATE NOT NULL,

    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT fk_prod_item
        FOREIGN KEY (item_id) REFERENCES item(id),

    CONSTRAINT fk_prod_comp
        FOREIGN KEY (composition_id) REFERENCES composition(id),

    CONSTRAINT fk_prod_blank_model
        FOREIGN KEY (blank_model_id) REFERENCES blank_model(id),

    CONSTRAINT fk_prod_comp_blank_model
        FOREIGN KEY (composition_id, blank_model_id)
        REFERENCES composition_blank_model(composition_id, blank_model_id)
);
------------------------------------------------------------
-- TABLE: firing
------------------------------------------------------------
CREATE TABLE firing (
    id INTEGER GENERATED ALWAYS AS IDENTITY,

    firing_date DATE NOT NULL,
    furnace VARCHAR(50),

    max_temperature INTEGER,
    duration_minutes INTEGER,

    notes VARCHAR(500),

    PRIMARY KEY (id)
);

------------------------------------------------------------
-- TABLE: lot
------------------------------------------------------------
CREATE TABLE lot (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(50) NOT NULL,

    firing_id INTEGER NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_lot_firing
        FOREIGN KEY (firing_id)
        REFERENCES firing(id),

    CONSTRAINT uq_lot_code UNIQUE (code)
);
------------------------------------------------------------
-- TABLE: firing_production
------------------------------------------------------------
CREATE TABLE firing_production (
    firing_id INTEGER NOT NULL,
    production_id INTEGER NOT NULL,

    PRIMARY KEY (firing_id, production_id),

    CONSTRAINT fk_fp_firing
        FOREIGN KEY (firing_id) REFERENCES firing(id),

    CONSTRAINT fk_fp_prod
        FOREIGN KEY (production_id) REFERENCES production(id)
);

------------------------------------------------------------
-- TABLE: stock
------------------------------------------------------------
CREATE TABLE stock (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    lot_id INTEGER NOT NULL,
    depot_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_lot
        FOREIGN KEY (lot_id)
        REFERENCES lot(id),
    CONSTRAINT fk_stock_depot
        FOREIGN KEY (depot_id)
        REFERENCES depot(id)
);

------------------------------------------------------------
-- TABLE: press
------------------------------------------------------------
CREATE TABLE press (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    bore DECIMAL(4,1) NOT NULL,
    punch DECIMAL(4,1) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT ck_press_bore_positive CHECK (bore > 0),
    CONSTRAINT ck_press_punch_positive CHECK (punch > 0)
);

------------------------------------------------------------
-- TABLE: blank_model
------------------------------------------------------------
CREATE TABLE blank_model (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(100) NOT NULL,

    superior_overmaterial_mm DECIMAL(4,1) NOT NULL,
    inferior_overmaterial_mm DECIMAL(4,1) NOT NULL,

    pressure_kg_cm2 DECIMAL(8,2) NOT NULL,
    grams_per_mm DECIMAL(8,3) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uq_blank_model_code UNIQUE (code),

    CONSTRAINT ck_blank_model_superior_non_negative CHECK (superior_overmaterial_mm >= 0),
    CONSTRAINT ck_blank_model_inferior_non_negative CHECK (inferior_overmaterial_mm >= 0),
    CONSTRAINT ck_blank_model_pressure_positive CHECK (pressure_kg_cm2 > 0),
    CONSTRAINT ck_blank_model_grams_positive CHECK (grams_per_mm > 0)
);


------------------------------------------------------------
-- TABLE: composition_blank_model
------------------------------------------------------------
CREATE TABLE composition_blank_model (
    composition_id INTEGER NOT NULL,
    blank_model_id INTEGER NOT NULL,

    PRIMARY KEY (composition_id, blank_model_id),

    CONSTRAINT fk_cbm_composition
        FOREIGN KEY (composition_id) REFERENCES composition(id),

    CONSTRAINT fk_cbm_blank_model
        FOREIGN KEY (blank_model_id) REFERENCES blank_model(id)
);

------------------------------------------------------------
-- TABLE: blank_model_layer
------------------------------------------------------------
CREATE TABLE blank_model_layer (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    blank_model_id INTEGER NOT NULL,
    layer_number INTEGER NOT NULL,
    disk_percentage DOUBLE NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_blank_layer_blank_model
        FOREIGN KEY (blank_model_id) REFERENCES blank_model(id),

    CONSTRAINT uq_blank_model_layer_order
        UNIQUE (blank_model_id, layer_number),

    CONSTRAINT ck_blank_layer_number_positive CHECK (layer_number > 0),
    CONSTRAINT ck_blank_layer_percentage_range CHECK (disk_percentage > 0 AND disk_percentage <= 100)
);
