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
-- TABLE: formula_set (set formule cubaggio versionato)
------------------------------------------------------------
CREATE TABLE formula_set (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    code VARCHAR(80) NOT NULL,
    version INTEGER NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uq_formula_set_code_version
        UNIQUE (code, version),

    CONSTRAINT ck_formula_set_version CHECK (version > 0)
);

------------------------------------------------------------
-- TABLE: formula_set_formula (formule multiple per set)
------------------------------------------------------------
CREATE TABLE formula_set_formula (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    formula_set_id INTEGER NOT NULL,
    formula_key VARCHAR(100) NOT NULL,
    formula_expression CLOB NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    CONSTRAINT fk_fsf_set
        FOREIGN KEY (formula_set_id) REFERENCES formula_set(id),

    CONSTRAINT uq_fsf_set_key UNIQUE (formula_set_id, formula_key),
    CONSTRAINT ck_fsf_order_index CHECK (order_index >= 0)
);

------------------------------------------------------------
-- TABLE: payload_contract (contratto payload misure)
------------------------------------------------------------
CREATE TABLE payload_contract (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    contract_code VARCHAR(100) NOT NULL,
    version INTEGER NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uq_payload_contract_code_version
        UNIQUE (contract_code, version),

    CONSTRAINT ck_payload_contract_version CHECK (version > 0)
);

------------------------------------------------------------
-- TABLE: payload_contract_field (campi disponibili nel payload)
------------------------------------------------------------
CREATE TABLE payload_contract_field (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    payload_contract_id INTEGER NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(120),
    data_type VARCHAR(50) NOT NULL,
    unit_code VARCHAR(30),
    order_index INTEGER NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    CONSTRAINT fk_pcf_contract
        FOREIGN KEY (payload_contract_id) REFERENCES payload_contract(id),

    CONSTRAINT uq_pcf_contract_field
        UNIQUE (payload_contract_id, field_key),

    CONSTRAINT ck_pcf_order_index CHECK (order_index >= 0)
);

------------------------------------------------------------
-- TABLE: payload_contract_field_request
-- (campi richiesti dal consumer per un payload contract)
------------------------------------------------------------
CREATE TABLE payload_contract_field_request (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    payload_contract_id INTEGER NOT NULL,
    payload_contract_field_id INTEGER NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    CONSTRAINT fk_pcfr_contract
        FOREIGN KEY (payload_contract_id) REFERENCES payload_contract(id),

    CONSTRAINT fk_pcfr_field
        FOREIGN KEY (payload_contract_field_id) REFERENCES payload_contract_field(id),

    CONSTRAINT uq_pcfr_contract_field
        UNIQUE (payload_contract_id, payload_contract_field_id),

    CONSTRAINT ck_pcfr_order_index CHECK (order_index >= 0)
);

------------------------------------------------------------
-- TABLE: formula_set_payload_contract
-- (ogni set formule usa un solo contratto payload)
------------------------------------------------------------
CREATE TABLE formula_set_payload_contract (
    formula_set_id INTEGER NOT NULL,
    payload_contract_id INTEGER NOT NULL,

    PRIMARY KEY (formula_set_id),

    CONSTRAINT fk_fspc_set
        FOREIGN KEY (formula_set_id) REFERENCES formula_set(id),

    CONSTRAINT fk_fspc_contract
        FOREIGN KEY (payload_contract_id) REFERENCES payload_contract(id)
);

------------------------------------------------------------
-- TABLE: formula_set_formula_input
-- (input di payload usati da ciascuna formula)
------------------------------------------------------------
CREATE TABLE formula_set_formula_input (
    formula_id INTEGER NOT NULL,
    field_key VARCHAR(100) NOT NULL,

    PRIMARY KEY (formula_id, field_key),

    CONSTRAINT fk_fsfi_formula
        FOREIGN KEY (formula_id) REFERENCES formula_set_formula(id)
);

