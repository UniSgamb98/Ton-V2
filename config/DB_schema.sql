------------------------------------------------------------
-- TABLE: depot
------------------------------------------------------------
CREATE TABLE depot (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(100) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

------------------------------------------------------------
-- TABLE: item
------------------------------------------------------------
CREATE TABLE item (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(100) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

------------------------------------------------------------
-- TABLE: lot
------------------------------------------------------------
CREATE TABLE lot (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    lot_code VARCHAR(100) NOT NULL,
    item_id INTEGER NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lot_item
        FOREIGN KEY (item_id)
        REFERENCES item(id)
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
-- TABLE: powder
------------------------------------------------------------
CREATE TABLE powder (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,

    strength DOUBLE,
    translucency DOUBLE,
    yttria VARCHAR(5),
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
    lot_id INTEGER NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT fk_comp_item FOREIGN KEY (item_id) REFERENCES item(id),
    CONSTRAINT fk_comp_lot  FOREIGN KEY (lot_id) REFERENCES lot(id),

    CONSTRAINT uq_comp_item_lot UNIQUE(item_id, lot_id)
);


------------------------------------------------------------
-- TABLE: composition_layer
------------------------------------------------------------
CREATE TABLE composition_layer (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    composition_id INTEGER NOT NULL,
    layer_number INTEGER NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_layer_composition
        FOREIGN KEY (composition_id) REFERENCES composition(id),

    CONSTRAINT uq_layer_order_per_composition
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