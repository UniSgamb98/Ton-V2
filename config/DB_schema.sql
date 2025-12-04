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