------------------------------------------------------------
-- TABLE: product_active_formula_set
-- (associazione set formule al product, indipendente dalla versione composition)
------------------------------------------------------------
CREATE TABLE product_active_formula_set (
    product_id INTEGER NOT NULL,
    formula_set_id INTEGER NOT NULL,

    PRIMARY KEY (product_id),

    CONSTRAINT fk_pafs_product
        FOREIGN KEY (product_id) REFERENCES product(id),

    CONSTRAINT fk_pafs_set
        FOREIGN KEY (formula_set_id) REFERENCES formula_set(id)
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
    notes VARCHAR(500),

    PRIMARY KEY (id),

    CONSTRAINT uq_firing UNIQUE (firing_date, furnace)
);

-- TABLE: production_order_line_firing
------------------------------------------------------------
CREATE TABLE production_order_line_firing (
    production_order_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    firing_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL,

    PRIMARY KEY (production_order_id, item_id, firing_id),

    CONSTRAINT fk_polf_line
        FOREIGN KEY (production_order_id, item_id)
        REFERENCES production_order_line(production_order_id, item_id),

    CONSTRAINT fk_polf_firing
        FOREIGN KEY (firing_id)
        REFERENCES firing(id),

    CONSTRAINT ck_polf_qty CHECK (quantity > 0)
);

------------------------------------------------------------
-- TABLE: cubage (testata minima)
------------------------------------------------------------
CREATE TABLE cubage (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    firing_id INTEGER NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_cubage_firing
        FOREIGN KEY (firing_id)
        REFERENCES firing(id)
);

------------------------------------------------------------
-- TABLE: cubage_calculation_run
-- (traccia il set formule usato per il cubage)
------------------------------------------------------------
CREATE TABLE cubage_calculation_run (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    cubage_id INTEGER NOT NULL,
    formula_set_id INTEGER NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_ccr_cubage
        FOREIGN KEY (cubage_id)
        REFERENCES cubage(id),

    CONSTRAINT fk_ccr_formula_set
        FOREIGN KEY (formula_set_id)
        REFERENCES formula_set(id),

    CONSTRAINT uq_ccr_cubage UNIQUE (cubage_id)
);

------------------------------------------------------------
-- TABLE: cubage_measurement (misura per singola unità)
------------------------------------------------------------
CREATE TABLE cubage_measurement (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    cubage_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    unit_index INTEGER NOT NULL,

    volume_mm3 DECIMAL(12,3) NOT NULL,
    density_g_cm3 DECIMAL(10,5) NOT NULL,
    mass_g DECIMAL(12,5),
    firing_temperature_snapshot INTEGER,

    PRIMARY KEY (id),

    CONSTRAINT fk_cm_cubage
        FOREIGN KEY (cubage_id)
        REFERENCES cubage(id),

    CONSTRAINT fk_cm_item
        FOREIGN KEY (item_id)
        REFERENCES item(id),

    CONSTRAINT uq_cm_unit
        UNIQUE (cubage_id, item_id, unit_index),

    CONSTRAINT ck_cm_unit_index CHECK (unit_index > 0),
    CONSTRAINT ck_cm_volume CHECK (volume_mm3 > 0),
    CONSTRAINT ck_cm_density CHECK (density_g_cm3 > 0),
    CONSTRAINT ck_cm_mass CHECK (mass_g IS NULL OR mass_g > 0)
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


------------------------------------------------------------
-- TABLE: firing_program
------------------------------------------------------------
CREATE TABLE firing_program (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uq_firing_program_name UNIQUE (name)
);

------------------------------------------------------------
-- TABLE: firing_program_step
------------------------------------------------------------
CREATE TABLE firing_program_step (
    id INTEGER GENERATED ALWAYS AS IDENTITY,
    firing_program_id INTEGER NOT NULL,
    step_order INTEGER NOT NULL,
    target_temperature DOUBLE NOT NULL,
    ramp_time_minutes INTEGER NOT NULL,
    hold_time_minutes INTEGER NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_fps_program
        FOREIGN KEY (firing_program_id)
        REFERENCES firing_program(id),

    CONSTRAINT uq_fps_program_step UNIQUE (firing_program_id, step_order),
    CONSTRAINT ck_fps_step_order CHECK (step_order > 0),
    CONSTRAINT ck_fps_target_temperature CHECK (target_temperature > 0),
    CONSTRAINT ck_fps_ramp_time CHECK (ramp_time_minutes >= 0),
    CONSTRAINT ck_fps_hold_time CHECK (hold_time_minutes >= 0)
);
