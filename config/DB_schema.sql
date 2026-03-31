------------------------------------------------------------
-- TABLE: product  (Famiglia: es. ZR A2)
------------------------------------------------------------
CREATE TABLE product (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(200),

    PRIMARY KEY (id),
    CONSTRAINT uq_product_code UNIQUE (code)
);

------------------------------------------------------------
-- TABLE: line (linea produzione / famiglia)
------------------------------------------------------------
CREATE TABLE line (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(100) NOT NULL,
    product_id INTEGER NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_line_product
        FOREIGN KEY (product_id) REFERENCES product(id),

    CONSTRAINT uq_line_name_product UNIQUE (name, product_id)
);

------------------------------------------------------------
-- TABLE: blank_model
------------------------------------------------------------
CREATE TABLE blank_model (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,

    diameter_mm DECIMAL(4,1) NOT NULL,

    superior_overmaterial_default_mm DECIMAL(4,1) NOT NULL,
    inferior_overmaterial_default_mm DECIMAL(4,1) NOT NULL,

    pressure_kg_cm2 DECIMAL(8,2) NOT NULL,
    grams_per_mm DECIMAL(8,3) NOT NULL,

    num_layers INTEGER NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uq_blank_model_code_version UNIQUE (code, version),

    CONSTRAINT ck_bm_diameter CHECK (diameter_mm > 0),
    CONSTRAINT ck_bm_layers CHECK (num_layers > 0),
    CONSTRAINT ck_bm_version CHECK (version > 0)
);

------------------------------------------------------------
-- TABLE: blank_model_layer
------------------------------------------------------------
CREATE TABLE blank_model_layer (
    blank_model_id INTEGER NOT NULL,
    layer_number INTEGER NOT NULL,
    disk_percentage DOUBLE NOT NULL,

    PRIMARY KEY (blank_model_id, layer_number),

    CONSTRAINT fk_bml_model
        FOREIGN KEY (blank_model_id) REFERENCES blank_model(id),

    CONSTRAINT ck_bml_layer CHECK (layer_number > 0),
    CONSTRAINT ck_bml_percentage CHECK (disk_percentage > 0 AND disk_percentage <= 100)
);

------------------------------------------------------------
-- TABLE: blank_model_height_overmaterial
------------------------------------------------------------
CREATE TABLE blank_model_height_overmaterial (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    blank_model_id INTEGER NOT NULL,

    min_height_mm DECIMAL(5,2) NOT NULL,
    max_height_mm DECIMAL(5,2) NOT NULL,

    superior_overmaterial_mm DECIMAL(4,1) NOT NULL,
    inferior_overmaterial_mm DECIMAL(4,1) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_bmho_model
        FOREIGN KEY (blank_model_id) REFERENCES blank_model(id),

    CONSTRAINT ck_bmho_range CHECK (max_height_mm > min_height_mm)
);

------------------------------------------------------------
-- TABLE: item (variante per altezza)
------------------------------------------------------------
CREATE TABLE item (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(80) NOT NULL,
    product_id INTEGER NOT NULL,
    blank_model_id INTEGER NOT NULL,

    height_mm DECIMAL(5,2) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_item_product
        FOREIGN KEY (product_id) REFERENCES product(id),

    CONSTRAINT fk_item_model
        FOREIGN KEY (blank_model_id) REFERENCES blank_model(id),

    CONSTRAINT uq_item_code UNIQUE (code),
    CONSTRAINT uq_item UNIQUE (product_id, height_mm),
    CONSTRAINT ck_item_height CHECK (height_mm > 0)
);

------------------------------------------------------------
-- TABLE: powder
------------------------------------------------------------
CREATE TABLE powder (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    view_order INTEGER NOT NULL DEFAULT 0,

    strength DOUBLE,
    translucency DOUBLE,
    yttria INTEGER,
    notes VARCHAR(500),

    PRIMARY KEY (id),
    CONSTRAINT uq_powder_code UNIQUE(code),
    CONSTRAINT ck_powder_view_order CHECK (view_order >= 0)
);

------------------------------------------------------------
-- TABLE: composition (versionata per product)
------------------------------------------------------------
CREATE TABLE composition (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    product_id INTEGER NOT NULL,

    version INTEGER NOT NULL,
    num_layers INTEGER NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT fk_comp_product
        FOREIGN KEY (product_id) REFERENCES product(id),

    CONSTRAINT uq_comp_product_version
        UNIQUE (product_id, version),

    CONSTRAINT uq_comp_product_id
        UNIQUE (product_id, id),

    CONSTRAINT ck_comp_version CHECK (version > 0),
    CONSTRAINT ck_comp_layers CHECK (num_layers > 0)
);

