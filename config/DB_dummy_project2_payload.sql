------------------------------------------------------------
-- DUMMY DATA: simulazione payload in arrivo dal Progetto 2
-- Scopo: consentire test manuali della creazione Formula Set
------------------------------------------------------------

------------------------------------------------------------
-- 1) Payload contract principali
------------------------------------------------------------
INSERT INTO payload_contract (contract_code, version)
VALUES ('PROJECT2_PAYLOAD', 2);

INSERT INTO payload_contract (contract_code, version)
VALUES ('PROJECT2_PAYLOAD', 1);

------------------------------------------------------------
-- 2) Campi payload standard (input + output)
------------------------------------------------------------
INSERT INTO payload_contract_field (payload_contract_id, field_key, display_name, data_type, unit_code, field_role, order_index)
SELECT id, 'input_1', 'Misura 1', 'DECIMAL', 'mm3', 'INPUT', 0
FROM payload_contract
WHERE contract_code = 'PROJECT2_PAYLOAD' AND version = 2;

INSERT INTO payload_contract_field (payload_contract_id, field_key, display_name, data_type, unit_code, field_role, order_index)
SELECT id, 'input_2', 'Misura 2', 'DECIMAL', 'g/cm3', 'INPUT', 1
FROM payload_contract
WHERE contract_code = 'PROJECT2_PAYLOAD' AND version = 2;

INSERT INTO payload_contract_field (payload_contract_id, field_key, display_name, data_type, unit_code, field_role, order_index)
SELECT id, 'input_3', 'Misura 3', 'DECIMAL', 'g', 'INPUT', 2
FROM payload_contract
WHERE contract_code = 'PROJECT2_PAYLOAD' AND version = 2;

INSERT INTO payload_contract_field (payload_contract_id, field_key, display_name, data_type, unit_code, field_role, order_index)
SELECT id, 'output_ratio', 'Rapporto volume-densità', 'DECIMAL', null, 'OUTPUT', 3
FROM payload_contract
WHERE contract_code = 'PROJECT2_PAYLOAD' AND version = 2;

INSERT INTO payload_contract_field (payload_contract_id, field_key, display_name, data_type, unit_code, field_role, order_index)
SELECT id, 'output_mass_adjusted', 'Massa corretta', 'DECIMAL', 'g', 'OUTPUT', 4
FROM payload_contract
WHERE contract_code = 'PROJECT2_PAYLOAD' AND version = 2;

------------------------------------------------------------
-- 3) Campi payload versione precedente (storico)
------------------------------------------------------------
INSERT INTO payload_contract_field (payload_contract_id, field_key, display_name, data_type, unit_code, field_role, order_index)
SELECT id, 'input_1', 'Misura 1', 'DECIMAL', 'mm3', 'INPUT', 0
FROM payload_contract
WHERE contract_code = 'PROJECT2_PAYLOAD' AND version = 1;

INSERT INTO payload_contract_field (payload_contract_id, field_key, display_name, data_type, unit_code, field_role, order_index)
SELECT id, 'input_2', 'Misura 2', 'DECIMAL', 'g/cm3', 'INPUT', 1
FROM payload_contract
WHERE contract_code = 'PROJECT2_PAYLOAD' AND version = 1;

INSERT INTO payload_contract_field (payload_contract_id, field_key, display_name, data_type, unit_code, field_role, order_index)
SELECT id, 'output_ratio', 'Rapporto volume-densità', 'DECIMAL', null, 'OUTPUT', 2
FROM payload_contract
WHERE contract_code = 'PROJECT2_PAYLOAD' AND version = 1;

------------------------------------------------------------
-- 4) Formula set di esempio
------------------------------------------------------------
INSERT INTO formula_set (code, version)
VALUES ('CUBAGE_CALC_STANDARD', 1);

INSERT INTO formula_set_payload_contract (formula_set_id, payload_contract_id)
SELECT fs.id, pc.id
FROM formula_set fs
JOIN payload_contract pc ON pc.contract_code = 'PROJECT2_PAYLOAD' AND pc.version = 2
WHERE fs.code = 'CUBAGE_CALC_STANDARD' AND fs.version = 1;

------------------------------------------------------------
-- 5) Formule di esempio nel set
------------------------------------------------------------
INSERT INTO formula_set_formula (formula_set_id, formula_key, formula_expression, order_index)
SELECT fs.id, 'output_ratio', 'input_1 / input_2', 0
FROM formula_set fs
WHERE fs.code = 'CUBAGE_CALC_STANDARD' AND fs.version = 1;

INSERT INTO formula_set_formula (formula_set_id, formula_key, formula_expression, order_index)
SELECT fs.id, 'output_mass_adjusted', 'input_3 * 1.05', 1
FROM formula_set fs
WHERE fs.code = 'CUBAGE_CALC_STANDARD' AND fs.version = 1;

------------------------------------------------------------
-- 6) Input usati da ogni formula (tracciamento dipendenze)
------------------------------------------------------------
INSERT INTO formula_set_formula_input (formula_id, field_key)
SELECT f.id, 'input_1'
FROM formula_set_formula f
JOIN formula_set fs ON fs.id = f.formula_set_id
WHERE fs.code = 'CUBAGE_CALC_STANDARD' AND fs.version = 1
  AND f.formula_key = 'output_ratio';

INSERT INTO formula_set_formula_input (formula_id, field_key)
SELECT f.id, 'input_2'
FROM formula_set_formula f
JOIN formula_set fs ON fs.id = f.formula_set_id
WHERE fs.code = 'CUBAGE_CALC_STANDARD' AND fs.version = 1
  AND f.formula_key = 'output_ratio';

INSERT INTO formula_set_formula_input (formula_id, field_key)
SELECT f.id, 'input_3'
FROM formula_set_formula f
JOIN formula_set fs ON fs.id = f.formula_set_id
WHERE fs.code = 'CUBAGE_CALC_STANDARD' AND fs.version = 1
  AND f.formula_key = 'output_mass_adjusted';
