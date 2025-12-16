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

    produced_qty INTEGER NOT NULL,
    production_date DATE NOT NULL,

    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT fk_prod_item
        FOREIGN KEY (item_id) REFERENCES item(id),

    CONSTRAINT fk_prod_comp
        FOREIGN KEY (composition_id) REFERENCES composition(id)
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