------------------------------------------------------------
-- TABLE: product_active_composition
------------------------------------------------------------
CREATE TABLE product_active_composition (
    product_id INTEGER NOT NULL,
    composition_id INTEGER NOT NULL,

    PRIMARY KEY (product_id),

    CONSTRAINT fk_pac_product
        FOREIGN KEY (product_id) REFERENCES product(id),

    CONSTRAINT fk_pac_composition
        FOREIGN KEY (product_id, composition_id)
        REFERENCES composition(product_id, id),

    CONSTRAINT uq_pac_comp UNIQUE (composition_id)
);

------------------------------------------------------------
-- TABLE: composition_layer_ingredient
------------------------------------------------------------
CREATE TABLE composition_layer_ingredient (
    composition_id INTEGER NOT NULL,
    layer_number INTEGER NOT NULL,
    powder_id INTEGER NOT NULL,

    percentage DOUBLE NOT NULL,

    PRIMARY KEY (composition_id, layer_number, powder_id),

    CONSTRAINT fk_cli_comp
        FOREIGN KEY (composition_id) REFERENCES composition(id),

    CONSTRAINT fk_cli_powder
        FOREIGN KEY (powder_id) REFERENCES powder(id),

    CONSTRAINT ck_cli_layer CHECK (layer_number > 0),
    CONSTRAINT ck_cli_percentage CHECK (percentage > 0 AND percentage <= 100)
);

------------------------------------------------------------
-- TABLE: composition_blank_model
------------------------------------------------------------
CREATE TABLE composition_blank_model (
    composition_id INTEGER NOT NULL,
    blank_model_id INTEGER NOT NULL,

    PRIMARY KEY (composition_id, blank_model_id),

    CONSTRAINT fk_cbm_comp
        FOREIGN KEY (composition_id) REFERENCES composition(id),

    CONSTRAINT fk_cbm_model
        FOREIGN KEY (blank_model_id) REFERENCES blank_model(id)
);

------------------------------------------------------------
-- TABLE: production_order (testata)
------------------------------------------------------------
CREATE TABLE production_order (
    id INTEGER GENERATED ALWAYS AS IDENTITY,

    product_id INTEGER NOT NULL,
    composition_id INTEGER NOT NULL,
    blank_model_id INTEGER NOT NULL,

    production_date DATE NOT NULL,

    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT fk_po_product
        FOREIGN KEY (product_id) REFERENCES product(id),

    CONSTRAINT fk_po_comp
        FOREIGN KEY (composition_id) REFERENCES composition(id),

    CONSTRAINT fk_po_model
        FOREIGN KEY (blank_model_id) REFERENCES blank_model(id),

    CONSTRAINT fk_po_comp_model
        FOREIGN KEY (composition_id, blank_model_id)
        REFERENCES composition_blank_model(composition_id, blank_model_id)
);

------------------------------------------------------------
-- TABLE: production_order_line (righe per altezza)
------------------------------------------------------------
CREATE TABLE production_order_line (
    production_order_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,

    quantity INTEGER NOT NULL,

    PRIMARY KEY (production_order_id, item_id),

    CONSTRAINT fk_pol_order
        FOREIGN KEY (production_order_id)
        REFERENCES production_order(id),

    CONSTRAINT fk_pol_item
        FOREIGN KEY (item_id)
        REFERENCES item(id),

    CONSTRAINT ck_pol_qty CHECK (quantity > 0)
);

------------------------------------------------------------
-- TABLE: furnace
------------------------------------------------------------
CREATE TABLE furnace (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    number VARCHAR(50) NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uq_furnace_number UNIQUE (number)
);

------------------------------------------------------------
-- TABLE: firing (semplificato)
------------------------------------------------------------
CREATE TABLE firing (
    id INTEGER GENERATED ALWAYS AS IDENTITY,

    firing_date DATE NOT NULL,
    furnace VARCHAR(50) NOT NULL,

    max_temperature INTEGER,
    duration_minutes INTEGER,

    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT uq_firing UNIQUE (firing_date, furnace)
);

------------------------------------------------------------
-- TABLE: production_order_firing
------------------------------------------------------------
CREATE TABLE production_order_firing (
    production_order_id INTEGER NOT NULL,
    firing_id INTEGER NOT NULL,

    PRIMARY KEY (production_order_id, firing_id),

    CONSTRAINT fk_pof_order
        FOREIGN KEY (production_order_id)
        REFERENCES production_order(id),

    CONSTRAINT fk_pof_firing
        FOREIGN KEY (firing_id)
        REFERENCES firing(id)
);

------------------------------------------------------------
-- TABLE: document_template
------------------------------------------------------------
CREATE TABLE document_template (
  id INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(120) NOT NULL UNIQUE,
  template_content CLOB NOT NULL,
  sql_query CLOB,
  preset_code VARCHAR(120),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

------------------------------------------------------------
-- TABLE: document_template_usage
------------------------------------------------------------
CREATE TABLE document_template_usage (
    template_id INTEGER NOT NULL PRIMARY KEY,
    last_used_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_dtu_template
        FOREIGN KEY (template_id) REFERENCES document_template(id)
);
